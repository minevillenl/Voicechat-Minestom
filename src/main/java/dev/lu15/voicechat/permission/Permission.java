package dev.lu15.voicechat.permission;

import org.jetbrains.annotations.NotNull;

/**
 * Voice chat actions that can be gated behind a {@link PermissionHandler}.
 *
 * <p>Mirrors the upstream Simple Voice Chat (Bukkit/Paper) permission set. Each
 * constant carries the upstream permission {@link #node()} and its default
 * {@link #defaultType()} so a {@link PermissionHandler} can map them to a real
 * permission system.
 */
public enum Permission {

    /** Required to receive (hear) voice audio. {@code voicechat.listen} */
    LISTEN("voicechat.listen", PermissionType.EVERYONE),

    /** Required to send (speak) microphone audio. {@code voicechat.speak} */
    SPEAK("voicechat.speak", PermissionType.EVERYONE),

    /** Required to create a new voice group. {@code voicechat.groups} */
    CREATE_GROUP("voicechat.groups.create", PermissionType.EVERYONE),

    /** Required to join an existing voice group. {@code voicechat.groups} */
    JOIN_GROUP("voicechat.groups.join", PermissionType.EVERYONE),

    /** Required for administrative voice chat actions. {@code voicechat.admin} */
    ADMIN("voicechat.admin", PermissionType.OPS);

    private final @NotNull String node;
    private final @NotNull PermissionType defaultType;

    Permission(@NotNull String node, @NotNull PermissionType defaultType) {
        this.node = node;
        this.defaultType = defaultType;
    }

    /** The upstream permission node, e.g. {@code voicechat.groups}. */
    public @NotNull String node() {
        return this.node;
    }

    /** The default audience for this permission. */
    public @NotNull PermissionType defaultType() {
        return this.defaultType;
    }

}
