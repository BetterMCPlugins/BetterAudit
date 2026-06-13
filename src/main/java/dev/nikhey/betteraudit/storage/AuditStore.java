package dev.nikhey.betteraudit.storage;

import dev.nikhey.betteraudit.model.ActionType;
import dev.nikhey.betteraudit.model.AuditEntry;
import org.slf4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SQLite-backed store. All access is funneled through a single worker thread,
 * so the plugin never touches the database from a server thread.
 */
public final class AuditStore {

    /** Versioned schema contract for external read-only consumers (e.g. BetterPanel). */
    public static final int SCHEMA_VERSION = 1;

    public record Stats(Map<ActionType, Integer> counts, long totalSessionSeconds, long vanishSeconds,
                        long firstSeen, long lastSeen) {
    }

    public record ActorSummary(UUID uuid, String name, int entries, long lastTime) {
    }

    private final File file;
    private final Logger logger;
    private final ExecutorService io;
    private Connection conn;

    public AuditStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        this.io = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "BetterAudit-DB");
            t.setDaemon(true);
            return t;
        });
    }

    public void init() throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create data folder " + parent);
        }
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            // Versioned schema contract: external readers (BetterPanel) pin a
            // supported range against this. Seeded once, never auto-bumped.
            st.execute("CREATE TABLE IF NOT EXISTS schema_meta (version INTEGER NOT NULL)");
            try (ResultSet rs = st.executeQuery("SELECT version FROM schema_meta LIMIT 1")) {
                if (!rs.next()) {
                    st.execute("INSERT INTO schema_meta (version) VALUES (" + SCHEMA_VERSION + ")");
                }
            }
            st.execute("""
                    CREATE TABLE IF NOT EXISTS audit (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        time INTEGER NOT NULL,
                        actor_uuid TEXT NOT NULL,
                        actor_name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        detail TEXT NOT NULL,
                        world TEXT,
                        x INTEGER NOT NULL DEFAULT 0,
                        y INTEGER NOT NULL DEFAULT 0,
                        z INTEGER NOT NULL DEFAULT 0,
                        dur INTEGER NOT NULL DEFAULT 0
                    )""");
            st.execute("CREATE INDEX IF NOT EXISTS idx_audit_time ON audit(time)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit(actor_name COLLATE NOCASE)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_audit_type ON audit(type)");
        }
    }

    public void insert(long time, UUID actor, String actorName, ActionType type, String detail,
                       String world, int x, int y, int z, long durationSeconds) {
        io.execute(() -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO audit (time, actor_uuid, actor_name, type, detail, world, x, y, z, dur) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                ps.setLong(1, time);
                ps.setString(2, actor.toString());
                ps.setString(3, actorName);
                ps.setString(4, type.name());
                ps.setString(5, detail);
                ps.setString(6, world);
                ps.setInt(7, x);
                ps.setInt(8, y);
                ps.setInt(9, z);
                ps.setLong(10, durationSeconds);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.error("Failed to insert audit entry", e);
            }
        });
    }

    public CompletableFuture<List<AuditEntry>> recent(int limit, int offset) {
        return query("SELECT * FROM audit ORDER BY time DESC, id DESC LIMIT ? OFFSET ?",
                ps -> {
                    ps.setInt(1, limit);
                    ps.setInt(2, offset);
                });
    }

    public CompletableFuture<List<AuditEntry>> byActor(String name, int limit, int offset) {
        return query("SELECT * FROM audit WHERE actor_name = ? COLLATE NOCASE ORDER BY time DESC, id DESC LIMIT ? OFFSET ?",
                ps -> {
                    ps.setString(1, name);
                    ps.setInt(2, limit);
                    ps.setInt(3, offset);
                });
    }

    public CompletableFuture<List<AuditEntry>> byType(ActionType type, int limit, int offset) {
        return query("SELECT * FROM audit WHERE type = ? ORDER BY time DESC, id DESC LIMIT ? OFFSET ?",
                ps -> {
                    ps.setString(1, type.name());
                    ps.setInt(2, limit);
                    ps.setInt(3, offset);
                });
    }

    public CompletableFuture<List<ActorSummary>> actors() {
        CompletableFuture<List<ActorSummary>> future = new CompletableFuture<>();
        io.execute(() -> {
            // Bare actor_name alongside MAX(time) makes SQLite pick the name from
            // the most recent row, so renamed players show their latest name.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT actor_uuid, actor_name, COUNT(*) c, MAX(time) t FROM audit GROUP BY actor_uuid ORDER BY t DESC");
                 ResultSet rs = ps.executeQuery()) {
                List<ActorSummary> actors = new ArrayList<>();
                while (rs.next()) {
                    actors.add(new ActorSummary(
                            UUID.fromString(rs.getString("actor_uuid")),
                            rs.getString("actor_name"),
                            rs.getInt("c"),
                            rs.getLong("t")));
                }
                future.complete(actors);
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Stats> stats(String name) {
        CompletableFuture<Stats> future = new CompletableFuture<>();
        io.execute(() -> {
            try {
                Map<ActionType, Integer> counts = new LinkedHashMap<>();
                long sessionSeconds = 0;
                long vanishSeconds = 0;
                long first = 0;
                long last = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT type, COUNT(*) c, SUM(dur) d, MIN(time) f, MAX(time) l FROM audit WHERE actor_name = ? COLLATE NOCASE GROUP BY type")) {
                    ps.setString(1, name);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ActionType type;
                            try {
                                type = ActionType.valueOf(rs.getString("type"));
                            } catch (IllegalArgumentException e) {
                                continue;
                            }
                            counts.put(type, rs.getInt("c"));
                            if (type == ActionType.SESSION_END) {
                                sessionSeconds = rs.getLong("d");
                            } else if (type == ActionType.VANISH) {
                                vanishSeconds = rs.getLong("d");
                            }
                            long f = rs.getLong("f");
                            long l = rs.getLong("l");
                            if (first == 0 || (f > 0 && f < first)) {
                                first = f;
                            }
                            if (l > last) {
                                last = l;
                            }
                        }
                    }
                }
                future.complete(new Stats(counts, sessionSeconds, vanishSeconds, first, last));
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Integer> purgeOlderThan(int days) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        long cutoff = System.currentTimeMillis() - days * 86_400_000L;
        io.execute(() -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM audit WHERE time < ?")) {
                ps.setLong(1, cutoff);
                future.complete(ps.executeUpdate());
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private CompletableFuture<List<AuditEntry>> query(String sql, Binder binder) {
        CompletableFuture<List<AuditEntry>> future = new CompletableFuture<>();
        io.execute(() -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    List<AuditEntry> entries = new ArrayList<>();
                    while (rs.next()) {
                        ActionType type;
                        try {
                            type = ActionType.valueOf(rs.getString("type"));
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                        entries.add(new AuditEntry(
                                rs.getLong("id"),
                                rs.getLong("time"),
                                UUID.fromString(rs.getString("actor_uuid")),
                                rs.getString("actor_name"),
                                type,
                                rs.getString("detail"),
                                rs.getString("world"),
                                rs.getInt("x"),
                                rs.getInt("y"),
                                rs.getInt("z"),
                                rs.getLong("dur")));
                    }
                    future.complete(entries);
                }
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public void close() {
        io.shutdown();
        try {
            if (!io.awaitTermination(5, TimeUnit.SECONDS)) {
                io.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.warn("Failed to close audit database cleanly", e);
        }
    }
}
