package net.radiant.render.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.glfw.GLFW.GLFW_ICONIFIED;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetWindowAttrib;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.EXTValidationFeatures.*;
import static org.lwjgl.vulkan.VK10.*;

/** Owns the surface, graphics queue and swapchain on the presentation thread. */
public final class VulkanPresenter implements AutoCloseable {
    private final Thread owner = Thread.currentThread();
    private final long window;
    private final AtomicInteger validationErrors = new AtomicInteger();
    private VkInstance instance;
    private VkDebugUtilsMessengerCallbackEXT debugCallback;
    private long debugMessenger;
    private long surface;
    private VkPhysicalDevice physicalDevice;
    private int queueFamily;
    private VkDevice device;
    private VkQueue queue;
    private long commandPool;
    private VkCommandBuffer commands;
    private long imageAvailable;
    private long frameFence;
    private long swapchain;
    private long[] images;
    private long[] renderFinished;
    private int framebufferWidth;
    private int framebufferHeight;

    public VulkanPresenter(long window) {
        this.window = window;
    }

    public void initialize(boolean validation) {
        requireOwner();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
            if (requiredExtensions == null) {
                throw new IllegalStateException("GLFW could not find Vulkan surface extensions");
            }
            PointerBuffer extensions = stack.mallocPointer(requiredExtensions.remaining() + (validation ? 2 : 0));
            extensions.put(requiredExtensions);
            if (validation) {
                extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
                extensions.put(stack.UTF8(VK_EXT_VALIDATION_FEATURES_EXTENSION_NAME));
            }
            extensions.flip();
            VkApplicationInfo app = VkApplicationInfo.calloc(stack).sType$Default()
                    .pApplicationName(stack.UTF8("Radiant")).apiVersion(VK11.VK_API_VERSION_1_1);
            VkInstanceCreateInfo info = VkInstanceCreateInfo.calloc(stack).sType$Default()
                    .pApplicationInfo(app).ppEnabledExtensionNames(extensions);
            VkDebugUtilsMessengerCreateInfoEXT debug = null;
            if (validation) {
                debugCallback = VkDebugUtilsMessengerCallbackEXT.create((severity, type, data, user) -> {
                    if ((severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                        validationErrors.incrementAndGet();
                    }
                    System.err.println(VkDebugUtilsMessengerCallbackDataEXT.create(data).pMessageString());
                    return VK_FALSE;
                });
                debug = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack).sType$Default()
                        .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                        .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                                | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                        .pfnUserCallback(debugCallback);
                VkValidationFeaturesEXT features = VkValidationFeaturesEXT.calloc(stack).sType$Default()
                        .pEnabledValidationFeatures(stack.ints(VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT))
                        .pNext(debug.address());
                info.ppEnabledLayerNames(stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation"))).pNext(features.address());
            }
            PointerBuffer instancePointer = stack.mallocPointer(1);
            check(vkCreateInstance(info, null, instancePointer), "create Vulkan instance");
            instance = new VkInstance(instancePointer.get(0), info);

            LongBuffer handle = stack.mallocLong(1);
            if (debug != null) {
                check(vkCreateDebugUtilsMessengerEXT(instance, debug, null, handle), "create Vulkan debug messenger");
                debugMessenger = handle.get(0);
            }
            check(glfwCreateWindowSurface(instance, window, null, handle), "create window surface");
            surface = handle.get(0);
            selectDevice(stack);
            createDevice(stack);
            createFrameResources(stack);
        }
    }

    private void selectDevice(MemoryStack stack) {
        IntBuffer count = stack.mallocInt(1);
        check(vkEnumeratePhysicalDevices(instance, count, null), "count physical devices");
        if (count.get(0) == 0) {
            throw new IllegalStateException("No Vulkan physical devices found");
        }
        PointerBuffer devices = stack.mallocPointer(count.get(0));
        check(vkEnumeratePhysicalDevices(instance, count, devices), "enumerate physical devices");
        int bestScore = -1;
        for (int i = 0; i < count.get(0); i++) {
            try (MemoryStack local = MemoryStack.stackPush()) {
                VkPhysicalDevice candidate = new VkPhysicalDevice(devices.get(i), instance);
                int family = findPresentQueue(candidate, local);
                if (family == -1) {
                    continue;
                }
                VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(local);
                vkGetPhysicalDeviceProperties(candidate, properties);
                int score = properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU ? 2 : 1;
                if (score > bestScore) {
                    bestScore = score;
                    physicalDevice = candidate;
                    queueFamily = family;
                }
            }
        }
        if (physicalDevice == null) {
            throw new IllegalStateException("No Vulkan device with swapchain support and a graphics/present queue");
        }
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(physicalDevice, properties);
        System.out.println("Vulkan device: " + properties.deviceNameString());
    }

