package com.quattage.mechano.content.block.power.alternator.rotor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BigRotorBlockEntity extends AbstractRotorBlockEntity {

    public BigRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        //TODO Auto-generated constructor stub
    }

    @Override
    public int getMultiplier() {
        return 10;
    }

    @Override
    public int getStatorCircumference() {
        return 12;
    }

    @Override
    public int getStatorRadius() {
        return 2;
    }

    @Override
    protected float getEfficiencyBonus() {
        return 1.3f;
    }
}
