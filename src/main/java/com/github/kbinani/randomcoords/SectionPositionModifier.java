package com.github.kbinani.randomcoords;

import com.comphenix.protocol.events.PacketContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class SectionPositionModifier {
    public static void Modify(PacketContainer packet, int index, int dx, int dy, int dz) {
        try {
            Field sectionPositionField = packet.getModifier().getField(index);
            Class<?> sectionPositionType = sectionPositionField.getType();
            Object sectionPosition = sectionPositionField.get(packet.getHandle());
            Method getXMethod = sectionPositionType.getMethod("getX");
            Method getYMethod = sectionPositionType.getMethod("getY");
            Method getZMethod = sectionPositionType.getMethod("getZ");
            Constructor<?> constructor = sectionPositionType.getDeclaredConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
            constructor.setAccessible(true);
            int x = (int) getXMethod.invoke(sectionPosition);
            int y = (int) getYMethod.invoke(sectionPosition);
            int z = (int) getZMethod.invoke(sectionPosition);
            Object changed = constructor.newInstance(x + dx, y + dy, z + dz);
            packet.getModifier().write(index, changed);
        } catch (Exception e) {
            System.err.println("SectionPositionModifier.Modify: " + e.getMessage());
        }
    }
}
