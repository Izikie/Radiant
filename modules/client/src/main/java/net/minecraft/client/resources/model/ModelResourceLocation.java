package net.minecraft.client.resources.model;

import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

public class ModelResourceLocation extends ResourceLocation {
    private final String variant;

    protected ModelResourceLocation(String domain, String path, String variant) {
        super(domain, path);
        this.variant = StringUtils.isEmpty(variant) ? "normal" : variant.toLowerCase();
    }

    protected ModelResourceLocation(String... components) {
        this(components[0], components[1], components[2]);
    }

    public ModelResourceLocation(String path, String variant) {
        this(parsePathString(path + '#' + (variant == null ? "normal" : variant)));
    }

    public ModelResourceLocation(ResourceLocation location, String variant) {
        this(location.getResourceDomain(), location.getResourcePath(), variant);
    }

    public ModelResourceLocation(String path) {
        this(parsePathString(path));
    }

    protected static String[] parsePathString(String pathString) {
        String[] components = new String[]{null, pathString, null};
        int i = pathString.indexOf('#');
        String s = pathString;

        if (i >= 0) {
            components[2] = pathString.substring(i + 1);

            if (i > 1) {
                s = pathString.substring(0, i);
            }
        }

        System.arraycopy(ResourceLocation.splitObjectName(s), 0, components, 0, 2);
        return components;
    }

    public String getVariant() {
        return this.variant;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ModelResourceLocation other && super.equals(obj)) {
            return this.variant.equals(other.variant);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + this.variant.hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + '#' + this.variant;
    }
}
