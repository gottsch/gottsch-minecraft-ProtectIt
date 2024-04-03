package mod.gottsch.forge.protectit.core.command;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.ParcelFactory;
import mod.gottsch.forge.protectit.core.parcel.ParcelType;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import mod.gottsch.forge.protectit.core.util.ModUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Mar 28, 2024
 *
 */
public class ParcelCommandDelegate {

    /**
     *
     * @param source
     * @param ownerName
     * @return
     */
    public static int listParcels(CommandSourceStack source, String ownerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        return listParcels(source, player);
    }

    public static int listParcels(CommandSourceStack source, ServerPlayer player) {
        List<Component> messages = new ArrayList<>();
        messages.add(Component.literal(""));
        messages.add(Component.translatable(LangUtil.chat("parcel.list"), player.getName().getString()).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
        messages.add(Component.literal(""));

        List<Component> components = formatList(messages, ParcelRegistry.findByOwner(player.getUUID()));
        components.forEach(component -> {
            source.sendSuccess(() -> component, false);
        });
        return 1;
    }

    static List<Component> formatList(List<Component> messages, List<Parcel> list) {

        if (list.isEmpty()) {
            messages.add(Component.translatable(LangUtil.chat("parcel.list.empty")).withStyle(ChatFormatting.AQUA));
        }
        else {
            list.forEach(parcel -> {
                messages.add(Component.translatable(parcel.getName().toUpperCase() + ": ").withStyle(ChatFormatting.AQUA)
                        .append(Component.translatable(String.format("(%s) to (%s)",
                                formatCoords(parcel.getMinCoords()),
                                formatCoords(parcel.getMaxCoords()))).withStyle(ChatFormatting.GREEN)
                        )
                        .append(Component.translatable(", size: (" + formatCoords(ModUtil.getSize( parcel.getSize()) ) + ")").withStyle(ChatFormatting.WHITE))
                );

//				[STYLE].withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockpos.getX() + " " + s1 + " " + blockpos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))
            });
        }
        return messages;
    }

    private static String formatCoords(ICoords coords) {
        return String.format("%s, %s, %s", coords.getX(), coords.getY(), coords.getZ());
    }

    /*
     *
     */
    public static int addParcel(CommandSourceStack source, String ownerName, BlockPos pos, int xSize, int ySizeUp, int ySizeDown, int zSize, String parcelType) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            // create a relative sized Box
            Box size = new Box(new Coords(0, -ySizeDown, 0), new Coords(xSize-1, ySizeUp-1, zSize-1));
            ICoords coords = new Coords(pos);
            Optional<Parcel> parcelOptional = ParcelFactory.create(ParcelType.valueOf(parcelType));
            if (parcelOptional.isPresent()) {
                Parcel parcel = parcelOptional.get();
                parcel.setOwnerId(player.getUUID());
                parcel.setCoords(coords);
                parcel.setSize(size);

                // ensure that it still meets overlap criteria
                Box parcelBox = parcel.getBox();
                Box inflatedBox = ModUtil.inflate(parcelBox, parcel.getBufferSize());
                // TODO need to change when Nation and Citizen parcels are implemented
                // TODO need a method that checks if placement is valid
                // ie Parcel.isPlacementValid(parcel) which checks all overlaps if any are person or citizen, or if nation, then check
                // 1) does this parcel belong to the nation
                // 2) is totally within bounds of nation
                if (ParcelRegistry.findBoxes(inflatedBox).isEmpty()) {
                    ParcelRegistry.add(parcel);
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.add.success")).withStyle(ChatFormatting.GREEN), false);
                    CommandHelper.save(source.getLevel());
                } else {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.add.failure_with_overlaps")).withStyle(ChatFormatting.GREEN), false);
                }

            } else {
                source.sendFailure(Component.translatable(LangUtil.chat("parcel.add.failure")).withStyle(ChatFormatting.GREEN));
            }

        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int removeParcel(CommandSourceStack source, String ownerName, String parcelName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // remove the border
                BlockEntity blockEntity = source.getLevel().getBlockEntity(parcel.get().getCoords().toPos());
                if (blockEntity instanceof FoundationStoneBlockEntity) {
                    ((FoundationStoneBlockEntity)blockEntity).removeParcelBorder(parcel.get().getCoords());
                }
                // unregister the parcel
                ParcelRegistry.removeParcel(parcel.get());

                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int renameParcel(CommandSourceStack source, String ownerName, String parcelName, String newName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                parcel.get().setName(newName);
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int transferParcel(CommandSourceStack source, String ownerName, String parcelName, String newOwnerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // get the new owner
                if(StringUtils.isNotBlank(newOwnerName)) {
                    ServerPlayer newOwner = source.getServer().getPlayerList().getPlayerByName(newOwnerName);
                    if (newOwner != null) {
                        parcel.get().setOwnerId(newOwner.getUUID());
                    } else {
                        CommandHelper.sendUnableToLocatePlayerMessage(source, newOwnerName);
                        return -1;
                    }
                }
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.transfer.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.transfer.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int clearOwnerParcels(CommandSourceStack source, String ownerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            // remove all properties from player
            ParcelRegistry.removeParcel(player.getUUID());
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.success")).withStyle(ChatFormatting.GREEN), false);
            CommandHelper.save(source.getLevel());
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int clearAllParcels(CommandSourceStack source) {
        // TODO move registry to a backup registry
        // TODO add an undo command
        ParcelRegistry.clear();
        // TODO message
        return 1;
    }

    /**
     * TODO flesh out to save to file(s)
     * @param source
     * @return
     */
    public static int backupParcels(CommandSourceStack source) {
        String dump = ParcelRegistry.toJson();
        ProtectIt.LOGGER.debug("backup -> {}", dump);
        return 1;
    }
}
