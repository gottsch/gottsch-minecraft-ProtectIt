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
package mod.gottsch.forge.protectit.core.registry;

import mod.gottsch.forge.gottschcore.bst.CoordsInterval;
import mod.gottsch.forge.gottschcore.bst.CoordsIntervalTree;
import mod.gottsch.forge.gottschcore.bst.IInterval;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.ParcelFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Predicate;

/**
 *
 * @author Mark Gottschling on Mar 17, 2024
 *
 */
public class ParcelRegistry {
    private static final String PARCELS_KEY = "parcels";

    private static final CoordsIntervalTree<UUID> TREE = new CoordsIntervalTree<UUID>();

    private static final Map<UUID, List<Parcel>> PARCELS_BY_OWNER = new HashMap<>();
    private static final Map<ICoords, Parcel> PARCELS_BY_COORDS = new HashMap<>();


    // singleton
    private ParcelRegistry() {}

    /**
     *
     */
    public static synchronized void clear() {
        PARCELS_BY_OWNER.clear();
        PARCELS_BY_COORDS.clear();
        TREE.clear();
    }

    /**
     *
     * @param tag
     * @return
     */
    public static synchronized CompoundTag save(CompoundTag tag) {
        ProtectIt.LOGGER.debug("saving parcel registry...");

        ListTag list = new ListTag();
        PARCELS_BY_COORDS.forEach((coords, parcel) -> {
            ProtectIt.LOGGER.debug("registry saving parcel -> {}", parcel);
            CompoundTag parcelTag = new CompoundTag();
            parcel.save(parcelTag);
            list.add(parcelTag);
        });
        tag.put(PARCELS_KEY, list);

        return tag;
    }

    /**
     *
     * @param tag
     */
    public static synchronized void load(CompoundTag tag) {
        ProtectIt.LOGGER.debug("loading registry...");
        clear();


        if (tag.contains(PARCELS_KEY)) {
            ListTag list = tag.getList(PARCELS_KEY, Tag.TAG_COMPOUND);
            list.forEach(element -> {
                Optional<Parcel> parcel = ParcelFactory.create(tag);
                parcel.ifPresent(action -> {
                    // load the parcel
                    action.load((CompoundTag) element);
                    ProtectIt.LOGGER.debug("loaded parcel -> {}", action);

                    // add to byCoords map
                    PARCELS_BY_COORDS.put(action.getCoords(), action);

                    // add to byOwner map
                    List<Parcel> parcelsByOwner;
                    if (!PARCELS_BY_OWNER.containsKey(action.getOwnerId())) {
                        // create new list entry
                        parcelsByOwner = PARCELS_BY_OWNER.put(action.getOwnerId(), new ArrayList<>());
                    } else {
                        parcelsByOwner =  PARCELS_BY_OWNER.get(action.getOwnerId());
                    }
                    parcelsByOwner.add(action);

                    // add to tree
                    TREE.insert(new CoordsInterval(action.getMinCoords(), action.getMaxCoords(), action.getOwnerId()));
                });
            });
        }
    }

    /**
     *
     * @param parcel
     * @return
     */
    public static Optional<Parcel> add(Parcel parcel) {
        ProtectIt.LOGGER.debug("adding parcel protection -> {}", parcel);

        // add to parcels by owner
        List<Parcel> parcels = null;

        if (!PARCELS_BY_OWNER.containsKey(parcel.getOwnerId())) {
            // create new list entry
            PARCELS_BY_OWNER.put(parcel.getOwnerId(), new ArrayList<>());
        }
        parcels = PARCELS_BY_OWNER.get(parcel.getOwnerId());
        parcels.add(parcel);

        // add to parcels by coords
        PARCELS_BY_COORDS.put(parcel.getCoords(), parcel);

        // add to BST
        IInterval<UUID> interval = TREE.insert(new CoordsInterval<UUID>(parcel.getMinCoords(), parcel.getMaxCoords(), parcel.getId()));

        return interval != null ? Optional.of(parcel) : Optional.empty();
    }

    /**
     *
     * @param id
     * @return
     */
    public static List<Parcel> findByOwner(UUID id) {
        List<Parcel> parcels = PARCELS_BY_OWNER.get(id);
        if (parcels == null) {
            parcels = new ArrayList<>();
        }
        return parcels;
    }

    public static Optional<Parcel> findByParcelId(UUID id) {
        List<Parcel> parcels = new ArrayList<>(1);
        for (Parcel parcel : PARCELS_BY_COORDS.values()) {
            if (parcel.getId().equals(id)) {
                parcels.add(parcel);
                break;
            }
        }
        return parcels.isEmpty() ? Optional.empty() : Optional.of(parcels.get(0));
    }

