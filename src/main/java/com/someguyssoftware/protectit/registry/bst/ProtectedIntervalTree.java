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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.nbt.CompoundNBT;

// NOTE this is currently not a balanced Binary Search Tree
public class ProtectedIntervalTree {
	private Interval root;

	public synchronized Interval insert(Interval interval) {
		root = insert(root, interval);
		return root;
	}

	/**
	 * 
	 * @param interval
	 * @param newInterval
	 * @return
	 */
	private Interval insert(Interval interval, Interval newInterval) {
		if (interval == null) {
			interval = newInterval;
			return interval;
		}

		if (interval.getMax() == null ||  newInterval.getEnd() > interval.getMax()) {
			interval.setMax(newInterval.getEnd());
		}
        if (interval.getMin() == null || newInterval.getStart() < interval.getMin()) {
        	interval.setMin(newInterval.getStart());
        }
        
		if (interval.compareTo(newInterval) <= 0) {

			if (interval.getRight() == null) {
				interval.setRight(newInterval);
			}
			else {
				insert(interval.getRight(), newInterval);
			}
		}
		else {
			if (interval.getLeft() == null) {
				interval.setLeft(newInterval);
			}
			else {
				insert(interval.getLeft(), newInterval);
			}
		}
		return interval;
	}

	/**
	 * Requires and extact match of intervals.
	 * @param target
	 * @return
	 */
	public synchronized Interval delete(Interval target) {
		root = delete(root, target);
		ProtectIt.LOGGER.debug("root is now -> {}", root);
		ProtectIt.LOGGER.debug("all intervals now -> {}", toStringList(root));
		return root;
	}

	/**
	 * 
	 * @param interval
	 * @param target
	 * @return
	 */
	private Interval delete(Interval interval, Interval target) {
		ProtectIt.LOGGER.debug("delete interval -> {}, target -> {}", interval, target);
		if (interval == null) {
			return interval;
		}

		if (interval.compareTo(target) < 0) {
			ProtectIt.LOGGER.debug("setting right...");
			interval.setRight(delete(interval.getRight(), target));
		}
		else if (interval.compareTo(target) > 0) {
			ProtectIt.LOGGER.debug("setting left...");
			interval.setLeft(delete(interval.getLeft(), target));
		}
		else {
			// node with no leaf nodes
			if (interval.getLeft() == null && interval.getRight() == null) {
				ProtectIt.LOGGER.debug("no child nodes...");
				return null;
			}
			else if (interval.getLeft() == null) {
				ProtectIt.LOGGER.debug("returning right...");
				return interval.getRight();
			}
			else if (interval.getRight() == null) {
				ProtectIt.LOGGER.debug("returing left...");
				return interval.getLeft();
			}
			else {
				ProtectIt.LOGGER.debug("inserting left into right...");
				// insert right tree into left tree
				insert(interval.getLeft(), interval.getRight());
				// return the left tree
				ProtectIt.LOGGER.debug("returning left -> {}", interval.getLeft());
				return interval.getLeft();
			}
		}
		return interval;
	}

	/**
	 * 
	 * @param target
	 * @param uuid
	 */
	public synchronized List<Interval> delete(Interval target, String uuid) {
		ProtectIt.LOGGER.debug("uuid -> {}", uuid);
		List<Interval> removed = Lists.newArrayList();
		List<Interval> list = getOverlapping(getRoot(), target, false);
		ProtectIt.LOGGER.debug("list of overlappings -> {}", list);
		list.forEach(i -> {
			ProtectIt.LOGGER.debug("overlapping interval -> {}", i);
			if (i.getData() != null && i.getData().getOwner().getUuid().equalsIgnoreCase(uuid)) {
				Interval interval = delete(getRoot(), i);
				ProtectIt.LOGGER.debug("removed interval");
				removed.add(interval);
			}
		});
		return removed;
	}
	
