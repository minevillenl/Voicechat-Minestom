package dev.lu15.voicechat.permission;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether a player may perform a voice chat {@link Permission}.
 *
 * <p>Minestom has no built-in permission system, so wire this to your own
 * (LuckPerms, a command framework, tags, etc). The default {@link #ALLOW_ALL}
 * grants everything.
 *
 * <p>Example:
 * <pre>{@code
 * VoiceChat.builder("0.0.0.0", 24454)
 *         .permissions((player, permission) -> switch (permission) {
 *             case CREATE_GROUP -> player.hasTag(ADMIN_TAG);
 *             case JOIN_GROUP -> true;
 *         })
 *         .enable();
 * }</pre>
 */
@FunctionalInterface
public interface PermissionHandler {

    /** Grants every permission to every player. */
    @NotNull PermissionHandler ALLOW_ALL = (player, permission) -> true;

    /**
     * @param player the player attempting the action
     * @param permission the action being attempted
     * @return {@code true} if the player is allowed to perform it
     */
    boolean hasPermission(@NotNull Player player, @NotNull Permission permission);

}
