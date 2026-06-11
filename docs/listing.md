# Listing copy for Modrinth / Hangar

Summary line (Modrinth "description" field, max ~250 chars):

> The black box recorder for your staff team. Every staff action — commands, gamemode, creative items, op changes, punishments, vanish, permissions — in one queryable timeline, with Discord alerts. Lightweight, modular, free.

Suggested tags: `admin-tools`, `moderation`, `utility`. Loaders: Paper, Folia. Versions: 1.21+.

---

## You gave your staff power. BetterAudit gives you visibility.

Every server with more than one staff member has the same blind spot: you can't see what your team actually does. Who switched to creative and grabbed a shulker of netherite? Who gave that rank? Is your new moderator actually active, or just online?

BetterAudit answers all of it with one small plugin:

**One timeline per staff member.** `/audit player <name>` shows everything they did — commands, gamemode switches, creative item grabs, op changes, punishments, inspections, WorldEdit, sessions. Hover any entry for the exact location.

**Alerts where you are.** High-risk actions (gamemode, creative items, /op, punishments, permission changes) alert staff in-game and your Discord — via webhook or straight through DiscordSRV with zero setup.

**Data for staff decisions.** `/audit stats <name>` shows punishment counts, recorded playtime, vanish time, first/last seen. Promote and demote on evidence, not vibes.

**A staff overview GUI.** `/audit menu` — one head per staff member, click to open their timeline.

### Integrations (all optional, auto-detected)

LuckPerms · LiteBans · AdvancedBan · EssentialsX · SuperVanish/PremiumVanish · DiscordSRV · PlaceholderAPI · WorldEdit/FAWE — plus configurable command lists that cover any inspection or economy plugin without needing its API.

### Built the way you'd want it built

- Eleven modules, each toggleable — run exactly as much auditing as you want
- Async SQLite storage, never on the main thread; ~70KB jar, zero dependencies
- Privacy-aware: auth commands (/login, /register) are never logged
- Automatic retention cleanup
- Paper 1.21+, Folia supported

### Commands & permissions

See the [README](https://github.com/BetterMCPlugins/BetterAudit#commands) for the full reference. Quick start: install, grant your staff ranks `betteraudit.tracked`, done — ops are tracked automatically.

Support: [Discord](https://discord.gg/UfnyJgbY4P) · [Issues](https://github.com/BetterMCPlugins/BetterAudit/issues)

---

Screenshot shot list (taken in-game, light UI pack off, default font):
1. `/audit menu` open with 4+ staff heads visible (hero image)
2. `/audit player <name>` chat timeline with mixed action types
3. A Discord alert embed (enable webhook, trigger a gamemode change)
4. `/audit stats` output for one moderator
