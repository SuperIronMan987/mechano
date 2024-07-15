package com.quattage.mechano.foundation.electricity.builder;

import com.quattage.mechano.foundation.electricity.core.anchor.AnchorTransform;

/***
 * A fluent builder class that makes creating AnchorPoint instances safer
 * and somewhat more intuitive.
 */
public class AnchorPointBuilder {
    
    private AnchorBankBuilder<?> activeBuilder;
    private AnchorTransform location;
    private int maxConnections = 1;

    public AnchorPointBuilder(AnchorBankBuilder<?> activeBuilder) {
        this.activeBuilder = activeBuilder;
    }

    /***
     * The local offset of this node. Relative to the northern bottom corner of the block. <p>
     * This is based on pixel measurements (usually out of 16) rather than raw vectors. For example, 
     * if you wanted to place a node on the center of the block (which would normally be 0.5, 0.5, 0.5),
     * you would use:
     * <pre> AnchorPointBuilder.at(8, 8, 8); </pre>
     * Since pixel measurements are used here, you can just copy/paste coordinates directly from Blockbench.
     * Coordinates greater than 16 or less than 0 are permitted.
     * @param x x Offset from center (as an int or double)
     * @param y y Offset from center (as an int or double)
     * @param z z Offset from center (as an int or double)
     * @return this ElectircNodeBuilder with the modified value.
     */
    public AnchorPointBuilder at(int x, int y, int z) {
        location = new AnchorTransform(x, y, z, activeBuilder.getTarget().getBlockState());
        return this;
    }
    
    /***
     * The maximum amount of allowed connections to this AnchorPoint.
     * @param max
     * @return
     */
    public AnchorPointBuilder connections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public AnchorBankBuilder<?> build() {
        return activeBuilder.add(location, maxConnections);
    };
}