	/**
	 * 
	 * @param interval
	 * @param predicate
	 */
	public synchronized Interval delete(Interval interval, Predicate<Interval> predicate) {
		if (interval == null) {
			return interval;
		}
		
		if (predicate.test(interval)) {
			return delete(interval);
		}
		
		Interval deletedInterval = null;		
		deletedInterval = this.delete(interval.getLeft(), predicate);

		if (deletedInterval == null) {
			deletedInterval = this.delete(interval.getRight(), predicate);
		}		
		return deletedInterval;
	}
	
	// better version of above
	public synchronized void delete2(Interval target, Predicate<Interval> predicate) {
		List<Interval> list = getOverlapping(getRoot(), target, false);
		list.forEach(i -> {
			if (predicate.test(target)) {
				delete(target);
			}
		});
	}
	
	/**
	 * 
	 * @param interval
	 */
	public List<String> toStringList(Interval interval) {
		List<Interval> list = new ArrayList<>();
		list(interval, list);
		
		List<String> display = new ArrayList<>();
		list.forEach(element -> {
			display.add(String.format("[%s] -> [%s]: owner -> %s (%s)", element.getCoords1().toShortString(), element.getCoords2().toShortString(), element.getData().getOwner().getName(), element.getData().getOwner().getUuid()));
		});
		
		return display;
	}
	
	/**
	 * 
	 * @param interval
	 * @param intervals
	 */
	public synchronized void list(Interval interval, List<Interval> intervals) {
		if (interval == null) {
			return;
		}

		if (interval.getLeft() != null) {
			list(interval.getLeft(), intervals);
		}

		intervals.add(interval);
		
		if (interval.getRight() != null) {
			list(interval.getRight(), intervals);
		}
	}
	
	/**
	 * 
	 * @param interval
	 * @param predicate
	 * @param intervals
	 */
	public synchronized void find(Interval interval, Predicate<Interval> predicate, List<Interval> intervals) {
		find(interval, predicate, intervals, true);
	}
	
	/**
	 * 
	 * @param interval
	 * @param predicate
	 * @param intervals
	 * @param findFirst find first occurrence only
	 * @return whether an overlap was found in this subtree
	 */
	public synchronized boolean find(Interval interval, Predicate<Interval> predicate, List<Interval> intervals, boolean findFirst) {
		boolean isFound = false;
		
		if (interval == null) {
			return false;
		}

		// check first to optimize findFirst search
		// add the interval to list
		if (predicate.test(interval)) {
			intervals.add(interval);
			if (findFirst) {
				return true;
			}
		}
		
		if (interval.getLeft() != null) {
			isFound = find(interval.getLeft(), predicate, intervals, findFirst);
			if (isFound && findFirst) {
				return true;
			}
		}

		if (interval.getRight() != null) {
			isFound = find(interval.getRight(), predicate, intervals, findFirst);
			if (isFound && findFirst) {
				return true;
			}
		}
		return isFound;
	}

	public List<Interval> getOverlapping(Interval interval, Interval testInterval, boolean findFast) {
		return getOverlapping(interval, testInterval, true, true);
	}	
	
	/**
	 * public wrapper to ensure that the return value is non-null
	 * @param interval
	 * @param testInterval
	 */
	public synchronized List<Interval> getOverlapping(Interval interval, Interval testInterval, boolean findFast, boolean includeBorder) {
		List<Interval> results = new ArrayList<>();
		if (includeBorder) {
			checkOverlap(interval, testInterval, results, findFast);
		}
		else {
			checkOverlapNoBorder(interval, testInterval, results, findFast);
		}
		return results;
	}


