

package com.quattage.mechano.foundation.electricity.rendering;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/***
 * Populates a given PoseStack with a dynamically generated WireModel
 */
public class WireModelRenderer {

    /***
     * Overall scale, or "thickness" of the wire
     */
    private static final float SCALE = 0.8f;

    /***
     * The wire's Level of Detail
     */
    private static final float LOD = 0.3f;

    /***
     * The maximum amount of iterations for a single wire. Used
     * to prevent lag or stack overflows in extreme edge cases 
     */
    private static final int LOD_LIMIT = 512;

    /***
     * Represents a hash, used as an identifier for a WireModel's place in the cache.
     */
    public static class BakedModelHashKey {
        private final int hash;

        public BakedModelHashKey(Vec3 fromPos, Vec3 toPos) {
            float verticalDistance = (float) (fromPos.y - toPos.y);
            float horizontalDistance = (float)new Vec3(fromPos.x, 0, fromPos.z)
                .distanceTo(new Vec3(toPos.x, 0, toPos.z));
            int hash = Float.floatToIntBits(verticalDistance);
            hash = 31 * hash + Float.floatToIntBits(horizontalDistance);
            this.hash = hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            BakedModelHashKey key = (BakedModelHashKey) obj;
            return hash == key.hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        public String toString() {
            return "[" + hash + "]";
        }
    }


    // HI HI HELLO
    // if you're reading this, pretty much all of this vector math has been ripped from
    // legoatoom's ConnectableChains mod: https://github.com/legoatoom/ConnectibleChains
    // It has been adapted in some subtle (rather distinct) ways, but it still remains 
    // extremely similar. This approach is really nice and I probably wouldn't have been
    // able to figure this out without direct reference from this mod.
    private final Object2ObjectOpenHashMap<BakedModelHashKey, WireModel> modelCache = new Object2ObjectOpenHashMap<>(256);
    public static final WireModelRenderer INSTANCE = new WireModelRenderer();

    /***
     * Renders a static wire. Builds this wire once. All successive calls use the Hashkey provided, 
     * rather than building the model from scratch every frame.
     * @param buffer VertexConsumer 
     * @param matrix PoseStack
     * @param key HashKey considering this model's start end end destinations
     * @param origin Vector with direction (orientation of wire) and magnitude (length of wire)
     * @param fromBlockLight Block Light at the starting position
     * @param toBlockLight Block Light at the destination position
     * @param fromSkyLight Sky light at the starting position
     * @param toSkyLight Sky light at the destination position
     */
    public void renderStatic(BakedModelHashKey key, VertexConsumer buffer, PoseStack matrix, Vector3f origin, 
        int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight, TextureAtlasSprite sprite) {

        WireModel model;
        if(modelCache.containsKey(key)) 
            model = modelCache.get(key);
        else {
            model = buildWireModel(1f, origin, true);
            modelCache.put(key, model);
        }
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight, sprite);
    }

