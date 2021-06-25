package com.github.kbinani.randomcoords;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MovingObjectPositionBlock;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

class PacketModifier {
    public static void ModifyServerBoundPacket(PacketContainer packet, Point chunkOffset) {
        switch (packet.getType().name()) {
            case "POSITION":
            case "POSITION_LOOK":
            case "FLYING":
            case "VEHICLE_MOVE": {
                OffsetServerBoundDoublesBlock(packet, chunkOffset, 0, 2);
                break;
            }
            case "BLOCK_DIG":
            case "TILE_NBT_QUERY":
            case "SET_COMMAND_BLOCK":
            case "SET_JIGSAW":
            case "STRUCT":
            case "UPDATE_SIGN": {
                OffsetServerBoundBlockPosition(packet, chunkOffset, 0);
                break;
            }
            case "USE_ITEM": {
                MovingObjectPositionBlock current = packet.getMovingBlockPositions().read(0);
                BlockPosition blockPosition = current.getBlockPosition();
                BlockPosition changed = new BlockPosition(
                        blockPosition.getX() + (chunkOffset.x << 4),
                        blockPosition.getY(),
                        blockPosition.getZ() + (chunkOffset.z << 4));
                current.setBlockPosition(changed);
                packet.getMovingBlockPositions().write(0, current);
                break;
            }
        }
    }

