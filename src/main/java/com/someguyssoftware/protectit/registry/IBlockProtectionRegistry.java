package com.someguyssoftware.protectit.registry;


import java.util.List;
import java.util.function.Predicate;

import com.someguyssoftware.gottschcore.spatial.Box;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.registry.bst.Interval;

import net.minecraft.nbt.CompoundNBT;

/**
 * 
 * @author Mark Gottschling on Oct 5, 2021
 *
 */
public interface IBlockProtectionRegistry {
	public void addProtection(ICoords coords);
	
	/**
	 * add protection for the region specified by coords1 -> coords2
	 * @param coords1
	 * @param coords2
	 */
	public void addProtection(ICoords coords1, ICoords coords2);
	
//	public void addProtection(ICoords coords, String uuid);
//	public void addProtection(ICoords coords1, ICoords coords2, String uuid);
	public void addProtection(ICoords coords, PlayerData data);
	public void addProtection(ICoords coords1, ICoords coords2, PlayerData data);
	
	/**
	 * removes any protection intervals that coords intersects with
	 */
	public void removeProtection(ICoords coords);
	public void removeProtection(ICoords coords1, ICoords coords2);
	public void removeProtection(ICoords coords1, ICoords coords2, String uuid);
	public void removeProtection(String uuid);
	
	/**
	 * determines if the point at coords is a protected region
	 * @param coords
	 * @return
	 */
	public boolean isProtected(ICoords coords);
	public boolean isProtected(ICoords coords1, ICoords coords2);
	
	/**
	  * is protected against player uuid
	  * @param coords1
	  * @param coords2
	  * @param uuid
	 * @return 
	  * @return
	  */
	boolean isProtectedAgainst(ICoords coords, String uuid);
	boolean isProtectedAgainst(ICoords coords1, ICoords coords2, String uuid);
	
	public List<Box> getProtections(ICoords coords);
	public List<Box> getProtections(ICoords coords1, ICoords coords2);
	public List<Interval> getProtections(ICoords coords1, ICoords coords2, boolean findFast);
	
	public void load(CompoundNBT nbt);	
	public CompoundNBT save(CompoundNBT nbt);



	public void clear();
	public List<Interval> list();
	public List<Interval> find(Predicate<Interval> predicate);
	
	public List<String> toStringList();

}
