package com.github.kbinani.randomcoords;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.GamePhase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class RandomCoords2 extends JavaPlugin implements Listener {
	private final PlayerRegistry registry = new PlayerRegistry(780);

	@Override
	public void onDisable() {
		ProtocolManager manager = ProtocolLibrary.getProtocolManager();
		manager.removePacketListeners(this);
		this.registry.clear();
	}

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);

		ProtocolManager manager = ProtocolLibrary.getProtocolManager();
		manager.addPacketListener(this.createServerBoundPacketListener());
		manager.addPacketListener(this.createClientBoundPacketListener());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event) {
		this.registry.forget(event.getPlayer());
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Location from = event.getFrom();
		Location to = event.getTo();
		if (to == null) {
			return;
		}
		World fromWorld = from.getWorld();
		World toWorld = to.getWorld();
		if (fromWorld == null || toWorld == null) {
			return;
		}
		Player player = event.getPlayer();
		switch (event.getCause()) {
			case NETHER_PORTAL:
			case END_PORTAL: {
				if (fromWorld.getUID().equals(toWorld.getUID())) {
					break;
				}
				this.registry.forgetWorld(player, fromWorld);
				break;
			}
			case PLUGIN: {
				if (!fromWorld.getUID().equals(toWorld.getUID())) {
					break;
				}
				try {
					int viewDistance = Math.min(48, player.getClientViewDistance());
					double blockDistance = from.distance(to);
					double chunkDistance = blockDistance / 16.0;
					if (chunkDistance >= viewDistance) {
						this.registry.forgetWorld(player, fromWorld);
					}
				} catch (Exception e) {
					getLogger().warning(e.getMessage());
				}
				break;
			}
		}
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		World from = player.getWorld();
		World to = event.getRespawnLocation().getWorld();
		if (to != null && from.getUID().equals(to.getUID())) {
			return;
		}
		this.registry.forgetWorld(player, from);
	}

	private PacketAdapter createClientBoundPacketListener() {
		PacketAdapter.AdapterParameteters options = PacketAdapter.params()
				.plugin(this)
				.connectionSide(ConnectionSide.SERVER_SIDE)
				.listenerPriority(ListenerPriority.HIGHEST)
				.gamePhase(GamePhase.BOTH)
				.types(PacketType.Play.Server.SPAWN_POSITION,
						PacketType.Play.Server.POSITION,
						PacketType.Play.Server.MAP_CHUNK,
						PacketType.Play.Server.LIGHT_UPDATE,
						PacketType.Play.Server.ENTITY_TELEPORT,
						PacketType.Play.Server.SPAWN_ENTITY,
						PacketType.Play.Server.SPAWN_ENTITY_LIVING,
						PacketType.Play.Server.BLOCK_CHANGE,
						PacketType.Play.Server.BLOCK_BREAK,
						PacketType.Play.Server.NAMED_SOUND_EFFECT,
						PacketType.Play.Server.MULTI_BLOCK_CHANGE,
						PacketType.Play.Server.UNLOAD_CHUNK,
						PacketType.Play.Server.VIEW_CENTRE,
						PacketType.Play.Server.EXPLOSION,
						PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
						PacketType.Play.Server.WORLD_PARTICLES,
						PacketType.Play.Server.SPAWN_ENTITY_PAINTING,
						PacketType.Play.Server.NAMED_ENTITY_SPAWN,
						PacketType.Play.Server.BLOCK_ACTION,
						PacketType.Play.Server.BLOCK_BREAK_ANIMATION,
						PacketType.Play.Server.WORLD_EVENT,
						PacketType.Play.Server.TILE_ENTITY_DATA,
						PacketType.Play.Server.OPEN_SIGN_EDITOR,
						PacketType.Play.Server.INITIALIZE_BORDER,
						PacketType.Play.Server.SET_BORDER_CENTER,
						PacketType.Play.Server.ENTITY_METADATA,
						PacketType.Play.Server.LOGIN,
						PacketType.Play.Server.VEHICLE_MOVE);
		final PlayerRegistry registry = this.registry;
		return new PacketAdapter(options) {
			@Override
			public void onPacketSending(PacketEvent event) {
				try {
					PacketContainer original = event.getPacket();
					PacketContainer packet;
					String type = original.getType().name();
					if (type.equals("LIGHT_UPDATE")) {
						packet = PacketModifier.ClonePacketPlayOutLightUpdate(original);
					} else if (type.equals("MULTI_BLOCK_CHANGE") || type.equals("LOGIN")) {
						packet = original.shallowClone();
					} else {
						packet = original.deepClone();
					}
					Player player = event.getPlayer();
					Point chunkOffset = registry.getChunkOffset(player);
					World world = player.getWorld();
					PacketModifier.ModifyClientBoundPacket(packet, chunkOffset, world);
					event.setPacket(packet);
				} catch (Exception e) {
					System.err.println("[sending.\"" + event.getPlayer().getName() + "\"."  + event.getPacket().getType().name() + "]" + e.toString());
				}
			}
		};
	}

	private PacketAdapter createServerBoundPacketListener() {
		PacketAdapter.AdapterParameteters options = PacketAdapter.params()
				.plugin(this)
				.connectionSide(ConnectionSide.CLIENT_SIDE)
				.listenerPriority(ListenerPriority.LOWEST)
				.gamePhase(GamePhase.BOTH)
				.types(PacketType.Play.Client.POSITION,
						PacketType.Play.Client.POSITION_LOOK,
						PacketType.Play.Client.BLOCK_DIG,
						PacketType.Play.Client.USE_ITEM,
						PacketType.Play.Client.GROUND,
						PacketType.Play.Client.TILE_NBT_QUERY,
						PacketType.Play.Client.VEHICLE_MOVE,
						PacketType.Play.Client.SET_COMMAND_BLOCK,
						PacketType.Play.Client.SET_JIGSAW,
						PacketType.Play.Client.STRUCT,
						PacketType.Play.Client.UPDATE_SIGN);
		final PlayerRegistry registry = this.registry;
		return new PacketAdapter(options) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				try {
					PacketContainer packet = event.getPacket();
					Point chunkOffset = registry.getChunkOffset(event.getPlayer());
					PacketModifier.ModifyServerBoundPacket(packet, chunkOffset);
					event.setPacket(packet);
				} catch (Exception e) {
					System.err.println("[receiving.\"" + event.getPlayer().getName() + "\"."  + event.getPacket().getType().name() + "]" + e.toString());
				}
			}
		};
	}
}
