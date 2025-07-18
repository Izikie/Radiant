package net.optifine.util;

import org.lwjgl.opengl.DisplayMode;

import java.util.Comparator;

public class DisplayModeComparator implements Comparator<Object> {
	public int compare(Object o1, Object o2) {
		DisplayMode displaymode = (DisplayMode) o1;
		DisplayMode displaymode1 = (DisplayMode) o2;
		return displaymode.getWidth() != displaymode1.getWidth() ? displaymode.getWidth() - displaymode1.getWidth() : (displaymode.getHeight() != displaymode1.getHeight() ? displaymode.getHeight() - displaymode1.getHeight() : (displaymode.getBitsPerPixel() != displaymode1.getBitsPerPixel() ? displaymode.getBitsPerPixel() - displaymode1.getBitsPerPixel() : (displaymode.getFrequency() != displaymode1.getFrequency() ? displaymode.getFrequency() - displaymode1.getFrequency() : 0)));
	}
}
