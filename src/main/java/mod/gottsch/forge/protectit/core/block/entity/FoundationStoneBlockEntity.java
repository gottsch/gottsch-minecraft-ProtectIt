/*
 * This file is part of  Protect It.
 * Copyright (c) 2021 Mark Gottschling (gottsch)
 *
 * All rights reserved.
 *
 * Protect It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Protect It is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Protect It.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.protectit.core.block.entity;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.*;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.item.Deed;
import mod.gottsch.forge.protectit.core.parcel.NationParcel;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.ParcelType;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import mod.gottsch.forge.protectit.core.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;

public class FoundationStoneBlockEntity extends BlockEntity {
    private static final String PARCEL_ID = "parcel_id";
    private static final String OWNER_ID = "owner_id";
    private static final String DEED_ID = "deed_id";
    private static final String NATION_ID = "nation_id";
    private static final String PARCEL_TYPE = "parcel_type";
    private static final String COORDS = "coords";
    private static final String SIZE = "size";
    private static final String OVERLAPS = "overlaps";
    private static final String EXPIRE_TIME = "expire_time";
    private static final String HAS_PARCEL = "has_parcel";

    private static final int TICKS_PER_SECOND = 20;
    private static final int FIVE_SECONDS = 5 * TICKS_PER_SECOND;
    private static final int ONE_MINUTE = 60 * TICKS_PER_SECOND;
    private static final int FIVE_MINUTES = 5 * ONE_MINUTE;

    private static final Map<OUTLINE, Pair<Block, Block>> OUTLINES = new HashMap<>();
    private enum OUTLINE { GOOD, BAD, GOOD_NATION;}

    // TODO replace/add state to Border Block -> PERSONAL, NATION, CITIZEN
    static {
        OUTLINES.put(OUTLINE.GOOD, Pair.of(ProtectItBlocks.PERSONAL_BORDER.get(), ProtectItBlocks.BUFFER.get()));
//        OUTLINES.put(OUTLINE.BAD, Pair.of(ProtectItBlocks.BAD_BORDER.get(), ProtectItBlocks.BAD_BARRIER.get()));
        OUTLINES.put(OUTLINE.GOOD_NATION, Pair.of(ProtectItBlocks.NATION_BORDER.get(), ProtectItBlocks.BUFFER.get()));

    }

    /*
     * relative sizing coords around (0, 0, 0)
     * ie a size of (0, -5, 0) -> (5, 5, 5) = (5, 11, 5).
     * when foundation stone is at (1, 1, 1), then the box
     * is (1, -4, 1) -> (6, 6, 6).
     */
    private Box size;

    private UUID parcelId;

    private UUID ownerId;

    private UUID deedId;
    private UUID nationId;

    private String parcelType;
    private ICoords coords;

    private List<Box> overlaps;

    private long expireTime;

    private boolean hasParcel;

    public FoundationStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ProtectItBlockEntities.FOUNDATION_STONE.get(), pos, state);
    }

    /**
     * Used to update the collection of parcels that this cornerstone's size
     * and position intersects with.
     */
    public void tickServer() {

        // find intersects from parcel registry every 5 seconds
//        if (getLevel().getGameTime() % FIVE_SECONDS == 0) {
//            FoundationStoneBlock block = (FoundationStoneBlock)getLevel().getBlockState(getBlockPos()).getBlock();
//            List<Box> overlaps = ParcelRegistry.findBoxes(size.getMinCoords(), size.getMaxCoords(), false, false);
//            getOverlaps().clear();
//            if (!overlaps.isEmpty()) {
//                getOverlaps().addAll(overlaps);
//            }
//        }

        if (getLevel().getGameTime() % ONE_MINUTE == 0) {
            updateParcelBorder();
        }

        // if there is an expire time (non-claimed parcel/foundation stone)
        // and game time has exceeded the expire time, then remove borders and foundation stone.
        if (getExpireTime() != 0 && getLevel().getGameTime() > getExpireTime()) {
            removeParcelBorder();
            getLevel().setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), getBufferSize());
        }
    }

    /**
     *
     * @return
     */
    public Box getBox(ICoords coords) {
        return new Box(coords.add(getSize().getMinCoords()),
                coords.add(getSize().getMaxCoords()));
    }

    public Box getDisplayBox(ICoords coords) {

        ParcelType parcelType = getParcelType() != null ? ParcelType.valueOf(getParcelType()) : ParcelType.PERSONAL;

        Box box;
        // check for nation block and make the box only +/-10 in height
        if (parcelType == ParcelType.NATION) {
            box = new Box(coords.add(getSize().getMinCoords().withY(-10)),
                    coords.add(getSize().getMaxCoords().withY(9))); // 10-1
        }
        else {
            box = new Box(coords.add(getSize().getMinCoords()),
                    coords.add(getSize().getMaxCoords()));
        }
        return box;
    }

    public int getBufferSize() {
        ParcelType parcelType = getParcelType() != null ? ParcelType.valueOf(getParcelType()) : ParcelType.PERSONAL;
        return switch (parcelType) {
            case PERSONAL -> Config.GENERAL.parcelBufferRadius.get();
            case NATION -> Config.GENERAL.nationParcelBufferRadius.get();
            case CITIZEN -> 0;
        };

    }
    /**
     *
     */
    public void updateParcelBorder() {
        // find the parcel this belongs to. this list should contain at most 2 parcel (nation and citizen).
        // find the least significant (smallest).
        Optional<Parcel> parcelOptional = ParcelRegistry.findLeastSignificant(new Coords(getBlockPos()));
        if (parcelOptional.isPresent()) {
            ProtectIt.LOGGER.debug("least significant parcel -> {}", parcelOptional.get());
            Parcel parcel = parcelOptional.get();
            parcel.populateBlockEntity(this);
            setExpireTime(0);
            setHasParcel(true);
        } else {
            if (hasParcel()) {
                removeParcelBorder(getCoords());
                setHasParcel(false);
            }
            else if (getExpireTime() == 0) {
                setExpireTime(getLevel().getGameTime() + FIVE_MINUTES);
            }
        }

        // update the borders
        placeParcelBorder();
    }

    public void placeParcelBorder() {
        Level level = getLevel();
        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());

        ICoords coords;
        int bufferRadius = 1;
        if (parcel.isPresent()) {
            ProtectIt.LOGGER.debug("place parcel border, parcel by id -> {}", parcel.get());
            coords = parcel.get().getCoords();
            if (parcel.get() instanceof NationParcel) {
                coords = coords.withY(getBlockPos().getY());
            }
            bufferRadius = parcel.get().getBufferSize();
        } else {
            coords = new Coords(this.getBlockPos());
            bufferRadius = getBufferSize();
        }
        ProtectIt.LOGGER.debug("using coords for outlines -> {}", coords);
        //        Pair<Block, Block> outlines = getOutlineBlocks();
        // add the border
        Box box = getDisplayBox(coords);
        BlockState borderState = getBorderBlockState(box);
        placeParcelBorder(box, borderState);

        // inflate the box
        box = ModUtil.inflate(box, bufferRadius);
        BlockState bufferState = getBufferBlockState(box);
        placeParcelBorder(box, bufferState);
    }

    public void placeParcelBorder(Box box, BlockState state) {
        // TODO AIR should be a tag and can replace air, water, and BorderBlocks
        addParcelBorder(box, Blocks.AIR, state);
    }

    public void removeParcelBorder() {
        Level level = getLevel();
        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());
        ICoords coords = new Coords(this.getBlockPos());
        if (parcel.isPresent()) {
            coords = new Coords(parcel.get().getCoords());
            if (parcel.get() instanceof NationParcel) {
                coords = coords.withY(getBlockPos().getY());
            }
        }
        removeParcelBorder(coords);
    }

    public void removeParcelBorder(ICoords coords) {
//        Pair<Block, Block> outlines = getOutlineBlocks();
        Box box = getDisplayBox(coords);
        BlockState borderState = getBorderBlockState(box);

        addParcelBorder(box, borderState.getBlock(), Blocks.AIR.defaultBlockState());
        box = ModUtil.inflate(box, getBufferSize());
//        BlockState bufferState = getBufferBlockState(box);
        addParcelBorder(box, ProtectItBlocks.BUFFER.get(), Blocks.AIR.defaultBlockState());
    }

    /**
     *
     * @param removeBlock
     * @param blockState
     */
    private void addParcelBorder(Block removeBlock, BlockState blockState) {
        Level level = getLevel();
        // TODO this portion doesn't work in the case where this stone had a parcel
        // but then the parcel is removed.  the findByParcelId will return null
        // and the wrong coords will be used to remove the border.
        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());
        ICoords coords = new Coords(this.getBlockPos());
        if (parcel.isPresent()) {
            coords = new Coords(parcel.get().getCoords());
        }
        // add the border
        Box box = getDisplayBox(coords);
        addParcelBorder(box, removeBlock, blockState);
    }

    /**
     *
     * @param box
     * @param removeBlock
     * @param blockState
     */
    private void addParcelBorder(Box box, Block removeBlock, BlockState blockState) {
//        Box box = getBox(coords);

        /* NOTE the for loops.
         * for x is "<=" because the Box was reduced by 1 during creation to ensure
         * it is the right size when including the origin.
         * thus y & z are "<" because we are iterating 2 less (1 on each side) because
         * the border is already generated by the x for loop.
         */
        // only iterate over the outline coords
        for (int x = 0; x < ModUtil.getSize(box).getX(); x++) {
            BlockPos pos = box.getMinCoords().toPos().offset(x, 0, 0);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(0, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(0, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(0, ModUtil.getSize(box).getY()-1, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }

        for (int z = 1; z < ModUtil.getSize(box).getZ(); z++) {
            BlockPos pos = box.getMinCoords().toPos().offset(0, 0, z);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(0, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(ModUtil.getSize(box).getX()-1, 0, 0);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(ModUtil.getSize(box).getX()-1, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }

        for (int y = 1; y < ModUtil.getSize(box).getY(); y++) {
            BlockPos pos = box.getMinCoords().toPos().offset(0, y, 0);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(ModUtil.getSize(box).getX()-1, 0, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(0, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(ModUtil.getSize(box).getX()-1, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }
    }

    private void replaceParcelBorderBlock(Level level, BlockPos pos, Block removeBlock, BlockState blockState) {
        BlockState borderState = level.getBlockState(pos);
        if ((borderState instanceof IBorderBlock) || borderState.is(removeBlock) || borderState.canBeReplaced()) {
            level.setBlockAndUpdate(pos, blockState);
        }
    }

    /**
     *
     * @return
     */
    protected BlockState getBorderBlockState(Box box) {
        // determine parcel type
        ParcelType parcelType = getParcelType() != null ? ParcelType.valueOf(getParcelType()) : ParcelType.PERSONAL;
        BlockState blockState = switch(parcelType) {
            case PERSONAL -> ProtectItBlocks.PERSONAL_BORDER.get().defaultBlockState();
            case CITIZEN -> ProtectItBlocks.CITIZEN_BORDER.get().defaultBlockState();
            case NATION -> ProtectItBlocks.NATION_BORDER.get().defaultBlockState();
        };

        /*
         * determine if there are overlaps with the buffered parcels
         */
        // compare against the buffer registry
        List<Parcel> overlaps = ParcelRegistry.findBuffer(box).stream().filter(p -> !p.getId().equals(getParcelId())).toList();
        // TODO turn this check into a method
        // TODO this totally doesn't work for Citizen deeds
        if (!overlaps.isEmpty()) {
            // interrogate each parcel and determine if it is the same parcel ie placing a foundation stone within a parcel
            for (Parcel parcel : overlaps) {
                // TODO this works fine for overlaps of non-nation parcels.
                // TODO but for nation parcel, need to check that this deed is totally within it.
                // TODO so need different checks depending on the parcelType of BE.
                if (parcel.getOwnerId().equals(getOwnerId())) {
                    // the parcels are owned by the same person. they can be closer or touching,
                    // ie. ignore buffers, only the parcels themselves can't overlap
                    List<Parcel> ownedOverlaps = ParcelRegistry.find(box).stream().filter(p -> !p.getId().equals(getParcelId())).toList();
                    if (!ownedOverlaps.isEmpty()) {
                        blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                    }
                } else {
                    blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                }
                break;
//                }
            }
        }
        return blockState;
    }

    protected BlockState getBufferBlockState(Box box) {
        // there is only 1 kind of buffer block currently
        BlockState blockState = ProtectItBlocks.BUFFER.get().defaultBlockState();

        /*
         * determine if the buffer overlaps with the parcels
         */

        // compare against the registry
        List<Parcel> overlaps = ParcelRegistry.find(box).stream().filter(p -> !p.getId().equals(getParcelId())).toList();
        // TODO turn this check into a method
        if (!overlaps.isEmpty()) {
            // interrogate each parcel and determine if it is the same parcel ie placing a foundation stone within a parcel
            for (Parcel parcel : overlaps) {
                // the parcels are owned by the same person. they can be closer or touching,
                // ie. ignore buffers, only the parcels themselves can't overlap
                if (!parcel.getOwnerId().equals(getOwnerId())) {
                    blockState = blockState.setValue(BufferBlock.INTERSECTS, BorderStatus.BAD);
                }
                break;
            }
        }
        return blockState;
    }

//    @Deprecated
//    protected Block getBorderBlock() {
//        Block borderBlock = ProtectItBlocks.PERSONAL_BORDER.get();
//        Box box = getBox(new Coords(getBlockPos()));
//        List<Parcel> overlaps = ParcelRegistry.find(box);// get the be box)
//        if (!overlaps.isEmpty()) {
//            // interrogate each parcel and determine if it is the same parcel
//            for (Parcel parcel : overlaps) {
//                if (!parcel.getId().equals(getParcelId())) {
//                    borderBlock = ProtectItBlocks.PERSONAL_BORDER.get();
//                    break;
//                }
//            }
////            borderBlock = ProtectItBlocks.BAD_BORDER.get();
//        }
//        return borderBlock;
//    }
//
//    /**
//     * TODO may redo this with new blocks ie placedBorder, placedBarrier, ownedBorder, ownedBarrier.
//     * @return
//     */
//    @Deprecated
//    protected Pair<Block, Block> getOutlineBlocks() {
//        MutablePair<Block, Block> outlines = MutablePair.of(OUTLINES.get(OUTLINE.GOOD).getLeft(), OUTLINES.get(OUTLINE.GOOD).getRight());
//
//        Box box = getBox(new Coords(getCoords()));
//        ProtectIt.LOGGER.debug("box from coords -> {}", box);
//        Box inflatedBox = ModUtil.inflate(box, getBufferSize());
//
//        List<Parcel> overlaps = ParcelRegistry.find(box);
//        if (!overlaps.isEmpty()) {
//            // interrogate each parcel and determine if it is the same parcel
//            for (Parcel parcel : overlaps) {
//                if (!parcel.getId().equals(getParcelId())) {
//                    outlines.setLeft(OUTLINES.get(OUTLINE.BAD).getLeft());
//                    break;
//                }
//            }
//        }
//
//        overlaps = ParcelRegistry.find(inflatedBox);
//        if (!overlaps.isEmpty()) {
//            // interrogate each parcel and determine if it is the same parcel
//            for (Parcel parcel : overlaps) {
//                if (!parcel.getId().equals(getParcelId())) {
////                    borderBlock = ProtectItBlocks.BAD_BORDER.get();
//                    outlines.setRight(OUTLINES.get(OUTLINE.BAD).getRight());
//                    break;
//                }
//            }
//
//        }
//        return outlines;
////        return !overlaps.isEmpty() ? OUTLINES.get(OUTLINE.BAD) : OUTLINES.get(OUTLINE.GOOD);
//    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (ObjectUtils.isNotEmpty(getSize())) {
            CompoundTag sizeTag = new CompoundTag();
            getSize().save(sizeTag);
            tag.put(SIZE, sizeTag);
        }

        if (ObjectUtils.isNotEmpty(getParcelId())) {
            tag.putUUID(PARCEL_ID, getParcelId());
        }

        if (ObjectUtils.isNotEmpty(getOwnerId())) {
            tag.putUUID(OWNER_ID, getOwnerId());
        }

        if (ObjectUtils.isNotEmpty(getDeedId())) {
            tag.putUUID(DEED_ID, getDeedId());
        }

        if (ObjectUtils.isNotEmpty(getNationId())) {
            tag.putUUID(NATION_ID, getNationId());
        }

        if (StringUtils.isNotBlank(getParcelType())) {
            tag.putString(PARCEL_TYPE, getParcelType());
        }

        if (ObjectUtils.isNotEmpty(getCoords())) {
            CompoundTag coordsTag = new CompoundTag();
            getCoords().save(coordsTag);
            tag.put(COORDS, coordsTag);
        }

        ListTag list = new ListTag();
        getOverlaps().forEach(box -> {
            CompoundTag element = new CompoundTag();
            box.save(element);
            list.add(element);
        });
        tag.put(OVERLAPS, list);

        tag.putLong(EXPIRE_TIME, getExpireTime());

        tag.putBoolean(HAS_PARCEL, hasParcel());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains(SIZE)) {
            setSize(Box.load(tag.getCompound(SIZE)));
        } else {
            setSize(Deed.DEFAULT_SIZE);
            ProtectIt.LOGGER.warn("size of parcel was not found. using default size.");
        }

        if (tag.contains(PARCEL_ID)) {
            setParcelId(tag.getUUID(PARCEL_ID));
        }
        if (tag.contains(OWNER_ID)) {
            setOwnerId(tag.getUUID(OWNER_ID));
        }
        if (tag.contains(DEED_ID)) {
            setDeedId(tag.getUUID(DEED_ID));
        }
        if (tag.contains(NATION_ID)) {
            setNationId(tag.getUUID(NATION_ID));
        }
        if (tag.contains(PARCEL_TYPE)) {
            setParcelType(tag.getString(PARCEL_TYPE));
        }
        if (tag.contains(COORDS)) {
            setCoords(Coords.EMPTY.load((CompoundTag) tag.get(COORDS)));
        }
        getOverlaps().clear();
        if (tag.contains(OVERLAPS)) {
            ListTag list = tag.getList(OVERLAPS, Tag.TAG_COMPOUND);
            list.forEach(element -> {
                Box box = Box.load((CompoundTag)element);
                if (box != null) {
                    getOverlaps().add(box);
                }
            });
        }

        if (tag.contains(EXPIRE_TIME)) {
            setExpireTime(tag.getLong(EXPIRE_TIME));
        }

        if (tag.contains(HAS_PARCEL)) {
            setHasParcel(tag.getBoolean(HAS_PARCEL));
        }
    }

    /*
     * Get the render bounding box. Typical block is 1x1x1.
     */
    @Override
    public AABB getRenderBoundingBox() {
        // always render regardless if TE is in FOV.
        return INFINITE_EXTENT_AABB;
    }

    /**
     * Sync client and server states
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag != null) {
            load(tag);
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        handleUpdateTag(tag);
    }

    public UUID getParcelId() {
        return parcelId;
    }

    public void setParcelId(UUID parcelId) {
        this.parcelId = parcelId;
    }

    public UUID getDeedId() {
        return deedId;
    }

    public void setDeedId(UUID deedId) {
        this.deedId = deedId;
    }

    public UUID getNationId() {
        return nationId;
    }

    public void setNationId(UUID nationId) {
        this.nationId = nationId;
    }

    public String getParcelType() {
        return parcelType;
    }

    public void setParcelType(String parcelType) {
        this.parcelType = parcelType;
    }

    public Box getSize() {
        return size;
    }

    public void setSize(Box size) {
        this.size = size;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public ICoords getCoords() {
        return coords;
    }

    public void setCoords(ICoords coords) {
        this.coords = coords;
    }

    //    @Override
    public List<Box> getOverlaps() {
        if (overlaps == null) {
            overlaps = new ArrayList<>();
        }
        return overlaps;
    }

    //    @Override
    public void setOverlaps(List<Box> overlaps) {
        this.overlaps = overlaps;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public boolean hasParcel() {
        return hasParcel;
    }

    public void setHasParcel(boolean hasParcel) {
        this.hasParcel = hasParcel;
    }
}
