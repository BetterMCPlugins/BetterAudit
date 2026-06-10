package dev.nikhey.betteraudit.model;

import java.util.UUID;

public record AuditEntry(
        long id,
        long time,
        UUID actorUuid,
        String actorName,
        ActionType type,
        String detail,
        String world,
        int x,
        int y,
        int z,
        long durationSeconds
) {
}
