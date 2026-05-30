# Simple Voice Chat for Minestom

> [!CAUTION]
> This library is in development. It is not feature-complete but is usable. Please report any issues you find.

A Minestom port of [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat).
Targets Minecraft **1.21.11** (voice chat compatibility version **20**).

> [!IMPORTANT]
> Minestom `1.21.11` requires a **Java 25** runtime, so this library does too.

### Features
- [x] Proximity voice chat
- [x] Sound categories
- [x] Groups (normal / open / isolated)
- [x] Permissions
- [x] Customisation
  - [x] Codec
  - [x] Distance
  - [x] MTU
  - [x] Keepalive interval
  - [x] Recording

### Installation

```kts
repositories {
    mavenCentral()
    maven("https://jitpack.io")
} 

dependencies {
    implementation("com.github.thebigtijn:voicechat-minestom:main-SNAPSHOT")
}
```

### Usage

```java
// start a voice chat server (UDP). the port can match the Minecraft bind.
VoiceChat voiceChat = VoiceChat.builder("0.0.0.0", 24454)
        // address clients connect to; leave blank to reuse the Minecraft server address.
        // never use "0.0.0.0" here that is a bind address, not a routable destination.
        .publicAddress("")
        .enable();
```

> [!NOTE]
> The voice server is UDP. When running behind Docker/NAT/a proxy, publish the UDP
> port (e.g. `-p 24454:24454/udp`) and set `publicAddress` to a routable host if
> clients cannot reach the Minecraft server address directly.

All options are optional; the defaults match upstream Simple Voice Chat:

```java
VoiceChat.builder("0.0.0.0", 24454)
        .codec(Codec.VOIP)        // VOIP | AUDIO | RESTRICTED_LOWDELAY
        .distance(48)             // proximity range in blocks
        .mtuSize(1024)            // max voice packet size
        .keepAlive(1000)          // keep-alive interval (ms); timeout = 10x
        .groupsEnabled(true)
        .allowRecording(false)
        .enable();
```

### Permissions

Minestom has no permission system, so wire one in via the builder. Without it,
everything is allowed (`PermissionHandler.ALLOW_ALL`).

```java
VoiceChat.builder("0.0.0.0", 24454)
        .permissions((player, permission) -> switch (permission) {
            case SPEAK, LISTEN, JOIN_GROUP -> true;
            case CREATE_GROUP -> player.hasTag(ADMIN_TAG);
            case ADMIN -> false;
        })
        .enable();
```

Each `Permission` exposes the upstream node and default audience for mapping to an
external system (e.g. LuckPerms):

| Permission | Node                      | Default |
| --- |---------------------------| --- |
| `LISTEN` | `voicechat.listen`        | everyone |
| `SPEAK` | `voicechat.speak`         | everyone |
| `CREATE_GROUP` | `voicechat.groups.create` | everyone |
| `JOIN_GROUP` | `voicechat.groups.join`   | everyone |
| `ADMIN` | `voicechat.admin`         | ops |

```java
.permissions((player, permission) -> luckPerms.has(player, permission.node()))
```
