package net.radiant.dev;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class DevGuiMainMenu extends GuiMainMenu {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevGuiMainMenu.class);

    private final MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
    private final Executor executor = Executors.newFixedThreadPool(1, r -> new Thread(r, "Account Thread"));

    @Override
    public void initGui() {
        super.initGui();

        int j = this.height / 4 + 48;
        this.buttonList.add(new GuiButton(-999, this.width / 2 - 100, j + 24 * 2, "Login"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);

        if (button.id != -999) {
            return;
        }

        executor.execute(() -> {
            try {
                LOGGER.info("Opening Microsoft Login Webview...");
                MicrosoftAuthResult result = this.authenticator.loginWithWebview();

                if (result != null) {
                    Minecraft.get().setSession(new Session(
                            result.profile().name(),
                            result.profile().id(),
                            result.accessToken(),
                            "MOJANG"
                    ));
                    LOGGER.info("Dev Session Updated: {}", result.profile().name());
                }
            } catch (MicrosoftAuthenticationException e) {
                LOGGER.error("Microsoft Authentication failed!", e);
            }
        });
    }
}
