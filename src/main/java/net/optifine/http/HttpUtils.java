package net.optifine.http;

import net.minecraft.client.Minecraft;
import net.optifine.Config;
import net.optifine.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtils {
	public static final String SERVER_URL = "http://s.optifine.net";
	public static final String POST_URL = "http://optifine.net";
	private static String playerItemsUrl = null;

	public static byte[] get(String urlStr) throws IOException {
		HttpURLConnection httpurlconnection = null;
		byte[] abyte1;

		try {
			URL url = new URI(urlStr).toURL();
			httpurlconnection = (HttpURLConnection) url.openConnection(Minecraft.get().getProxy());
			httpurlconnection.setDoInput(true);
			httpurlconnection.setDoOutput(false);
			httpurlconnection.connect();

			if (httpurlconnection.getResponseCode() / 100 != 2) {
				if (httpurlconnection.getErrorStream() != null) {
					Config.readAll(httpurlconnection.getErrorStream());
				}

				throw new IOException("HTTP response: " + httpurlconnection.getResponseCode());
			}

			InputStream inputstream = httpurlconnection.getInputStream();
			byte[] abyte = new byte[httpurlconnection.getContentLength()];
			int i = 0;

			while (true) {
				int j = inputstream.read(abyte, i, abyte.length - i);

				if (j < 0) {
					throw new IOException("Input stream closed: " + urlStr);
				}

				i += j;

				if (i >= abyte.length) {
					break;
				}
			}

			abyte1 = abyte;
		} catch (URISyntaxException exception) {
			throw new RuntimeException(exception);
		} finally {
			if (httpurlconnection != null) {
				httpurlconnection.disconnect();
			}
		}

		return abyte1;
	}

	public static synchronized String getPlayerItemsUrl() {
		if (playerItemsUrl == null) {
			try {
				boolean flag = Config.parseBoolean(System.getProperty("player.models.local"), false);

				if (flag) {
					File file1 = Minecraft.get().mcDataDir;
					File file2 = new File(file1, "playermodels");
					playerItemsUrl = file2.toURI().toURL().toExternalForm();
				}
			} catch (Exception exception) {
				Log.error(exception.getClass().getName() + ": " + exception.getMessage());
			}

			if (playerItemsUrl == null) {
				playerItemsUrl = "http://s.optifine.net";
			}
		}

		return playerItemsUrl;
	}
}
