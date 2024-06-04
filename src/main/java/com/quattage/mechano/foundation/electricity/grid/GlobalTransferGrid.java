package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.anchor.interaction.AnchorInteractType;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GIDPair;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

/***
 * The GlobalTransferGrid is an elevated level controller for LocalTransferGrids.
 * It offers functionality to intelligently manage a list of networks, where
 * individual networks are stored as subsystems which can be added to, removed from,
 * split, merged, repaired, etc.
 */
public class GlobalTransferGrid {

    private final ArrayList<LocalTransferGrid> subgrids = new ArrayList<LocalTransferGrid>();
    private final Level world;

    public GlobalTransferGrid(Level world) {
        if(world == null)
            throw new NullPointerException("Error instantiating new GlobalTransferGrid - World cannot be null!");
        if(world.isClientSide)
            throw new IllegalStateException("Error instantiating new GlobalTransferGrid - GlobalTransferGrid cannot be registered on the client!");
        this.world = world;
    }

    public static GlobalTransferGrid of(Level world) {
        if(world == null) throw new NullPointerException("Error getting GlobalTransferGrid - World is null!");
        if(world.isClientSide()) return null;
        LazyOptional<GlobalTransferGrid> network = world.getCapability(Mechano.CAPABILITIES.SERVER_GRID_CAPABILITY);
        if(!network.isPresent()) throw new RuntimeException("Error getting GlobalTransferGrid from " + world.dimension().location() 
            + " - No handler registered for this dimension!");
        GlobalTransferGrid realNetwork = network.orElseThrow(RuntimeException::new);
        return realNetwork;
    }

