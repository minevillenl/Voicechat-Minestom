package dev.lu15.voicechat.event;

import dev.lu15.voicechat.network.minecraft.Group;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player leaves their voice group.
 */
public final class PlayerLeaveGroupVoiceChatEvent implements PlayerEvent {

    private final @NotNull Player player;
    private final @Nullable Group group;

    public PlayerLeaveGroupVoiceChatEvent(@NotNull Player player, @Nullable Group group) {
        this.player = player;
        this.group = group;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    /** The group the player left, or {@code null} if they were not in one. */
    public @Nullable Group getGroup() {
        return this.group;
    }

}
