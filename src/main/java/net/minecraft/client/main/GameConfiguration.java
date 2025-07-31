package net.minecraft.client.main;

import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.util.Session;

import java.io.File;
import java.net.Proxy;

public record GameConfiguration(
        UserInformation userInfo,
        DisplayInformation displayInfo,
        FolderInformation folderInfo,
        ServerInformation serverInfo
) {

    public record UserInformation(
            Session session,
            PropertyMap userProperties,
            PropertyMap profileProperties,
            Proxy proxy
    ) {
    }

    public record DisplayInformation(
            int width,
            int height,
            boolean fullscreen,
            boolean checkGlErrors
    ) {
    }

    public record FolderInformation(
            File mcDataDir,
            File resourcePacksDir,
            File assetsDir
    ) {
    }

    public record ServerInformation(
            String address,
            int port
    ) {
    }
}
