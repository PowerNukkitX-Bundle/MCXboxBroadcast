# MCXboxBroadcast for PowerNukkitX

[![License: GPL-3.0](https://img.shields.io/github/license/PowerNukkitX-Bundle/MCXboxBroadcast)](LICENSE)
[![Build and Release](https://github.com/PowerNukkitX-Bundle/MCXboxBroadcast/actions/workflows/release.yml/badge.svg)](https://github.com/PowerNukkitX-Bundle/MCXboxBroadcast/actions/workflows/release.yml)
[![Discord](https://img.shields.io/discord/1139621390908133396?label=discord&color=5865F2)](https://discord.gg/Tp3tA2kdCN)

A [PowerNukkitX](https://github.com/PowerNukkitX/PowerNukkitX) plugin that broadcasts your server as a joinable session over Xbox Live. Friends of the authenticated Xbox account can find and join the server directly from Minecraft's Friends tab.

![Example screenshot](https://github.com/user-attachments/assets/0af59bd6-eedf-4d64-94fb-5d66260b0454)

## Disclaimer

Use this project at your own risk. The contributors are not responsible for any damage or loss caused by this software. Using a separate Xbox account is recommended because the plugin emulates client functionality that may be subject to Microsoft's terms of service.

## Features

- Native PowerNukkitX plugin integration
- Automatic synchronization of MOTD and player counts
- Automatic Xbox friend and follower management
- Multi-account support
- Configurable public address and port
- Configurable WebRTC/NetherNet ICE port range
- Optional Slack or Discord notifications
- Custom Xbox profile showcase image
- Native PowerNukkitX command parameter tree and client-side suggestions

## Requirements

- PowerNukkitX with API 3.0.0
- Java 25 or newer
- An Xbox account that can play Minecraft
- A publicly reachable Bedrock server address and port

## Installation

1. Download `MCXboxBroadcast.jar` from the latest GitHub release.
2. Place it in the PowerNukkitX `plugins` directory.
3. Start or restart the server.
4. Wait for the plugin to display a Microsoft device-login URL and authentication code in the console.
5. Open the displayed URL, enter the code, and sign in with the Xbox account that should advertise the server.
6. Add or follow that account from another Xbox account.
7. Open Minecraft's Friends tab. The PowerNukkitX server should appear as a joinable session.

The plugin stores its configuration and authentication data in `plugins/MCXboxBroadcast/`.

## Configuration

On its first start, the plugin creates `plugins/MCXboxBroadcast/config.yml`.

The most relevant session settings are:

| Setting | Description |
| --- | --- |
| `session.remote-address` | Public address advertised to joining players. `auto` attempts to determine it automatically. |
| `session.remote-port` | Public Bedrock port. `auto` uses the PowerNukkitX listener port. |
| `session.update-interval` | Interval in seconds for updating the Xbox session. Must be at least 20 seconds. |
| `session.ice-port-range.min` | Lowest UDP port used for WebRTC/NetherNet, or `0` for the operating-system default. |
| `session.ice-port-range.max` | Highest UDP port used for WebRTC/NetherNet, or `0` for the operating-system default. |

When the server is behind NAT, a proxy, or port forwarding, set `remote-address` and `remote-port` to the public values players must use. MOTD, online players, and maximum player count are read directly from PowerNukkitX.

## Commands

The main command is `/mcxboxbroadcast`; `/mcbroadcast` is available as an alias.

| Command | Description |
| --- | --- |
| `/mcxboxbroadcast restart` | Recreates the Xbox Live session. |
| `/mcxboxbroadcast dumpsession` | Writes the current and previous session responses to JSON files for debugging. |
| `/mcxboxbroadcast accounts list` | Lists the active primary and sub-accounts. |
| `/mcxboxbroadcast accounts add <sub-session-id>` | Adds a sub-account. |
| `/mcxboxbroadcast accounts remove <sub-session-id>` | Removes a sub-account. |
| `/mcxboxbroadcast version` | Displays the installed plugin version. |

Except for `version`, commands can only be executed from the server console.

## Custom image

To use a custom showcase image for the Xbox account, place a file named `screenshot.jpg` in `plugins/MCXboxBroadcast/` before restarting the plugin.

Recommended image properties:

- Resolution: `1200x675`
- JPEG quality: `90`
- Chroma subsampling: `4:2:0`

Xbox Live may take several minutes to display an updated image.

## Building from source

Clone the repository and run:

```bash
./gradlew :bootstrap-pnx:build
```

The finished plugin is created at:

```text
bootstrap/pnx/build/libs/MCXboxBroadcast.jar
```

The PowerNukkitX server and protocol dependencies are used for compilation but are not included in the plugin JAR.
