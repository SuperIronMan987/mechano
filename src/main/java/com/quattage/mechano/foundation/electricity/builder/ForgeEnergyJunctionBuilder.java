package com.quattage.mechano.foundation.electricity.builder;

import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.foundation.electricity.WattBatteryHandlable;
import com.quattage.mechano.foundation.electricity.core.InteractionJunction;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;

/***
 * A fluent builder for ForgeEnergyJunctions
 */
public class ForgeEnergyJunctionBuilder<T extends SmartBlockEntity & WattBatteryHandlable> {

    private boolean isInput = true;
    private boolean isOutput = true;
    

    private boolean denyOrAllow = false;
    private Block[] blocksList = null;

    private final WattBatteryHandlerBuilder<T> base;
    private final Relative relative;

    

    public ForgeEnergyJunctionBuilder(WattBatteryHandlerBuilder<T> base, Relative relative) {
        this.base = base;
        this.relative = relative;
    }

    /***
     * This interaction will only send energy to adjacent blocks
     * @return This InteractionPolicyBuilder for chaining
     */
    public ForgeEnergyJunctionBuilder<T> onlySendEnergy() {
        isInput = false;
        isOutput = true;
        return this;
    }

    /***
     * This interaction will only recieve energy from adjacent blocks
     * @return This InteractionPolicyBuilder for chaining
     */
    public ForgeEnergyJunctionBuilder<T> onlyRecieveEnergy() {
        isInput = true;
        isOutput = false;
        return this;
    }

    public ForgeEnergyJunctionBuilder<T> sendsAndReceivesEnergy() {
        isInput = true;
        isOutput = true;
        return this;
    }

    /***
     * Blocks that are defined within the returned fluent substructure will be whitelisted
     * to the resulting InteractionPolicy.
     * @return A new BlockListBuilder within this InteractionPolicyBuilder.
     */
    public BlockListBuilder<T> onlyInteractsWith() {
        denyOrAllow = true;
        return new BlockListBuilder<T>(this);
    }

    /***
     * Blocks that are defined within this fluent substructure will be blacklisted
     * from the resulting InteractionPolicy.
     * @return A new BlockListBuilder within this InteractionPolicyBuilder.
     */
    public BlockListBuilder<T> interactsWithAllExcept() {
        denyOrAllow = false;
        return new BlockListBuilder<T>(this);
    }

    

    public WattBatteryHandlerBuilder<T> buildInteraction() {
        base.addInteraction(new InteractionJunction(new RelativeDirection(relative), isInput, isOutput, blocksList, denyOrAllow));
        return base;
    }

    @SuppressWarnings("unused")
    private class BlockListBuilder<R extends SmartBlockEntity & WattBatteryHandlable> {

        private final ForgeEnergyJunctionBuilder<R> base;
        private final ArrayList<Block> blocksList = new ArrayList<Block>();
    
        public BlockListBuilder(ForgeEnergyJunctionBuilder<R> base) {
            this.base = base;
        }
    
        /***
         * Adds a block to this BlockListBuilder
         * @param block
         * @return This BlockListBuilder for chaining
         */
        public BlockListBuilder<R> block(Block block) {
            blocksList.add(block);
            return this;
        }
    
        public ForgeEnergyJunctionBuilder<R> confirm() {
            base.blocksList = blocksList.toArray(new Block[0]);
            return base;
        }
    }
}


