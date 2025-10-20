package net.minecraft.client.gui.spectator;

import net.minecraft.client.gui.spectator.categories.TeleportToPlayer;
import net.minecraft.client.gui.spectator.categories.TeleportToTeam;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.chat.IChatComponent;

import java.util.ArrayList;
import java.util.List;

public class BaseSpectatorGroup implements ISpectatorMenuView {
    private final List<ISpectatorMenuObject> field_178671_a = new ArrayList<>();

    public BaseSpectatorGroup() {
        this.field_178671_a.add(new TeleportToPlayer());
        this.field_178671_a.add(new TeleportToTeam());
    }

    @Override
    public List<ISpectatorMenuObject> func_178669_a() {
        return this.field_178671_a;
    }

    @Override
    public IChatComponent func_178670_b() {
        return new ChatComponentText("Press a key to select a command, and again to use it.");
    }
}
