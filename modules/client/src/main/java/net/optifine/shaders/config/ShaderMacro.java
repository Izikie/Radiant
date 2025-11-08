package net.optifine.shaders.config;

public record ShaderMacro(String name, String value) {
	public String toString() {
		return "#define " + this.name + " " + this.value;
	}
}
