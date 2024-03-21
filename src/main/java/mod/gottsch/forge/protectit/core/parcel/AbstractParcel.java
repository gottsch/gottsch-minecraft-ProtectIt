/*
 * This file is part of  Protect It.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.protectit.core.parcel;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 17, 2024
 *
 */
public abstract class AbstractParcel implements Parcel {
    public static final String NAME_KEY = "name";
    public static final String ID_KEY = "id";
    public static final String OWNER_KEY = "owner";
    public static final String DEED_KEY = "deed";

    public static final String COORDS_KEY = "coords";
    public static final String SIZE_KEY = "size";
//    public static final String WHITELIST_KEY = "whitelist";

    public static final String TYPE = "type";

    private UUID id;
    private UUID ownerId;
    private UUID deedId;
    private String name;
    private ICoords coords;
    private Box size;

    @Override
    public void save(CompoundTag tag) {
        ProtectIt.LOGGER.debug("saving parcel -> {}", this);

        tag.putUUID(ID_KEY, getId());
        tag.putString(NAME_KEY, getName());
        tag.putUUID(OWNER_KEY, getOwnerId());
        tag.putUUID(DEED_KEY, getDeedId());
        CompoundTag coordsTag = new CompoundTag();
        getCoords().save(coordsTag);
        tag.put(COORDS_KEY, coordsTag);

        CompoundTag sizeTag = new CompoundTag();
        getSize().save(sizeTag);
        tag.put(SIZE_KEY, sizeTag);


//        ListTag list = new ListTag();
//        getWhitelist().forEach(data -> {
//            CompoundTag playerNbt = new CompoundTag();
//            data.save(playerNbt);
//            list.add(playerNbt);
//        });
//        tag.put(WHITELIST_KEY, list);
    }

    @Override
    public Parcel load(CompoundTag tag) {
        // TODO load the common
        if (tag.contains(ID_KEY)) {
            setId(tag.getUUID(ID_KEY));
        } else if (this.getId() == null) {
            setId(UUID.randomUUID());
        }
        if (tag.contains(NAME_KEY)) {
            setName(tag.getString(NAME_KEY));
        }
        if (tag.contains(OWNER_KEY)) {
            setOwnerId(tag.getUUID(OWNER_KEY));
        }
        if (tag.contains(DEED_KEY)) {
            setId(tag.getUUID(DEED_KEY));
        }
        if (tag.contains(COORDS_KEY)) {
            setCoords(Coords.EMPTY.load(tag.getCompound(COORDS_KEY)));
        }
        if (tag.contains(SIZE_KEY)) {
            setSize(Box.load(tag.getCompound(SIZE_KEY)));
        }
//        if (nbt.contains(WHITELIST_KEY)) {
//            ListTag list = nbt.getList(WHITELIST_KEY, 10);
//            list.forEach(element -> {
//                PlayerData playerData = new PlayerData("");
//                playerData.load((CompoundTag)element);
//                getWhitelist().add(playerData);
//            });
//        }

        return this;
    }

    @Override
    public int getArea() {
        ICoords absoluteSize = getSize().getSize();
        return absoluteSize.getX() * absoluteSize.getZ() * absoluteSize.getY();
    }

    @Override
    public ICoords getMinCoords() {

        return getCoords().add(getSize().getMinCoords());
    }

    @Override
    public ICoords getMaxCoords() {

        return getCoords().add(getSize().getMaxCoords());
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public UUID getDeedId() {
        return deedId;
    }

    @Override
    public void setDeedId(UUID deedId) {
        this.deedId = deedId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ICoords getCoords() {
        return coords;
    }

    @Override
    public void setCoords(ICoords coords) {
        this.coords = coords;
    }

    @Override
    public Box getSize() {
        return size;
    }

    @Override
    public void setSize(Box size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "AbstractParcel{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", deedId=" + deedId +
                ", name='" + name + '\'' +
                ", coords=" + coords +
                ", size=" + size +
                '}';
    }
}
