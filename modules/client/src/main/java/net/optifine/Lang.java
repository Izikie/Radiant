package net.optifine;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Lang {
	private static final Logger LOGGER = LoggerFactory.getLogger(Lang.class);
	private static final Splitter SPLITTER = Splitter.on('=').limit(2);
	private static final Pattern PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");

	public static void resourcesReloaded() {
		Map<String, String> map = I18n.getLocaleProperties();
		List<String> list = new ArrayList<>();
		String s = "optifine/lang/";
		String s1 = "en_US";
		String s2 = ".lang";
		list.add(s + s1 + s2);

		if (!Config.getGameSettings().language.equals(s1)) {
			list.add(s + Config.getGameSettings().language + s2);
		}

		String[] astring = list.toArray(new String[0]);
		loadResources(Minecraft.get().getDefaultResourcePack(), astring, map);
		IResourcePack[] airesourcepack = Config.getResourcePacks();

		for (IResourcePack iresourcepack : airesourcepack) {
			loadResources(iresourcepack, astring, map);
		}
	}

	private static void loadResources(IResourcePack rp, String[] files, Map<String, String> localeProperties) {
		try {
			for (String s : files) {
				ResourceLocation resourcelocation = new ResourceLocation(s);

				if (rp.resourceExists(resourcelocation)) {
					InputStream inputstream = rp.getInputStream(resourcelocation);

					if (inputstream != null) {
						loadLocaleData(inputstream, localeProperties);
					}
				}
			}
		} catch (IOException exception) {
			LOGGER.error("Couldn't read strings from {}", rp, exception);
		}
	}

	public static void loadLocaleData(InputStream is, Map<String, String> localeProperties) throws IOException {
		Iterator<String> iterator = IOUtils.readLines(is, StandardCharsets.UTF_8).iterator();
		is.close();

		while (iterator.hasNext()) {
			String s = iterator.next();

			if (!s.isEmpty() && s.charAt(0) != 35) {
				String[] astring = Iterables.toArray(SPLITTER.split(s), String.class);

				if (astring.length == 2) {
					String s1 = astring[0];
					String s2 = PATTERN.matcher(astring[1]).replaceAll("%$1s");
					localeProperties.put(s1, s2);
				}
			}
		}
	}

	public static String get(String key) {
		return I18n.format(key);
	}

	public static String get(String key, String def) {
		String s = I18n.format(key);
		return s != null && !s.equals(key) ? s : def;
	}

	public static String getOn() {
		return I18n.format("options.on");
	}

	public static String getOff() {
		return I18n.format("options.off");
	}

	public static String getFast() {
		return I18n.format("options.graphics.fast");
	}

	public static String getFancy() {
		return I18n.format("options.graphics.fancy");
	}

	public static String getDefault() {
		return I18n.format("generator.default");
	}
}
