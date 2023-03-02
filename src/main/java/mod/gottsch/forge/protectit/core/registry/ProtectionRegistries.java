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
package mod.gottsch.forge.protectit.core.registry;

/**
 * 
 * @author Mark Gottschling on Oct 5, 2021
 *
 */
public class ProtectionRegistries {
	/*
	 * block protection registry - ie. land claims
	 */
	private static final IBlockProtectionRegistry REGISTRY = new BlockProtectionRegistry();
	
	/*
	 * PVP protection registry - player vs player protection areas ie. safe zones
	 */
	private static final IPvpProtectionRegistry PVP_REGISTRY = new PvpProtectionRegistry();
	
	/*
	 *  use accessors instead of directly accessing static property because the backing may change
	 */
	public static IBlockProtectionRegistry block() {
		return REGISTRY;
	}
	
	public static IPvpProtectionRegistry pvp() {
		return PVP_REGISTRY;
	}
}
