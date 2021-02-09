package com.github.kbinani.randomcoords;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class PlayerRegistry {
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
