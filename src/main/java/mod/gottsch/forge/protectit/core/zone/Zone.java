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
package mod.gottsch.forge.protectit.core.zone;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.util.UuidUtil;
import net.minecraft.nbt.CompoundTag;

/**
 * 
 * @author Mark Gottschling Mar 21, 2023
 *
 */
public class Zone {
	private static final String UUID_KEY = "uuid";
	private static final String NAME_KEY = "name";
	private static final String BOX_KEY = "box";
	private static final String PERMISSION_KEY = "permissions";
	private static final String CREATE_TIME_KEY = "createTime";

	public static final Zone EMPTY = new Zone(UuidUtil.EMPTY_UUID, "", Box.EMPTY);

	/**
	 * member properties
	 */

	// identity
	private UUID uuid;
	private String name;
	
	// location / dimensions
	private Box box;
	
	// event permssion
	private byte permissions;
	
	// other
	private long createTime;

	public Zone() {
		this(Box.EMPTY);
	}
	
	public Zone(Box box) {
		this(UUID.randomUUID(), "", box);
	}
	
	public Zone(UUID id, String name, Box box) {
		setUuid(id);
		setName(name);
		setBox(box);
	}
	
	/**
	 * 
	 * @param tag
	 */
	public void save(CompoundTag tag) {
		ProtectIt.LOGGER.debug("saving zone -> {}", this);

		tag.putUUID(UUID_KEY, getUuid());
		
		if (!StringUtils.isBlank(getName())) {
			tag.putString(NAME_KEY, getName());
		} else {
			tag.putString(NAME_KEY, "");
		}
		
		CompoundTag boxTag = new CompoundTag();
		getBox().save(boxTag);
		tag.put(BOX_KEY, boxTag);
		
		tag.putByte(PERMISSION_KEY, getPermissions());
		
		tag.putLong(CREATE_TIME_KEY, getCreateTime());
	}
	
	/**
	 * 
	 * @param tag
	 * @return
	 */
	public Zone load(CompoundTag tag) {
		if (tag.contains(UUID_KEY)) {
			setUuid(tag.getUUID(UUID_KEY));
		}
		else if (this.getUuid() == null) {
			setUuid(UUID.randomUUID());
		}
		
		if (tag.contains(NAME_KEY)) {
			setName(tag.getString(NAME_KEY));
		}
		
		if (tag.contains(BOX_KEY)) {
			setBox(Box.load(tag.getCompound(BOX_KEY)));
		}

		if (tag.contains(PERMISSION_KEY)) {
			setPermissions(tag.getByte(PERMISSION_KEY));
		}
		
		if (tag.contains(CREATE_TIME_KEY)) {
			setCreateTime(tag.getLong(CREATE_TIME_KEY));
		}
		return this;
	}
	/**
	 * 
	 * @param box2
	 * @return
	 */
	public boolean intersects(Box box2) {
		Box box = this.getBox();
		return box.getMinCoords().getX() <= box2.getMaxCoords().getX() && box.getMaxCoords().getX() >= box2.getMinCoords().getX() 
				&& box.getMinCoords().getY() <= box2.getMaxCoords().getY() && box.getMaxCoords().getY() >= box2.getMinCoords().getY()
				&& box.getMinCoords().getZ() <=  box2.getMaxCoords().getZ() && box.getMaxCoords().getZ() >= box2.getMinCoords().getZ();
	}
	
	/**
	 * General permission query.
	 * @param permission
	 * @return
	 */
	public boolean hasPermission(int permission) {
		return getPermission(permission) == 1;
	}

	/**
	 * 
	 * @param position
	 * @return
	 */
	public byte getPermission(int position) {
		return (byte) ((getPermissions() >> position) & 1);
	}

	/**
	 * 
	 * @param position
	 * @param value
	 */
	public void setPermission(int position, boolean value) {
		byte data = getPermissions();
		if (value) {
			data |= 1 << position;
		}
		else {
			data &= ~(1 << position);
		}
		setPermissions(data);
	}
	
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Box getBox() {
		return box;
	}

	public void setBox(Box box) {
		this.box = box;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public byte getPermissions() {
		return permissions;
	}

	public void setPermissions(byte permissions) {
		this.permissions = permissions;
	}
}
