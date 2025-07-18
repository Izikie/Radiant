package net.optifine.shaders.config;

import net.minecraft.src.Config;
import net.optifine.shaders.Shaders;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MacroProcessor {
	public static InputStream process(InputStream in, String path) throws IOException {
		String s = Config.readInputStream(in, "ASCII");
		String s1 = getMacroHeader(s);

		if (!s1.isEmpty()) {
			s = s1 + s;

			if (Shaders.SAVE_FINAL_SHADERS) {
				String s2 = path.replace(':', '/') + ".pre";
				Shaders.saveShader(s2, s);
			}

			s = process(s);
		}

		if (Shaders.SAVE_FINAL_SHADERS) {
			String s3 = path.replace(':', '/');
			Shaders.saveShader(s3, s);
		}

		byte[] abyte = s.getBytes(StandardCharsets.US_ASCII);
		return new ByteArrayInputStream(abyte);
	}

	public static String process(String strIn) throws IOException {
		StringReader stringreader = new StringReader(strIn);
		BufferedReader bufferedreader = new BufferedReader(stringreader);
		MacroState macrostate = new MacroState();
		StringBuilder stringbuilder = new StringBuilder();

		while (true) {
			String s = bufferedreader.readLine();

			if (s == null) {
				s = stringbuilder.toString();
				return s;
			}

			if (macrostate.processLine(s) && !MacroState.isMacroLine(s)) {
				stringbuilder.append(s);
				stringbuilder.append("\n");
			}
		}
	}

	private static String getMacroHeader(String str) throws IOException {
		StringBuilder stringbuilder = new StringBuilder();
		List<ShaderOption> list = null;
		List<ShaderMacro> list1 = null;
		StringReader stringreader = new StringReader(str);
		BufferedReader bufferedreader = new BufferedReader(stringreader);

		while (true) {
			String s = bufferedreader.readLine();

			if (s == null) {
				return stringbuilder.toString();
			}

			if (MacroState.isMacroLine(s)) {
				if (stringbuilder.isEmpty()) {
					stringbuilder.append(ShaderMacros.getFixedMacroLines());
				}

				if (list1 == null) {
					list1 = new ArrayList(Arrays.asList(ShaderMacros.getExtensions()));
				}

				Iterator iterator = list1.iterator();

				while (iterator.hasNext()) {
					ShaderMacro shadermacro = (ShaderMacro) iterator.next();
					if (s.contains(shadermacro.name())) {
						stringbuilder.append(shadermacro);
						stringbuilder.append("\n");
						iterator.remove();
					}
				}
			}
		}
	}

	private static List<ShaderOption> getMacroOptions() {
		List<ShaderOption> list = new ArrayList<>();
		ShaderOption[] ashaderoption = Shaders.getShaderPackOptions();

		for (ShaderOption shaderoption : ashaderoption) {
			String s = shaderoption.getSourceLine();

			if (s != null && s.startsWith("#")) {
				list.add(shaderoption);
			}
		}

		return list;
	}
}
