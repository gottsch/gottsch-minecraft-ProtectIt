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
import mod.gottsch.forge.protectit.core.block.FoundationStoneBlock;
import mod.gottsch.forge.protectit.core.block.IBorderBlock;
import mod.gottsch.forge.protectit.core.block.ProtectItBlocks;
import mod.gottsch.forge.protectit.core.item.Deed;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.ParcelFactory;
import mod.gottsch.forge.protectit.core.parcel.ParcelUtil;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FoundationStoneBlockEntity extends BlockEntity {
    private static final String PARCEL_ID = "parcel_id";
    private static final String OWNER_ID = "owner_id";
    private static final String DEED_ID = "deed_id";
    private static final String SIZE = "size";
    private static final String OVERLAPS = "overlaps";
    private static final String EXPIRE_TIME = "expire_time";

    private static final int TICKS_PER_SECOND = 20;
    private static final int FIVE_SECONDS = 5 * TICKS_PER_SECOND;
    private static final int ONE_MINUTE = 60 * TICKS_PER_SECOND;
    private static final int FIVE_MINUTES = 5 * ONE_MINUTE;

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

    private List<Box> overlaps;

    private long expireTime;

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
            getLevel().setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /**
     * TODO size is wrong. it needs to be absolute. only the command and deed
     * need the negative. when calculating box. need to take absoluted.
     * @return
     */
    public Box getBox(ICoords coords) {
        return new Box(coords.add(getSize().getMinCoords()),
                coords.add(getSize().getMaxCoords()));
    }

    /**
     *
     */
    public void updateParcelBorder() {
        // find the parcel this belongs to
        // this list should contain at most 2 parcel (nation and citizen). find the least significant (smallest).
        List<Parcel> list = ParcelRegistry.find(new Coords(getBlockPos()), new Coords(getBlockPos()), false, true);
        ProtectIt.LOGGER.debug("found parcels -> {}", list);
        Optional<Parcel> parcelOptional = ParcelUtil.findLeastSignificant(list);
        if (parcelOptional.isPresent()) {
            Parcel parcel = parcelOptional.get();
            setParcelId(parcel.getId());
            setDeedId(parcel.getDeedId());
            setOwnerId(parcel.getOwnerId());
            setSize(parcel.getSize());
            setExpireTime(0);

        } else {
            if (getExpireTime() == 0) {
                setExpireTime(getLevel().getGameTime() + FIVE_MINUTES);
            }
        }

        // update the borders
        placeParcelBorder();
    }

    public void placeGoodParcelBorder() {
        placeParcelBorder(ProtectItBlocks.GOOD_BORDER.get().defaultBlockState());
    }

    public void placeParcelBorder() {
        // TODO make own method
        // determine what type of border to place, ie good, warn, bad
        Block borderBlock = ProtectItBlocks.GOOD_BORDER.get();
        Box box = getBox(new Coords(getBlockPos()));
        List<Parcel> overlaps = ParcelRegistry.find(box);// get the be box)
        if (!overlaps.isEmpty()) {
            // interrogate each parcel and determine if it is owned by me
            for (Parcel parcel : overlaps) {
                if (!parcel.getOwnerId().equals(getOwnerId())) {
                    borderBlock = ProtectItBlocks.BAD_BORDER.get();
                    break;
                }
            }
        }
        // TODO add WARN condition

        placeParcelBorder(borderBlock.defaultBlockState());
    }

    public void placeParcelBorder(BlockState state) {
        // TODO AIR should be a tag and can replace air, water, and BorderBlocks
        addParcelBorder(this, Blocks.AIR, state);
    }

    public void removeGoodParcelBorder() {
        removeParcelBorder(ProtectItBlocks.GOOD_BORDER.get(), Blocks.AIR.defaultBlockState());
    }

    public void removeParcelBorder() {
        // determine what type of border to place, ie good, warn, bad
        Block borderBlock = ProtectItBlocks.GOOD_BORDER.get();
        Box box = getBox(new Coords(getBlockPos()));
        List<Parcel> overlaps = ParcelRegistry.find(box);// get the be box)
        if (!overlaps.isEmpty()) {
            // interrogate each parcel and determine if it is owned by me
            for (Parcel parcel : overlaps) {
                if (!parcel.getOwnerId().equals(getOwnerId())) {
                    borderBlock = ProtectItBlocks.BAD_BORDER.get();
                    break;
                }
            }
        }
        // TODO add WARN condition

        removeParcelBorder(borderBlock, Blocks.AIR.defaultBlockState());
    }

    public void removeParcelBorder(Block block, BlockState state) {
        addParcelBorder(this, block, state);
    }

    private void addParcelBorder(FoundationStoneBlockEntity blockEntity, Block removeBlock, BlockState blockState) {
        Level level = blockEntity.getLevel();
        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(blockEntity.parcelId);
        ICoords coords = new Coords(this.getBlockPos());
        if (parcel.isPresent()) {
            coords = new Coords(parcel.get().getCoords());
        }
        Box box = blockEntity.getBox(coords);

        // only iterate over the outline coords
        for (int x = 0; x < blockEntity.getSize().getSize().getX(); x++) {
            BlockPos pos = box.getMinCoords().toPos().offset(x, 0, 0);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(0, blockEntity.getSize().getSize().getY()-1, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(0, 0, blockEntity.getSize().getSize().getZ()-1);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(0, blockEntity.getSize().getSize().getY()-1, blockEntity.getSize().getSize().getZ()-1);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }

        for (int z = 1; z < blockEntity.getSize().getSize().getZ()-1; z++) {
            BlockPos pos = box.getMinCoords().toPos().offset(0, 0, z);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(0, blockEntity.getSize().getSize().getY()-1, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(blockEntity.getSize().getSize().getX()-1, 0, 0);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(blockEntity.getSize().getSize().getX()-1, blockEntity.getSize().getSize().getY()-1, 0);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }

        for (int y = 1; y < blockEntity.getSize().getSize().getY()-1; y++) {
            BlockPos pos = box.getMinCoords().toPos().offset(0, y, 0);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(blockEntity.getSize().getSize().getX()-1, 0, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(0, 0, blockEntity.getSize().getSize().getZ()-1);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(blockEntity.getSize().getSize().getX()-1, 0, blockEntity.getSize().getSize().getZ()-1);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }
    }

    private void replaceParcelBorderBlock(Level level, BlockPos pos, Block removeBlock, BlockState blockState) {
        BlockState borderState = level.getBlockState(pos);
        if ((borderState instanceof IBorderBlock) || borderState.is(removeBlock) || borderState.canBeReplaced()) {
            level.setBlockAndUpdate(pos, blockState);
        }
    }

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

        ListTag list = new ListTag();
        getOverlaps().forEach(box -> {
            CompoundTag element = new CompoundTag();
            box.save(element);
            list.add(element);
        });
        tag.put(OVERLAPS, list);

        tag.putLong(EXPIRE_TIME, getExpireTime());
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
}
