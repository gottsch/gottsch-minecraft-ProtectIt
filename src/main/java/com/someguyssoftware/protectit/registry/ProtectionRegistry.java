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
 */package com.someguyssoftware.protectit.registry;

 import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;

 /**
  * 
  * @author Mark Gottschling on Sep 15, 2021
  *
  */
 public class ProtectionRegistry {

	 private static final String REGISTRY_NAME = "registry";
	 private static final int COMPOUND_TYPE = 10;
	 private static final int INT_TYPE = 3;

	 private static final XaxisRegistry REGISTRY = new XaxisRegistry();

	 /**
	  * 
	  * @param pos
	  */
	 public static void addProtection(BlockPos pos) {
		 addProtection(pos.getX(), pos.getY());
	 }

	 /**
	  * 
	  * @param x
	  * @param z
	  */
	 public static void addProtection(Integer x, Integer z) {
		 REGISTRY.protect(x).protect(z);
	 }

	 /**
	  * 
	  * @param pos
	  * @param pos2
	  */
	 public static void addProtection(BlockPos pos, BlockPos pos2) {
		 addProtection(pos.getX(), pos.getZ(), pos2.getX(), pos2.getZ());
	 }

	 /**
	  * 
	  * @param x1
	  * @param z1
	  * @param x2
	  * @param z2
	  */
	 public static void addProtection(Integer x1, Integer z1, Integer x2, Integer z2) {
		 for (Integer x = x1; x <= x2; x++) {
			 ZaxisRegistry zaxis = REGISTRY.protect(x);
			 for (Integer z = z1; z <= z2; z++) {
				 zaxis.protect(z);
			 }
		 }
	 }

	 public static void removeProtection(BlockPos pos) {
		 removeProtection(pos.getX(), pos.getZ());
	 }

	 public static void removeProtection(BlockPos pos, BlockPos pos2) {
		 removeProtection(pos.getX(), pos.getZ(), pos2.getX(), pos2.getZ());
	 }

	 public static void removeProtection(int x, int z) {
		 REGISTRY.unprotect(x, z);
	 }

	 /**
	  * 
	  * @param x1
	  * @param z1
	  * @param x2
	  * @param z2
	  */
	 public static void removeProtection(int x1, int z1, int x2, int z2) {
		 for (Integer x = x1; x <= x2; x++) {
			 for (Integer z = z1; z <= z2; z++) {
				 REGISTRY.unprotect(x, z);
			 }
		 }
	 }

	 /**
	  * Convenience method
	  * 
	  * @param pos
	  * @return
	  */
	 public static boolean isProtected(BlockPos pos) {
		 return isProtected(pos.getX(), pos.getZ());
	 }

	 /**
	  * 
	  * @param x
	  * @param z
	  * @return
	  */
	 public static boolean isProtected(Integer x, Integer z) {
		 if (REGISTRY.getMin() == null || REGISTRY.getMax() == null) {
			 return false;
		 }
		 if (x < REGISTRY.getMin() || x > REGISTRY.getMax()) {
			 return false;
		 }
		 else if (REGISTRY.has(x)) {
			 ZaxisRegistry zaxis = REGISTRY.get(x);
			 if (zaxis.getMin() == null || zaxis.getMax() == null) {
				 return false;
			 }
			 if (z < zaxis.getMin() || z > zaxis.getMax()) {
				 return false;
			 }
			 else {
				 return zaxis.has(z);
			 }
		 }
		 return false;
	 }

	 /**
	  * 
	  * @author Mark Gottschling on Sep 15, 2021
	  *
	  */
	 private static class XaxisRegistry {
		 private Integer min;
		 private Integer max;

		 private Map<Integer, ZaxisRegistry> registry = new HashMap<>();

		 public XaxisRegistry() {}

		 /**
		  * 
		  * @param x
		  * @return
		  */
		 public boolean has(Integer x) {
			 return getRegistry().containsKey(x);
		 }

		 public ZaxisRegistry get(Integer x) {
			 return getRegistry().get(x);
		 }

		 /**
		  * TODO should be protect(x, z) and this method calls zaxis.protect(z);
		  * @param x
		  * @return
		  */
		 public ZaxisRegistry protect(Integer x) {
			 ZaxisRegistry zaxis;
			 if (has(x)) {
				 zaxis = get(x);
			 }
			 else {				
				 zaxis = new ZaxisRegistry();

				 // set the min/max values
				 updateMinMax(x);

				 // update the registry
				 getRegistry().put(x, zaxis);
			 }
			 return zaxis;
		 }

		 /**
		  * 
		  * @param x
		  * @param z
		  */
		 public void unprotect(int x, int z) {
			 if (!has(x)) {
				 return;
			 }
			 ZaxisRegistry zaxis = get(x);
			 zaxis.unprotect(z);
			 if (zaxis.getRegistry().isEmpty()) {
				 getRegistry().remove(x);
				 if (x == getMin() || x == getMax()) {
					 refreshMinMax();
				 }
			 }
		 }

		 private void refreshMinMax() {
			 // cycle thru all the x values and test against min/max
			 getRegistry().keySet().forEach(x -> {
				 updateMinMax(x);
			 });
		 }

		 private void updateMinMax(Integer x) {
			 if (getMin() == null || x < getMin()) {
				 setMin(x);
			 }			
			 if (getMax() == null || x > getMax()) {
				 setMax(x);
			 }
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

		 public Map<Integer, ZaxisRegistry> getRegistry() {
			 return registry;
		 }
	 }

	 /**
	  * 
	  * @author Mark Gottschling on Sep 15, 2021
	  *
	  */
	 private static class ZaxisRegistry {
		 private Integer min;
		 private Integer max;

		 private HashSet<Integer> registry = new HashSet<>();

		 /**
		  * 
		  * @param z
		  * @return
		  */
		 public boolean has(Integer z) {
			 return getRegistry().contains(z);
		 }

		 /**
		  * 
		  * @param z
		  */
		 public void protect(Integer z) {			
			 if (!getRegistry().contains(z)) {
				 // set the min/max values
				 updateMinMax(z);
				 // add to the registry
				 getRegistry().add(z);
			 }
		 }

		 /**
		  * 
		  * @param z
		  */
		 public void unprotect(int z) {
			 if (!has(z)) {
				 return;
			 }
			 // remove from registry
			 getRegistry().remove(z);

			 // update min/max
			 if (getRegistry().isEmpty()) {
				 if (z == getMin() || z == getMax()) {
					 refreshMinMax();
				 }
			 }
		 }

		 private void refreshMinMax() {
			 // cycle thru all the z values and test against min/max
			 getRegistry().forEach(z -> {
				 updateMinMax(z);
			 });
		 }

		 /**
		  * 
		  * @param z
		  */
		 private void updateMinMax(Integer z) {
			 if (getMin() == null || z < getMin()) {
				 setMin(z);
			 }

			 if (getMax() == null || z > getMax()) {
				 setMax(z);
			 }
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

		 public HashSet<Integer> getRegistry() {
			 return registry;
		 }
	 }

	 /**
	  * 
	  * @param nbt
	  */
	 public static void load(CompoundNBT nbt) {
		 ListNBT xRegistry = nbt.getList(REGISTRY_NAME, COMPOUND_TYPE);
		 xRegistry.forEach(xelem -> {
			 if (((CompoundNBT)xelem).contains(REGISTRY_NAME)) {
				 ListNBT zRegistry = ((CompoundNBT)xelem).getList(REGISTRY_NAME, INT_TYPE);
				 zRegistry.forEach(zelem -> {
					 addProtection(((CompoundNBT)xelem).getInt("x"), ((IntNBT)zelem).getAsInt());
				 });
			 }
		 });
	 }

	 /**
	  * 
	  * @param nbt
	  * @return
	  */
	 public static CompoundNBT save(CompoundNBT nbt) {
		 //		nbt.putInt("min", REGISTRY.getMin());
		 //		nbt.putInt("max", REGISTRY.getMax());

		 ListNBT xaxisRegistry = new ListNBT();
		 REGISTRY.getRegistry().forEach((key, value) -> {
			 CompoundNBT element = new CompoundNBT();
			 //			element.putInt("min", value.getMin());
			 //			element.putInt("max", value.getMax());
			 element.putInt("x", key);

			 ListNBT zaxisRegistry = new ListNBT();
			 value.getRegistry().forEach(zVal -> {
				 IntNBT z = IntNBT.valueOf(zVal);
				 zaxisRegistry.add(z);
			 });
			 element.put(REGISTRY_NAME, zaxisRegistry);
			 xaxisRegistry.add(element);
		 });
		 nbt.put(REGISTRY_NAME, xaxisRegistry);
		 return nbt;
	 }

	public static List<String> list() {
		List<String> protections = new ArrayList<>();
		REGISTRY.getRegistry().forEach((x, zaxis) -> {			
			protections.add(String.format("[%s] -> [%s]", x, zaxis.getRegistry().stream().map(z -> z.toString()).collect(Collectors.joining(", "))));
		});
		return protections;
	}
 }
