# PlayerWarps

A simple Paper player warps plugin with item-only creation costs.

## Supported Minecraft versions

PlayerWarps is built against the Paper 1.21.1 API for compatibility across the current 1.21.x and 26.x server line.

- 1.21.1
- 1.21.2
- 1.21.3
- 1.21.4
- 1.21.5
- 1.21.6
- 1.21.7
- 1.21.8
- 1.21.9
- 1.21.10
- 1.21.11
- 26.1
- 26.1.1
- 26.1.2
- 26.2

## Features

- `/pwarp` public warps GUI
- `/pwarp set <name>` create a warp
- `/pwarp <name>` teleport to a warp
- `/pwarp delete <name>` delete your warp
- `/pwarp rename <old> <new>` rename your warp
- `/pwarp seticon <name>` set the warp icon from the item in your hand
- `/pwarp manage` manage your own warps
- Item-only warp creation costs
- Configurable GUI aliases in `config.yml`
- Fully configurable GUI layout in `gui.yml`

## Build

```bash
./gradlew build
```

The jar will be in:

```text
build/libs/PlayerWarps-1.0.4.jar
```

## Config notes

Add GUI aliases with:

```yml
settings:
  gui-aliases:
    - warps
    - pwarps
```

Customize the GUI in:

```text
plugins/PlayerWarps/gui.yml
```
