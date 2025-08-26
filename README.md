# WispWorks ![Version](https://img.shields.io/badge/version-1.1_ALPHA-purple.svg)

---
[![Discord](https://img.shields.io/badge/JOIN_OUR_DISCORD-5662f6?style=for-the-badge&logo=DISCORD&logoColor=white)]([https://discord.gg/rPnNBDGS6k)
[![Spigot](https://img.shields.io/badge/SPIGOT-ffd000?style=for-the-badge&logo=SPIGOT&logoColor=white)](https://www.spigotmc.org/resources/wispworks.128346/)
## Status


⚠️ **This is an ALPHA version** of **WispWorks**.
Expect **incomplete features, bugs, and breaking changes**.
Many new mechanics and improvements are planned for future releases.

For the latest documentation, examples, and tutorials, please check the **[WispWorks Wiki](https://github.com/arminosz/wispworks/wiki)**.

---

## Description

**WispWorks** is a customizable **magic plugin** for Minecraft servers (Spigot/Paper).
It introduces:

* **Magic Cauldrons** for crafting unique recipes.
* **Rituals** with effects like summoning guardians, growing crops, flying torches.
* **Mutations** for mobs and plants, allowing transformations.
* **Custom recipes** possibility to create custom rituals that run commands or recipes with with cauldrons and rituals

---

## Installation

1. Download the latest `.jar` from [Spigot](https://www.spigotmc.org/resources/wispworks.128346/).
2. Place it inside your server’s `plugins/` folder.
3. Restart the server.
4. Edit `config.yml` inside `plugins/WispWorks/` to customize.
5. Use `/ww reload` to reload your modifications into the plugin.

---

## Commands

| Command                           | Description                       |
| --------------------------------- | --------------------------------- |
| `/wispworks reload`               | Reloads the plugin configuration. |
| `/wispworks info`                 | Shows plugin information.         |
| `/wispworks give <item> [player]` | Gives mutation items.             |
| Aliases: `/ww`                    | Alias               |

---

## Permissions

| Permission         | Description                            | Default |
| ------------------ | -------------------------------------- | ------- |
| `wispworks.admin`  | Full admin access to WispWorks.        | OP      |
| `wispworks.use`    | Allows players to use magic cauldrons. | true    |
| `wispworks.craft`  | Allows crafting via cauldrons.         | true    |
| `wispworks.ritual` | Allows using the ritual system.        | true    |

---

## Roadmap

* Expand ritual effects (weather, teleportation, world events).
* Add more advanced magic mechanics.
* Integration with other popular plugins (e.g., MythicMobs).
