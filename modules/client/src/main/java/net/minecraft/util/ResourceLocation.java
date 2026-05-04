package net.minecraft.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class ResourceLocation {
    protected final String resourceDomain;
    protected final String resourcePath;

    public ResourceLocation(String domain, String path) {
        this.resourceDomain = StringUtils.isEmpty(domain) ? "minecraft" : domain.toLowerCase();
        this.resourcePath = path;
        Objects.requireNonNull(this.resourcePath);
    }

    public ResourceLocation(String name) {
        this(splitObjectName(name));
    }

    protected ResourceLocation(String... components) {
        this(components[0], components[1]);
    }

    protected static String[] splitObjectName(String toSplit) {
        String[] str = new String[]{null, toSplit};
        int i = toSplit.indexOf(':');

        if (i >= 0) {
            str[1] = toSplit.substring(i + 1);

            if (i > 1) {
                str[0] = toSplit.substring(0, i);
            }
        }

        return str;
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public String getResourceDomain() {
        return this.resourceDomain;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResourceLocation other)) {
            return false;
        }

        return Objects.equals(this.resourceDomain, other.resourceDomain) &&
                Objects.equals(this.resourcePath, other.resourcePath);
    }

    @Override
    public int hashCode() {
        return 31 * this.resourceDomain.hashCode() + this.resourcePath.hashCode();
    }

    @Override
    public String toString() {
        return this.resourceDomain + ':' + this.resourcePath;
    }
}
