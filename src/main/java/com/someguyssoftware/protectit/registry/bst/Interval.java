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
package com.someguyssoftware.protectit.registry.bst;


import com.someguyssoftware.protectit.ProtectIt;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;


/**
 * 
 * @author Mark Gottschling on Oct 2, 2021
 *
 */
public class Interval implements Comparable<Interval> {
	private static final String LEFT_KEY = "left";
	private static final String RIGHT_KEY = "right";
	private static final String MIN_KEY = "min";
	private static final String MAX_KEY = "max";
	private static final String DATA_KEY = "data";

	public static final Interval EMPTY = new Interval(new Coords(0, -255, 0), new Coords(0, -255, 0));
	
	private ICoords coords1;
	private ICoords coords2;
	private Integer min;
	private Integer max;
	private Interval left;
	private Interval right;

	// extra mod specific data
	private OwnershipData data;

	/**
	 * 
	 * @param coords1 the starting coords
	 * @param coords2 the ending coords
	 */
	public Interval(ICoords coords1, ICoords coords2) {
		this.coords1 = coords1;
		this.coords2 = coords2;
		this.min = coords1.getX();
		this.max = coords2.getX();
		this.data = new OwnershipData();
	}

	/**
	 * 
	 * @param coords1
	 * @param coords2
	 * @param data
	 */
	public Interval(ICoords coords1, ICoords coords2, OwnershipData data) {
		this(coords1, coords2);
		this.data = data;
	}

	/**
	 * 
	 * @return
	 */
	public Box toBox() {
		return new Box(getCoords1(), getCoords2());
	}
	
	@Override
	public int compareTo(Interval interval) {
		if (this.getStart() < interval.getStart()) {
			ProtectIt.LOGGER.debug("this.c1.x < interval.c1.x");
			return -1;
		} else if (this.getStart() == interval.getStart()) {

			if (getEnd() == interval.getEnd()) {
				if (getStartZ() < interval.getStartZ()) {
					return -1;
				} else if (getStartZ() == interval.getStartZ()) {
					if (getEndZ() == interval.getEndZ()) {
//						return 0;
						///////						
						if (getStartY() < interval.getStartY()) {
							return -1;
						} else if (getStartY() == interval.getStartY()) {
							if (getEndY() == interval.getEndY()) {
								return 0;
							}
							ProtectIt.LOGGER.debug("this.c2.y -> {}, interval.c2.y -> {}", this.getEndY(), interval.getEndY());
							return this.getEndY() < interval.getEndY() ? -1 : 1;
						} else {
							ProtectIt.LOGGER.debug("this.c1.y -> {}, interval.c1.y -> {}", this.getEndY(), interval.getEndY());
							return 1;
						}
						//////////////
					}
					ProtectIt.LOGGER.debug("this.c2.z -> {}, interval.c2.z -> {}", this.getEndZ(), interval.getEndZ());
					return this.getEndZ() < interval.getEndZ() ? -1 : 1;
				} else {
					ProtectIt.LOGGER.debug("this.c1.z -> {}, interval.c1.z -> {}", this.getEndZ(), interval.getEndZ());
					return 1;
				}
			} else {
				ProtectIt.LOGGER.debug("this.c2.x -> {}, interval.c2.x -> {}", this.getEnd(), interval.getEnd());
				return this.getEnd() < interval.getEnd() ? -1 : 1;
			}
		} else {
			ProtectIt.LOGGER.debug("this.c1.x > interval.c1.x -> {} > {}", this.getStart(), interval.getStart());
			return 1;
		}
	}

