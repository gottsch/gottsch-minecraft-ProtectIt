/*
 * This file is part of  Protect It.
 * Copyright (c) 2021, Mark Gottschling (gottsch)
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

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.nbt.CompoundNBT;

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
	private Data data;

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
//		this.uuid = "";
		this.data = new Data("");
	}
	
	/**
	 * 
	 * @param coords1
	 * @param coords2
	 * @param uuid
	 */
//	@Deprecated
//	public Interval(ICoords coords1, ICoords coords2, String uuid) {
//		this.coords1 = coords1;
//		this.coords2 = coords2;
//		this.uuid = uuid;
//	}
//	
//	@Deprecated
//	public Interval(ICoords coords1, ICoords coords2, String uuid, String playerName) {
//		this.coords1 = coords1;
//		this.coords2 = coords2;
//		this.uuid = uuid;
//		this.playerName = playerName;
//	}
	
	public Interval(ICoords coords1, ICoords coords2, Data data) {
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
			return -1;
		} else if (this.getStart() == interval.getStart()) {

			if (getEnd() == interval.getEnd()) {
				if (getStartZ() < interval.getStartZ()) {
					return -1;
				} else if (getStartZ() == interval.getStartZ()) {
					if (getEndZ() == interval.getEndZ()) {
						return 0;
					}
					return this.getEndZ() < interval.getEndZ() ? -1 : 1;
				} else {
					return 1;
				}
			} else {
				return this.getEnd() < interval.getEnd() ? -1 : 1;
			}
		} else {
			return 1;
		}
	}

	/**
	 * 
	 * @param nbt
	 */
	public void save(CompoundNBT nbt) {
		ProtectIt.LOGGER.debug("saving interval -> {}", this);

		CompoundNBT coordsNbt1 = new CompoundNBT();
		CompoundNBT coordsNbt2 = new CompoundNBT();

		coordsNbt1 = saveCoords(coords1);
		coordsNbt2 = saveCoords(coords2);

		nbt.put("coords1", coordsNbt1);
		nbt.put("coords2", coordsNbt2);

		nbt.putInt(MIN_KEY, min);
		nbt.putInt(MAX_KEY, max);
		
		CompoundNBT dataNbt = new CompoundNBT();
		dataNbt.putString("uuid", getData().getUuid());
		dataNbt.putString("playerName", (getData().getPlayerName() == null) ? "" : getData().getPlayerName());		
		nbt.put(DATA_KEY, dataNbt);
		
//		nbt.putString("uuid", (uuid == null) ? "" : uuid);
//		nbt.putString("playerName", (playerName == null) ? "" : playerName);
		
		if (getLeft() != null) {
			CompoundNBT left = new CompoundNBT();
			getLeft().save(left);
			nbt.put(LEFT_KEY, left);
		}

		if (getRight() != null) {
			CompoundNBT right = new CompoundNBT();
			getRight().save(right);
			nbt.put(RIGHT_KEY, right);
		}
	}

	/**
	 * 
	 * @param nbt
	 * @return
	 */
	public static Interval load(CompoundNBT nbt) {
		Interval interval;
		ICoords c1;
		ICoords c2;
		if (nbt.contains("coords1")) {
			c1 = loadCoords(nbt, "coords1");
		}
		else {
			return Interval.EMPTY;
		}
		
		if (nbt.contains("coords2")) {
			c2 = loadCoords(nbt, "coords2");
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
			CompoundNBT dataNbt = (CompoundNBT) nbt.get(DATA_KEY);
			if (dataNbt.contains("uuid")) {
				interval.getData().setUuid(dataNbt.getString("uuid"));
			}
			if (dataNbt.contains("playerName")) {
				interval.getData().setPlayerName(dataNbt.getString("playerName"));
			}
		}
		
//		if (nbt.contains("uuid")) {
//			interval.setUuid(nbt.getString("uuid"));
//		}
//		else {
//			interval.setUuid("");
//		}
//		
//		if (nbt.contains("playerName")) {
//			interval.setPlayerName(nbt.getString("playerName"));
//		}
//		else {
//			interval.setPlayerName("");
//		}		
		
		if (nbt.contains(LEFT_KEY)) {
			Interval left = Interval.load((CompoundNBT) nbt.get(LEFT_KEY));
			if (!left.equals(Interval.EMPTY)) {
				interval.setLeft(left);
			}
		}
		
		if (nbt.contains(RIGHT_KEY)) {
			Interval right = Interval.load((CompoundNBT) nbt.get(RIGHT_KEY));
			if (!right.equals(Interval.EMPTY)) {
				interval.setRight(right);
			}			
		}
		
		ProtectIt.LOGGER.debug("loaded -> {}", interval);
		return interval;
	}
	
	/**
	 * 
	 * @param coords
	 * @return
	 */
	private static CompoundNBT saveCoords(ICoords coords) {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putInt("x", coords.getX());
		nbt.putInt("y", coords.getY());
		nbt.putInt("z", coords.getZ());
		return nbt;
	}

	/**
	 * 
	 * @param nbt
	 * @param name
	 * @return
	 */
	private static ICoords loadCoords(CompoundNBT nbt, String name) {
		CompoundNBT coords = nbt.getCompound(name);
		int x = coords.getInt("x");
		int y = coords.getInt("y");
		int z = coords.getInt("z");
		return new Coords(x, y, z);
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

	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}
	
	/**
	 * 
	 * @author Mark Gottschling on Oct 9, 2021
	 *
	 */
	public static class Data {
		private String uuid;
		private String playerName;
		
		public Data(String uuid) {
			this.uuid = (uuid == null) ? "" : uuid;
		}
		
		public Data(String uuid, String name) {
			this(uuid);
			this.playerName = name;
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getPlayerName() {
			return playerName;
		}

		public void setPlayerName(String playerName) {
			this.playerName = playerName;
		}

		@Override
		public String toString() {
			return "Data [uuid=" + uuid + ", playerName=" + playerName + "]";
		}
	}
}
