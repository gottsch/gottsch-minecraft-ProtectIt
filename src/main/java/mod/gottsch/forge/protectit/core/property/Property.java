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
import mod.gottsch.forge.protectit.core.registry.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;


/**
 * 
 * @author Mark Gottschling on Nov 4, 2021
 *
 */
public class Property {
	public static Property EMPTY = new Property(Coords.EMPTY, Box.EMPTY);

	private static final String NO_NAME = "";
//	private static final String NAME_KEY = "name";
	private static final String NAMES_KEY = "names";
	private static final String UUID_KEY = "uuid";
	private static final String OWNER_KEY = "owner";
	private static final String COORDS_KEY = "coords";
	private static final String BOX_KEY = "box";
	private static final String WHITELIST_KEY = "whitelist";
	private static final String PERMISSION_KEY = "permissions";
	private static final String SUBDIVIDE_KEY = "subdivisible";
	private static final String PARENT_KEY = "parent";
	private static final String CHILDREN_KEY = "children";
	private static final String LANDLORD_KEY = "landlord";
	private static final String CREATE_TIME_KEY = "createTime";
	
	private UUID uuid;
//	private String name;
	private Map<UUID, String> names;
	private PlayerData landlord;
	private PlayerData owner;
	private List<PlayerData> whitelist;
	private ICoords coords;
	private Box box;
	
	// event permssion
	private byte permissions;
	
	// subdivisible permission
	private boolean subdivisible;

	private UUID parent;
	private List<Property> children;

	private long createTime;
	
	// TODO probably a good candidate for a Builder
	// TODO add equals, hashCode()

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
	public Property(ICoords coords, Box box) {
		setCoords(coords);
		setBox(box);
		setOwner(new PlayerData());
		setNameByOwner(NO_NAME);
		setUuid(UUID.randomUUID());
		setPermissions((byte)0); // ie no permission granted
	}

	public Property(ICoords coords, Box box, PlayerData data) {
		this(coords, box);
		setOwner(data);
	}
	
	public Property(ICoords coords, Box box, PlayerData data, String name) {
		this(coords, box, data);
		setNameByOwner(name);
	}
	
	@Deprecated
	public Property(ICoords coords, Box box, PlayerData data, String name, UUID uuid) {
		this(coords, box, data, name);
		setUuid(uuid);
	}
	
