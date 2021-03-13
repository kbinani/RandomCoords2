package com.github.kbinani.randomcoords;

import org.bukkit.Location;

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

    public void forgetWorld(UUID worldUid) {
        this.offsets.remove(worldUid);
    }

    private Point makeRandomChunkOffset() {
        int x = -this.maxChunkOffset + random.nextInt(2 * this.maxChunkOffset);
        int z = -this.maxChunkOffset + random.nextInt(2 * this.maxChunkOffset);
        return new Point(x, z);
    }
}
