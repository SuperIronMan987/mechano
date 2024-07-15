package com.quattage.mechano.foundation.electricity.grid.landmarks;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/***
 * GID is used as an identifier for hashing GridVertex objects
 */
public class GID {

    private final BlockPos pos;
    private final int subIndex;

    public GID(BlockPos pos, int subIndex) {
        if(pos == null) throw new NullPointerException("Failed to create SVID - BlockPos is null!");
        this.pos = pos;
        this.subIndex = subIndex;
    }

    public GID(BlockPos pos) {
        if(pos == null) throw new NullPointerException("Failed to create SVID - BlockPos is null!");
        this.pos = pos;
        this.subIndex = -1;
    }

    public static GID of(CompoundTag nbt) {
        return new GID(new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z")), nbt.getInt("i"));
    }

    public static GID ofLinked(CompoundTag nbt) {
        return of(nbt);
    }

    public static boolean isValidTag(CompoundTag nbt) {
        if(nbt.isEmpty()) return false;
        return nbt.contains("x") && nbt.contains("y") && nbt.contains("z") && nbt.contains("i");
    }

    public String toString() {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + subIndex + "]";
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public int getSubIndex() {
        return subIndex;
    }

    public GID copy() {
        return new GID(pos, subIndex);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof GID other) {
            return other.pos.equals(this.pos) && other.subIndex == this.subIndex;
        }

        return false;
    }

    public boolean isIn(Set<GID> ids) {
        return ids.contains(this);
    }

    @Override
    public int hashCode() {
        return (pos.hashCode() * 31) + subIndex;
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("x", pos.getX());
        in.putInt("y", pos.getY());
        in.putInt("z", pos.getZ());
        in.putInt("i", subIndex);
        return in;
    }
}
