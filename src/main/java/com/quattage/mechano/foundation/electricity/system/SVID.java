package com.quattage.mechano.foundation.electricity.system;

import net.minecraft.core.BlockPos;

/***
 * SVID (System Vertex ID) is a (not really) lightweight class for downmixing a SystemVertex 
 * into only its barest components for storage in datasets that require hashing.
 */
public class SVID {


    private final BlockPos pos;
    private final int subIndex;
    
    public SVID(SystemVertex vert) {
        if(vert == null) throw new NullPointerException("Failed to create SVID - SystemVertex is null!");
        this.pos = vert.getPos();
        this.subIndex = vert.getSubIndex();
    }

    public SVID(BlockPos pos, int subIndex) {
        if(pos == null) throw new NullPointerException("Failed to create SVID - BlockPos is null!");
        this.pos = pos;
        this.subIndex = subIndex;
    }

    public SVID(BlockPos pos) {
        if(pos == null) throw new NullPointerException("Failed to create SVID - BlockPos is null!");
        this.pos = pos;
        this.subIndex = -1;
    }

    public String toString() {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + subIndex + "]";
    }

    public SystemVertex toVertex() {
        return new SystemVertex(pos, subIndex);
    }

    public BlockPos getPos() {
        return pos;
    }

    public SVID copy() {
        return new SVID(pos, subIndex);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SVID other) {
            return other.pos.equals(this.pos) && other.subIndex == this.subIndex;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}
