package mod.gottsch.forge.protectit.core.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.core.command.CommandHelper;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.property.PropertyUtils;
import mod.gottsch.forge.protectit.core.registry.PlayerData;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 
 * @author Mark Gottschling Mar 2, 2023
 *
 */
public class PropertyLease extends Deed {

	public PropertyLease(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
		appendSummary(stack, worldIn, tooltip, flag);

		if (stack.hasTag()) {
			CompoundTag tag = stack.getOrCreateTag();
			LangUtil.appendAdvancedHoverText(tooltip, tt -> {
				String propertyName = tag.getString("propertyName");
				propertyName = "".equals(propertyName) ? Component.translatable(LangUtil.tooltip("lease.any_property")).getString() : propertyName;

				tooltip.add(Component.literal(LangUtil.NEWLINE));
				tooltip.add(Component.translatable(getTitleKey()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD, ChatFormatting.UNDERLINE));
				tooltip.add(Component.literal(LangUtil.NEWLINE));
				tooltip.add(Component.translatable(LangUtil.tooltip("lease.owner_name")).withStyle(ChatFormatting.WHITE)
						.append(Component.literal(tag.getString("ownerName")).withStyle(ChatFormatting.AQUA)));
				tooltip.add(Component.translatable(LangUtil.tooltip("lease.property_name")).withStyle(ChatFormatting.WHITE)
						.append(Component.literal(propertyName).withStyle(ChatFormatting.AQUA)));
				
				if (tag.contains("propertyBox")) {
					Box box = Box.load(tag.getCompound("propertyBox"));
					tooltip.add(Component.translatable(LangUtil.tooltip("lease.property_location")).withStyle(ChatFormatting.WHITE)
							.append(Component.translatable(String.format("(%s) to (%s)", 
									PropertyUtils.formatCoords(box.getMinCoords()), 
									PropertyUtils.formatCoords(box.getMaxCoords())
									))
							.withStyle(style -> {
								return style.withColor(ChatFormatting.GREEN);
//										.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + box.getMinCoords().getX() + " " + 
//								box.getMinCoords().getY() + " " + box.getMinCoords().getZ()))
//										.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")));
							})
							));
				}
			});
		}
	}
	
	@Override
	public  void appendSummary(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
		Component component = Component.translatable(LangUtil.tooltip("lease.howto"));
		for (String s : component.getString().split("~")) {	
			tooltip.add(Component.translatable(LangUtil.INDENT2)
					.append(Component.literal(s).withStyle(ChatFormatting.GREEN)));
		}
	}
	@Override
	public  void appendHoverSpecials(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flag) {
	}
	
	@Override
	public String getTitleKey() {
		return LangUtil.tooltip("lease.title");
	}
	
	@Override
	public Optional<Property> selectProperty(Player player, Property property) {
		ICoords coords = new Coords(player.blockPosition());
		
		// find the first unclaimed property that intersect the player position
		Property selectedProperty = null;
		if (!property.getChildren().isEmpty()) {
			List<Property> properties = property.getChildren();
			properties.addAll(property.getChildren().stream().flatMap(p -> p.getChildren().stream()).toList());
			// determine if any of the childen intersect with the coords and properties don't have an owner
			for (Property child : properties) {
				if (child.intersects(new Box(coords))) {
					selectedProperty = child;
					break;
				}
			}
		}
		return Optional.ofNullable(selectedProperty);
	}
	
	@Override
	public boolean ownershipCheck(Player player, Property property, UUID itemOwnerUuid, UUID itemPropertyUuid) {
		boolean isValid = false;
		UUID propertyOwnerUuid;
		
		if (ObjectUtils.isEmpty(property.getLandlord()) || StringUtils.isEmpty(property.getLandlord().getUuid())) {
			propertyOwnerUuid = EMPTY_UUID;
		}
		else {
			propertyOwnerUuid = UUID.fromString(property.getLandlord().getUuid());
		}

		if (propertyOwnerUuid.equals(itemOwnerUuid)) {
			isValid = true;

			// check the property ownship against the list item ownership
			if (!itemPropertyUuid.equals(EMPTY_UUID) && (!itemPropertyUuid.equals(property.getUuid()) && !itemPropertyUuid.equals(property.getParent())) ) {
				isValid = false;
			}
		}

		return isValid;
	}
}
