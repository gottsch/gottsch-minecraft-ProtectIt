/*
 * This file is part of  Protect It.
 * Copyright (c) 2023 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.protectit.core.tags;

import mod.gottsch.forge.protectit.ProtectIt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;

/**
 * 
 * @author Mark Gottschling Mar 30, 2023
 *
 */
@Mod.EventBusSubscriber(modid = ProtectIt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModTags {
	
	public static class Blocks {

		public static final TagKey<Block> INTERACT_PERMISSION = mod(ProtectIt.MODID, "interact_permission");
		public static final TagKey<Block> DOOR_PERMISSION = mod(ProtectIt.MODID, "door_permission");
		public static final TagKey<Block> INVENTORY_PERMISSION = mod(ProtectIt.MODID, "inventory_permission");
		
		public static TagKey<Block> mod(String domain, String path) {
			return BlockTags.create(new ResourceLocation(domain, path));
		}
	}
	
}
