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

import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.someguyssoftware.gottschcore.spatial.Coords;
import com.someguyssoftware.gottschcore.spatial.ICoords;
import com.someguyssoftware.protectit.ProtectIt;
import com.someguyssoftware.protectit.network.RegistryMutatorMessageToClient.Builder;
import com.someguyssoftware.protectit.registry.bst.Interval;

import net.minecraft.network.PacketBuffer;

/**
 * 
 * @author Mark Gottschling on Nov 5, 2021
 *
 */
public class RegistryWhitelistMutatorMessageToClient extends RegistryMutatorMessageToClient {
	public static final String WHITELIST_ADD_ACTION = "whitelist add";
	public static final String WHITELIST_REMOVE_ACTION = "whitelist remove";

	private List<Interval> intervals	;					// 6

	public static class Builder {
		public String type;
		public String action;
		public String uuid;
		public ICoords coords1;
		public ICoords coords2;		
		public String playerName; 
		public List<Interval> intervals;
		
		public Builder(String type, String action, String uuid) {
			this.type = type;
			this.action = action;
			this.uuid = uuid;
		}
		
		public Builder with(Consumer<Builder> builder)  {
			builder.accept(this);
			return this;
		}
		
		public RegistryWhitelistMutatorMessageToClient build() {
			return  new RegistryWhitelistMutatorMessageToClient(this);
		}
	}
	
	/**
	 * 
	 * @param builder
	 */
	protected RegistryWhitelistMutatorMessageToClient(Builder builder) {
		setValid(true);
		setType(builder.type);
		setAction(builder.action);
		setUuid(builder.uuid);
		setCoords1(builder.coords1);
		setCoords2(builder.coords2);
		setPlayerName(builder.playerName);
		this.intervals = builder.intervals;
	}
	
	/**
	 * 
	 */
	public RegistryWhitelistMutatorMessageToClient() {
		setValid(false);
	}
	
	/**
	 * 
	 * @param buf
	 */
	public void encode(PacketBuffer buf) {
//		if (!isValid()) {
//			return;
//		}
//
//		buf.writeUtf(StringUtils.defaultString(type, ""));
//		buf.writeUtf(StringUtils.defaultString(action, ""));
//		buf.writeUtf(StringUtils.defaultString(uuid, NULL_UUID));
//
//		if (coords1 == null) {
//			writeCoords(EMPTY_COORDS, buf);
//		}
//		else {
//			writeCoords(coords1, buf);
//		}
//		if (coords2 == null) {
//			writeCoords(EMPTY_COORDS, buf);
//		}
//		else {
//			writeCoords(coords2, buf);
//		}
//
//		buf.writeUtf(StringUtils.defaultString(playerName, ""));
		super.encode(buf);
		
		buf.writeInt(intervals.size());
		
		intervals.forEach(interval -> {
			if (interval.getCoords1() == null) {
				writeCoords(EMPTY_COORDS, buf);
			}
			else {
				writeCoords(interval.getCoords1(), buf);
			}
			if (interval.getCoords2() == null) {
				writeCoords(EMPTY_COORDS, buf);
			}
			else {
				writeCoords(interval.getCoords2(), buf);
			}
			buf.writeUtf(StringUtils.defaultString(interval.getData().getOwner().getUuid(), NULL_UUID));
			buf.writeUtf(StringUtils.defaultString(interval.getData().getOwner().getName(), ""));

		});
	}
	
	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static RegistryWhitelistMutatorMessageToClient decode(PacketBuffer buf) {
		RegistryWhitelistMutatorMessageToClient message;
		
		try {
			String type = buf.readUtf();
			String action = buf.readUtf();
			String uuid = buf.readUtf();
			
			ICoords coords1 = readCoords(buf);
			ICoords coords2 = readCoords(buf);

			String playerName = buf.readUtf();
			
			// TODO add interval decode here
			List<Interval> intervals = null;
			//////
			
			message = new RegistryWhitelistMutatorMessageToClient.Builder(type, action, uuid)
					.with($ -> {
						$.coords1 = coords1;
						$.coords2 = coords2;
						$.playerName = playerName;
						$.intervals = intervals;
					}).build();
			message.setValid(true);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
			message = new RegistryWhitelistMutatorMessageToClient();
		}
		return message;
	}
	

	protected List<Interval> getIntervals() {
		return intervals;
	}

	protected void setIntervals(List<Interval> intervals) {
		this.intervals = intervals;
	}

}
