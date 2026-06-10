package dev.nikhey.betteraudit.model;

public enum ActionType {
    COMMAND("Command", 0x95A5A6),
    GAMEMODE("Gamemode change", 0xE67E22),
    CREATIVE_TAKE("Creative item", 0xE74C3C),
    OP_CHANGE("Op change", 0xC0392B),
    PUNISHMENT("Punishment", 0x9B59B6),
    INSPECTION("Inspection", 0x2980B9),
    ECONOMY("Economy", 0xF39C12),
    WORLD_EDIT("WorldEdit", 0x8E44AD),
    VANISH("Vanish", 0x1ABC9C),
    PERMISSION_CHANGE("Permission change", 0xF1C40F),
    SESSION_START("Joined", 0x2ECC71),
    SESSION_END("Left", 0x3498DB);

    private final String display;
    private final int discordColor;

    ActionType(String display, int discordColor) {
        this.display = display;
        this.discordColor = discordColor;
    }

    public String display() {
        return display;
    }

    public int discordColor() {
        return discordColor;
    }
}
