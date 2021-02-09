package com.github.kbinani.randomcoords;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class PlayerOffset {
    private final int maxChunkOffset;
    private final ConcurrentHashMap<UUID, Point> offsets = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    PlayerOffset(int maxChunkOffset) {
        this.maxChunkOffset = maxChunkOffset;
    }

    public Point getChunkOffset(UUID worldUid, Location current) {
        Point chunkOffset = this.offsets.get(worldUid);
        if (chunkOffset == null) {
            Point offset = this.makeRandomChunkOffset();
            chunkOffset = new Point(current.getBlockX() + offset.x, current.getBlockZ() + offset.z);
            this.offsets.put(worldUid, chunkOffset);
        }
        return chunkOffset;
    }

    private Point makeRandomChunkOffset() {
        int x = -this.maxChunkOffset + random.nextInt(2 * this.maxChunkOffset);
        int z = -this.maxChunkOffset + random.nextInt(2 * this.maxChunkOffset);
        return new Point(x, z);
    }
}

public class PlayerRegistry {
    private final int maxChunkOffset;
    private final ConcurrentHashMap<UUID, PlayerOffset> players = new ConcurrentHashMap<>();

    PlayerRegistry(int maxChunkOffset) {
        this.maxChunkOffset = maxChunkOffset;
    }

    public void clear() {
        this.players.clear();
    }

    public void forget(Player player) {
        this.players.remove(player.getUniqueId());
    }

    public Point getChunkOffset(Player player) {
        UUID playerUid = player.getUniqueId();
        UUID worldUid = player.getWorld().getUID();
        Location current = player.getLocation();
        PlayerOffset offset = this.players.get(playerUid);
        if (offset == null) {
            offset = new PlayerOffset(this.maxChunkOffset);
            this.players.put(playerUid, offset);
        }
        return offset.getChunkOffset(worldUid, current);
    }
}
