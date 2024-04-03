package mod.gottsch.forge.protectit.core.command;

import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Mar 28, 2024
 *
 */
public class ParcelWhitelistCommandDelegate {


    public static int addToWhitelist(CommandSourceStack source, String ownerName, String parcelName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                parcel.get().getWhitelist().add(player.getUUID());
                CommandHelper.save(source.getLevel());
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.add.success")).withStyle(ChatFormatting.RED), false);

            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.add.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        return 1;
    }

    public static int displayWhitelist(CommandSourceStack source, String ownerName, String parcelName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                parcel.get().getWhitelist();
                CommandHelper.sendNewLineMessage(source);
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.list"))
                        .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
                        .append(Component.translatable(parcelName)
                                .withStyle(ChatFormatting.AQUA)), false);
                CommandHelper.sendNewLineMessage(source);
                parcel.get().getWhitelist().forEach(uuid -> {
                    // get the name for the uuid
                    ServerPlayer whitelistPlayer = source.getServer().getPlayerList().getPlayer(uuid);
                    if (whitelistPlayer != null) {
                        source.sendSuccess(() -> Component.translatable(whitelistPlayer.getName().getString()).withStyle(ChatFormatting.GREEN), false);
                    }
                });

            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.add.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }
        return 1;
    }

}