    public static void ModifyClientBoundPacket(PacketContainer packet, Point chunkOffset, World world) {
        switch (packet.getType().name()) {
            case "SPAWN_POSITION":
            case "BLOCK_CHANGE":
            case "BLOCK_BREAK":
            case "SPAWN_ENTITY_PAINTING":
            case "BLOCK_ACTION":
            case "BLOCK_BREAK_ANIMATION":
            case "WORLD_EVENT":
            case "OPEN_SIGN_EDITOR": {
                OffsetClientBoundBlockPosition(packet, chunkOffset, 0);
                break;
            }
            case "POSITION":
            case "ENTITY_TELEPORT":
            case "SPAWN_ENTITY":
            case "SPAWN_ENTITY_LIVING":
            case "SPAWN_ENTITY_EXPERIENCE_ORB":
            case "WORLD_PARTICLES":
            case "NAMED_ENTITY_SPAWN": {
                OffsetClientBoundDoublesBlock(packet, chunkOffset, 0, 2);
                break;
            }
            case "MAP_CHUNK": {
                int bx = chunkOffset.x << 4;
                int bz = chunkOffset.z << 4;
                try {
                    List<NbtBase<?>> data = packet.getListNbtModifier().read(0);
                    for (int i = 0; i < data.size(); i++) {
                        NbtBase<?> nbt = data.get(i);
                        if (!(nbt instanceof NbtCompound)) {
                            continue;
                        }
                        NbtCompound compound = (NbtCompound) nbt;
                        if (compound.containsKey("x") && compound.containsKey("z")) {
                            int x = compound.getInteger("x") - bx;
                            int z = compound.getInteger("z") - bz;
                            compound.put("x", x);
                            compound.put("z", z);
                            data.set(i, compound);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ModifyClientBoundPacket(MAP_CHUNK): " + e.getMessage());
                }

                World.Environment dimension = world.getEnvironment();
                Biome biome;
                switch (dimension) {
                    case NETHER:
                        biome = Biome.NETHER_WASTES;
                        break;
                    case THE_END:
                        biome = Biome.END_MIDLANDS;
                        break;
                    default:
                        biome = Biome.PLAINS;
                        break;
                }
                int[] biomes = packet.getIntegerArrays().read(0);
                Arrays.fill(biomes, biome.ordinal());
                packet.getIntegerArrays().write(0, biomes);

                OffsetClientBoundIntegersChunk(packet, chunkOffset, 0, 1);
                break;
            }
            case "LIGHT_UPDATE":
            case "UNLOAD_CHUNK":
            case "VIEW_CENTRE": {
                OffsetClientBoundIntegersChunk(packet, chunkOffset, 0, 1);
                break;
            }
            case "NAMED_SOUND_EFFECT": {
                int bx = chunkOffset.x << 4;
                int bz = chunkOffset.z << 4;
                StructureModifier<Integer> modifier = packet.getIntegers();
                final int x = modifier.read(0);
                final int z = modifier.read(2);
                modifier.write(0, x - bx * 8);
                modifier.write(2, z - bz * 8);
                break;
            }
            case "MULTI_BLOCK_CHANGE": {
                SectionPositionModifier.Modify(packet, 0, -chunkOffset.x, 0, -chunkOffset.z);
                break;
            }
            case "EXPLOSION": {
                OffsetClientBoundDoublesBlock(packet, chunkOffset, 0, 2);
                StructureModifier<List<BlockPosition>> modifier = packet.getBlockPositionCollectionModifier();
                List<BlockPosition> original = modifier.read(0);
                if (original.size() > 0) {
                    final int bx = chunkOffset.x << 4;
                    final int bz = chunkOffset.z << 4;
                    List<BlockPosition> blockPositions = original.stream().map((position) -> {
                        return new BlockPosition(position.getX() - bx, position.getY(), position.getZ() - bz);
                    }).collect(Collectors.toList());
                    modifier.write(0, blockPositions);
                }
                break;
            }
            case "TILE_ENTITY_DATA": {
                OffsetClientBoundBlockPosition(packet, chunkOffset, 0);
                StructureModifier<NbtBase<?>> modifier = packet.getNbtModifier();
                NbtBase<?> data = modifier.read(0);
                if (data instanceof NbtCompound) {
                    NbtCompound compound = (NbtCompound) data;
                    if (compound.containsKey("x") && compound.containsKey("z")) {
                        int x = compound.getInteger("x") - (chunkOffset.x << 4);
                        int z = compound.getInteger("z") - (chunkOffset.z << 4);
                        compound.put("x", x);
                        compound.put("z", z);
                        modifier.write(0, data);
                    }
                }
                break;
            }
            case "WORLD_BORDER": {
                EnumWrappers.WorldBorderAction action = packet.getWorldBorderActions().read(0);
                if (action == EnumWrappers.WorldBorderAction.INITIALIZE || action == EnumWrappers.WorldBorderAction.SET_CENTER) {
                    OffsetClientBoundDoublesBlock(packet, chunkOffset, 0, 1);
                }
                break;
            }
            case "ENTITY_METADATA": {
                EntityMetadataModifier.Modify(packet, chunkOffset);
                break;
            }
            case "LOGIN": {
                SecureRandom rnd = new SecureRandom();
                packet.getLongs().write(0, rnd.nextLong());
                break;
            }
        }
    }

    public static PacketContainer ClonePacketPlayOutLightUpdate(PacketContainer packet) {
        try {
            StructureModifier<Integer> integers = packet.getIntegers();
            int chunkX = integers.read(0);
            int chunkZ = integers.read(1);

            Field cField = packet.getModifier().getField(2);
            BitSet c = (BitSet) cField.get(packet.getHandle());
            BitSet cCopy = BitSet.valueOf(c.toLongArray());

            Field dField = packet.getModifier().getField(3);
            BitSet d = (BitSet) dField.get(packet.getHandle());
            BitSet dCopy = BitSet.valueOf(d.toLongArray());

            Field eField = packet.getModifier().getField(4);
            BitSet e = (BitSet) eField.get(packet.getHandle());
            BitSet eCopy = BitSet.valueOf(e.toLongArray());

            Field fField = packet.getModifier().getField(5);
            BitSet f = (BitSet) fField.get(packet.getHandle());
            BitSet fCopy = BitSet.valueOf(f.toLongArray());

            Field gField = packet.getModifier().getField(6);
            gField.setAccessible(true);
            List<Byte> g = (List<Byte>) gField.get(packet.getHandle());
            ArrayList<Byte> gCopy = new ArrayList<>(g);

            Field hField = packet.getModifier().getField(7);
            hField.setAccessible(true);
            List<Byte> h = (List<Byte>) hField.get(packet.getHandle());
            ArrayList<Byte> hCopy = new ArrayList<>(h);

            boolean i = packet.getBooleans().read(0);

            PacketContainer cloned = new PacketContainer(PacketType.Play.Server.LIGHT_UPDATE);
            cloned.getModifier().write(0, chunkX);
            cloned.getModifier().write(1, chunkZ);
            cloned.getModifier().write(2, cCopy);
            cloned.getModifier().write(3, dCopy);
            cloned.getModifier().write(4, eCopy);
            cloned.getModifier().write(5, fCopy);
            cloned.getModifier().write(6, gCopy);
            cloned.getModifier().write(7, hCopy);
            cloned.getBooleans().write(0, i);
            return cloned;
        } catch (Exception e) {
            System.err.println("ClonePacketPlayOutLightUpdate: " + e.getMessage());
        }
        return packet.deepClone();
    }

    private static void OffsetServerBoundDoublesBlock(PacketContainer packet, Point chunkOffset, int xIndex, int zIndex) {
        StructureModifier<Double> modifier = packet.getDoubles();
        if (modifier.size() <= xIndex) {
            System.err.println("OffsetServerBoundDoublesBlock: xIndex out of bound. xIndex=" + xIndex + ", size=" + modifier.size());
            return;
        }
        if (modifier.size() <= zIndex) {
            System.err.println("OffsetServerBoundDoublesBlock: zIndex out of bound. zIndex=" + zIndex + ", size=" + modifier.size());
            return;
        }
        double cx = modifier.read(xIndex);
        double cz = modifier.read(zIndex);
        modifier.write(xIndex, cx + (chunkOffset.x << 4));
        modifier.write(zIndex, cz + (chunkOffset.z << 4));
    }

    private static void OffsetServerBoundBlockPosition(PacketContainer packet, Point chunkOffset, int index) {
        StructureModifier<BlockPosition> modifier = packet.getBlockPositionModifier();
        if (modifier.size() <= index) {
            System.err.println("OffsetServerBoundBlockPosition; index out of bound. index=" + index + ", size=" + modifier.size());
            return;
        }
        BlockPosition current = modifier.read(index);
        BlockPosition modified = new BlockPosition(
                current.getX() + (chunkOffset.x << 4),
                current.getY(),
                current.getZ() + (chunkOffset.z << 4));
        modifier.write(index, modified);
    }

    private static void OffsetClientBoundBlockPosition(PacketContainer packet, Point chunkOffset, int index) {
        StructureModifier<BlockPosition> modifier = packet.getBlockPositionModifier();
        if (modifier.size() <= index) {
            System.err.println("OffsetClientBoundBlockPosition; index out of bound. index=" + index + ", size=" + modifier.size());
            return;
        }
        BlockPosition current = modifier.read(index);
        BlockPosition modified = new BlockPosition(
                current.getX() - (chunkOffset.x << 4),
                current.getY(),
                current.getZ() - (chunkOffset.z << 4));
        modifier.write(index, modified);
    }

    private static void OffsetClientBoundIntegersChunk(PacketContainer packet, Point chunkOffset, int xIndex, int zIndex) {
        StructureModifier<Integer> modifier = packet.getIntegers();
        if (modifier.size() <= xIndex) {
            System.err.println("OffsetClientBoundIntegersChunk: xIndex out of bound. xIndex=" + xIndex + ", size=" + modifier.size());
            return;
        }
        if (modifier.size() <= zIndex) {
            System.err.println("OffsetClientBoundIntegersChunk: zIndex out of bound. zIndex=" + zIndex + ", size=" + modifier.size());
            return;
        }
        int cx = modifier.read(xIndex);
        int cz = modifier.read(zIndex);
        modifier.write(xIndex, cx - chunkOffset.x);
        modifier.write(zIndex, cz - chunkOffset.z);
    }

    private static void OffsetClientBoundDoublesBlock(PacketContainer packet, Point chunkOffset, int xIndex, int zIndex) {
        StructureModifier<Double> modifier = packet.getDoubles();
        if (modifier.size() <= xIndex) {
            System.err.println("OffsetClientBoundDoublesBlock: xIndex out of bound. xIndex=" + xIndex + ", size=" + modifier.size());
            return;
        }
        if (modifier.size() <= zIndex) {
            System.err.println("OffsetClientBoundDoublesBlock: zIndex out of bound. zIndex=" + zIndex + ", size=" + modifier.size());
            return;
        }
        double cx = modifier.read(xIndex);
        double cz = modifier.read(zIndex);
        modifier.write(xIndex, cx - (chunkOffset.x << 4));
        modifier.write(zIndex, cz - (chunkOffset.z << 4));
    }

    private static void Dump(PacketContainer packet) {
        System.out.println("======================================");
        System.out.println(packet.getType().name());
        System.out.println("--------------------------------------");
        System.out.println("    type=" + packet.getType().name());
        System.out.println("    handle=" + packet.getHandle());

        final StructureModifier<Object> modifier = packet.getModifier();
        for (Field f : modifier.getFields()) {
            String name = f.getName();
            try {
                Class<?> t = f.getType();
                String access = "";
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                    access = "private ";
                }
                Object v = f.get(packet.getHandle());
                System.out.println("    " + access + name + ": " + t + " = " + v);
            } catch (Exception e) {
                System.out.println("  " + name + ": (error); e=" + e);
            }
        }
    }
}
