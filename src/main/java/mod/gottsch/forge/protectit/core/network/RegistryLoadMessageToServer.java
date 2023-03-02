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
package mod.gottsch.forge.protectit.core.network;

import org.apache.commons.lang3.StringUtils;

import mod.gottsch.forge.protectit.ProtectIt;
import net.minecraft.network.FriendlyByteBuf;

/**
 *
 * @author Mark Gottschling on Oct 14, 2021
 *
 */
@Deprecated
public class RegistryLoadMessageToServer {
	private boolean valid;
	private String uuid;


	public RegistryLoadMessageToServer() {
		valid = false;
	}

	public RegistryLoadMessageToServer(String uuid) {
		this.valid = true;
		this.uuid = uuid;
	}

	/**
	 * 
	 * @param buf
	 */
	public void encode(FriendlyByteBuf buf) {
		if (!isValid()) {
			return;
		}
		buf.writeUtf(StringUtils.defaultString(uuid, ""));
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public static RegistryLoadMessageToServer decode(FriendlyByteBuf buf) {
		RegistryLoadMessageToServer message;

		try {
			String uuid = buf.readUtf();
			message = new RegistryLoadMessageToServer(uuid);
		}
		catch(Exception e) {
			ProtectIt.LOGGER.error("An error occurred attempting to read message: ", e);
			message = new RegistryLoadMessageToServer();
		}
		return message;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
