# NullProtect

Minecraft server protection suit

## Features
 - HWID blacklist/whitelist (Requires client mod)
 - Player account activation code

## Building
 - **NeoForge mod (client)** `gradlew :mod:neoforge:build`
 - **Fabric mod (client)** `gradlew :mod:fabric:build`
 - **Server plugin** `gradlew :server:shadowJar`

## Installing

**Client**: Install the mod if needs HWID authentication

**Server**: Install [packetevents](https://github.com/retrooper/packetevents) and plugin
