package dev.nikhey.betteraudit.menu;

import dev.nikhey.betteraudit.Recorder;
import dev.nikhey.betteraudit.storage.AuditStore;
import dev.nikhey.betteraudit.storage.AuditStore.ActorSummary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Staff overview: one head per recorded actor, sorted by most recent
 * activity. Clicking a head opens that player's chat timeline.
 */
public final class AuditMenu implements Listener {

    private static final int HEADS_PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_NEXT = 53;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());

    private static final class MenuHolder implements InventoryHolder {
        private final List<ActorSummary> actors;
        private final int page;
        private Inventory inventory;

        private MenuHolder(List<ActorSummary> actors, int page) {
            this.actors = actors;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final Plugin plugin;
    private final AuditStore store;

    public AuditMenu(Plugin plugin, AuditStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void open(Player player) {
        store.actors().whenComplete((actors, error) -> {
            if (error != null || actors == null) {
                player.sendMessage(Component.text("Failed to load the staff overview.", NamedTextColor.RED));
                return;
            }
            if (actors.isEmpty()) {
                player.sendMessage(Component.text("No audit data recorded yet.", NamedTextColor.GRAY));
                return;
            }
            openPage(player, actors, 1);
        });
    }

    private void openPage(Player player, List<ActorSummary> actors, int page) {
        // Inventories must be created and opened on the player's thread (Folia: region thread).
        player.getScheduler().run(plugin, task -> {
            MenuHolder holder = new MenuHolder(actors, page);
            Inventory inv = Bukkit.createInventory(holder, 54,
                    Component.text("Staff audit overview", NamedTextColor.DARK_GRAY));
            holder.inventory = inv;

            int start = (page - 1) * HEADS_PER_PAGE;
            int end = Math.min(start + HEADS_PER_PAGE, actors.size());
            for (int i = start; i < end; i++) {
                inv.setItem(i - start, headFor(actors.get(i)));
            }
            if (page > 1) {
                inv.setItem(SLOT_PREV, navItem("« Previous page"));
            }
            if (end < actors.size()) {
                inv.setItem(SLOT_NEXT, navItem("Next page »"));
            }
            player.openInventory(inv);
        }, null);
    }

    private ItemStack headFor(ActorSummary actor) {
        boolean console = actor.uuid().equals(Recorder.CONSOLE_UUID);
        ItemStack item = new ItemStack(console ? Material.COMMAND_BLOCK : Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(actor.uuid()));
        }
        meta.displayName(Component.text(actor.name(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                line("Entries: ", String.valueOf(actor.entries())),
                line("Last activity: ", TIME_FORMAT.format(Instant.ofEpochMilli(actor.lastTime()))),
                Component.text("Click to view timeline", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static Component line(String label, String value) {
        return Component.text()
                .append(Component.text(label, NamedTextColor.GRAY))
                .append(Component.text(value, NamedTextColor.WHITE))
                .build()
                .decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack navItem(String label) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getInventory()) {
            return;
        }
        int slot = event.getSlot();
        if (slot == SLOT_PREV && holder.page > 1) {
            openPage(player, holder.actors, holder.page - 1);
            return;
        }
        if (slot == SLOT_NEXT && holder.page * HEADS_PER_PAGE < holder.actors.size()) {
            openPage(player, holder.actors, holder.page + 1);
            return;
        }
        int index = (holder.page - 1) * HEADS_PER_PAGE + slot;
        if (slot >= HEADS_PER_PAGE || index >= holder.actors.size()) {
            return;
        }
        ActorSummary actor = holder.actors.get(index);
        player.closeInventory();
        player.performCommand("audit player " + actor.name());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