	/**
	 * 
	 * @param interval
	 * @param testInterval
	 * @param results
	 * @param findFirst find first occurrence only
	 * @return whether an overlap was found in this subtree
	 */
	private boolean checkOverlap(Interval interval, Interval testInterval, List<Interval> results, boolean findFast) {
		if (interval == null) {
			return false;
		}

		// short-circuit
        if(testInterval.getStart() > interval.getMax() || testInterval.getEnd() < interval.getMin()) {
        	return false;
        }

		if (!((interval.getStart() > testInterval.getEnd()) || (interval.getEnd() < testInterval.getStart()))) {
			// x-axis overlaps, check z-axis
			if (!((interval.getStartZ() > testInterval.getEndZ()) || (interval.getEndZ() < testInterval.getStartZ()))) {
				// z-axis overlaps, check y-axis
				if (!((interval.getStartY() > testInterval.getEndY()) || (interval.getEndY() < testInterval.getStartY()))) {

					results.add(interval);
					if (findFast) {
						return true;
					}				
				}
			}
		}

		// walk the left branch
		if ((interval.getLeft() != null) && (interval.getLeft().getMax() >= testInterval.getStart())) {
			if (this.checkOverlap(interval.getLeft(), testInterval, results, findFast) && findFast) {
				return true;
			}
		}

		// walk the right branch
		if (this.checkOverlap(interval.getRight(), testInterval, results, findFast) && findFast) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param interval
	 * @param testInterval
	 * @param results
	 * @param findFirst find first occurrence only
	 * @return whether an overlap was found in this subtree
	 */
	private boolean checkOverlapNoBorder(Interval interval, Interval testInterval, List<Interval> results, boolean findFast) {
		if (interval == null) {
			return false;
		}

		// short-circuit
        if(testInterval.getStart() > interval.getMax() || testInterval.getEnd() < interval.getMin()) { //TESTING - adding >=, <= instead of >, <
        	return false;
        }
        
        // TODO add logging
//    	ProtectIt.LOGGER.debug("testing: interval -> {}, test -> {}", interval, testInterval);
//    	ProtectIt.LOGGER.debug("testing x: i.startx -> {}, t.endx -> {}, i.endx -> {}, t.startx -> {}", interval.getStart(), testInterval.getEnd(),
//    			interval.getEnd(), testInterval.getStart());
		if (!((interval.getStart() >= testInterval.getEnd()) || (interval.getEnd() <= testInterval.getStart()))) { // TESTING - adding >= and <= to all comparisons
//			ProtectIt.LOGGER.info("has x overlap");
//	    	ProtectIt.LOGGER.debug("testing z: i.startz -> {}, t.endz -> {}, i.endz -> {}, t.startz -> {}", interval.getStartZ(), testInterval.getEndZ(),
//	    			interval.getEndZ(), testInterval.getStartZ());
			// x-axis overlaps, check z-axis
			if (!((interval.getStartZ() >= testInterval.getEndZ()) || (interval.getEndZ() <= testInterval.getStartZ()))) {
//				ProtectIt.LOGGER.info("has z overlap");
//		    	ProtectIt.LOGGER.debug("testing y: i.starty -> {}, t.endy -> {}, i.endy -> {}, t.starty -> {}", interval.getStartY(), testInterval.getEndY(),
//		    			interval.getEndY(), testInterval.getStartY());
				// z-axis overlaps, check y-axis
				if (!((interval.getStartY() >= testInterval.getEndY()) || (interval.getEndY() <= testInterval.getStartY()))) {
//					ProtectIt.LOGGER.info("has y overlap - adding to overlaps");
					results.add(interval);
					if (findFast) {
						return true;
					}				
				}
			}
		}

		// walk the left branch
		if ((interval.getLeft() != null) && (interval.getLeft().getMax() > testInterval.getStart())) { // TESTING replaced >= with >
			if (this.checkOverlapNoBorder(interval.getLeft(), testInterval, results, findFast) && findFast) {
				return true;
			}
		}

		// walk the right branch
		if (this.checkOverlapNoBorder(interval.getRight(), testInterval, results, findFast) && findFast) {
			return true;
		}
		
		return false;
	}

	/**
	 * 
	 * @param nbt
	 * @param interval
	 * @return
	 */
	public synchronized CompoundNBT save(CompoundNBT nbt) {
		if (getRoot() == null) {
			return nbt;
		}
		getRoot().save(nbt);	        	        
		return nbt;
	}

	/**
	 * 
	 * @param nbt
	 */
	public synchronized void load(CompoundNBT nbt) {
		Interval root = Interval.load(nbt);
		if (!root.equals(Interval.EMPTY)) {
			setRoot(root);
		}
	}

	public void clear() {
		setRoot(null);
	}

	public synchronized Interval getRoot() {
		return root;
	}

	public synchronized void setRoot(Interval root) {
		this.root = root;
	}
}
