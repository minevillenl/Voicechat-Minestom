package dev.lu15.voicechat;

import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.network.minecraft.Packet;
import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.Collection;

import dev.lu15.voicechat.permission.PermissionHandler;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public sealed interface VoiceChat permits VoiceChatImpl {

    @NotNull String NAMESPACE = "voicechat";

    /**
     * Construct a new voice chat server. The server will start after building.
     * @param address the address to bind to
     * @param port the port to bind to, this can be the same as the Minecraft server port
     * @return a new voice chat server builder
     */
    static @NotNull Builder builder(@NotNull String address, int port) {
        return new VoiceChatImpl.BuilderImpl(address, port);
    }

    <T extends Packet<T>> void sendPacket(@NotNull Player player, @NotNull T packet);

    <T extends VoicePacket<T>> void sendPacket(@NotNull Player player, @NotNull T packet);

    @NotNull @Unmodifiable Collection<Category> getCategories();

    @NotNull RegistryKey<Category> addCategory(@NotNull Key id, @NotNull Category category);

    boolean removeCategory(@NotNull RegistryKey<Category> category);

    static @NotNull Key key(@NotNull @KeyPattern.Value String key) {
        return Key.key(NAMESPACE, key);
    }

    sealed interface Builder permits VoiceChatImpl.BuilderImpl {

        /**
         * Set the event node to use for voice chat events. This must be registered by yourself.
         * @param eventNode the event node
         * @return this builder
         */
        @NotNull Builder eventNode(@NotNull EventNode<Event> eventNode);

        /**
         * Set the public address of the voice server. This is used to tell clients where to connect to.
         * By default, this is blank and clients will use the address they connected to the Minecraft server with.
         * @param publicAddress the public address of the voice server
         * @return this builder
         */
        @NotNull Builder publicAddress(@NotNull String publicAddress);

        /**
         * Set the permission handler used to gate voice chat actions such as
         * creating or joining groups. Defaults to {@link PermissionHandler#ALLOW_ALL}.
         * @param permissions the permission handler
         * @return this builder
         */
        @NotNull Builder permissions(@NotNull PermissionHandler permissions);

        /**
         * Set the audio codec advertised to clients. Defaults to {@link Codec#VOIP}.
         * @param codec the codec
         * @return this builder
         */
        @NotNull Builder codec(@NotNull Codec codec);

        /**
         * Set the maximum voice packet size (MTU) in bytes. Defaults to {@code 1024}.
         * @param mtuSize the mtu size
         * @return this builder
         */
        @NotNull Builder mtuSize(int mtuSize);

        /**
         * Set the default proximity voice distance in blocks. Defaults to {@code 48}.
         * @param distance the distance
         * @return this builder
         */
        @NotNull Builder distance(double distance);

        /**
         * Set the keep-alive interval in milliseconds. Connections are dropped after
         * ten missed intervals. Defaults to {@code 1000}.
         * @param keepAlive the keep-alive interval
         * @return this builder
         */
        @NotNull Builder keepAlive(int keepAlive);

        /**
         * Set whether groups are enabled. Defaults to {@code true}.
         * @param groupsEnabled whether groups are enabled
         * @return this builder
         */
        @NotNull Builder groupsEnabled(boolean groupsEnabled);

        /**
         * Set whether clients are allowed to record audio. Defaults to {@code false}.
         * @param allowRecording whether recording is allowed
         * @return this builder
         */
        @NotNull Builder allowRecording(boolean allowRecording);

        /**
         * Set the maximum number of Minecraft-channel voice chat packets (group and
         * state changes) a player may send per second before being kicked. This does
         * not affect UDP voice traffic. Set to {@code -1} to disable. Defaults to {@code 16}.
         * @param packetRateLimit the per-second packet limit
         * @return this builder
         */
        @NotNull Builder packetRateLimit(int packetRateLimit);

        /**
         * Enable the voice chat server.
         * @return the voice chat server
         */
        @NotNull VoiceChat enable();

    }

}
