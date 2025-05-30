package net.minecraft.util;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

public class HttpUtil {
    public static final ListeningExecutorService DOWNLOAD_EXECUTOR =
            MoreExecutors.listeningDecorator(
                    Executors.newCachedThreadPool(
                            new ThreadFactoryBuilder()
                                .setDaemon(true)
                                .setNameFormat("Downloader %d")
                                .build()
                    )
            );
    private static final Logger LOGGER = LogManager.getLogger();

    public static String buildPostString(Map<String, Object> data) {
        StringBuilder builder = new StringBuilder();

        for (Entry<String, Object> entry : data.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append('&');
            }

            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));

            if (entry.getValue() != null) {
                builder.append('=');

                builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
            }
        }

        return builder.toString();
    }

    public static ListenableFuture<Object> downloadResourcePack(final File saveFile, final String packUrl, final Map<String, String> p_180192_2_, final int maxSize, final IProgressUpdate p_180192_4_, final Proxy p_180192_5_) {
        ListenableFuture<?> listenablefuture = DOWNLOAD_EXECUTOR.submit(() -> {
            HttpURLConnection httpurlconnection = null;
            InputStream inputstream = null;
            OutputStream outputstream = null;

            if (p_180192_4_ != null) {
                p_180192_4_.resetProgressAndMessage("Downloading Resource Pack");
                p_180192_4_.displayLoadingString("Making Request...");
            }

            try {
                try {
                    byte[] abyte = new byte[4096];
                    URL url = new URI(packUrl).toURL();
                    httpurlconnection = (HttpURLConnection) url.openConnection(p_180192_5_);
                    float f = 0.0F;
                    float f1 = p_180192_2_.size();

                    for (Entry<String, String> entry : p_180192_2_.entrySet()) {
                        httpurlconnection.setRequestProperty(entry.getKey(), entry.getValue());

                        if (p_180192_4_ != null) {
                            p_180192_4_.setLoadingProgress((int) (++f / f1 * 100.0F));
                        }
                    }

                    inputstream = httpurlconnection.getInputStream();
                    f1 = httpurlconnection.getContentLength();
                    int i = httpurlconnection.getContentLength();

                    if (p_180192_4_ != null) {
                        p_180192_4_.displayLoadingString(String.format("Downloading file (%.2f MB)...", f1 / 1000.0F / 1000.0F));
                    }

                    if (saveFile.exists()) {
                        long j = saveFile.length();

                        if (j == i) {
                            if (p_180192_4_ != null) {
                                p_180192_4_.setDoneWorking();
                            }

                            return;
                        }

                        HttpUtil.LOGGER.warn("Deleting {} as it does not match what we currently have ({} vs our {}).", saveFile, i, j);
                        FileUtils.deleteQuietly(saveFile);
                    } else if (saveFile.getParentFile() != null) {
                        saveFile.getParentFile().mkdirs();
                    }

                    outputstream = new DataOutputStream(new FileOutputStream(saveFile));

                    if (maxSize > 0 && f1 > maxSize) {
                        if (p_180192_4_ != null) {
                            p_180192_4_.setDoneWorking();
                        }

                        throw new IOException("Filesize is bigger than maximum allowed (file is " + f + ", limit is " + maxSize + ")");
                    }

                    int k;

                    while ((k = inputstream.read(abyte)) >= 0) {
                        f += k;

                        if (p_180192_4_ != null) {
                            p_180192_4_.setLoadingProgress((int) (f / f1 * 100.0F));
                        }

                        if (maxSize > 0 && f > maxSize) {
                            if (p_180192_4_ != null) {
                                p_180192_4_.setDoneWorking();
                            }

                            throw new IOException("Filesize was bigger than maximum allowed (got >= " + f + ", limit was " + maxSize + ")");
                        }

                        if (Thread.interrupted()) {
                            HttpUtil.LOGGER.error("INTERRUPTED");

                            if (p_180192_4_ != null) {
                                p_180192_4_.setDoneWorking();
                            }

                            return;
                        }

                        outputstream.write(abyte, 0, k);
                    }

                    if (p_180192_4_ != null) {
                        p_180192_4_.setDoneWorking();
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();

                    if (httpurlconnection != null) {
                        InputStream inputstream1 = httpurlconnection.getErrorStream();

                        try {
                            HttpUtil.LOGGER.error(IOUtils.toString(inputstream1, StandardCharsets.UTF_8));
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }

                    if (p_180192_4_ != null) {
                        p_180192_4_.setDoneWorking();
                    }
                }
            } finally {
                IOUtils.closeQuietly(inputstream);
                IOUtils.closeQuietly(outputstream);
            }
        });
        return (ListenableFuture<Object>) listenablefuture;
    }

    public static int getSuitableLanPort() throws IOException {
        ServerSocket serversocket = null;
        int i;

        try {
            serversocket = new ServerSocket(0);
            i = serversocket.getLocalPort();
        } finally {
            try {
                if (serversocket != null) {
                    serversocket.close();
                }
            } catch (IOException exception) {
            }
        }

        return i;
    }

    public static String get(URL url) throws IOException {
        HttpURLConnection httpurlconnection = (HttpURLConnection) url.openConnection();
        httpurlconnection.setRequestMethod("GET");
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(httpurlconnection.getInputStream()));
        StringBuilder stringbuilder = new StringBuilder();
        String s;

        while ((s = bufferedreader.readLine()) != null) {
            stringbuilder.append(s);
            stringbuilder.append('\r');
        }

        bufferedreader.close();
        return stringbuilder.toString();
    }
}
