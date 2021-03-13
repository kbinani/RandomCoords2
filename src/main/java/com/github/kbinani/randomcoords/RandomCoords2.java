package com.github.kbinani.randomcoords;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.GamePhase;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
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
						PacketType.Play.Server.WORLD_BORDER,
						PacketType.Play.Server.ENTITY_SOUND,
						PacketType.Play.Server.ENTITY_METADATA,
						PacketType.Play.Server.LOGIN);
		final PlayerRegistry registry = this.registry;
		return new PacketAdapter(options) {
			@Override
			public void onPacketSending(PacketEvent event) {
				try {
					PacketContainer original = event.getPacket();
					PacketContainer packet;
					if (original.getType().name().equals("LIGHT_UPDATE")) {
						packet = PacketModifier.ClonePacketPlayOutLightUpdate(original);
					} else {
						packet = original.deepClone();
					}
					Point chunkOffset = registry.getChunkOffset(event.getPlayer());
					PacketModifier.ModifyClientBoundPacket(packet, chunkOffset);
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
						PacketType.Play.Client.FLYING,
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
