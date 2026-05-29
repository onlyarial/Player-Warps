# PlayerWarps

A simple Paper 1.21.11 player warps plugin with item-only creation costs.

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
build/libs/PlayerWarps-1.0.0.jar
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
