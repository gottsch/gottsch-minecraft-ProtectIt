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

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.ProtectIt;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertyUtil;
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
 * @author Mark Gottschling Feb 27, 2023
 *
 */
public class FiefdomGrant extends Item {
	// TODO find a better home for this
	private static final UUID EMPTY_UUID = new UUID(0, 0);

	public FiefdomGrant(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, worldIn, tooltip, flag);

		Component component = Component.translatable(LangUtil.tooltip("subdivide_license.howto"));
		for (String s : component.getString().split("~")) {	
			tooltip.add(Component.translatable(LangUtil.INDENT2)
					.append(Component.literal(s).withStyle(ChatFormatting.GREEN)));
		}

		if (stack.hasTag()) {
			CompoundTag tag = stack.getOrCreateTag();
			LangUtil.appendAdvancedHoverText(tooltip, tt -> {
				String propertyName = tag.getString("propertyName");
				propertyName = "".equals(propertyName) ? Component.translatable(LangUtil.tooltip("subdivide_license.any_property")).getString() : propertyName;

				// TODO add any long text here like Owner Name, Property Name
				tooltip.add(Component.translatable(LangUtil.tooltip("subdivide_license.title")).withStyle(ChatFormatting.WHITE));
				tooltip.add(Component.translatable(LangUtil.tooltip("subdivide_license.owner_name"), tag.getString("ownerName")).withStyle(ChatFormatting.WHITE));
				tooltip.add(Component.translatable(LangUtil.tooltip("subdivide_license.property_name"), propertyName).withStyle(ChatFormatting.WHITE));
				if (tag.contains("propertyBox")) {
					Box box = Box.load(tag.getCompound("propertyBox"));
					tooltip.add(Component.translatable(LangUtil.tooltip("subdivide_license.property_location")).withStyle(ChatFormatting.WHITE)
							.append(Component.translatable(String.format("(%s) to (%s)", 
									PropertyUtil.formatCoords(box.getMinCoords()), 
									PropertyUtil.formatCoords(box.getMaxCoords())
									)).withStyle(ChatFormatting.GREEN)));
				}
			});
		}
	}

	public  void appendHoverSpecials(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
	}

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
				itemOwnerUuid = tag.getUUID("owner");
			}
			if (tag.contains("property")) {
				itemPropertyUuid = tag.getUUID("property");
			}

			// TODO locate property (by owner, property or both
			if (itemOwnerUuid != null) {
				ICoords coords = new Coords(player.blockPosition());
				// get all properties by coords and uuid
				List<Box> protections = ProtectionRegistries.property().getProtections(coords, coords, false, false);
				if (protections.isEmpty()) {
					player.sendSystemMessage(Component.translatable(LangUtil.message("block_region.not_protected")));
					return InteractionResultHolder.pass(itemStack);
				}

				// parent property
//				Property property = ProtectionRegistries.block().getPropertyByCoords(protections.get(0).getMinCoords());
				List<Property> properties = protections.stream().flatMap(p -> ProtectionRegistries.property().getPropertyByCoords(p.getMinCoords()).stream()).toList();
				Optional<Property> property = PropertyUtil.getLeastSignificant(properties);
				Property selectedProperty = property.get();

//				Property selectedProperty = property;
//				if (!property.getChildren().isEmpty()) {
//					// determine if any of the childen intersect with the coords
//					for (Property child : property.getChildren()) {
//						if (child.intersects(new Box(coords))) {
//							selectedProperty = child;
//							break;
//						}
//					}
//				}
				ProtectIt.LOGGER.debug("selected property -> {}", selectedProperty);
				
				// TODO test if the owners match
				UUID propertyOwnerUuid = selectedProperty.getOwner().getUuid();
//				if (ObjectUtils.isEmpty(selectedProperty.getOwner()) || StringUtils.isEmpty(selectedProperty.getOwner().getUuid())) {
//					propertyOwnerUuid = EMPTY_UUID;
//				}
//				else {
//					propertyOwnerUuid = UUID.fromString(selectedProperty.getOwner().getUuid());
//				}

				boolean isValid = false;
				if (propertyOwnerUuid.equals(itemOwnerUuid)) {
					isValid = true;

					if (!itemPropertyUuid.equals(EMPTY_UUID) && !itemPropertyUuid.equals(selectedProperty.getUuid())) {
						isValid = false;
						player.sendSystemMessage(Component.translatable(LangUtil.message("property.subdivide.owners_not_same"))
								.withStyle(ChatFormatting.WHITE));
					}
				}
				else {
					player.sendSystemMessage(Component.translatable(LangUtil.message("property.subdivide.propertiess_not_same"))
							.withStyle(ChatFormatting.WHITE));
				}

				// update the property with the the flag
				if (isValid) {
					selectedProperty.setFiefdom(true);
					player.sendSystemMessage(Component.translatable(LangUtil.message("property.subdivide.enable.success"))
							.withStyle(ChatFormatting.WHITE)
							.append(Component.translatable(selectedProperty.getName()).withStyle(ChatFormatting.AQUA)));
					return InteractionResultHolder.consume(itemStack);
				}
			}
		}

		return InteractionResultHolder.pass(itemStack);
	}
}
