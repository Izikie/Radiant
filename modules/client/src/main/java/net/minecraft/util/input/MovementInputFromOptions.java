package net.minecraft.util.input;

import net.minecraft.client.settings.GameSettings;
import net.radiant.benchmark.ClientBenchmark;

public class MovementInputFromOptions extends MovementInput {
    private final GameSettings gameSettings;

    public MovementInputFromOptions(GameSettings gameSettingsIn) {
        this.gameSettings = gameSettingsIn;
    }

    @Override
    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;
        if (ClientBenchmark.controlsCamera()) {
            this.jump = false;
            this.sneak = false;
            return;
        }

        if (this.gameSettings.keyBindForward.isKeyDown()) {
            ++this.moveForward;
        }

        if (this.gameSettings.keyBindBack.isKeyDown()) {
            --this.moveForward;
        }

        if (this.gameSettings.keyBindLeft.isKeyDown()) {
            ++this.moveStrafe;
        }

        if (this.gameSettings.keyBindRight.isKeyDown()) {
            --this.moveStrafe;
        }

        this.jump = this.gameSettings.keyBindJump.isKeyDown();
        this.sneak = this.gameSettings.keyBindSneak.isKeyDown();

        if (this.sneak) {
            this.moveStrafe = (float) (this.moveStrafe * 0.3D);
            this.moveForward = (float) (this.moveForward * 0.3D);
        }
    }
}
