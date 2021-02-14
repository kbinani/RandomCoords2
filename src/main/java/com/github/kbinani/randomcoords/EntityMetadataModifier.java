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
        StructureModifier<List<WrappedWatchableObject>> list = packet.getWatchableCollectionModifier();
        List<WrappedWatchableObject> objs = list.read(0);
        for (WrappedWatchableObject obj : objs) {
            int idx = obj.getIndex();
            Object v = obj.getRawValue();
            if (idx != 16) {
                continue;
            }
            if (!(v instanceof Optional)) {
                continue;
            }
            Optional<?> ov = (Optional<?>) v;
            if (!ov.isPresent()) {
                continue;
            }
            Object unwrapped = ov.get();
            Class<?> c = unwrapped.getClass();
            Constructor<?> ctor;
            Method getX;
            Method getY;
            Method getZ;
            try {
                ctor = c.getDeclaredConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
                getX = c.getMethod("getX");
                getY = c.getMethod("getY");
                getZ = c.getMethod("getZ");
            } catch (Exception e) {
                continue;
            }
            try {
                int x = (int) getX.invoke(unwrapped);
                int y = (int) getY.invoke(unwrapped);
                int z = (int) getZ.invoke(unwrapped);
                Object modified = ctor.newInstance(x - (chunkOffset.x << 4), y, z - (chunkOffset.z << 4));
                obj.setValue(Optional.of(modified));
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }
}
