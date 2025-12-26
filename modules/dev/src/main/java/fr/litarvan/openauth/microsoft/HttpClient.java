package fr.litarvan.openauth.microsoft;

import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpClient {
    public static final String MIME_TYPE_JSON = "application/json";
    public static final String MIME_TYPE_URLENCODED_FORM = "application/x-www-form-urlencoded";

    private final Gson gson;
    private final Proxy proxy;

    public HttpClient() {
        this(Proxy.NO_PROXY);
    }

    public HttpClient(Proxy proxy) {
        this.gson = new Gson();
        this.proxy = proxy;
    }


    public String getText(String url, Map<String, String> params) throws MicrosoftAuthenticationException {
        return readResponse(createConnection(url + '?' + buildParams(params)));
    }

    public <T> T getJson(String url, String token, Class<T> responseClass) throws MicrosoftAuthenticationException {
        HttpURLConnection connection = createConnection(url);
        connection.addRequestProperty("Authorization", "Bearer " + token);
        connection.addRequestProperty("Accept", MIME_TYPE_JSON);

        return readJson(connection, responseClass);
    }

    public HttpURLConnection postForm(String url, Map<String, String> params) throws MicrosoftAuthenticationException {
        return post(url, MIME_TYPE_URLENCODED_FORM, "*/*", buildParams(params));
    }

    public <T> T postJson(String url, Object request, Class<T> responseClass) throws MicrosoftAuthenticationException {
        HttpURLConnection connection = post(url, MIME_TYPE_JSON, MIME_TYPE_JSON, gson.toJson(request));
        return readJson(connection, responseClass);
    }

    public <T> T postFormGetJson(String url, Map<String, String> params, Class<T> responseClass) throws MicrosoftAuthenticationException {
        return readJson(postForm(url, params), responseClass);
    }


    protected HttpURLConnection post(String url, String contentType, String accept, String data) throws MicrosoftAuthenticationException {
        HttpURLConnection connection = createConnection(url);
        connection.setDoOutput(true);
        connection.addRequestProperty("Content-Type", contentType);
        connection.addRequestProperty("Accept", accept);

        try {
            connection.setRequestMethod("POST");
            connection.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MicrosoftAuthenticationException(e);
        }

        return connection;
    }

    protected <T> T readJson(HttpURLConnection connection, Class<T> responseType) throws MicrosoftAuthenticationException {
        String raw = readResponse(connection);

        try {
            return gson.fromJson(raw, responseType);
        } catch (Exception e) {
            throw new MicrosoftAuthenticationException("Failed to parse JSON: " + raw);
        }
    }

    protected String readResponse(HttpURLConnection connection) throws MicrosoftAuthenticationException {
        String redirection = connection.getHeaderField("Location");
        if (redirection != null) {
            return readResponse(createConnection(redirection));
        }

        int code = -1;
        String msg = null;
        try {
            code = connection.getResponseCode();
            msg = connection.getResponseMessage();
        } catch (IOException e) {
            // response code may still be read from the exception path below
        }

        InputStream inputStream;
        try {
            if (code >= 200 && code < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }
        } catch (IOException e) {
            inputStream = connection.getErrorStream();
        }

        if (inputStream == null) {
            System.out.println("Body: <empty>");
            if (code < 200 || code >= 300) {
                throw new MicrosoftAuthenticationException("HTTP error " + code + ": <no body>");
            }
            return "";
        }

        StringBuilder response = new StringBuilder();

        // check if the url corresponds to a related authentication url
        if (this.checkUrl(connection.getURL())) {
            // then patch the input stream like in the old MicrosoftPatchedHttpURLConnection class.
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int n;
                byte[] data = new byte[8192];

                while ((n = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, n);
                }

                byte[] patched = buffer
                        .toString(StandardCharsets.UTF_8)
                        .replaceAll("integrity ?=", "integrity.disabled=")
                        .replaceAll("setAttribute\\(\"integrity\"", "setAttribute(\"integrity.disabled\"")
                        .getBytes(StandardCharsets.UTF_8);

                inputStream = new ByteArrayInputStream(patched);
            } catch (IOException e) {
                throw new MicrosoftAuthenticationException(e);
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new MicrosoftAuthenticationException(e);
        }

        if (code < 200 || code >= 300) {
            throw new MicrosoftAuthenticationException("HTTP error " + code + ": " + response);
        }

        return response.toString();
    }

    private boolean checkUrl(URL url) {
        return (("login.microsoftonline.com".equals(url.getHost()) && url.getPath().endsWith("/oauth2/authorize"))
                || ("login.live.com".equals(url.getHost()) && "/oauth20_authorize.srf".equals(url.getPath()))
                || ("login.live.com".equals(url.getHost()) && "/ppsecure/post.srf".equals(url.getPath()))
                || ("login.microsoftonline.com".equals(url.getHost()) && "/login.srf".equals(url.getPath()))
                || ("login.microsoftonline.com".equals(url.getHost()) && url.getPath().endsWith("/login"))
                || ("login.microsoftonline.com".equals(url.getHost()) && url.getPath().endsWith("/SAS/ProcessAuth"))
                || ("login.microsoftonline.com".equals(url.getHost()) && url.getPath().endsWith("/federation/oauth2"))
                || ("login.microsoftonline.com".equals(url.getHost()) && url.getPath().endsWith("/oauth2/v2.0/authorize")));
    }

    protected HttpURLConnection followRedirects(HttpURLConnection connection) throws MicrosoftAuthenticationException {
        String redirection = connection.getHeaderField("Location");
        if (redirection != null) {
            connection = followRedirects(createConnection(redirection));
        }

        return connection;
    }

    protected String buildParams(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        params.forEach((key, value) -> {
            if (query.length() > 0) {
                query.append('&');
            }

            query.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });

        return query.toString();
    }

    protected HttpURLConnection createConnection(String url) throws MicrosoftAuthenticationException {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection(proxy);
        } catch (IOException e) {
            throw new MicrosoftAuthenticationException(e);
        }

        String userAgent = "Mozilla/5.0 (XboxReplay; XboxLiveAuth/3.0) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/71.0.3578.98 " +
                "Safari/537.36";

        connection.setConnectTimeout(30 * 1000); // 30s
        connection.setReadTimeout(60 * 1000); // 60s
        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setRequestProperty("User-Agent", userAgent);

        return connection;
    }

    // Simple masking to avoid printing full tokens in logs
    private String maskTokensInString(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        // mask common JWT and token patterns conservatively:
        return s.replaceAll("([A-Za-z0-9_-]{6})[A-Za-z0-9._-]{6,}(?:[A-Za-z0-9_-]{4})", "$1****$0");
    }
}
