package dev.lu15.voicechat.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player disconnects from the voice chat server, either by leaving
 * the Minecraft server or by their voice connection timing out.
 */
public final class PlayerLeaveVoiceChatEvent implements PlayerEvent {

    private final @NotNull Player player;

    public PlayerLeaveVoiceChatEvent(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

}
