# Simple Voice Chat for Minestom

> [!CAUTION]
> This library is in development. It is not feature-complete but is usable. Please report any issues you find.

### Features
- [x] Proximity voice chat
- [x] Sound categories
- [ ] Groups
- [ ] Customisation
  - [ ] Codec
  - [x] Distance
  - [ ] MTU
  - [ ] Keepalive interval
  - [ ] Recording

```kts
repositories {
    mavenCentral()
    maven("https://jitpack.io") 
}

dependencies { 
    implementation("com.github.thebigtijn:voicechat-minestom:main-SNAPSHOT")
}
```

```java
// create a new voice chat server, you can use the same port as the Minecraft bind
VoiceChat voiceChat = VoiceChat.builder("0.0.0.0", 25565).enable();
```