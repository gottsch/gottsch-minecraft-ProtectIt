package mod.gottsch.forge.protectit.core.item;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import mod.gottsch.forge.protectit.core.property.Property;
import mod.gottsch.forge.protectit.core.registry.ProtectionRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 
 * @author Mark Gottschling Feb 27, 2023
 *
 */
public class SubdivideLicense extends Item {
	/*
	 * the owner uuid. 
	 * the property to subdivide owner must match this owner
	 */
	private UUID owner;
	/*
	 * the property uuid.
	 * to further restrict which property can be subdivided
	 */
	private UUID property;
	
	public SubdivideLicense(Properties properties) {
		super(properties);
	}

	@Override
	   public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
	      ItemStack itemStack = player.getItemInHand(hand);

		// exit if on the client
		if (WorldInfo.isClientSide(world)) {
	         return InteractionResultHolder.pass(itemStack);
		}
        CompoundTag tag = itemStack.getTag();
        if (tag != null)	{
        	if (tag.contains("owner")) {
        		UUID owner = tag.getUUID("owner");
        	}
        	if (tag.contains("property")) {
        		UUID property = tag.getUUID("property");
        	}
        	
        	// TODO locate property (by owner, property or both
        	if (owner != null) {
        		ICoords coords = new Coords(player.blockPosition());
        		// get all properties by coords and uuid
        		List<Box> protections = ProtectionRegistries.block().getProtections(coords);
        		if (protections.isEmpty()) {
        			// TODO message
        			return InteractionResultHolder.pass(itemStack);
        		}
//        		List<Property> properties = protections.stream()
//        				.map(b -> ProtectionRegistries.block().getClaimByCoords(b.getMinCoords()))
//        				.filter(p -> p.getOwner().getUuid().equalsIgnoreCase(owner.toString()))
//        				.collect(Collectors.toList());
//        		if (properties.isEmpty()) {
//        			// TODO message
//        			InteractionResultHolder.pass(itemStack);
//        		}        		
//        		Property property = properties.get(0);
        		Property property = ProtectionRegistries.block().getClaimByCoords(protections.get(0).getMinCoords()); 		
        		
        		Property selectedProperty = property;
        		if (!property.getChildren().isEmpty()) {
        			// determine if any of the childen intersect with the coords
        			for (Property child : property.getChildren()) {
        				if (child.intersects(new Box(coords))) {
        					selectedProperty = child;
        					break;
        				}
        			}
        		}
        		
        		// update the property with the the flag
        		selectedProperty.setSubdivisible(true);
        		
        		return InteractionResultHolder.consume(itemStack);
        	}
        }

        return InteractionResultHolder.pass(itemStack);
	}
	
	public UUID getOwner() {
		return owner;
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	public UUID getProperty() {
		return property;
	}

	public void setProperty(UUID property) {
		this.property = property;
	}

}
