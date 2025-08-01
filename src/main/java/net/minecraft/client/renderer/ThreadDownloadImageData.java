package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;
import net.optifine.http.HttpPipeline;
import net.optifine.http.HttpRequest;
import net.optifine.http.HttpResponse;
import net.optifine.player.CapeImageBuffer;
import net.optifine.shaders.ShadersTex;
import net.radiant.NativeImage;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadDownloadImageData extends SimpleTexture {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDownloadImageData.class);
    private static final AtomicInteger THREAD_DOWNLOAD_COUNTER = new AtomicInteger(0);
    private final File cacheFile;
    private final String imageUrl;
    private final IImageBuffer imageBuffer;
    private NativeImage image;
    private Thread imageThread;
    private boolean textureUploaded;
    public Boolean imageFound = null;
    public boolean pipeline = false;

    public ThreadDownloadImageData(File cacheFileIn, String imageUrlIn, ResourceLocation textureResourceLocation, IImageBuffer imageBufferIn) {
        super(textureResourceLocation);
        this.cacheFile = cacheFileIn;
        this.imageUrl = imageUrlIn;
        this.imageBuffer = imageBufferIn;
    }

    private void checkTextureUploaded() {
        if (!this.textureUploaded && this.image != null) {
            this.textureUploaded = true;

            if (this.textureLocation != null) {
                this.deleteGlTexture();
            }

            if (Config.isShaders()) {
                ShadersTex.loadSimpleTexture(super.getGlTextureId(), this.image, false, false, Config.getResourceManager(), this.textureLocation, this.getMultiTexID());
            } else {
                TextureUtil.uploadTextureImage(super.getGlTextureId(), this.image);
            }
        }
    }

    public int getGlTextureId() {
        this.checkTextureUploaded();
        return super.getGlTextureId();
    }

    public void setImage(NativeImage bufferedImageIn) {
        this.image = bufferedImageIn;

        if (this.imageBuffer != null) {
            this.imageBuffer.skinAvailable();
        }

        this.imageFound = this.image != null;
    }

    public void loadTexture(IResourceManager resourceManager) throws IOException {
        if (this.image == null && this.textureLocation != null) {
            super.loadTexture(resourceManager);
        }

        if (this.imageThread == null) {
            if (this.cacheFile != null && this.cacheFile.isFile()) {
                LOGGER.debug("Loading http texture from local cache ({})", new Object[]{this.cacheFile});

                try {
                    this.image = NativeImage.loadFromFile(this.cacheFile);

                    if (this.imageBuffer != null) {
                        this.setImage(this.imageBuffer.parseUserSkin(this.image));
                    }

                    this.loadingFinished();
                } catch (IOException exception) {
                    LOGGER.error("Couldn't load skin {}", this.cacheFile, exception);
                    this.loadTextureFromServer();
                }
            } else {
                this.loadTextureFromServer();
            }
        }
    }

    protected void loadTextureFromServer() {
        this.imageThread = new Thread("Texture Downloader #" + THREAD_DOWNLOAD_COUNTER.incrementAndGet()) {
            public void run() {
                HttpURLConnection httpurlconnection = null;
                ThreadDownloadImageData.LOGGER.debug("Downloading http texture from {} to {}", new Object[]{ThreadDownloadImageData.this.imageUrl, ThreadDownloadImageData.this.cacheFile});

                if (ThreadDownloadImageData.this.shouldPipeline()) {
                    ThreadDownloadImageData.this.loadPipelined();
                } else {
                    try {
                        httpurlconnection = (HttpURLConnection) (new URI(ThreadDownloadImageData.this.imageUrl).toURL()).openConnection(Minecraft.get().getProxy());
                        httpurlconnection.setDoInput(true);
                        httpurlconnection.setDoOutput(false);
                        httpurlconnection.connect();

                        if (httpurlconnection.getResponseCode() / 100 != 2) {
                            if (httpurlconnection.getErrorStream() != null) {
                                Config.readAll(httpurlconnection.getErrorStream());
                            }

                            return;
                        }

                        NativeImage image1;

                        if (ThreadDownloadImageData.this.cacheFile != null) {
                            FileUtils.copyInputStreamToFile(httpurlconnection.getInputStream(), ThreadDownloadImageData.this.cacheFile);
                            image1 = NativeImage.loadFromFile(ThreadDownloadImageData.this.cacheFile);
                        } else {
                            image1 = TextureUtil.readNativeImage(httpurlconnection.getInputStream());
                        }

                        if (ThreadDownloadImageData.this.imageBuffer != null) {
                            image1 = ThreadDownloadImageData.this.imageBuffer.parseUserSkin(image1);
                        }

                        ThreadDownloadImageData.this.setImage(image1);
                    } catch (Exception exception) {
                        ThreadDownloadImageData.LOGGER.error("Couldn't download http texture: {}: {}", exception.getClass().getName(), exception.getMessage());
                    } finally {
                        if (httpurlconnection != null) {
                            httpurlconnection.disconnect();
                        }

                        ThreadDownloadImageData.this.loadingFinished();
                    }
                }
            }
        };
        this.imageThread.setDaemon(true);
        this.imageThread.start();
    }

    private boolean shouldPipeline() {
        if (!this.pipeline) {
            return false;
        } else {
            Proxy proxy = Minecraft.get().getProxy();
            return (proxy.type() == Type.DIRECT || proxy.type() == Type.SOCKS) && this.imageUrl.startsWith("http://");
        }
    }

    private void loadPipelined() {
        try {
            HttpRequest httprequest = HttpPipeline.makeRequest(this.imageUrl, Minecraft.get().getProxy());
            HttpResponse httpresponse = HttpPipeline.executeRequest(httprequest);

            if (httpresponse.getStatus() / 100 != 2) {
                return;
            }

            byte[] abyte = httpresponse.getBody();
            ByteArrayInputStream bytearrayinputstream = new ByteArrayInputStream(abyte);
            NativeImage image1;

            if (this.cacheFile != null) {
                FileUtils.copyInputStreamToFile(bytearrayinputstream, this.cacheFile);
                image1 = NativeImage.loadFromFile(this.cacheFile);
            } else {
                image1 = TextureUtil.readNativeImage(bytearrayinputstream);
            }

            if (this.imageBuffer != null) {
                image1 = this.imageBuffer.parseUserSkin(image1);
            }

            this.setImage(image1);
        } catch (Exception exception) {
            LOGGER.error("Couldn't download http texture: {}: {}", exception.getClass().getName(), exception.getMessage());
        } finally {
            this.loadingFinished();
        }
    }

    private void loadingFinished() {
        this.imageFound = this.image != null;

        if (this.imageBuffer instanceof CapeImageBuffer capeimagebuffer) {
            capeimagebuffer.cleanup();
        }
    }

    public IImageBuffer getImageBuffer() {
        return this.imageBuffer;
    }
}
