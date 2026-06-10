package dev.nikhey.betteraudit.model;

public enum ActionType {
    COMMAND("Command", 0x95A5A6),
    GAMEMODE("Gamemode change", 0xE67E22),
    CREATIVE_TAKE("Creative item", 0xE74C3C),
    OP_CHANGE("Op change", 0xC0392B),
    PUNISHMENT("Punishment", 0x9B59B6),
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
