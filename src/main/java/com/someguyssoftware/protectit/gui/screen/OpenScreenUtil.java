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
package com.someguyssoftware.protectit.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.Player;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.DistExecutor;

/**
 * 
 * @author Mark Gottschling on Nov 22, 2021
 *
 */
public class OpenScreenUtil {
	
	/**
	 * TODO take in a screen ID or create multiple methods depending on the screen needed and params required
	 * @param player
	 * @param itemStack
	 * @param hand
	 * @return
	 */
    public static DistExecutor.SafeRunnable openEditClaimBookScreen(Player player, ItemStack itemStack, Hand hand) {
        Minecraft.getInstance().setScreen(new EditClaimBookScreen(player, itemStack, hand));
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() { }
        };
    }
}
