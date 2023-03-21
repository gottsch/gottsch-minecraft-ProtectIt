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
package mod.gottsch.forge.protectit.core.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.google.common.collect.Maps;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.registry.PlayerIdentity;
import mod.gottsch.forge.protectit.core.util.UuidUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;


/**
 * 
 * @author Mark Gottschling on Nov 4, 2021
 *
 */
public class Property {
	private static final String NO_NAME = "";

	private static final String NAMES_KEY = "names";
	private static final String UUID_KEY = "uuid";
	private static final String OWNER_KEY = "owner";
	private static final String LORD_KEY = "lord";

	private static final String BOX_KEY = "box";

	private static final String WHITELIST_KEY = "whitelist";
	private static final String PERMISSION_KEY = "permissions";
	private static final String FIEFDOM_KEY = "fiefdom";
	private static final String PARENT_KEY = "parent";
	private static final String CHILDREN_KEY = "children";

	private static final String CREATE_TIME_KEY = "createTime";

	public static Property EMPTY = new Property(Coords.EMPTY, Box.EMPTY);

	/**
	 * member properties
	 */

	// identity
	private UUID uuid;
	private Map<UUID, String> names;

	// location / dimensions
	private Box box;

	// ownership
	private PlayerIdentity lord;
	private PlayerIdentity owner;
	private List<PlayerIdentity> whitelist;

	// event permssion
	private byte permissions;

	// subdivisible permission
	private boolean fiefdom;

	// property hierarchy
	private UUID parent;
	private List<UUID> children;

	// other
	private long createTime;


	/**
	 * Empty constructor
	 */
	public Property() {
		this(Coords.EMPTY, Box.EMPTY);
	}

	/**
	 * 
	 * @param coords
	 * @param box
	 */
	public Property(@Deprecated ICoords coords, Box box) {
		setBox(box);
		setLord(PlayerIdentity.EMPTY);
		setOwner(PlayerIdentity.EMPTY);
		setNameByOwner(NO_NAME);
		setUuid(UUID.randomUUID());
		// no permission granted
		setPermissions((byte)0);
	}

	public Property(@Deprecated ICoords coords, Box box, PlayerIdentity owner) {
		this(coords, box);
		setLord(owner);
		setOwner(owner);
	}

	public Property(@Deprecated ICoords coords, Box box, PlayerIdentity owner, String name) {
		this(coords, box, owner);
		setNameByOwner(name);
	}

	public Property(UUID uuid, String name, @Deprecated ICoords coords, Box box, PlayerIdentity owner) {
		this(coords, box);
		setUuid(uuid);
		setNameByOwner(name);
	}

	public boolean isDomain() {
		return !hasParent() && getLord().equals(getOwner());
	}

	public boolean isFief() {
		return hasParent();
	}

	public boolean isFiefAvailable() {
		return isFief() && (getLord().equals(getOwner()) || getOwner().equals(PlayerIdentity.EMPTY));
	}

	public boolean hasParent() {
		return getParent() != null && !getParent().equals(UuidUtil.EMPTY_UUID);
	}

	public boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	/**
	 * 
	 * @param tag
	 */
	public void save(CompoundTag tag) {
		ProtectIt.LOGGER.debug("saving property -> {}", this);

		tag.putUUID(UUID_KEY, getUuid());

		if (!getNames().isEmpty()) {
			ListTag listTag = new ListTag();
			getNames().forEach((key, value) -> {
				CompoundTag nameTag = new CompoundTag();
				nameTag.putUUID("key", key);
				nameTag.putString("value", value);
				listTag.add(nameTag);
			});
			tag.put(NAMES_KEY, listTag);
		}

		if (getLord() != null) {
			CompoundTag landlordTag = new CompoundTag();
			getLord().save(landlordTag);
			tag.put(LORD_KEY, landlordTag);
		}

		if (getOwner() != null) {
			CompoundTag ownerNbt = new CompoundTag();
			getOwner().save(ownerNbt);
			tag.put(OWNER_KEY, ownerNbt);
		}

		ListTag list = new ListTag();
		getWhitelist().forEach(data -> {
			CompoundTag playerNbt = new CompoundTag();
			data.save(playerNbt);
			list.add(playerNbt);
		});
		tag.put(WHITELIST_KEY, list);

		CompoundTag boxTag = new CompoundTag();
		getBox().save(boxTag);
		tag.put(BOX_KEY, boxTag);

		tag.putByte(PERMISSION_KEY, getPermissions());
		tag.putBoolean(FIEFDOM_KEY, isFiefdom());

		if (getParent() != null) {
			tag.putUUID(PARENT_KEY, getParent());
		}

		if (!getChildren().isEmpty()) {
			ListTag childrenTag = new ListTag();
			getChildren().forEach(child -> {
				CompoundTag childTag = new CompoundTag();
				//				child.save(childTag);
				childTag.putUUID(UUID_KEY, child);
				childrenTag.add(childTag);
			});
			tag.put(CHILDREN_KEY, childrenTag);
		}
		ProtectIt.LOGGER.debug("saving createTime -> {}", getCreateTime());
		tag.putLong(CREATE_TIME_KEY, getCreateTime());
	}

