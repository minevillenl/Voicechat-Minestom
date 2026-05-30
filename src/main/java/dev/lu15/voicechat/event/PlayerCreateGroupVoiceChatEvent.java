package dev.lu15.voicechat.event;

import dev.lu15.voicechat.network.minecraft.Group;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player creates a voice group. Cancelling this event prevents the
 * group from being created.
 */
public final class PlayerCreateGroupVoiceChatEvent implements PlayerEvent, CancellableEvent {

    private final @NotNull Player player;
    private final @NotNull Group group;

    private boolean cancelled;

    public PlayerCreateGroupVoiceChatEvent(@NotNull Player player, @NotNull Group group) {
        this.player = player;
        this.group = group;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    public @NotNull Group getGroup() {
        return this.group;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

}
