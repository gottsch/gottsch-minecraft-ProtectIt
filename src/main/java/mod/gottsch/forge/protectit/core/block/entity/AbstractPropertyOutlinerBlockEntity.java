/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.item.Deed;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 
 * @author Mark Gottschling Mar 5, 2023
 *
 */
public abstract class AbstractPropertyOutlinerBlockEntity extends BlockEntity {

	private static final int TICKS_PER_SECOND = 20;
	protected static final int FIVE_SECONDS = 5 * TICKS_PER_SECOND;
	
	private static final String PROPERTY_COORDS_TAG = "propertyCoords";
	private static final String PROPERTY_UUID_TAG = "propertyUuid";
	private static final String PROPERTY_BOX_TAG = "propertyBox";

	@Deprecated
	private ICoords propertyCoords;
	private UUID propertyUuid;
	/*
	 *  the size of the property.
	 *  NOTE including the size is a better method for client-side rendering as
	 *   it doesn't rely on the BlockRegistry on the client.
	 *   this can replace the propertyCoords as they are contained in the box.
	 */
	private Box propertyBox;
	
	public AbstractPropertyOutlinerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	
	/**
	 * 
	 */
	public void tickServer() {
		// fetch overlaps from protection registry every 5 seconds
		if (getLevel().getGameTime() % FIVE_SECONDS == 0) {
			ICoords c1 = new Coords(getBlockPos());
			Box box = new Box(c1);
			List<Box> protections = ProtectionRegistries.block().getProtections(box.getMinCoords(), box.getMaxCoords(), true, false);
			if (!protections.isEmpty()) {
				// get current properties
				UUID previousUuid = getPropertyUuid();

				// get the property
				List<Property> properties = protections.stream().map(b -> ProtectionRegistries.block().getPropertyByCoords(b.getMinCoords())).collect(Collectors.toList());
				Optional<Property> property = selectProperty(properties, box);

				if (property.isPresent()) {
					setPropertyCoords(property.get().getCoords());
					setPropertyUuid(property.get().getUuid());
					setPropertyBox(property.get().getBox());
				}
				else {
					setPropertyCoords(Coords.EMPTY);
					setPropertyUuid(Deed.EMPTY_UUID);
					setPropertyBox(Box.EMPTY);
				}
				// if the value has changed then send an update.
				if (!getPropertyUuid().equals(previousUuid)) {
					getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
				}
			}
		}
	}

	protected abstract Optional<Property> selectProperty(List<Property> properties, Box box);

	/**
	 * 
	 */
	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (getPropertyCoords() != null) {
			CompoundTag coordsTag = new CompoundTag();
			getPropertyCoords().save(coordsTag);
			tag.put(PROPERTY_COORDS_TAG, coordsTag);
		}
		if (getPropertyUuid() != null) {
			tag.putUUID(PROPERTY_UUID_TAG, getPropertyUuid());
		}
		if (getPropertyBox() != null) {
			CompoundTag boxTag = new CompoundTag();
			getPropertyBox().save(boxTag);
			tag.put(PROPERTY_BOX_TAG, boxTag);
		}
	}
	
	/**
	 * 
	 */
	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains(PROPERTY_COORDS_TAG)) {
			setPropertyCoords(Coords.EMPTY.load(tag.getCompound(PROPERTY_COORDS_TAG)));
		}
		if (tag.contains(PROPERTY_UUID_TAG)) {
			setPropertyUuid(tag.getUUID(PROPERTY_UUID_TAG));
		}
		if (tag.contains(PROPERTY_BOX_TAG)) {
			setPropertyBox(Box.load(tag.getCompound(PROPERTY_BOX_TAG)));
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
	 * collect data to send to client
	 */
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = new CompoundTag(); //super.getUpdateTag();
		saveAdditional(nbt);
		return nbt;
	}
	
	/*
	 * handle on client
	 */
	@Override
	public void handleUpdateTag(CompoundTag tag) {
		//super.handleUpdateTag(state, tag);
		load(tag);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
	
	public ICoords getPropertyCoords() {
		return propertyCoords;
	}

	public void setPropertyCoords(ICoords propertyCoords) {
		this.propertyCoords = propertyCoords;
	}

	public UUID getPropertyUuid() {
		return propertyUuid;
	}

	public void setPropertyUuid(UUID propertyUuid) {
		this.propertyUuid = propertyUuid;
	}

	public Box getPropertyBox() {
		return propertyBox;
	}

	public void setPropertyBox(Box box) {
		this.propertyBox = box;
	}
}
