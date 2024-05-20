package com.quattage.mechano.content.block.power.alternator.rotor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlockEntity;
import com.quattage.mechano.content.block.power.alternator.stator.AbstractStatorBlock;
import com.quattage.mechano.foundation.helper.shape.CircleGetter;
import com.quattage.mechano.foundation.helper.shape.ShapeGetter;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractRotorBlockEntity extends KineticBlockEntity {

    private final ShapeGetter circle = ShapeGetter.ofShape(CircleGetter.class).withRadius(getStatorRadius()).build();

    private byte statorCount = 0;

    @Nullable
    private BlockPos controllerPos = null;

    public AbstractRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
    protected void read(CompoundTag nbt, boolean clientPacket) {

        if(nbt.contains("cX")) {
            controllerPos = new BlockPos(
                nbt.getInt("cX"),
                nbt.getInt("cY"),
                nbt.getInt("cZ")
            );
        } else controllerPos = null;

        statorCount = nbt.getByte("sC");
        super.read(nbt, clientPacket);
    }

    @Override
    protected void write(CompoundTag nbt, boolean clientPacket) {

        if(controllerPos == null) {
            nbt.remove("cX");
            nbt.remove("cY");
            nbt.remove("cZ");
        } else {
            nbt.putInt("cX", controllerPos.getX());
            nbt.putInt("cY", controllerPos.getY());
            nbt.putInt("cZ", controllerPos.getZ());
        }

        nbt.putByte("sC", statorCount);

        super.write(nbt, clientPacket);
    }

    @Override
    public void initialize() {
        super.initialize();
        findConnectedStators(true);
    }

    protected void findConnectedStators(boolean notifyIfChanged) {

        final Set<BlockPos> visited = new HashSet<>();

        int oldCount = statorCount;
        statorCount = 0;
        circle.moveTo(getBlockPos()).setAxis(getBlockState().getValue(RotatedPillarBlock.AXIS)).evaluatePlacement(perimeterPos -> {
            
            if(visited.contains(perimeterPos)) return null;
            BlockState perimeterState = getLevel().getBlockState(perimeterPos);
            if(perimeterState.getBlock() instanceof AbstractStatorBlock asb) {
                if(asb.hasRotor(getLevel(), perimeterPos, perimeterState))
                    statorCount++;
            }

            visited.add(perimeterPos);
            return null;
        });

        if(notifyIfChanged && (oldCount != statorCount)) 
            notifyUpdate();
    }

    public void incStatorCount() {
        statorCount++;
        if(statorCount > getStatorCircumference()) {
            statorCount = (byte)getStatorCircumference();
            return;
        }

        getAndRefreshController();
        notifyUpdate();
    }

    public void decStatorCount() {
        statorCount--;
        if(statorCount < 0) {
            statorCount = 0;
            return;
        }

        getAndRefreshController();
        notifyUpdate();
    }

    private void getAndRefreshController() {
        if(controllerPos == null) return;
        if(getLevel().getBlockEntity(controllerPos) instanceof SlipRingShaftBlockEntity srbe)
            srbe.initialize();
    }

    public abstract int getStatorCircumference();
    protected abstract int getStatorRadius();

    protected BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        notifyUpdate();
    }

    protected void setStatorCount(byte statorCount) {
        this.statorCount = statorCount;
        notifyUpdate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Lang.text(statorCount + " stators").forGoggles(tooltip);
        Lang.text(controllerPos + "").forGoggles(tooltip);
        return true;
    }

    public byte getStatorCount() {
        return statorCount;
    }

    public int getMultiplier() {
        return 1;
    }
}