    private int findPresentQueue(VkPhysicalDevice candidate, MemoryStack stack) {
        IntBuffer count = stack.mallocInt(1);
        check(vkEnumerateDeviceExtensionProperties(candidate, (ByteBuffer) null, count, null), "count device extensions");
        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(count.get(0), stack);
        check(vkEnumerateDeviceExtensionProperties(candidate, (ByteBuffer) null, count, extensions), "enumerate device extensions");
        if (extensions.stream().noneMatch(extension -> VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(extension.extensionNameString()))) {
            return -1;
        }
        vkGetPhysicalDeviceQueueFamilyProperties(candidate, count, null);
        VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.malloc(count.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(candidate, count, families);
        IntBuffer supported = stack.mallocInt(1);
        for (int i = 0; i < count.get(0); i++) {
            if ((families.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                check(vkGetPhysicalDeviceSurfaceSupportKHR(candidate, i, surface, supported), "query surface support");
                if (supported.get(0) != VK_FALSE) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void createDevice(MemoryStack stack) {
        VkDeviceQueueCreateInfo.Buffer queues = VkDeviceQueueCreateInfo.calloc(1, stack);
        queues.get(0).sType$Default().queueFamilyIndex(queueFamily).pQueuePriorities(stack.floats(1.0f));
        VkDeviceCreateInfo info = VkDeviceCreateInfo.calloc(stack).sType$Default()
                .pQueueCreateInfos(queues)
                .ppEnabledExtensionNames(stack.pointers(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME)));
        PointerBuffer pointer = stack.mallocPointer(1);
        check(vkCreateDevice(physicalDevice, info, null, pointer), "create Vulkan device");
        device = new VkDevice(pointer.get(0), physicalDevice, info);
        vkGetDeviceQueue(device, queueFamily, 0, pointer);
        queue = new VkQueue(pointer.get(0), device);
    }

    private void createFrameResources(MemoryStack stack) {
        LongBuffer handle = stack.mallocLong(1);
        VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack).sType$Default().queueFamilyIndex(queueFamily);
        check(vkCreateCommandPool(device, poolInfo, null, handle), "create command pool");
        commandPool = handle.get(0);
        VkCommandBufferAllocateInfo commandInfo = VkCommandBufferAllocateInfo.calloc(stack).sType$Default()
                .commandPool(commandPool).level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(1);
        PointerBuffer pointer = stack.mallocPointer(1);
        check(vkAllocateCommandBuffers(device, commandInfo, pointer), "allocate command buffer");
        commands = new VkCommandBuffer(pointer.get(0), device);
        VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack).sType$Default().flags(VK_FENCE_CREATE_SIGNALED_BIT);
        check(vkCreateFence(device, fenceInfo, null, handle), "create frame fence");
        frameFence = handle.get(0);
        imageAvailable = createSemaphore(stack, handle);
    }

    private long createSemaphore(MemoryStack stack, LongBuffer handle) {
        VkSemaphoreCreateInfo info = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
        check(vkCreateSemaphore(device, info, null, handle), "create semaphore");
        return handle.get(0);
    }

    private void createSwapchain(MemoryStack stack, int width, int height) {
        VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.malloc(stack);
        check(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, caps), "query surface capabilities");
        if ((caps.supportedUsageFlags() & VK_IMAGE_USAGE_TRANSFER_DST_BIT) == 0) {
            throw new IllegalStateException("Vulkan surface does not support transfer writes");
        }
        IntBuffer count = stack.mallocInt(1);
        check(vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, null), "count surface formats");
        if (count.get(0) == 0) {
            throw new IllegalStateException("Vulkan surface has no supported formats");
        }
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
        check(vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, formats), "query surface formats");
        VkSurfaceFormatKHR format = formats.get(0);
        for (int i = 0; i < count.get(0); i++) {
            VkSurfaceFormatKHR candidate = formats.get(i);
            if (candidate.format() == VK_FORMAT_B8G8R8A8_SRGB && candidate.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                format = candidate;
                break;
            }
        }
        int imageCount = caps.minImageCount() + 1;
        if (caps.maxImageCount() != 0) {
            imageCount = Math.min(imageCount, caps.maxImageCount());
        }
        VkSwapchainCreateInfoKHR info = VkSwapchainCreateInfoKHR.calloc(stack).sType$Default()
                .surface(surface).minImageCount(imageCount)
                .imageFormat(format.format() == VK_FORMAT_UNDEFINED ? VK_FORMAT_B8G8R8A8_SRGB : format.format())
                .imageColorSpace(format.colorSpace()).imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT).imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .preTransform(caps.currentTransform()).compositeAlpha(Integer.lowestOneBit(caps.supportedCompositeAlpha()))
                .presentMode(VK_PRESENT_MODE_FIFO_KHR).clipped(true);
        if (caps.currentExtent().width() == -1) {
            info.imageExtent().set(
                    Math.clamp(width, caps.minImageExtent().width(), caps.maxImageExtent().width()),
                    Math.clamp(height, caps.minImageExtent().height(), caps.maxImageExtent().height()));
        } else {
            info.imageExtent().set(caps.currentExtent());
        }
        LongBuffer handle = stack.mallocLong(1);
        check(vkCreateSwapchainKHR(device, info, null, handle), "create swapchain");
        swapchain = handle.get(0);
        check(vkGetSwapchainImagesKHR(device, swapchain, count, null), "count swapchain images");
        LongBuffer swapchainImages = stack.mallocLong(count.get(0));
        check(vkGetSwapchainImagesKHR(device, swapchain, count, swapchainImages), "query swapchain images");
        images = new long[count.get(0)];
        renderFinished = new long[images.length];
        for (int i = 0; i < images.length; i++) {
            images[i] = swapchainImages.get(i);
            // The presentation engine finishes waiting before it gives us this image again.
            renderFinished[i] = createSemaphore(stack, handle);
        }
        framebufferWidth = width;
        framebufferHeight = height;
    }

    public boolean present(float red, float green, float blue) {
        requireOwner();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetFramebufferSize(window, width, height);
            if (width.get(0) == 0 || height.get(0) == 0 || glfwGetWindowAttrib(window, GLFW_ICONIFIED) == GLFW_TRUE) {
                return false;
            }
            if (swapchain == 0 || framebufferWidth != width.get(0) || framebufferHeight != height.get(0)) {
                destroySwapchain();
                createSwapchain(stack, width.get(0), height.get(0));
            }

            check(vkWaitForFences(device, frameFence, true, -1L), "wait for frame fence");
            IntBuffer imageIndex = stack.mallocInt(1);
            int acquire = vkAcquireNextImageKHR(device, swapchain, -1L, imageAvailable, 0, imageIndex);
            if (acquire == VK_ERROR_OUT_OF_DATE_KHR) {
                destroySwapchain();
                return false;
            }
            if (acquire != VK_SUBOPTIMAL_KHR) {
                check(acquire, "acquire swapchain image");
            }
            int index = imageIndex.get(0);
            check(vkResetCommandPool(device, commandPool, 0), "reset command pool");
            recordClear(stack, images[index], red, green, blue);
            VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default()
                    .waitSemaphoreCount(1).pWaitSemaphores(stack.longs(imageAvailable))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_TRANSFER_BIT))
                    .pCommandBuffers(stack.pointers(commands)).pSignalSemaphores(stack.longs(renderFinished[index]));
            check(vkResetFences(device, frameFence), "reset frame fence");
            check(vkQueueSubmit(queue, submit, frameFence), "submit frame");
            VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack).sType$Default()
                    .pWaitSemaphores(stack.longs(renderFinished[index])).swapchainCount(1)
                    .pSwapchains(stack.longs(swapchain)).pImageIndices(imageIndex);
            int result = vkQueuePresentKHR(queue, present);
            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || acquire == VK_SUBOPTIMAL_KHR) {
                if (result != VK_ERROR_OUT_OF_DATE_KHR && result != VK_SUBOPTIMAL_KHR) {
                    check(result, "present frame");
                }
                destroySwapchain();
                return result != VK_ERROR_OUT_OF_DATE_KHR;
            }
            check(result, "present frame");
            return true;
        }
    }

    private void recordClear(MemoryStack stack, long image, float red, float green, float blue) {
        VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack).sType$Default()
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        check(vkBeginCommandBuffer(commands, begin), "begin frame commands");
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        VkImageMemoryBarrier transition = barrier.get(0).sType$Default().image(image)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
        transition.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
        vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barrier);
        VkClearColorValue color = VkClearColorValue.calloc(stack).float32(0, red).float32(1, green).float32(2, blue).float32(3, 1);
        VkImageSubresourceRange.Buffer range = VkImageSubresourceRange.calloc(1, stack);
        range.get(0).set(transition.subresourceRange());
        vkCmdClearColorImage(commands, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, color, range);
        transition.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(0);
        vkCmdPipelineBarrier(commands, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0, null, null, barrier);
        check(vkEndCommandBuffer(commands), "end frame commands");
    }

    public int validationErrors() {
        return validationErrors.get();
    }

    private void destroySwapchain() {
        if (swapchain != 0) {
            vkDeviceWaitIdle(device);
            if (renderFinished != null) {
                for (long semaphore : renderFinished) {
                    if (semaphore != 0) {
                        vkDestroySemaphore(device, semaphore, null);
                    }
                }
                renderFinished = null;
            }
            vkDestroySwapchainKHR(device, swapchain, null);
            swapchain = 0;
        }
    }

    @Override
    public void close() {
        requireOwner();
        destroySwapchain();
        if (device != null) {
            vkDeviceWaitIdle(device);
            if (imageAvailable != 0) vkDestroySemaphore(device, imageAvailable, null);
            if (frameFence != 0) vkDestroyFence(device, frameFence, null);
            if (commandPool != 0) vkDestroyCommandPool(device, commandPool, null);
            vkDestroyDevice(device, null);
            device = null;
        }
        if (instance != null) {
            if (surface != 0) vkDestroySurfaceKHR(instance, surface, null);
            if (debugMessenger != 0) vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
            vkDestroyInstance(instance, null);
            instance = null;
        }
        if (debugCallback != null) {
            debugCallback.free();
            debugCallback = null;
        }
    }

    private void requireOwner() {
        if (owner != Thread.currentThread()) {
            throw new IllegalStateException("Vulkan presenter accessed from outside its owning thread");
        }
    }

    private static void check(int result, String operation) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException("Could not " + operation + " (VkResult " + result + ")");
        }
    }
}
