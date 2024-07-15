package com.quattage.mechano.foundation.block.orientation.relative;

import com.simibubi.create.foundation.utility.Color;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import org.joml.Quaternionf;

/***
 * Represents a direction in the local space. Modifiable by global directions
 * to achieve rotations.
 */
public enum Relative {
    FRONT(0, 0, 0, Axis.Z, Direction.NORTH, 176, 48, 255),
    BACK(0, 180, 0, Axis.Z, Direction.SOUTH, 161, 117, 118),
    LEFT(0, 90, 0, Axis.X, Direction.WEST, 255, 48, 104),
    RIGHT(0, 270, 0, Axis.X, Direction.EAST, 204, 117, 140),
    TOP(270, 0, 0, Axis.Y, Direction.UP, 48, 255, 161),
    BOTTOM(90, 0, 0, Axis.Y, Direction.DOWN, 114, 176, 159);


    private final Quaternionf relMatrix;
    private final Axis followingAxis;
    private final Direction defaultDir;
    private final Color debugColor;

    private Relative(int x, int y, int z, Axis followingAxis, Direction defaultDir, int r, int g, int b) {
        this.relMatrix = new Quaternionf().rotateXYZ(x, y, z);
        this.followingAxis = followingAxis;
        this.defaultDir = defaultDir;
        this.debugColor = new Color(r, g, b);
    }

    private Relative(int x, int y, int z, Axis followingAxis, Direction defaultDir) {
        this.relMatrix = new Quaternionf().rotateXYZ(x, y, z);
        this.followingAxis = followingAxis;
        this.defaultDir = defaultDir;
        this.debugColor = null;
    }

    /***
     * Gets the Color of this RelativeDirection.
     * This is only used as a way to visually 
     * distinguish RelativeDirections for debugging.
     * @return
     */
    public Color getColor() {
        if(debugColor == null) return new Color(255, 255, 255);
        return debugColor;
    }

    public Direction getDefaultDir() {
        return defaultDir;
    }

    public Quaternionf getRelMatrix() {
        return relMatrix;
    }

    public Axis getAxis() {
        return followingAxis;
    }

    public Relative copy() {
        return Relative.values()[this.ordinal()];
    }

    public boolean equals(Relative other) {
        if(other == null) return false;
        return this.ordinal() == other.ordinal();
    }

    public void writeTo(CompoundTag in) {
        in.putInt(name(), ordinal());
    }

    public String toString() {
        return name();
    }

    public static Relative from(Direction dir) {
        return switch (dir) {
            case NORTH -> FRONT;
            case SOUTH -> BACK;
            case WEST -> LEFT;
            case EAST -> RIGHT;
            case UP -> TOP;
            case DOWN -> BOTTOM;
        };
    }
}
