package dev.lu15.voicechat.permission;

/**
 * The default audience of a {@link Permission}, mirroring Simple Voice Chat's
 * upstream permission defaults. Minestom has no built-in permission or operator
 * system, so this is metadata for your own {@link PermissionHandler} to honour.
 */
public enum PermissionType {

    /** Granted to everyone by default. */
    EVERYONE,

    /** Granted to no one by default. */
    NOONE,

    /** Granted to server operators / admins by default. */
    OPS

}