    public static List<Parcel> findByParcel(Predicate<Parcel> predicate) {
        List<Parcel> parcels = new ArrayList<>();
        PARCELS_BY_COORDS.values().forEach(parcel -> {
            if (predicate.test(parcel)) {
                parcels.add(parcel);
            }
        });
        return parcels;
    }

    //////////////////////////////////
    ///// find() is a slower version of findBoxes() since it requires looking up the parcel from
    ///// the internal map.
    /////////////////////////////////
    public static List<Parcel> find(ICoords coords) {
        return find(coords, coords);
    }

    public static List<Parcel> find(ICoords coords1, ICoords coords2) {
        return find(coords1, coords2, false);
    }

    public static List<Parcel> find(ICoords coords1, ICoords coords2, boolean findFast) {
        return find(coords1, coords2, findFast, true);
    }

    public static List<Parcel> find(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder) {
        List<IInterval<UUID>> intervals = TREE.getOverlapping(TREE.getRoot(), new CoordsInterval<UUID>(coords1, coords2), findFast, includeBorder);
        List<Parcel> parcels = new ArrayList<>();
        intervals.forEach(i -> {
            // find the parcel from the map
            Parcel p = PARCELS_BY_COORDS.get(((CoordsInterval<UUID>)i).getCoords1());
            if (p != null) {
                parcels.add(p);
            }
        });
        return parcels;
    }
    ///////////////////////////////


    public static List<Box> findBoxes(ICoords coords) {
        return findBoxes(coords, coords);
    }

    public static List<Box> findBoxes(ICoords coords1, ICoords coords2) {
        return findBoxes(coords1, coords2, false);
    }

    public static List<Box> findBoxes(ICoords coords1, ICoords coords2, boolean findFast) {
        return findBoxes(coords1, coords2, findFast, true);
    }

    public static List<Box> findBoxes(ICoords coords1, ICoords coords2, boolean findFast, boolean includeBorder) {
        List<IInterval<UUID>> intervals = TREE.getOverlapping(TREE.getRoot(), new CoordsInterval<UUID>(coords1, coords2), findFast, includeBorder);
        List<Box> boxes = new ArrayList<>();
        intervals.forEach(i -> {
            boxes.add(new Box(((CoordsInterval<UUID>)i).getCoords1(), ((CoordsInterval<UUID>)i).getCoords2()));
        });
        return boxes;
    }

    public static boolean intersectsParcel(ICoords coords) {
        return intersectsParcel(coords, coords);
    }

    public static boolean intersectsParcel(ICoords coords1, ICoords coords2) {
        return intersectsParcel(coords1, coords2, true);
    }

    /**
     * Used to determine a the provided area intersects with a parcel
     * @param coords1
     * @param coords2
     * @param includeBorders
     * @return
     */
    public static boolean intersectsParcel(ICoords coords1, ICoords coords2, boolean includeBorders) {
        List<Box> parcels = findBoxes(coords1, coords2, true, includeBorders);
        return !parcels.isEmpty();
    }

    public static boolean isProtectedAgainst(ICoords coords, UUID entityId) {
        return isProtectedAgainst(coords, coords, entityId);
    }

    public static boolean isProtectedAgainst(ICoords coords1, ICoords coords2, UUID entityId) {
        List<IInterval<UUID>> intervals = TREE.getOverlapping(TREE.getRoot(), new CoordsInterval<>(coords1, coords2));
        if (intervals.isEmpty()) {
            return false;
        }
        else {

            // interrogate each interval to determine if the uuid is the owner
            for (IInterval<UUID> interval : intervals) {
                // short circuit if owner or no owner
                if (interval.getData() == null || interval.getData().equals(entityId)) {
                    break;
                }
//                if (p.getData() == null) {
//                    break; // was true. but if no owner, it is not protected against you? how does this work with CitizenParcels
//                }

                // get the parcel
                CoordsInterval<UUID> coordsInterval = (CoordsInterval<UUID>)interval;
                Parcel parcel = PARCELS_BY_COORDS.get(coordsInterval.getCoords1());
                ProtectIt.LOGGER.debug("isProtectedAgainst.parcelsByCoords -> {}, parcel -> {}", parcel.getMinCoords(), parcel);
                // cycle through whitelist
//                if (!parcel.getWhitelist().isEmpty()) {
//                    ProtectIt.LOGGER.debug("isProtectedAgainst whitelist is not null");
//
//                    for (PlayerData id : parcel.getWhitelist()) {
//                        ProtectIt.LOGGER.debug("isProtectedAgainst compare whitelist id -> {} to uuid -> {}", id.getUuid(), uuid);
//                        if (id.getUuid().equalsIgnoreCase(uuid)) {
//                            return false;
//                        }
//                    }
//                }
                return true;
            }
        }
        return false;
    }
}
