# PlayerWarps

A simple Paper 1.21.11 player warps plugin with item-cost support only. No Vault or economy hook.

## Commands

- `/pwarp` - Open the public player warps GUI
- `/pwarp <name>` - Teleport to a warp
- `/pwarp set <name>` - Create a player warp
- `/pwarp delete <name>` - Delete your warp
- `/pwarp rename <oldName> <newName>` - Rename your warp
- `/pwarp seticon <name>` - Set your warp icon to the item in your hand
- `/pwarp manage` - Manage your own warps
- `/pwarp reload` - Reload config and warps

## Permissions

- `playerwarps.use` - Use player warps
- `playerwarps.set` - Create player warps
- `playerwarps.delete` - Delete player warps
- `playerwarps.gui` - Open the GUI
- `playerwarps.admin` - Admin bypass/reload

## Build

Install Java 21. Then generate the Gradle wrapper once:

```bash
gradle wrapper --gradle-version 8.10.2
```

Then build:

```bash
chmod +x gradlew
./gradlew build
```

The plugin jar will be in:

```text
build/libs/PlayerWarps-1.0.0.jar
```
