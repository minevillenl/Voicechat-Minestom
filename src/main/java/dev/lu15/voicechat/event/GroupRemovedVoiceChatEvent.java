package dev.lu15.voicechat.event;

import dev.lu15.voicechat.network.minecraft.Group;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a voice group is removed, for example when the last member leaves a
 * non-persistent group.
 */
public final class GroupRemovedVoiceChatEvent implements Event {

    private final @NotNull Group group;

    public GroupRemovedVoiceChatEvent(@NotNull Group group) {
        this.group = group;
    }

    public @NotNull Group getGroup() {
        return this.group;
    }

}