    protected void readFrom(CompoundTag in) {
        CompoundTag net = in.getCompound(getDimensionName());

        ListTag subs = net.getList("all", Tag.TAG_COMPOUND);
        Mechano.LOGGER.warn("Global network in [" + getDimensionName() + "] is reading " + subs.size() + " TransferNetworks from NBT");

        for(int x = 0; x < subs.size(); x++) {
            CompoundTag subsystem = subs.getCompound(x);
            Mechano.LOGGER.info("Adding a LocalTransferGrid containing the following data:\n" + subsystem);
            LocalTransferGrid sysToAdd = new LocalTransferGrid(this, subsystem, world);
            if(!sysToAdd.isEmpty()) subgrids.add(sysToAdd); 
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        out.put("all", writeAllSubsystems());
        in.put(getDimensionName(), out);
        return in;
    }

    private ListTag writeAllSubsystems() {
        ListTag out = new ListTag();
        for(LocalTransferGrid grid : subgrids)
            out.add(grid.writeTo(new CompoundTag()));
        return out;
    }

    public String getDimensionName() { 
        if(world == null) return "NONE";
        return world.dimension().location().toString();
    }

    public int getSubsystemCount() {
        return subgrids.size();
    }

    /***
     * Links two GridVerticies without question. If these vertices don't belong to an active grid,
     * a new grid is made from them. If both vertices are in independent subgrids, these
     * subgrids are merged.
     * @param idA
     * @param idB
     */
    public AnchorInteractType link(GID idA, GID idB, int wireType) {
        Pair<Integer, LocalTransferGrid> sysA = getSystemContaining(idA);
        Pair<Integer, LocalTransferGrid> sysB = getSystemContaining(idB);

        if(idA.equals(idB)) return AnchorInteractType.GENERIC;
        if(doesLinkExist(idA, idB)) return AnchorInteractType.LINK_EXISTS;

        BlockEntity beA = world.getBlockEntity(idA.getBlockPos());
        if(!(beA instanceof WireAnchorBlockEntity wbeA)) return AnchorInteractType.GENERIC;
        BlockEntity beB = world.getBlockEntity(idB.getBlockPos());
        if(!(beB instanceof WireAnchorBlockEntity wbeB)) return AnchorInteractType.GENERIC;

        if(sysA == null && sysB == null) {
            LocalTransferGrid newSystem = new LocalTransferGrid(this);

            GridVertex vA = new GridVertex(wbeA, newSystem, idA);
            GridVertex vB = new GridVertex(wbeB, newSystem, idB);

            newSystem.addVert(vA);
            newSystem.addVert(vB);
            newSystem.linkVerts(vA, vB, wireType, true);
            subgrids.add(newSystem);

        } else if(sysA != null && sysB == null) {
            
            sysA.getSecond().addVert(new GridVertex(wbeB, sysA.getSecond(), idB));
            sysA.getSecond().linkVerts(idA, idB, wireType, true);

        } else if(sysA == null && sysB != null) {
            sysB.getSecond().addVert(new GridVertex(wbeA, sysB.getSecond(), idA));
            sysB.getSecond().linkVerts(idA, idB, wireType, true);

        } else if(sysA.getFirst() == sysB.getFirst()) {
            sysA.getSecond().linkVerts(idA, idB, wireType, true);

        } else if(sysA.getFirst() != sysB.getFirst()) {

            subgrids.remove((int)sysA.getFirst());
            subgrids.remove(sysB.getSecond());
            LocalTransferGrid merged = LocalTransferGrid.ofMerged(this, true, sysA.getSecond(), sysB.getSecond());
            merged.linkVerts(idA, idB, wireType, false);
            merged.findAllPaths(true); 

            subgrids.add(merged);
        }

        return AnchorInteractType.LINK_ADDED;
    }

    /***
     * Removes the link between vertices at two given positions as long as they exist.
     * @throws NullPointerException if no node could be found at either given BlockPos
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(GID linkOne, GID linkTwo, boolean clean) {
        GridVertex nodeOne = getVertAt(linkOne);
        GridVertex nodeTwo = getVertAt(linkTwo);
        if(nodeOne == null) throw new NullPointerException("Failed to unlink GridVertex from a global context - " + 
            "No valid GridVertex at " + linkOne + " could be found! (first provided parameter)");

        if(nodeTwo == null) throw new NullPointerException("Failed to unlink GridVertex from a global context - " + 
            "No valid GridVertex at " + linkTwo + " could be found! (second provided parameter)");

        nodeOne.unlinkFrom(nodeTwo);
        nodeTwo.unlinkFrom(nodeOne);

        if(clean)
            declusterize();

    }

    public void findAndDestroyVertex(GID id, boolean shouldClean) {
        boolean modified = false;
        for(LocalTransferGrid grid : subgrids)
            if(grid.removeVert(id)) modified = true;
        if(modified && shouldClean) declusterize();
    }

    public void destroyVertex(GridVertex vert, boolean shouldClean) {
        if(vert.getOrFindParent().removeVert(vert.getID()) && shouldClean)
            declusterize(vert.getOrFindParent());
    }

    /***
     * Splits LocalTransferGrids by their discontinuities.
     * Called automatically whenever a node is removed
     * from the system.
     */
    private void declusterize() {
        ArrayList<LocalTransferGrid> evaluated = new ArrayList<>();
        for(LocalTransferGrid grid : subgrids) 
            evaluated.addAll(grid.trySplit());
        subgrids.clear();
        subgrids.addAll(evaluated);

        for(LocalTransferGrid g : subgrids)
            g.resetVertices();
    } 

    /***
     * Splits a given LocalTransferGrid by its discontinuities.
     * @param grid Grid to split
     */
    private void declusterize(LocalTransferGrid grid) {
        ArrayList<LocalTransferGrid> evaluated = new ArrayList<>();
        evaluated.addAll(grid.trySplit());
        subgrids.remove(grid);
        subgrids.addAll(evaluated);

        for(LocalTransferGrid g : subgrids)
            g.resetVertices();
    }

    /***
     * Gets the LocalTransferGrid that contains the given id, or null of one does not exist.
     * @param id
     * @return A pair, where the first member is the index of the LocalTransferGrid,
     * and the second member is the LocalTransferGrid itself
     */
    public Pair<Integer, LocalTransferGrid> getSystemContaining(GID id) {
        int x = 0;
        for(LocalTransferGrid grid : subgrids) {
            if(grid.getVert(id) != null) return Pair.of(x, grid);
            x++;
        }
        return null;
    }

    public GridVertex getVertAt(GID id) {
        for(LocalTransferGrid grid : subgrids) {
            GridVertex node = grid.getVert(id);
            if(node != null) return node;
        }
        return null;
    }

    private boolean doesLinkExist(GID idA, GID idB) {
        for(LocalTransferGrid subgrid : subgrids) 
            if(subgrid.getEdgeMap().containsKey(new GIDPair(idA, idB))) return true;
        return false;
    }

    /***
     * @return True if the vertex at the given GID is both present and has available connections. A Vertex is
     * assumed available if it does not currently exist.
     */
    public boolean isVertAvailable(GID id) {
        Pair<AnchorPoint, WireAnchorBlockEntity> anchorPair = AnchorPoint.getAnchorAt(world, id);
        if(anchorPair == null) return false;
        AnchorPoint anchor = anchorPair.getFirst();
        if(anchor == null) return false;

        GridVertex vert = anchorPair.getFirst().getParticipant();
        if(vert == null) vert = getVertAt(id);
        if(vert == null) return true;
        
        if(anchor.getMaxConnections() > vert.links.size()) return true;
        return false;
    }

    public Set<BlockPos> poolAllPositions() {

        Set<BlockPos> out = new HashSet<>();

        for(LocalTransferGrid subgrid : subgrids) {
            for(GridVertex vert : subgrid.allVerts())
                out.add(vert.getID().getBlockPos());
        }

        return out;
    }

    public int removeAllVertsAt(BlockPos pos) {
        int removed = 0;
        for(LocalTransferGrid subgrid : subgrids) {
            Iterator<GridVertex> matrixIterator = subgrid.allVerts().iterator();
            while(matrixIterator.hasNext()) {
                GridVertex vert = matrixIterator.next();
                if(vert.getID().getBlockPos().equals(pos)) {
                    vert.markRemoved();
                    matrixIterator.remove();
                    removed++;
                }
            }
            subgrid.cleanEdges();
            declusterize(subgrid);
        }
        
        return removed;
    }

    public void clear() {
        subgrids.clear();
    }

    public ArrayList<LocalTransferGrid> getSubgrids() {
        return subgrids;
    }

    public boolean isClient() {
        return world.isClientSide();
    }

    public Level getWorld() {
        return world;
    }

    public String toString() {
        String head = "[";
        if(subgrids.isEmpty()) return "[EMPTY]";

        String systems = "";
        for(int x = 0; x < subgrids.size(); x++) {
            systems += "System " + x + ": \n" + subgrids.get(x);
            if(x != subgrids.size() - 1)
                systems += "\t\n";
        }

        return head + "\n" + systems + "]";
    }
}