package com.github.kbinani.randomcoords;

class Point {
    public final int x;
    public final int z;

    Point(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public String toString() {
        return "{" + this.x + "," + this.z + "}";
    }
}
