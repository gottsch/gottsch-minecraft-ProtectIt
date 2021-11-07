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
package com.someguyssoftware.protectit.network;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;

import net.minecraft.network.PacketBuffer;

/**
 * 
 * @author Mark Gottschling on Oct 13, 2021
 *
 */
public class RegistryMutatorMessageToClient {
	public static final String BLOCK_TYPE = "block";
	public static final String PVP_TYPE = "pvp";
	public static final String ADD_ACTION = "add";
	public static final String REMOVE_ACTION = "remove";
	public static final String CLEAR_ACTION = "clear";
	
	public static final String NULL_UUID = "NULL";	
	public static final ICoords EMPTY_COORDS = new Coords(0, -255, 0);
	
	private boolean valid;
	private String type;												//0
	private String action;											//1
	private String uuid;												//2
	private ICoords coords1;									//3
	private ICoords coords2;									//4
	private String playerName; 								//5

	public static class Builder {
		public String type;
		public String action;
		public String uuid;
		public ICoords coords1;
		public ICoords coords2;		
		public String playerName; 
		
		public Builder(String type, String action, String uuid) {
			this.type = type;
			this.action = action;
			this.uuid = uuid;
		}
		
		public Builder with(Consumer<Builder> builder)  {
			builder.accept(this);
			return this;
		}
		
		public RegistryMutatorMessageToClient build() {
			return  new RegistryMutatorMessageToClient(this);
		}
	}
	
	/**
	 * 
	 * @param builder
	 */
	protected RegistryMutatorMessageToClient(Builder builder) {
		valid = true;
		this.type = builder.type;
		this.action = builder.action;
		this.uuid = builder.uuid;
		this.coords1 = builder.coords1;
		this.coords2 = builder.coords2;
		this.playerName = builder.playerName;
	}
	
	/**
	 * 
	 */
	public RegistryMutatorMessageToClient() {
		valid = false;
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(PacketBuffer buf) {
		if (!isValid()) {
			return;
		}

		buf.writeUtf(StringUtils.defaultString(type, ""));
		buf.writeUtf(StringUtils.defaultString(action, ""));
		buf.writeUtf(StringUtils.defaultString(uuid, NULL_UUID));

		if (coords1 == null) {
			writeCoords(EMPTY_COORDS, buf);
		}
		else {
			writeCoords(coords1, buf);
		}
		if (coords2 == null) {
			writeCoords(EMPTY_COORDS, buf);
		}
		else {
			writeCoords(coords2, buf);
		}

		buf.writeUtf(StringUtils.defaultString(playerName, ""));
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static RegistryMutatorMessageToClient decode(PacketBuffer buf) {
		RegistryMutatorMessageToClient message;
		
		try {
			String type = buf.readUtf();
			String action = buf.readUtf();
			String uuid = buf.readUtf();
			
			ICoords coords1 = readCoords(buf);
			ICoords coords2 = readCoords(buf);

			String playerName = buf.readUtf();
			
			message = new RegistryMutatorMessageToClient.Builder(type, action, uuid)
					.with($ -> {
						$.coords1 = coords1;
						$.coords2 = coords2;
						$.playerName = playerName;
					}).build();
			message.setValid(true);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
			message = new RegistryMutatorMessageToClient();
		}
		return message;
	}
		
	protected void writeCoords(ICoords coords, PacketBuffer buf) {
		if (coords != null) {
			buf.writeInt(coords.getX());
			buf.writeInt(coords.getY());
			buf.writeInt(coords.getZ());
		}
	}
	
	protected static ICoords readCoords(PacketBuffer buf) {
		ICoords coords = new Coords(buf.readInt(), buf.readInt(), buf.readInt());
		return coords;
	}
	
	public boolean isValid() {
		return valid;
	}
	public void setValid(boolean messageIsValid) {
		this.valid = messageIsValid;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
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
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "RegistryMutatorMessageToClient [valid=" + valid + ", type=" + type + ", action=" + action + ", uuid="
				+ uuid + ", coords1=" + coords1 + ", coords2=" + coords2 + ", playerName=" + playerName + "]";
	}

}
