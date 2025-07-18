package net.optifine;

import net.minecraft.util.ResourceLocation;
import net.optifine.config.ConnectedParser;

import java.util.Properties;

public class CustomPanoramaProperties {
	private final String path;
	private final ResourceLocation[] panoramaLocations;
	private final int weight;
	private final int blur1;
	private final int blur2;
	private final int blur3;
	private final int overlay1Top;
	private final int overlay1Bottom;
	private final int overlay2Top;
	private final int overlay2Bottom;

	public CustomPanoramaProperties(String path, Properties props) {
		ConnectedParser connectedparser = new ConnectedParser("CustomPanorama");
		this.path = path;
		this.panoramaLocations = new ResourceLocation[6];

		for (int i = 0; i < this.panoramaLocations.length; ++i) {
			this.panoramaLocations[i] = new ResourceLocation(path + "/panorama_" + i + ".png");
		}

		this.weight = connectedparser.parseInt(props.getProperty("weight"), 1);
		this.blur1 = connectedparser.parseInt(props.getProperty("blur1"), 64);
		this.blur2 = connectedparser.parseInt(props.getProperty("blur2"), 3);
		this.blur3 = connectedparser.parseInt(props.getProperty("blur3"), 3);
		this.overlay1Top = ConnectedParser.parseColor4(props.getProperty("overlay1.top"), -2130706433);
		this.overlay1Bottom = ConnectedParser.parseColor4(props.getProperty("overlay1.bottom"), 16777215);
		this.overlay2Top = ConnectedParser.parseColor4(props.getProperty("overlay2.top"), 0);
		this.overlay2Bottom = ConnectedParser.parseColor4(props.getProperty("overlay2.bottom"), Integer.MIN_VALUE);
	}

	public ResourceLocation[] getPanoramaLocations() {
		return this.panoramaLocations;
	}

	public int getWeight() {
		return this.weight;
	}

	public int getBlur1() {
		return this.blur1;
	}

	public int getBlur2() {
		return this.blur2;
	}

	public int getBlur3() {
		return this.blur3;
	}

	public int getOverlay1Top() {
		return this.overlay1Top;
	}

	public int getOverlay1Bottom() {
		return this.overlay1Bottom;
	}

	public int getOverlay2Top() {
		return this.overlay2Top;
	}

	public int getOverlay2Bottom() {
		return this.overlay2Bottom;
	}

	public String toString() {
		return this.path + ", weight: " + this.weight + ", blur: " + this.blur1 + " " + this.blur2 + " " + this.blur3 + ", overlay: " + this.overlay1Top + " " + this.overlay1Bottom + " " + this.overlay2Top + " " + this.overlay2Bottom;
	}
}
