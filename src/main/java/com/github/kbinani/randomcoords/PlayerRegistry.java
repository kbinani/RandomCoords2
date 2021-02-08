package com.github.kbinani.randomcoords;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry {
    private final int maxChunkOffset;
    private final Random random = new Random();
    private final ConcurrentHashMap<UUID, Point> players = new ConcurrentHashMap<>();

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
        UUID uuid = player.getUniqueId();
        Point chunkOffset = this.players.get(uuid);
        if (chunkOffset == null) {
            Location current = player.getLocation();
            Point offset = this.makeRandomChunkOffset();
            chunkOffset = new Point(current.getBlockX() + offset.x, current.getBlockZ() + offset.z);
            this.players.put(player.getUniqueId(), chunkOffset);
        }
        return chunkOffset;
    }

    private Point makeRandomChunkOffset() {
        int x = -this.maxChunkOffset + this.random.nextInt(2 * this.maxChunkOffset);
        int z = -this.maxChunkOffset + this.random.nextInt(2 * this.maxChunkOffset);
        return new Point(x, z);
    }
}