    /***
     * Renders a dynamic wire (a wire that can move, doesn't use the cache at all)
     * This is useful for wires that aren't yet confirmed, or placed in-world.
     */
    public void renderDynamic(VertexConsumer buffer, PoseStack matrix, Vector3f origin, 
        int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight) {
        WireModel model = buildWireModel(1f, origin, false);
        if(model == null) return;
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight, null);
    }

    /***
     * Renders a dynamic wire (a wire that can move, doesn't use the cache at all)
     * This is useful for wires that aren't yet confirmed, or placed in-world.
     */
    public void renderDynamic(VertexConsumer buffer, PoseStack matrix, Vector3f origin, 
        float sagOverride, int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight) {
        WireModel model = buildWireModel(sagOverride, origin, false);
        if(model == null) return;
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight, null);
    }

    /***
     * Renders a dynamic wire (a wire that can move, doesn't use the cache at all)
     * This is useful for wires that aren't yet confirmed, or placed in-world.
     */
    public void renderDynamic(VertexConsumer buffer, PoseStack matrix, Vector3f origin, 
        int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight, boolean isGlowingRed, int alpha) {
        WireModel model = buildWireModel(1f, origin, false);
        if(model == null) return;
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight, isGlowingRed, alpha, null);
    }

    /***
     * Renders a dynamic wire (a wire that can move, doesn't use the cache at all)
     * This is useful for wires that aren't yet confirmed, or placed in-world.
     */
    public void renderDynamic(VertexConsumer buffer, PoseStack matrix, Vector3f origin, 
        float sagOverride, int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight, boolean isGlowingRed, int alpha) {
        WireModel model = buildWireModel(sagOverride, origin, false);
        if(model == null) return;
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight, isGlowingRed, alpha, null);
    }

    public void purgeCache() {
        modelCache.clear();
    }

    // builds a wire model and returns the result
    @Nullable
    private WireModel buildWireModel(float sagOverride,  Vector3f origin, boolean backface) {
        int capacity = (int)(2 * new Vec3(origin).lengthSqr());

        if(capacity <= 0 || capacity >= Integer.MAX_VALUE) return null;
        
        WireModel.WireBuilder builder = WireModel.builder(capacity);

        float dXZ = (float)Math.sqrt(origin.x() * origin.x() + origin.z() * origin.z());
        if(dXZ < 0.1) {
            buildVerticalWireCountour(builder, origin, 0.785398f, WireUV.SKEW_A);
            buildVerticalWireCountour(builder, origin, 2.35619f, WireUV.SKEW_B);
        } else {
            buildWireContour(builder, origin, sagOverride,  0.785398f, false, WireUV.SKEW_A, 1f, dXZ);
            buildWireContour(builder, origin, sagOverride, 2.35619f, false, WireUV.SKEW_B, 1f, dXZ);
            if(backface) {
                buildWireContour(builder, origin, sagOverride, 3.92699f, false, WireUV.SKEW_A, 1f, dXZ);
                buildWireContour(builder, origin, sagOverride, 5.49779f, false, WireUV.SKEW_B, 1f, dXZ);
            }
        }
        return builder.build();
    }

    // builds one elongated manifold that makes up one "face" of the wire. In this case,
    // the math breaks down as dX approaches 0, so perfectly vertical wires use a different, 
    // more simplified algorithm.
    private void buildVerticalWireCountour(WireModel.WireBuilder builder, 
        Vector3f vec, float angle, WireUV uv) {

        float contextualLength = 1f / LOD;
        float chainWidth = (uv.x1() - uv.x0()) / 16 * SCALE;

        Vector3f normal = new Vector3f((float)Math.cos(angle), 0, (float)Math.sin(Math.toRadians(angle)));
        normal.mul(chainWidth);

        Vector3f vert00 = new Vector3f(-normal.x() / 2, 0, -normal.z() / 2), vert01 = new Vector3f(vert00);
        vert01.add(normal);
        Vector3f vert10 = new Vector3f(-normal.x() / 2, 0, -normal.z() / 2), vert11 = new Vector3f(vert10);
        vert11.add(normal);

        float uvv0 = 0, uvv1 = 0;
        boolean lastIter_ = false;
        for (int segment = 0; segment < LOD_LIMIT; segment++) {
            if(vert00.y() + contextualLength >= vec.y()) {
                lastIter_ = true;
                contextualLength = vec.y() - vert00.y();
            }

            vert10.add(0, contextualLength * 3, 0);
            vert11.add(0, contextualLength * 3, 0);

            uvv1 += contextualLength / SCALE;

            builder.addVertex(vert00).withUV(uv.x0() / 16f, uvv0).next();
            builder.addVertex(vert01).withUV(uv.x1() / 16f, uvv0).next();
            builder.addVertex(vert11).withUV(uv.x1() / 16f, uvv1).next();
            builder.addVertex(vert10).withUV(uv.x0() / 16f, uvv1).next();

            if(lastIter_) break;

            uvv0 = uvv1;

            vert00.set(vert10);
            vert01.set(vert11);
        }
    }

    // builds one elongated manifold that makes up one "face" of the wire.
    // The other face (to create the X shape seen in-game) is done with
    // a second call to this method.
    private void buildWireContour(WireModel.WireBuilder builder, Vector3f vec, float sagOverride, 
        float angle, boolean inv, WireUV uv, float offset, float distanceXZ) {

        float animatedSag = (Mth.clamp(0.8267f * (float)Math.pow(1.06814f, distanceXZ), 0.5f, 4.8f)) * sagOverride;

        float realLength, desiredLength = 1 / (Mth.clamp(2.05118f * (float)Math.pow(0.882237f, distanceXZ), 0.8f, 2.8f));
        float distance = VectorHelper.getLength(vec);

        Vector3f vertA1 = new Vector3f(), vertA2 = new Vector3f(), 
            vertB2 = new Vector3f(), vertB1 = new Vector3f();

        Vector3f normal = new Vector3f(), rotAxis = new Vector3f(), 
            point0 = new Vector3f(), point1 = new Vector3f();

        float width = (uv.x1() - uv.x0()) / 16 * SCALE;
        float wrongDistanceFactor = distance / distanceXZ;
        animatedSag *= distance;

        float uvv0, uvv1 = 0, gradient, x, y;

        point0.set(0, (float) VectorHelper.drip2(0, distance, vec.y()), 0);
        gradient = (float) VectorHelper.drip2prime(0, distance, vec.y());
        normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
        normal.normalize();

        x = VectorHelper.estimateDeltaX(desiredLength, gradient);
        gradient = (float) VectorHelper.drip2prime(animatedSag, x * wrongDistanceFactor, distance, vec.y());
        y = (float) VectorHelper.drip2(animatedSag, x * wrongDistanceFactor, distance, vec.y());
        point1.set(x, y, 0);

        rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
        rotAxis.normalize();

        // float offsetC = (float)((width / 2) * Math.cos(angle));
        // float offsetS = (float)((width / 2) * Math.sin(angle));

        normal.rotateAxis(angle, rotAxis.x, rotAxis.y, rotAxis.z);
        normal.mul(width);
        vertB1.set(point0.x() - normal.x() / 2, point0.y() - normal.y() / 2, point0.z() - normal.z() / 2);
        vertB2.set(vertA1);
        vertB2.add(normal);

        realLength = point0.distance(point1);
    
        boolean lastIter = false;
        for (int segment = 0; segment < LOD_LIMIT; segment++) {

            rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
            rotAxis.normalize();

            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.rotateAxis(angle, rotAxis.x, rotAxis.y, rotAxis.z);
            normal.mul(width);

            vertA1.set(vertB1);
            vertA2.set(vertB2);

            vertB1.set(point1.x() - normal.x() / 2, point1.y() - normal.y() / 2, point1.z() - normal.z() / 2);
            vertB2.set(vertB1);
            vertB2.add(normal);

            uvv0 = uvv1;
            uvv1 = uvv0 + (realLength / SCALE) / 3;

            builder.addVertex(vertA1).withUV(uv.x0() / 16f, uvv0).next();
            builder.addVertex(vertA2).withUV(uv.x1() / 16f, uvv0).next();
            builder.addVertex(vertB2).withUV(uv.x1() / 16f, uvv1).next();
            builder.addVertex(vertB1).withUV(uv.x0() / 16f, uvv1).next();

            if(lastIter) break;

            point0.set(point1);

            x += VectorHelper.estimateDeltaX(desiredLength, gradient);
            if (x >= distanceXZ) {
                lastIter = true;
                x = distanceXZ;
            }

            gradient = (float) VectorHelper.drip2prime(animatedSag, x * wrongDistanceFactor, distance, vec.y());
            y = (float) VectorHelper.drip2(animatedSag, x * wrongDistanceFactor, distance, vec.y());

            point1.set(x, y, 0);
            realLength = point0.distance(point1);
        }
    }

    public static Vector3f getWireOffset(Vec3 start, Vec3 end) {
        Vector3f offset = end.subtract(start).toVector3f();
        offset.set(offset.x(), 0, offset.z());
        offset.normalize();
        offset.mul(1 / 64f);
        return offset;
    }

    public static int[] deriveLightmap(Level world, Vec3 from, Vec3 to) {
        return deriveLightmap(world, VectorHelper.toBlockPos(from), VectorHelper.toBlockPos(to));
    }

    public static int[] deriveLightmap(Level world, BlockPos from, BlockPos to) {
        int[] out = new int[4];

        out[0] = world.getBrightness(LightLayer.BLOCK, from);
        out[1] = world.getBrightness(LightLayer.BLOCK, to);
        out[2] = world.getBrightness(LightLayer.SKY, from);
        out[3] = world.getBrightness(LightLayer.SKY, to);

        return out;
    }
}
