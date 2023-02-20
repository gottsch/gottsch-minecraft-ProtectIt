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
package com.someguyssoftware.protectit.network;

import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;


/**
 * 
 * @author Mark Gottschling on Nov 20, 2021
 *
 */
@Deprecated
public class ClaimBookMessageToServer {
	private boolean valid;
	private ItemStack book;
	private int slot;

	/**
	 * 
	 */
	public ClaimBookMessageToServer() {
		setValid(false);
	}
	
	public ClaimBookMessageToServer(ItemStack stack, int slot) {
		setBook(stack);
		setSlot(slot);
		setValid(true);
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		if (!isValid()) {
			return;
		}
		buf.writeItem(getBook());
		buf.writeVarInt(getSlot());
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static ClaimBookMessageToServer decode(FriendlyByteBuf buf) {
		ClaimBookMessageToServer message = new ClaimBookMessageToServer();

		try {
			message.setBook(buf.readItem());
			message.setSlot(buf.readVarInt());
			message.setValid(true);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
		}
		return message;
	}
	
	protected ItemStack getBook() {
		return book;
	}

	protected void setBook(ItemStack book) {
		this.book = book;
	}

	protected int getSlot() {
		return slot;
	}

	protected void setSlot(int slot) {
		this.slot = slot;
	}

	protected boolean isValid() {
		return valid;
	}

	protected void setValid(boolean valid) {
		this.valid = valid;
	}
}
