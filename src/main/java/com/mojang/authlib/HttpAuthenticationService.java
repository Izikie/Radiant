package com.mojang.authlib;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public abstract class HttpAuthenticationService extends BaseAuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAuthenticationService.class);

    private final Proxy proxy;

    protected HttpAuthenticationService(Proxy proxy) {
        this.proxy = Objects.requireNonNull(proxy);
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    protected HttpURLConnection createUrlConnection(URL url) throws IOException {
        Objects.requireNonNull(url);
        LOGGER.debug("Opening connection to {}", url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(this.proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        return connection;
    }

    public String performPostRequest(URL url, String post, String contentType) throws IOException {
        Objects.requireNonNull(url);
        Objects.requireNonNull(post);
        Objects.requireNonNull(contentType);
        HttpURLConnection connection = createUrlConnection(url);
        byte[] postAsBytes = post.getBytes(StandardCharsets.UTF_8);

        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
        connection.setDoOutput(true);

        LOGGER.debug("Writing POST data to {}: {}", url, post);

        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            IOUtils.write(postAsBytes, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        LOGGER.debug("Reading data from {}", url);

        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            LOGGER.debug("Successful read, server response was {}", connection.getResponseCode());
            LOGGER.debug("Response: {}", result);
            return result;
        } catch (IOException e) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();

            if (inputStream != null) {
                LOGGER.debug("Reading error page from {}", url);
                String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                LOGGER.debug("Successful read, server response was {}", connection.getResponseCode());
                LOGGER.debug("Response: {}", result);
                return result;
            }
            LOGGER.debug("Request failed", e);
            throw e;
        } finally {

            IOUtils.closeQuietly(inputStream);
        }
    }


    public String performGetRequest(URL url) throws IOException {
        Objects.requireNonNull(url);
        HttpURLConnection connection = createUrlConnection(url);

        LOGGER.debug("Reading data from {}", url);

        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            LOGGER.debug("Successful read, server response was {}", connection.getResponseCode());
            LOGGER.debug("Response: {}", result);
            return result;
        } catch (IOException e) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();

            if (inputStream != null) {
                LOGGER.debug("Reading error page from {}", url);
                String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                LOGGER.debug("Successful read, server response was {}", connection.getResponseCode());
                LOGGER.debug("Response: {}", result);
                return result;
            }
            LOGGER.debug("Request failed", e);
            throw e;
        } finally {

            IOUtils.closeQuietly(inputStream);
        }
    }


    public static URL constantURL(String url) {
        try {
            return new URI(url).toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            throw new Error("Couldn't create constant for " + url, ex);
        }
    }


    public static String buildQuery(Map<String, Object> query) {
        if (query == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, Object> entry : query.entrySet()) {
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


    public static URL concatenateURL(URL url, String query) {
        try {
            if (url.getQuery() != null && !url.getQuery().isEmpty()) {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&" + query);
            }
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Could not concatenate given URL with GET arguments!", ex);
        }
    }
}
