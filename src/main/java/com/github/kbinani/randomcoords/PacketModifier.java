package com.github.kbinani.randomcoords;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MovingObjectPositionBlock;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

import java.util.List;
import java.util.stream.Collectors;

public class PacketModifier {
    public static void ModifyServerBoundPacket(PacketContainer packet, Point chunkOffset) {
        switch (packet.getType().name()) {
            case "POSITION":
            case "POSITION_LOOK":
            case "FLYING":
            case "VEHICLE_MOVE": {
                offsetServerBoundDoublesBlock(packet, chunkOffset, 0, 2);
                break;
            }
            case "BLOCK_DIG":
            case "TILE_NBT_QUERY":
            case "SET_COMMAND_BLOCK":
            case "SET_JIGSAW":
            case "STRUCT":
            case "UPDATE_SIGN": {
                offsetServerBoundBlockPosition(packet, chunkOffset, 0);
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

    public static void ModifyClientBoundPacket(PacketContainer packet, Point chunkOffset) {
        switch (packet.getType().name()) {
            case "SPAWN_POSITION":
            case "BLOCK_CHANGE":
            case "BLOCK_BREAK":
            case "SPAWN_ENTITY_PAINTING":
            case "BLOCK_ACTION":
            case "BLOCK_BREAK_ANIMATION":
            case "WORLD_EVENT":
            case "OPEN_SIGN_EDITOR": {
                offsetClientBoundBlockPosition(packet, chunkOffset, 0);
                break;
            }
            case "POSITION":
            case "ENTITY_TELEPORT":
            case "SPAWN_ENTITY":
            case "SPAWN_ENTITY_LIVING":
            case "SPAWN_ENTITY_EXPERIENCE_ORB":
            case "WORLD_PARTICLES":
            case "NAMED_ENTITY_SPAWN": {
                offsetClientBoundDoublesBlock(packet, chunkOffset, 0, 2);
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
                    System.err.println(e);
                }
                offsetClientBoundIntegersChunk(packet, chunkOffset, 0, 1);
                break;
            }
            case "LIGHT_UPDATE":
            case "UNLOAD_CHUNK":
            case "VIEW_CENTRE": {
                offsetClientBoundIntegersChunk(packet, chunkOffset, 0, 1);
                break;
            }
            case "NAMED_SOUND_EFFECT":
            case "ENTITY_SOUND": {
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
                SectionPositionModifier.modify(packet, 0, -chunkOffset.x, 0, -chunkOffset.z);
                break;
            }
            case "EXPLOSION": {
                offsetClientBoundDoublesBlock(packet, chunkOffset, 0, 2);
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
                offsetClientBoundBlockPosition(packet, chunkOffset, 0);
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
                    offsetClientBoundDoublesBlock(packet, chunkOffset, 0, 1);
                }
                break;
            }
        }

    }

    private static void offsetServerBoundDoublesBlock(PacketContainer packet, Point chunkOffset, int xIndex, int zIndex) {
        StructureModifier<Double> modifier = packet.getDoubles();
        if (modifier.size() <= xIndex) {
            System.err.println("offsetServerBoundDoublesBlock: xIndex out of bound. xIndex=" + xIndex + ", size=" + modifier.size());
            return;
        }
        if (modifier.size() <= zIndex) {
            System.err.println("offsetServerBoundDoublesBlock: zIndex out of bound. zIndex=" + zIndex + ", size=" + modifier.size());
            return;
        }
        double cx = modifier.read(xIndex);
        double cz = modifier.read(zIndex);
        modifier.write(xIndex, cx + (chunkOffset.x << 4));
        modifier.write(zIndex, cz + (chunkOffset.z << 4));
    }

    private static void offsetServerBoundBlockPosition(PacketContainer packet, Point chunkOffset, int index) {
        StructureModifier<BlockPosition> modifier = packet.getBlockPositionModifier();
        if (modifier.size() <= index) {
            System.err.println("offsetServerBoundBlockPosition; index out of bound. index=" + index + ", size=" + modifier.size());
            return;
        }
        BlockPosition current = modifier.read(index);
        BlockPosition modified = new BlockPosition(
                current.getX() + (chunkOffset.x << 4),
                current.getY(),
                current.getZ() + (chunkOffset.z << 4));
        modifier.write(index, modified);
    }

    private static void offsetClientBoundBlockPosition(PacketContainer packet, Point chunkOffset, int index) {
        StructureModifier<BlockPosition> modifier = packet.getBlockPositionModifier();
        if (modifier.size() <= index) {
            System.err.println("offsetClientBoundBlockPosition; index out of bound. index=" + index + ", size=" + modifier.size());
            return;
        }
        BlockPosition current = modifier.read(index);
        BlockPosition modified = new BlockPosition(
                current.getX() - (chunkOffset.x << 4),
                current.getY(),
                current.getZ() - (chunkOffset.z << 4));
        modifier.write(index, modified);
    }

    private static void offsetClientBoundIntegersChunk(PacketContainer packet, Point chunkOffset, int xIndex, int zIndex) {
        StructureModifier<Integer> modifier = packet.getIntegers();
        if (modifier.size() <= xIndex) {
            System.err.println("offsetClientBoundIntegersChunk: xIndex out of bound. xIndex=" + xIndex + ", size=" + modifier.size());
            return;
        }
        if (modifier.size() <= zIndex) {
            System.err.println("offsetClientBoundIntegersChunk: zIndex out of bound. zIndex=" + zIndex + ", size=" + modifier.size());
            return;
        }
        int cx = modifier.read(xIndex);
        int cz = modifier.read(zIndex);
        modifier.write(xIndex, cx - chunkOffset.x);
        modifier.write(zIndex, cz - chunkOffset.z);
    }

    private static void offsetClientBoundDoublesBlock(PacketContainer packet, Point chunkOffset, int xIndex, int zIndex) {
        StructureModifier<Double> modifier = packet.getDoubles();
        if (modifier.size() <= xIndex) {
            System.err.println("offsetClientBoundDoublesBlock: xIndex out of bound. xIndex=" + xIndex + ", size=" + modifier.size());
            return;
        }
        if (modifier.size() <= zIndex) {
            System.err.println("offsetClientBoundDoublesBlock: zIndex out of bound. zIndex=" + zIndex + ", size=" + modifier.size());
            return;
        }
        double cx = modifier.read(xIndex);
        double cz = modifier.read(zIndex);
        modifier.write(xIndex, cx - (chunkOffset.x << 4));
        modifier.write(zIndex, cz - (chunkOffset.z << 4));
    }
}
