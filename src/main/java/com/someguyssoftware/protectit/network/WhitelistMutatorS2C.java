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

import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.claim.Property;
import com.someguyssoftware.protectit.registry.PlayerData;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 
 * @author Mark Gottschling on Nov 5, 2021
 *
 */
@Deprecated
public class WhitelistMutatorS2C {
	public static final String ADD_ACTION = "add";
	public static final String REMOVE_ACTION = "remove";
	public static final String REPLACE_ACTION = "replace";

	public static final ICoords EMPTY_COORDS = new Coords(0, -255, 0);
	public static final String NULL_UUID = "NULL";	
	
	private boolean valid;
	public String type;						// 0
	public String action;					// 1
	public List<Property> claims;	// 2

	public static class Builder {
		public String type;
		public String action;
		public List<Property> claims;

		public Builder(String type, String action, List<Property> claims) {
			this.type = type;
			this.action = action;
			this.claims = claims;
		}

		public Builder with(Consumer<Builder> builder)  {
			builder.accept(this);
			return this;
		}

		public WhitelistMutatorS2C build() {
			return  new WhitelistMutatorS2C(this);
		}
	}

	/**
	 * 
	 * @param builder
	 */
	protected WhitelistMutatorS2C(Builder builder) {
		setValid(true);
		setType(builder.type);
		setAction(builder.action);
		setClaims(builder.claims);
	}

	/**
	 * 
	 */
	public WhitelistMutatorS2C() {
		setValid(false);
	}

	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		if (!isValid()) {
			return;
		}

		buf.writeUtf(StringUtils.defaultString(type, ""));
		buf.writeUtf(StringUtils.defaultString(action, ""));

		buf.writeInt(claims.size());

		claims.forEach(claim -> {

			if (claim.getBox().getMinCoords() == null) {
				writeCoords(EMPTY_COORDS, buf);
			}
			else {
				writeCoords(claim.getBox().getMinCoords(), buf);
			}

			buf.writeInt(claim.getWhitelist().size());
			claim.getWhitelist().forEach(playerData -> {
				buf.writeUtf(StringUtils.defaultString(playerData.getUuid(), NULL_UUID));
				buf.writeUtf(StringUtils.defaultString(playerData.getName(), ""));
			});
		});
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static WhitelistMutatorS2C decode(FriendlyByteBuf buf) {
		WhitelistMutatorS2C message;

		List<Property> claims = Lists.newArrayList();
		
		try {
			String type = buf.readUtf();
			String action = buf.readUtf();

			int numOfClaims = buf.readInt();
			for (int index = 0; index < numOfClaims; index++) {
				ICoords coords = readCoords(buf);
				int numOfPlayerData = buf.readInt();
				List<PlayerData> whitelist = Lists.newArrayList();
				for (int playerIndex = 0; playerIndex < numOfPlayerData; playerIndex++) {
					String uuid = buf.readUtf();
					String name = buf.readUtf();
					PlayerData data = new PlayerData(uuid, name);
					whitelist.add(data);
				}
				Property claim = new Property(coords, new Box(coords, coords));
				claim.setWhitelist(whitelist);
				claims.add(claim);
			}

			message = new WhitelistMutatorS2C.Builder(type, action, claims).build();
			message.setValid(true);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
			message = new WhitelistMutatorS2C();
		}
		return message;
	}

	protected void writeCoords(ICoords coords, FriendlyByteBuf buf) {
		if (coords != null) {
			buf.writeInt(coords.getX());
			buf.writeInt(coords.getY());
			buf.writeInt(coords.getZ());
		}
	}
	
	protected static ICoords readCoords(FriendlyByteBuf buf) {
		ICoords coords = new Coords(buf.readInt(), buf.readInt(), buf.readInt());
		return coords;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public List<Property> getClaims() {
		return claims;
	}

	public void setClaims(List<Property> claims) {
		this.claims = claims;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	@Override
	public String toString() {
		return "RegistryWhitelistMutatorMessageToClient [valid=" + valid + ", type=" + type + ", action=" + action
				+ ", claims=" + claims + "]";
	}
}
