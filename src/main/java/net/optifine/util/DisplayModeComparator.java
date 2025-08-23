package net.optifine.util;

import net.radiant.lwjgl.opengl.DisplayMode;

import java.util.Comparator;

public class DisplayModeComparator implements Comparator<DisplayMode> {
	public int compare(DisplayMode o1, DisplayMode o2) {
        return o1.getWidth() != o2.getWidth() ? o1.getWidth() - o2.getWidth() : (o1.getHeight() != o2.getHeight() ? o1.getHeight() - o2.getHeight() : (o1.getBitsPerPixel() != o2.getBitsPerPixel() ? o1.getBitsPerPixel() - o2.getBitsPerPixel() : (o1.getFrequency() != o2.getFrequency() ? o1.getFrequency() - o2.getFrequency() : 0)));
	}
}
