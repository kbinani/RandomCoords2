package com.github.kbinani.randomcoords;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

class EntityMetadataModifier {
    public static void Modify(PacketContainer packet, Point chunkOffset) {
        try {
            StructureModifier<List<WrappedWatchableObject>> list = packet.getWatchableCollectionModifier();
            List<WrappedWatchableObject> objs = list.read(0);
            for (WrappedWatchableObject obj : objs) {
                int idx = obj.getIndex();
                Object v = obj.getRawValue();
                if (idx == 16) {
                    // Optional<BlockPosition>
                    if (v instanceof Optional) {
                        Optional<?> ov = (Optional<?>) v;
                        if (ov.isPresent()) {
                            Object unwrapped = ov.get();
                            Class<?> c = unwrapped.getClass();
                            Constructor<?> ctor = c.getDeclaredConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
                            Method getX = c.getMethod("getX");
                            int x = (int) getX.invoke(unwrapped);
                            Method getY = c.getMethod("getY");
                            int y = (int) getY.invoke(unwrapped);
                            Method getZ = c.getMethod("getZ");
                            int z = (int) getZ.invoke(unwrapped);
                            Object modified = ctor.newInstance(x - (chunkOffset.x << 4), y, z - (chunkOffset.z << 4));
                            obj.setValue(Optional.of(modified));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
