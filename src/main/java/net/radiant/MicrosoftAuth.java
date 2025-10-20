package net.radiant;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MicrosoftAuth extends GuiScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftAuth.class);
    private static final MicrosoftAuthenticator AUTHENTICATOR = new MicrosoftAuthenticator();

    private String status = "Direct Login";
    private final GuiScreen parentScreen;
    private GuiTextField username;
    private GuiTextField password;

    public MicrosoftAuth(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        int btnY = this.height - 28;
        this.buttonList.add(new GuiButton(0, this.width / 2 - 155, btnY, 150, 20, "Login"));
        this.buttonList.add(new GuiButton(1, this.width / 2 + 5, btnY, 150, 20, I18n.format("gui.cancel")));

        int textX = this.width / 2 - 100;
        this.username = new GuiTextField(0, this.fontRendererObj, textX, this.height / 2 - 80, 200, 20);
        this.password = new GuiTextField(1, this.fontRendererObj, textX, this.height / 2 - 50, 200, 20);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, status, width / 2, height / 2 - 95 - fontRendererObj.FONT_HEIGHT * 2, -1);

        username.drawTextBox();
        password.drawTextBox();

        if (username.getText().isEmpty())
            drawString(fontRendererObj, "Email", width / 2 - 95, height / 2 - 74, -1);
        if (password.getText().isEmpty())
            drawString(fontRendererObj, "Password", width / 2 - 95, height / 2 - 44, -1);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;

        if (button.id == 0) {
            status = "Logging in...";

            if (username.getText().isEmpty() || password.getText().isEmpty()) {
                status = "Enter an email & password";
            } else {
                login();
            }
        } else if (button.id == 1) {
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        username.textboxKeyTyped(typedChar, keyCode);
        password.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    public void login() {
        Thread loginThread = new Thread(() -> {
            try {
                MicrosoftAuthResult result = AUTHENTICATOR.loginWithCredentials(username.getText(), password.getText());

                if (result != null) {
                    MinecraftProfile profile = result.getProfile();

                    mc.setSession(new Session(
                            profile.getName(),
                            profile.getId(),
                            result.getAccessToken(),
                            "MICROSOFT"));

                    LOGGER.info("Logged in as: {}", profile.getName());
                    status = "Logged in as " + profile.getName();
                } else {
                    status = "Login failed";
                    LOGGER.error("Login failed, result is null");
                }
            } catch (MicrosoftAuthenticationException exception) {
                status = "Login Failed";
                LOGGER.error("Microsoft authentication failed: {}", exception.getMessage());
            }

            System.gc();
        });

        loginThread.start();
    }
}
