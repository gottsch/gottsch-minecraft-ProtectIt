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
package mod.gottsch.forge.protectit.core.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertyUtil;
import mod.gottsch.forge.protectit.core.registry.PlayerIdentity;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 
 * @author Mark Gottschling Mar 2, 2023
 *
 */
public class Deed extends Item {

	public static final UUID EMPTY_UUID = new UUID(0, 0);
	public static final String OWNER_ID_KEY = "owner";
	public static final String OWNER_NAME_KEY = "ownerName";
	public static final String PROPERTY_ID_KEY = "property";
	public static final String PROPERTY_NAME_KEY = "propertyName";

	/**
	 * 
	 * @param properties
	 */
	public Deed(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, worldIn, tooltip, flag);

		appendSummary(stack, worldIn, tooltip, flag);

		if (stack.hasTag()) {
			CompoundTag tag = stack.getOrCreateTag();
			LangUtil.appendAdvancedHoverText(tooltip, tt -> {
				String propertyName = tag.getString("propertyName");
				propertyName = "".equals(propertyName) ? "" : propertyName;

				tooltip.add(Component.literal(LangUtil.NEWLINE));
				tooltip.add(Component.translatable(getTitleKey()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD, ChatFormatting.UNDERLINE));
				tooltip.add(Component.literal(LangUtil.NEWLINE));
				tooltip.add(Component.translatable(LangUtil.tooltip("deed.owner_name")).withStyle(ChatFormatting.WHITE)
						.append(Component.literal(tag.getString("ownerName")).withStyle(ChatFormatting.AQUA)));
				tooltip.add(Component.translatable(LangUtil.tooltip("deed.property_name")).withStyle(ChatFormatting.WHITE)
						.append(Component.literal(propertyName).withStyle(ChatFormatting.AQUA)));
				
				if (tag.contains("propertyBox")) {
					Box box = Box.load(tag.getCompound("propertyBox"));
					tooltip.add(Component.translatable(LangUtil.tooltip("deed.property_location")).withStyle(ChatFormatting.WHITE)
							.append(Component.translatable(String.format("(%s) to (%s)", 
									PropertyUtil.formatCoords(box.getMinCoords()), 
									PropertyUtil.formatCoords(box.getMaxCoords())
									))
							.withStyle(ChatFormatting.GREEN)));
				}
			});
		}
	}

	public  void appendSummary(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
		Component component = Component.translatable(LangUtil.tooltip("deed.howto"));
		for (String s : component.getString().split("~")) {	
			tooltip.add(Component.translatable(LangUtil.INDENT2)
					.append(Component.literal(s).withStyle(ChatFormatting.GREEN)));
		}
	}
	
	public  void appendHoverSpecials(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
	}

	public String getTitleKey() {
		return LangUtil.tooltip("deed.title");
	}

	/**
	 * 
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);

		// exit if on the client
		if (WorldInfo.isClientSide(world)) {
			return InteractionResultHolder.pass(itemStack);
		}

		UUID itemOwnerUuid = EMPTY_UUID;
		UUID itemPropertyUuid = EMPTY_UUID;		
		CompoundTag tag = itemStack.getTag();

		if (tag != null)	{
			if (tag.contains("owner")) {
				itemOwnerUuid = tag.getUUID(OWNER_ID_KEY);
			}
			if (tag.contains("property")) {
				itemPropertyUuid = tag.getUUID(PROPERTY_ID_KEY);
			}

			if (itemOwnerUuid != null) {
				// get the top-level property
				Optional<Property> property = getProperty(player);
				if (property.isEmpty()) {
					player.sendSystemMessage(Component.translatable(LangUtil.message("block_region.not_protected")));
					return InteractionResultHolder.pass(itemStack);
				}

				// TODO this is not working with a fief deed
				/* TODO this will not work with fiefs because you could have a nested fief within a fief of exact same size.
				 * by only selecting the first found, you may never select the correct property and thus not able to use
				 * the deed. need to cycle through all the nested properties and test the ownership criteria against each.
				 */

				// select the property to execute on
				Optional<Property> selectedProperty = selectProperty(player, property.get());
				if (selectedProperty.isEmpty()) {
					sendCriteriaNotMetMessage(player);
					return InteractionResultHolder.pass(itemStack);
				}
				ProtectIt.LOGGER.debug("selected property -> {}", selectedProperty.get());

				boolean isValid = false;
				isValid = ownershipCheck(player, selectedProperty.get(), itemOwnerUuid, itemPropertyUuid);

				// claim property
				if (isValid) {
					ProtectionRegistries.property().updateOwner(selectedProperty.get(), new PlayerIdentity(player.getUUID(), player.getName().getString()));
					
					// TODO send message to client
					
					player.sendSystemMessage(Component.translatable(LangUtil.message("property.transfer.success"))
							.withStyle(ChatFormatting.WHITE)
							.append(Component.translatable(selectedProperty.get().getName()).withStyle(ChatFormatting.AQUA)));

					itemStack.shrink(1);
					return InteractionResultHolder.consume(itemStack);
				}
				else {
					player.sendSystemMessage(Component.translatable(LangUtil.message("property.transfer.owners_not_same"))
							.withStyle(ChatFormatting.RED));
				}
			}
		}

		return InteractionResultHolder.pass(itemStack);
	}

	/**
	 * 
	 * @param player
	 * @param property
	 * @param itemOwnerUuid
	 * @param itemPropertyUuid
	 * @return
	 */
	public boolean ownershipCheck(Player player, Property property, UUID itemOwnerUuid, UUID itemPropertyUuid) {
		boolean isValid = false;
		UUID propertyOwnerUuid;
		
		if (ObjectUtils.isEmpty(property.getOwner()) || ObjectUtils.isEmpty(property.getOwner().getUuid())) {
			propertyOwnerUuid = EMPTY_UUID;
		}
		else {
			propertyOwnerUuid = property.getOwner().getUuid();
		}

		if (propertyOwnerUuid.equals(itemOwnerUuid) && itemPropertyUuid.equals(property.getUuid())) {
			isValid = true;
		}

		return isValid;
	}

	/**
	 * 
	 * @param player
	 */
	protected void sendCriteriaNotMetMessage(Player player) {
		player.sendSystemMessage(Component.translatable(LangUtil.message("property.transfer.criteria_not_met"))
				.withStyle(ChatFormatting.RED));
	}

	/**
	 * 
	 * @param player
	 * @param property
	 * @return
	 */
	public Optional<Property> selectProperty(Player player, Property property) {
		return Optional.of(property);
	}

	/**
	 * 
	 * @param player
	 * @return
	 */
	public Optional<Property> getProperty(Player player	) {
		ICoords coords = new Coords(player.blockPosition());
		// get all properties by coords and uuid
		List<Box> protections = ProtectionRegistries.property().getProtections(coords);
		if (protections.isEmpty()) {
			return Optional.empty();
		}
//		return Optional.ofNullable(ProtectionRegistries.block().getPropertyByCoords(protections.get(0).getMinCoords()));
		List<Property> properties = protections.stream().flatMap(p -> ProtectionRegistries.property().getPropertyByCoords(p.getMinCoords()).stream()).toList();
		Optional<Property> property = PropertyUtil.getMostSignificant(properties);
		return property;
	}
}
