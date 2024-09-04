# NullProtect

Minecraft server protection suit

## Features
 - HWID blacklist/whitelist (Requires client mod)
 - Player account activation code

## Building

Due to the type of functionality of this mod, it is recommended that changes be made to its networking portion before distribution (if you need HWID authentication)

Changes and redistributions of this mod are permitted, but the original license must be included in the distribution copy

 - **NeoForge mod (client)** `gradlew :mod:neoforge:build`
 - **Fabric mod (client)** `gradlew :mod:fabric:build`
 - **Server plugin** `gradlew :server:shadowJar`

## Installing

**Client**: Install the mod if needs HWID authentication

**Server**: Install [packetevents](https://github.com/retrooper/packetevents) and plugin