	public Property(UUID uuid, String name, ICoords coords, Box box, PlayerData data) {
		this(coords, box);
		setUuid(uuid);
		setNameByOwner(name);
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
		
		if (getLandlord() != null) {
			CompoundTag landlordTag = new CompoundTag();
			getLandlord().save(landlordTag);
			tag.put(LANDLORD_KEY, landlordTag);
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
		
		CompoundTag coordsNbt = new CompoundTag();
		getCoords().save(coordsNbt);
		tag.put(COORDS_KEY, coordsNbt);

		CompoundTag boxNbt = new CompoundTag();
		getBox().save(boxNbt);
		tag.put(BOX_KEY, boxNbt);
				
		tag.putByte(PERMISSION_KEY, getPermissions());
		tag.putBoolean(SUBDIVIDE_KEY, isSubdivisible());

		if (getParent() != null) {
			tag.putUUID(PARENT_KEY, getParent());
		}
		
		if (!getChildren().isEmpty()) {
			ListTag childrenTag = new ListTag();
			getChildren().forEach(child -> {
				CompoundTag childTag = new CompoundTag();
				child.save(childTag);
				childrenTag.add(childTag);
			});
			tag.put(CHILDREN_KEY, childrenTag);
			tag.putLong(CREATE_TIME_KEY, createTime);
		}
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
		if (tag.contains(LANDLORD_KEY)) {
			PlayerData data = new PlayerData();
			data.load(tag.getCompound(LANDLORD_KEY));
			setLandlord(data);
		}
		if (tag.contains(OWNER_KEY)) {
			getOwner().load(tag.getCompound(OWNER_KEY));
		}
		if (tag.contains(WHITELIST_KEY)) {
			ListTag list = tag.getList(WHITELIST_KEY, 10);
			list.forEach(element -> {
				PlayerData playerData = new PlayerData("");
				playerData.load((CompoundTag)element);
				getWhitelist().add(playerData);
			});
		}
		
		if (tag.contains(COORDS_KEY)) {
			setCoords(Coords.EMPTY.load(tag.getCompound(COORDS_KEY)));
		}
		if (tag.contains(BOX_KEY)) {
			setBox(Box.load(tag.getCompound(BOX_KEY)));
		}
		
		if (tag.contains(PERMISSION_KEY)) {
			setPermissions(tag.getByte(PERMISSION_KEY));
		}
		
		if (tag.contains(SUBDIVIDE_KEY)) {
			setSubdivisible(tag.getBoolean(SUBDIVIDE_KEY));
		}
		
		if (tag.contains(PARENT_KEY)) {
			setParent(tag.getUUID(PARENT_KEY));
		}
		
		if (tag.contains(CHILDREN_KEY)) {
			ListTag childrenTag = tag.getList(CHILDREN_KEY, Tag.TAG_COMPOUND);
			childrenTag.forEach(childTag -> {
				getChildren().add(new Property().load((CompoundTag)childTag));
			});
			
			if (tag.contains(CREATE_TIME_KEY)) {
				setCreateTime(tag.getLong(CREATE_TIME_KEY));
			}
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
	 * @deprecated check the value of ProtectionRegistries.block.getHotel()
	 * @return
	 */
//	@Deprecated
//	public boolean isHotel() {
//		return isHotelable() && hasChildren();
//	}
//
//
//	public boolean isRoom() {
//		return !isHotelable() && parent != null;
//	}
	
	/**
	 * Specific interact permission query
	 * @return
	 */
	public boolean hasInteractPermission() {
		return getPermission(Permission.INTERACT_PERMISSION.value) == 1;
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
	
	public PlayerData getOwner() {
		return owner;
	}

	public void setOwner(PlayerData owner) {
		if (this.owner != null) {
			// remove old name by owner
			String name = getNameByOwner();
			getNames().remove(UUID.fromString(this.owner.getUuid()));
			setName(UUID.fromString(owner.getUuid()), name);
		}
		this.owner = owner;
	}

	public List<PlayerData> getWhitelist() {
		if (whitelist == null) {
			whitelist = new ArrayList<>();
		}
		return whitelist;
	}

	public void setWhitelist(List<PlayerData> whitelist) {
		this.whitelist = whitelist;
	}

	public ICoords getCoords() {
		return coords;
	}

	public void setCoords(ICoords coords) {
		this.coords = coords;
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

//	@Override
//	public String toString() {
//		return "Claim [name=" + name + ", owner=" + owner + ", whitelist=" + whitelist + ", coords=" + coords + ", box="
//				+ box + "]";
//	}



	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public boolean isSubdivisible() {
		return subdivisible;
	}

	public void setSubdivisible(boolean hotelable) {
		this.subdivisible = hotelable;
	}
//
//	public ICoords getParent() {
//		return parent;
//	}
//
//	public void setParent(ICoords parent) {
//		this.parent = parent;
//	}
//
//	public boolean hasChildren() {
//		return children;
//	}



	public PlayerData getLandlord() {
		return landlord;
	}

	public void setLandlord(PlayerData tenant) {
		this.landlord = tenant;
	}

//	public Property getParent() {
//		if (parent == null) {
//			parent = EMPTY;
//		}
//		return parent;
//	}
//
//	public void setParent(Property parent) {
//		this.parent = parent;
//	}

	public List<Property> getChildren() {
		if (children == null) {
			children = new ArrayList<>();
		}
		return children;
	}

	public void setChildren(List<Property> children) {
		this.children = children;
	}

	public String getNameByOwner() {
		return getName(UUID.fromString(getOwner().getUuid()));
	}
	
	public void setNameByOwner(String name) {
		setName(UUID.fromString(getOwner().getUuid()), name);
	}
	
	public String getNameByLandlord() {
		return getName(UUID.fromString(getLandlord().getUuid()));
	}
	
	public void setNameByLandlord(String name) {
		setName(UUID.fromString(getLandlord().getUuid()), name);
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

	@Override
	public int hashCode() {
		return Objects.hash(coords, uuid);
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
		return Objects.equals(coords, other.coords) && Objects.equals(uuid, other.uuid);
	}

	@Override
	public String toString() {
		return "Property [uuid=" + uuid + ", names=" + names + ", landlord=" + landlord + ", owner=" + owner
				+ ", whitelist=" + whitelist + ", coords=" + coords + ", box=" + box + ", permissions=" + permissions
				+ ", subdivisible=" + subdivisible + "]";
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
}