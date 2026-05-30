package dev.lu15.voicechat;

import dev.lu15.voicechat.event.PlayerJoinVoiceChatEvent;
import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.network.minecraft.Group;
import dev.lu15.voicechat.network.minecraft.VoiceState;
import dev.lu15.voicechat.event.PlayerHandshakeVoiceChatEvent;
import dev.lu15.voicechat.event.PlayerUpdateVoiceStateEvent;
import dev.lu15.voicechat.network.minecraft.MinecraftPacketHandler;
import dev.lu15.voicechat.network.minecraft.Packet;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.CategoryAddedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.CategoryRemovedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupChangedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupCreatedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupRemovedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.VoiceStateUpdatedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.HandshakeAcknowledgePacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.CreateGroupPacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.HandshakePacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.JoinGroupPacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.LeaveGroupPacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.UpdateStatePacket;
import dev.lu15.voicechat.network.voice.VoicePacket;
import dev.lu15.voicechat.network.voice.VoiceServer;
import dev.lu15.voicechat.network.voice.encryption.SecretUtilities;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VoiceChatImpl implements VoiceChat {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(VoiceChatImpl.class);

    private final @NotNull MinecraftPacketHandler packetHandler = new MinecraftPacketHandler();
    private final @NotNull DynamicRegistry<Category> categories = DynamicRegistry.create(VoiceChat.key("categories"));

    // active voice groups, and the (nullable) plaintext password required to join each
    private final @NotNull Map<UUID, Group> groups = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, String> groupPasswords = new ConcurrentHashMap<>();

    private final @NotNull VoiceServer server;
    private final int port;
    private final @NotNull String publicAddress;
    private final @NotNull PermissionHandler permissions;

    @SuppressWarnings("PatternValidation")
    private VoiceChatImpl(@NotNull InetAddress address, int port, @NotNull EventNode<Event> eventNode, @NotNull String publicAddress, @NotNull PermissionHandler permissions) {
        this.port = port;
        this.publicAddress = publicAddress;
        this.permissions = permissions;

        // minestom doesn't allow removal of items from registries by default, so
        // we have to enable this feature to allow for the removal of categories
        System.setProperty("minestom.registry.unsafe-ops", "true");

        EventNode<Event> voiceServerEventNode = EventNode.all("voice-server");
        eventNode.addChild(voiceServerEventNode);
        this.server = new VoiceServer(this, address, port, voiceServerEventNode, permissions);

        this.server.start();
        LOGGER.info("voice server started on {}:{}", address, port);

        eventNode.addListener(PlayerPluginMessageEvent.class, event -> {
            String channel = event.getIdentifier();
            if (!Key.parseable(channel)) return;
            Key identifier = Key.key(channel);

            if (!identifier.namespace().equals("voicechat")) return;

            try {
                Packet<?> packet = this.packetHandler.read(channel, event.getMessage());
                final Player player = event.getPlayer();
                switch (packet) {
                    case HandshakePacket p -> this.handle(player, p);
                    case UpdateStatePacket p -> this.handle(player, p);
                    case CreateGroupPacket p -> this.handle(player, p);
                    case JoinGroupPacket p -> this.handle(player, p);
                    case LeaveGroupPacket p -> this.handle(player, p);
                    case null -> LOGGER.warn("received unknown packet from {}: {}", player.getUsername(), channel);
                    default -> throw new UnsupportedOperationException("unimplemented packet: " + packet);
                }
            } catch (Exception e) {
                // we ignore this exception because it's most
                // likely to be caused by the client sending
                // an invalid packet.
                LOGGER.debug("failed to read plugin message", e);
            }
        });

        // send existing categories and groups to newly joining players
        eventNode.addListener(PlayerJoinVoiceChatEvent.class, event -> {
            Player player = event.getPlayer();
            for (Category category : this.categories.values()) {
                RegistryKey<Category> key = this.categories.getKey(category);
                if (key == null) throw new IllegalStateException("category not found in registry");
                this.sendPacket(player, new CategoryAddedPacket(key.key(), category));
            }
            for (Group group : this.groups.values()) {
                this.sendPacket(player, new GroupCreatedPacket(group));
            }
        });

        // when a player leaves, drop any non-persistent group that is now empty
        eventNode.addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            if (player.hasTag(Tags.GROUP)) {
                player.removeTag(Tags.GROUP);
                this.cleanupGroups();
            }
        });
    }

    private void handle(@NotNull Player player, @NotNull HandshakePacket packet) {
        if (packet.version() != 20) {
            LOGGER.warn("player {} using wrong version: {}", player.getUsername(), packet.version());
            return;
        }

        if (SecretUtilities.hasSecret(player)) {
            LOGGER.warn("player {} already has a secret", player.getUsername());
            return;
        }

        PlayerHandshakeVoiceChatEvent event = new PlayerHandshakeVoiceChatEvent(player, SecretUtilities.generateSecret());

        EventDispatcher.callCancellable(event, () -> {
            SecretUtilities.setSecret(player, event.getSecret());

            player.sendPacket(this.packetHandler.write(new HandshakeAcknowledgePacket(
                    event.getSecret(),
                    this.port,
                    player.getUuid(), // why is this sent? the client already knows the player's uuid
                    Codec.VOIP, // todo: configurable
                    1024, // todo: configurable
                    48, // todo: configurable
                    1000, // todo: configurable
                    true, // groups enabled
                    this.publicAddress,
                    false // todo: configurable
            )));
        });
    }

    private void handle(@NotNull Player player, @NotNull UpdateStatePacket packet) {
        // todo: set state when players disconnect from voice chat server - NOT when they disconnect from the minecraft server
        // preserve the player's current group across mute/unmute toggles
        this.broadcastState(player, packet.disabled(), player.getTag(Tags.GROUP));
    }

    private void handle(@NotNull Player player, @NotNull CreateGroupPacket packet) {
        if (!this.permissions.hasPermission(player, Permission.CREATE_GROUP)) {
            this.denyPermission(player);
            return;
        }

        Group group = new Group(
                UUID.randomUUID(),
                packet.name(),
                packet.password() != null,
                false, // persistent
                false, // hidden
                packet.type()
        );
        this.groups.put(group.id(), group);
        if (packet.password() != null) this.groupPasswords.put(group.id(), packet.password());

        this.broadcastToClients(new GroupCreatedPacket(group));

        // the creator immediately joins their new group
        this.broadcastState(player, this.isDisabled(player), group);
        this.sendPacket(player, new GroupChangedPacket(group.id(), false));
    }

    private void handle(@NotNull Player player, @NotNull JoinGroupPacket packet) {
        if (!this.permissions.hasPermission(player, Permission.JOIN_GROUP)) {
            this.denyPermission(player);
            return;
        }

        Group group = this.groups.get(packet.group());
        if (group == null) {
            this.sendPacket(player, new GroupChangedPacket(null, false));
            return;
        }

        String password = this.groupPasswords.get(group.id());
        if (password != null && !password.equals(packet.password())) {
            this.sendPacket(player, new GroupChangedPacket(null, true)); // incorrect password
            return;
        }

        this.broadcastState(player, this.isDisabled(player), group);
        this.sendPacket(player, new GroupChangedPacket(group.id(), false));
    }

    private void handle(@NotNull Player player, @NotNull LeaveGroupPacket packet) {
        this.broadcastState(player, this.isDisabled(player), null);
        this.sendPacket(player, new GroupChangedPacket(null, false));
        this.cleanupGroups();
    }

    private boolean isDisabled(@NotNull Player player) {
        VoiceState state = player.getTag(Tags.PLAYER_STATE);
        return state != null && state.disabled();
    }

    private void denyPermission(@NotNull Player player) {
        player.sendActionBar(Component.text("You don't have permission to do that."));
    }

    // updates the player's group tag and broadcasts their new voice state to everyone
    private void broadcastState(@NotNull Player player, boolean disabled, @Nullable Group group) {
        if (group == null) player.removeTag(Tags.GROUP);
        else player.setTag(Tags.GROUP, group);

        VoiceState state = new VoiceState(
                disabled,
                false,
                player.getUuid(),
                player.getUsername(),
                group == null ? null : group.id()
        );
        player.setTag(Tags.PLAYER_STATE, state);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStateUpdatedPacket(state)));

        EventDispatcher.call(new PlayerUpdateVoiceStateEvent(player, state));
    }

    // removes non-persistent groups that no longer have any members
    private void cleanupGroups() {
        Set<UUID> used = new HashSet<>();
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            Group group = player.getTag(Tags.GROUP);
            if (group != null) used.add(group.id());
        }

        for (UUID id : new ArrayList<>(this.groups.keySet())) {
            Group group = this.groups.get(id);
            if (group == null || group.persistent()) continue;
            if (used.contains(id)) continue;

            this.groups.remove(id);
            this.groupPasswords.remove(id);
            this.broadcastToClients(new GroupRemovedPacket(id));
        }
    }

    private <T extends Packet<T>> void broadcastToClients(@NotNull T packet) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.hasTag(Tags.VOICE_CLIENT)) continue; // only send to voice chat clients
            this.sendPacket(player, packet);
        }
    }

    @Override
    public <T extends Packet<T>> void sendPacket(@NotNull Player player, @NotNull T packet) {
        player.sendPacket(this.packetHandler.write(packet));
    }

    @Override
    public <T extends VoicePacket<T>> void sendPacket(@NotNull Player player, @NotNull T packet) {
        this.server.write(player, packet);
    }

    @Override
    public @NotNull @Unmodifiable Collection<Category> getCategories() {
        return Collections.unmodifiableCollection(this.categories.values());
    }

    @Override
    public @NotNull RegistryKey<Category> addCategory(@NotNull Key id, @NotNull Category category) {
        Category existing = this.categories.get(id);
        RegistryKey<Category> key = this.categories.register(id, category);

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.hasTag(Tags.VOICE_CLIENT)) continue; // only send to voice chat clients

            // remove the existing category if it exists, then add the new one
            if (existing != null) this.sendPacket(player, new CategoryRemovedPacket(id));
            this.sendPacket(player, new CategoryAddedPacket(id, category));
        }

        return key;
    }

    @Override
    public boolean removeCategory(@NotNull RegistryKey<Category> category) {
        boolean removed = this.categories.remove(category.key());
        if (!removed) return false;

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.hasTag(Tags.VOICE_CLIENT)) continue; // only send to voice chat clients
            this.sendPacket(player, new CategoryRemovedPacket(category.key()));
        }

        return true;
    }

    final static class BuilderImpl implements Builder {

        private final @NotNull InetAddress address;
        private final int port;

        private @NotNull String publicAddress = ""; // this causes the client to attempt to connect to the same ip as the minecraft server
        private @NotNull PermissionHandler permissions = PermissionHandler.ALLOW_ALL;

        private @Nullable EventNode<Event> eventNode;

        BuilderImpl(@NotNull String address, int port) {
            try {
                this.address = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("invalid address", e);
            }
            this.port = port;
        }

        @Override
        public @NotNull Builder eventNode(@NotNull EventNode<Event> eventNode) {
            this.eventNode = eventNode;
            return this;
        }

        @Override
        public @NotNull Builder publicAddress(@NotNull String publicAddress) {
            this.publicAddress = publicAddress;
            return this;
        }

        @Override
        public @NotNull Builder permissions(@NotNull PermissionHandler permissions) {
            this.permissions = permissions;
            return this;
        }

        @Override
        public @NotNull VoiceChat enable() {
            // if the user did not provide an event node, create and register one
            if (this.eventNode == null) {
                this.eventNode = EventNode.all("voice-chat");
                MinecraftServer.getGlobalEventHandler().addChild(this.eventNode);
            }

            return new VoiceChatImpl(this.address, this.port, this.eventNode, this.publicAddress, this.permissions);
        }

    }

}