	/**
	 * 
	 * @param tag
	 * @return
	 */
	public Property load(CompoundTag tag) {
		if (tag.contains(UUID_KEY)) {
			setUuid(tag.getUUID(UUID_KEY));
		}
		else if (this.getUuid() == null) {
			setUuid(UUID.randomUUID());
		}

		if (tag.contains(NAMES_KEY)) {
			ListTag namesTag = tag.getList(NAMES_KEY, Tag.TAG_COMPOUND);
			namesTag.forEach(nameTag -> {
				getNames().put(((CompoundTag)nameTag).getUUID("key"), ((CompoundTag)nameTag).getString("value"));
			});
		}
		if (tag.contains(LORD_KEY)) {
			PlayerIdentity data = new PlayerIdentity();
			data.load(tag.getCompound(LORD_KEY));
			setLord(data);
		}
		if (tag.contains(OWNER_KEY)) {
			getOwner().load(tag.getCompound(OWNER_KEY));
		}
		if (tag.contains(WHITELIST_KEY)) {
			ListTag list = tag.getList(WHITELIST_KEY, 10);
			list.forEach(element -> {
				PlayerIdentity playerData = new PlayerIdentity();
				playerData.load((CompoundTag)element);
				getWhitelist().add(playerData);
			});
		}

		if (tag.contains(BOX_KEY)) {
			setBox(Box.load(tag.getCompound(BOX_KEY)));
		}

		if (tag.contains(PERMISSION_KEY)) {
			setPermissions(tag.getByte(PERMISSION_KEY));
		}

		if (tag.contains(FIEFDOM_KEY)) {
			setFiefdom(tag.getBoolean(FIEFDOM_KEY));
		}

		if (tag.contains(PARENT_KEY)) {
			setParent(tag.getUUID(PARENT_KEY));
		}

		if (tag.contains(CHILDREN_KEY)) {
			ListTag childrenTag = tag.getList(CHILDREN_KEY, Tag.TAG_COMPOUND);
			childrenTag.forEach(childTag -> {
				//				getChildren().add(new Property().load((CompoundTag)childTag));
				if (((CompoundTag)childTag).contains(UUID_KEY)) {
					getChildren().add( ((CompoundTag)childTag).getUUID(UUID_KEY) );
				}
			});
		}
		
		if (tag.contains(CREATE_TIME_KEY)) {
			setCreateTime(tag.getLong(CREATE_TIME_KEY));
			ProtectIt.LOGGER.debug("loading createTime -> nbt -> {}, object -> {}", tag.getLong(CREATE_TIME_KEY), getCreateTime());
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
	 * Specific interact permission query
	 * @return
	 */
	public boolean hasInteractPermission() {
		return getPermission(Permission.INTERACT_PERMISSION.value) == 1;
	}

	public boolean hasDoorPermission() {
		return getPermission(Permission.DOOR_INTERACT_PERMISSION.value) == 1;
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

	public byte getPermissions() {
		return permissions;
	}

	public void setPermissions(byte permissions) {
		this.permissions = permissions;
	}

	public PlayerIdentity getOwner() {
		return owner;
	}

	public void setOwner(PlayerIdentity owner) {
		if (this.owner != null) {
			// remove name by old owner
			String name = getNameByOwner();
			getNames().remove(this.owner.getUuid());
			// add name by new owner
			setName(owner.getUuid(), name);
		}
		this.owner = owner;
	}

	public List<PlayerIdentity> getWhitelist() {
		if (whitelist == null) {
			whitelist = new ArrayList<>();
		}
		return whitelist;
	}

	public void setWhitelist(List<PlayerIdentity> whitelist) {
		this.whitelist = whitelist;
	}

	public Box getBox() {
		return box;
	}

	public void setBox(Box box) {
		this.box = box;
	}

	public String getName() {
		return getNameByOwner();
	}

	public void setName(String name) {
		setNameByOwner(name);
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public boolean isFiefdom() {
		return fiefdom;
	}

	public void setFiefdom(boolean hotelable) {
		this.fiefdom = hotelable;
	}

	public PlayerIdentity getLord() {
		return lord;
	}

	public void setLord(PlayerIdentity lord) {
		this.lord = lord;
	}

	public List<UUID> getChildren() {
		if (children == null) {
			children = new ArrayList<>();
		}
		return children;
	}

	public void setChildren(List<UUID> children) {
		this.children = children;
	}

	public String getNameByOwner() {
		return getName(getOwner().getUuid());
	}

	public void setNameByOwner(String name) {
		setName(getOwner().getUuid(), name);
	}

	public String getNameByLandlord() {
		return getName(getLord().getUuid());
	}

	public void setNameByLandlord(String name) {
		setName(getLord().getUuid(), name);
	}

	public String getName(UUID owner) {
		if (getNames().containsKey(owner)) {
			return getNames().get(owner);
		}
		return NO_NAME;
	}

	public void setName(UUID owner, String name) {
		getNames().put(owner, name);
	}

	public Map<UUID, String> getNames() {
		if (names == null) {
			names = Maps.newHashMap();
		}
		return names;
	}

	public void setNames(Map<UUID, String> nameMap) {
		this.names = nameMap;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public UUID getParent() {
		return parent;
	}

	public void setParent(UUID parent) {
		this.parent = parent;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createTime, uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Property other = (Property) obj;
		return createTime == other.createTime && Objects.equals(uuid, other.uuid);
	}

	@Override
	public String toString() {
		return "Property [uuid=" + uuid + ", names=" + names + ", box=" + box + ", lord=" + lord + ", owner=" + owner
				+ ", whitelist=" + whitelist + ", permissions=" + permissions + ", fiefdom=" + fiefdom + ", parent="
				+ parent + ", children=" + children + ", createTime=" + createTime + "]";
	}	

}