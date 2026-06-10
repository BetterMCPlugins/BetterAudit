# BetterAudit

**The black box recorder for your staff team.** Every staff action — commands, gamemode switches, creative item grabs, op changes, punishments, sessions — logged to one queryable timeline, with real-time Discord alerts.

You gave your moderators power. BetterAudit gives you visibility.

## Why this exists

Every server with more than one staff member has the same problem: the owner can't see what staff actually do. The plugins that used to cover this (AuditTrail, AdminLog, Staff Monitor) have been abandoned for years, CoreProtect logs blocks rather than staff behavior, and the rest of the niche is scattered across single-purpose micro-plugins.

BetterAudit is one small, modern, free plugin that answers three questions:

1. **What did this moderator do?** — `/audit player <name>` shows their full timeline.
2. **Is something risky happening right now?** — gamemode changes, creative item spawning, `/op` and punishments alert staff in-game and your Discord channel instantly.
3. **Is my team active and fair?** — `/audit stats <name>` shows punishment counts, playtime, and activity per moderator: real data for promote/demote decisions.

## Features

- **Six independent modules**, each toggleable: command log, gamemode changes, creative item takes, op/deop tracking, join/leave sessions with playtime, punishment commands.
- **Discord webhook alerts** with per-action-type filtering — audit from your phone.
- **In-game alerts** for everyone with `betteraudit.notify`.
- **Queryable timeline**: `/audit recent`, `/audit player`, `/audit type`, with clickable pagination and hover coordinates.
- **Per-staff stats**: action counts, recorded playtime, first/last seen.
- **Privacy-aware**: auth commands (`/login`, `/register`, ...) are never logged, so passwords can't leak into the database.
- **Automatic retention**: entries older than N days are purged on a schedule.
- **Zero dependencies, async everywhere**: SQLite storage on a dedicated thread, never on the main thread. Folia-supported.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/audit recent [page]` | Latest entries across the server | `betteraudit.use` |
| `/audit player <name> [page]` | One player's timeline | `betteraudit.use` |
| `/audit type <type> [page]` | Filter by action type | `betteraudit.use` |
| `/audit stats <name>` | Per-staff statistics | `betteraudit.use` |
| `/audit purge <days>` | Delete entries older than N days | `betteraudit.admin` |
| `/audit reload` | Reload the configuration | `betteraudit.admin` |

## Permissions

- `betteraudit.use` — query the log (default: op)
- `betteraudit.admin` — purge and reload (default: op)
- `betteraudit.notify` — receive in-game alerts (default: op)
- `betteraudit.tracked` — players with this permission are recorded; ops always are (default: false)
- `betteraudit.exempt` — never record this player (default: false)

## Who gets tracked?

`tracking.mode: STAFF` (default) records ops and anyone with `betteraudit.tracked` — grant it to your staff ranks in LuckPerms. `tracking.mode: ALL` records everyone.

## Building

```
mvn package
```

Requires Java 21+. The jar lands in `target/BetterAudit-<version>.jar`. Drop it into `plugins/` on any Paper 1.21+ server.

## Roadmap

- Web dashboard (paid tier)
- Velocity network sync (paid tier)
- Vanish duration tracking via common vanish plugin hooks
- Inventory inspection (invsee) logging