	/**
	 * 
	 * @param nbt
	 */
	public void save(CompoundTag nbt) {
		ProtectIt.LOGGER.debug("saving interval -> {}", this);

		CompoundTag coordsNbt1 = new CompoundTag();
		CompoundTag coordsNbt2 = new CompoundTag();

//		coordsNbt1 = saveCoords(coords1);
//		coordsNbt2 = saveCoords(coords2);

		coords1.save(coordsNbt1);
		coords2.save(coordsNbt2);
		
		nbt.put("coords1", coordsNbt1);
		nbt.put("coords2", coordsNbt2);

		nbt.putInt(MIN_KEY, min);
		nbt.putInt(MAX_KEY, max);
		
		CompoundTag dataNbt = new CompoundTag();
//		dataNbt.putString("uuid", getData().getOwner().getUuid());
//		dataNbt.putString("playerName", (getData().getOwner().getName() == null) ? "" : getData().getOwner().getName());		
		getData().save(dataNbt);
		nbt.put(DATA_KEY, dataNbt);
		
		if (getLeft() != null) {
			CompoundTag left = new CompoundTag();
			getLeft().save(left);
			nbt.put(LEFT_KEY, left);
		}

		if (getRight() != null) {
			CompoundTag right = new CompoundTag();
			getRight().save(right);
			nbt.put(RIGHT_KEY, right);
		}
	}

	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public static Interval load(CompoundTag nbt) {
		Interval interval;
		ICoords c1;
		ICoords c2;
		if (nbt.contains("coords1")) {
//			c1 = loadCoords(nbt, "coords1");
			c1 = Coords.EMPTY.load(nbt.getCompound("coords1"));
		}
		else {
			return Interval.EMPTY;
		}
		
		if (nbt.contains("coords2")) {
//			c2 = loadCoords(nbt, "coords2");
			c2 = Coords.EMPTY.load(nbt.getCompound("coords2"));
		}
		else {
			return Interval.EMPTY;
		}		
		interval = new Interval(c1, c2);
		
		if (nbt.contains(MIN_KEY)) {
			interval.setMin(nbt.getInt(MIN_KEY));
		}
		if (nbt.contains(MAX_KEY)) {
			interval.setMax(nbt.getInt(MAX_KEY));
		}
		
		if (nbt.contains(DATA_KEY)) {
			CompoundTag dataNbt = (CompoundTag) nbt.get(DATA_KEY);
//			if (dataNbt.contains("uuid")) {
//				interval.getData().getOwner().setUuid(dataNbt.getString("uuid"));
//			}
//			if (dataNbt.contains("playerName")) {
//				interval.getData().getOwner().setName(dataNbt.getString("playerName"));
//			}
			interval.getData().load(dataNbt);
		}
		
		if (nbt.contains(LEFT_KEY)) {
			Interval left = Interval.load((CompoundTag) nbt.get(LEFT_KEY));
			if (!left.equals(Interval.EMPTY)) {
				interval.setLeft(left);
			}
		}
		
		if (nbt.contains(RIGHT_KEY)) {
			Interval right = Interval.load((CompoundTag) nbt.get(RIGHT_KEY));
			if (!right.equals(Interval.EMPTY)) {
				interval.setRight(right);
			}			
		}
		
		ProtectIt.LOGGER.debug("loaded -> {}", interval);
		return interval;
	}

	public int getStart() {
		return coords1.getX();
	}

	public int getEnd() {
		return coords2.getX();
	}

	public Integer getMin() {
		return min;
	}

	public void setMin(Integer min) {
		this.min = min;
	}
	
	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	public Interval getLeft() {
		return left;
	}

	public void setLeft(Interval left) {
		this.left = left;
	}

	public Interval getRight() {
		return right;
	}

	public void setRight(Interval right) {
		this.right = right;
	}

	public int getStartZ() {
		return coords1.getZ();
	}

	public int getEndZ() {
		return coords2.getZ();
	}

	public int getStartY() {
		return coords1.getY();
	}

	public int getEndY() {
		return coords2.getY();
	}

	public ICoords getCoords1() {
		return coords1;
	}

	public void setCoords1(ICoords coords1) {
		this.coords1 = coords1;
	}

	public ICoords getCoords2() {
		return coords2;
	}

	public void setCoords2(ICoords coords2) {
		this.coords2 = coords2;
	}

	@Override
	public String toString() {
		return "Interval [coords1=" + coords1 + ", coords2=" + coords2 + ", min=" + min + ", max=" + max +
				", left=" + ((left == null) ? "null" : "value") + ", right=" + ((right == null) ? "null" : "value") + ", data=" + data + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coords1 == null) ? 0 : coords1.hashCode());
		result = prime * result + ((coords2 == null) ? 0 : coords2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Interval other = (Interval) obj;
		if (coords1 == null) {
			if (other.coords1 != null)
				return false;
		} else if (!coords1.equals(other.coords1))
			return false;
		if (coords2 == null) {
			if (other.coords2 != null)
				return false;
		} else if (!coords2.equals(other.coords2))
			return false;
		return true;
	}

	public OwnershipData getData() {
		return data;
	}

	public void setData(OwnershipData data) {
		this.data = data;
	}
	
//	/**
//	 * 
//	 * @author Mark Gottschling on Oct 9, 2021
//	 *
//	 */
//	@Deprecated
//	public static class Data {
//		private String uuid;
//		private String playerName;
//		
//		public Data(String uuid) {
//			this.uuid = (uuid == null) ? "" : uuid;
//		}
//		
//		public Data(String uuid, String name) {
//			this(uuid);
//			this.playerName = name;
//		}
//
//		public String getUuid() {
//			return uuid;
//		}
//
//		public void setUuid(String uuid) {
//			this.uuid = uuid;
//		}
//
//		public String getPlayerName() {
//			return playerName;
//		}
//
//		public void setPlayerName(String playerName) {
//			this.playerName = playerName;
//		}
//
//		@Override
//		public String toString() {
//			return "Data [uuid=" + uuid + ", playerName=" + playerName + "]";
//		}
//	}
}
