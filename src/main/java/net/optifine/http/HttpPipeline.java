package net.optifine.http;

import net.minecraft.src.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpPipeline {
	public static final String HEADER_USER_AGENT = "User-Agent";
	public static final String HEADER_HOST = "Host";
	public static final String HEADER_ACCEPT = "Accept";
	public static final String HEADER_LOCATION = "Location";
	public static final String HEADER_KEEP_ALIVE = "Keep-Alive";
	public static final String HEADER_CONNECTION = "Connection";
	public static final String HEADER_VALUE_KEEP_ALIVE = "keep-alive";
	public static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
	public static final String HEADER_VALUE_CHUNKED = "chunked";
	private static final Map MAP_CONNECTIONS = new HashMap<>();

	public static void addRequest(String urlStr, HttpListener listener) throws IOException, URISyntaxException {
		addRequest(urlStr, listener, Proxy.NO_PROXY);
	}

	public static void addRequest(String urlStr, HttpListener listener, Proxy proxy) throws IOException, URISyntaxException {
		HttpRequest httprequest = makeRequest(urlStr, proxy);
		HttpPipelineRequest httppipelinerequest = new HttpPipelineRequest(httprequest, listener);
		addRequest(httppipelinerequest);
	}

	public static HttpRequest makeRequest(String urlStr, Proxy proxy) throws IOException, URISyntaxException {
		URL url = new URI(urlStr).toURL();

		if (!url.getProtocol().equals("http")) {
			throw new IOException("Only protocol http is supported: " + url);
		} else {
			String s = url.getFile();
			String s1 = url.getHost();
			int i = url.getPort();

			if (i <= 0) {
				i = 80;
			}

			String s2 = "GET";
			String s3 = "HTTP/1.1";
			Map<String, String> map = new LinkedHashMap<>();
			map.put("User-Agent", "Java/" + System.getProperty("java.version"));
			map.put("Host", s1);
			map.put("Accept", "text/html, image/gif, image/png");
			map.put("Connection", "keep-alive");
			byte[] abyte = new byte[0];
			return new HttpRequest(s1, i, proxy, s2, s, s3, map, abyte);
		}
	}

	public static void addRequest(HttpPipelineRequest pr) {
		HttpRequest httprequest = pr.getHttpRequest();

		for (HttpPipelineConnection httppipelineconnection = getConnection(httprequest.getHost(), httprequest.getPort(), httprequest.getProxy()); !httppipelineconnection.addRequest(pr); httppipelineconnection = getConnection(httprequest.getHost(), httprequest.getPort(), httprequest.getProxy())) {
			removeConnection(httprequest.getHost(), httprequest.getPort(), httprequest.getProxy(), httppipelineconnection);
		}
	}

	private static synchronized HttpPipelineConnection getConnection(String host, int port, Proxy proxy) {
		String s = makeConnectionKey(host, port, proxy);
		HttpPipelineConnection httppipelineconnection = (HttpPipelineConnection) MAP_CONNECTIONS.get(s);

		if (httppipelineconnection == null) {
			httppipelineconnection = new HttpPipelineConnection(host, port, proxy);
			MAP_CONNECTIONS.put(s, httppipelineconnection);
		}

		return httppipelineconnection;
	}

	private static synchronized void removeConnection(String host, int port, Proxy proxy, HttpPipelineConnection hpc) {
		String s = makeConnectionKey(host, port, proxy);
		HttpPipelineConnection httppipelineconnection = (HttpPipelineConnection) MAP_CONNECTIONS.get(s);

		if (httppipelineconnection == hpc) {
			MAP_CONNECTIONS.remove(s);
		}
	}

	private static String makeConnectionKey(String host, int port, Proxy proxy) {
		return host + ":" + port + "-" + proxy;
	}

	public static byte[] get(String urlStr) throws IOException, URISyntaxException {
		return get(urlStr, Proxy.NO_PROXY);
	}

	public static byte[] get(String urlStr, Proxy proxy) throws IOException, URISyntaxException {
		if (urlStr.startsWith("file:")) {
			URL url = new URI(urlStr).toURL();
			InputStream inputstream = url.openStream();
			return Config.readAll(inputstream);
		} else {
			HttpRequest httprequest = makeRequest(urlStr, proxy);
			HttpResponse httpresponse = executeRequest(httprequest);

			if (httpresponse.getStatus() / 100 != 2) {
				throw new IOException("HTTP response: " + httpresponse.getStatus());
			} else {
				return httpresponse.getBody();
			}
		}
	}

	public static HttpResponse executeRequest(HttpRequest req) throws IOException {
		final Map<String, Object> map = new HashMap<>();
		String s = "Response";
		String s1 = "Exception";
		HttpListener httplistener = new HttpListener() {
			public void finished(HttpRequest req, HttpResponse resp) {
				synchronized (map) {
					map.put("Response", resp);
					map.notifyAll();
				}
			}

			public void failed(HttpRequest req, Exception exception) {
				synchronized (map) {
					map.put("Exception", exception);
					map.notifyAll();
				}
			}
		};

		synchronized (map) {
			HttpPipelineRequest httppipelinerequest = new HttpPipelineRequest(req, httplistener);
			addRequest(httppipelinerequest);

			try {
				map.wait();
			} catch (InterruptedException exception) {
				throw new InterruptedIOException("Interrupted");
			}

			Exception exception = (Exception) map.get("Exception");

			if (exception != null) {
				if (exception instanceof IOException ioException) {
					throw ioException;
				} else if (exception instanceof RuntimeException runtimeException) {
					throw runtimeException;
				} else {
					throw new RuntimeException(exception.getMessage(), exception);
				}
			} else {
				HttpResponse httpresponse = (HttpResponse) map.get("Response");

				if (httpresponse == null) {
					throw new IOException("Response is null");
				} else {
					return httpresponse;
				}
			}
		}
	}

	public static boolean hasActiveRequests() {
		for (Object o : MAP_CONNECTIONS.values()) {
			HttpPipelineConnection httppipelineconnection = (HttpPipelineConnection) o;
			if (httppipelineconnection.hasActiveRequests()) {
				return true;
			}
		}

		return false;
	}
}
