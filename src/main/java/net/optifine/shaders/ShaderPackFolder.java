package net.optifine.shaders;

import net.optifine.util.StrUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ShaderPackFolder implements IShaderPack {
	protected final File packFile;

	public ShaderPackFolder(String name, File file) {
		this.packFile = file;
	}

	public void close() {
	}

	public InputStream getResourceAsStream(String resName) {
		try {
			String s = StrUtils.removePrefixSuffix(resName, "/", "/");
			File file1 = new File(this.packFile, s);
			return !file1.exists() ? null : new BufferedInputStream(new FileInputStream(file1));
		} catch (Exception exception) {
			return null;
		}
	}

	public boolean hasDirectory(String name) {
		File file1 = new File(this.packFile, name.substring(1));
		return file1.exists() && file1.isDirectory();
	}

	public String getName() {
		return this.packFile.getName();
	}
}
