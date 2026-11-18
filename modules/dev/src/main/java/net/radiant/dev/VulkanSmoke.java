package net.radiant.dev;

import net.radiant.render.vulkan.VulkanPresenter;
import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

public final class VulkanSmoke {
    private VulkanSmoke() {
    }

    public static void main(String[] args) {
        int targetFrames = Math.clamp(Integer.getInteger("radiant.vulkan.smokeFrames", 120), 30, 600);
        boolean validation = Boolean.parseBoolean(System.getProperty("radiant.vulkan.validation", "true"));
        try (GLFWErrorCallback errors = GLFWErrorCallback.createPrint(System.err)) {
            errors.set();
            try {
                if (!glfwInit() || !glfwVulkanSupported()) {
                    throw new IllegalStateException("Could not initialize GLFW Vulkan support");
                }
                run(targetFrames, validation);
            } finally {
                glfwTerminate();
                glfwSetErrorCallback(null);
            }
        }
    }

    private static void run(int targetFrames, boolean validation) {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        long window = glfwCreateWindow(640, 360, "Radiant Vulkan Bring-up", 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Could not create the Vulkan window");
        }
        try {
            VulkanPresenter presenter = new VulkanPresenter(window);
            int frames = 0;
            try (presenter) {
                presenter.initialize(validation);
                long deadline = System.nanoTime() + 30_000_000_000L;
                while (!glfwWindowShouldClose(window) && frames < targetFrames && System.nanoTime() < deadline) {
                    glfwPollEvents();
                    float progress = (float) frames / targetFrames;
                    if (presenter.present(0.08f, 0.10f + 0.45f * progress, 0.38f)) {
                        frames++;
                        if (frames == targetFrames / 3) {
                            glfwIconifyWindow(window);
                            glfwPollEvents();
                            if (glfwGetWindowAttrib(window, GLFW_ICONIFIED) == GLFW_TRUE
                                    && presenter.present(0, 0, 0)) {
                                throw new IllegalStateException("Vulkan presented while minimized");
                            }
                            glfwRestoreWindow(window);
                        }
                        if (frames == targetFrames / 2) {
                            glfwSetWindowSize(window, 800, 450);
                        }
                    } else {
                        glfwWaitEventsTimeout(0.01);
                    }
                }
            }
            if (frames != targetFrames || presenter.validationErrors() != 0) {
                throw new IllegalStateException("Vulkan smoke test presented " + frames + "/" + targetFrames
                        + " frames with " + presenter.validationErrors() + " validation errors");
            }
            System.out.printf("Vulkan smoke test presented %d frames with %d validation errors (validation %s)%n",
                    frames, presenter.validationErrors(), validation ? "enabled" : "disabled");
        } finally {
            glfwDestroyWindow(window);
        }
    }
}
