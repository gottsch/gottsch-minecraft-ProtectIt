package mod.gottsch.forge.protectit.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.item.Deed;
import mod.gottsch.forge.protectit.core.item.DeedFactory;
import mod.gottsch.forge.protectit.core.parcel.NationParcel;
import mod.gottsch.forge.protectit.core.parcel.Parcel;
import mod.gottsch.forge.protectit.core.parcel.ParcelFactory;
import mod.gottsch.forge.protectit.core.parcel.ParcelType;
import mod.gottsch.forge.protectit.core.persistence.PersistedData;
import mod.gottsch.forge.protectit.core.registry.ParcelRegistry;
import mod.gottsch.forge.protectit.core.util.LangUtil;
import mod.gottsch.forge.protectit.core.util.ModUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
public class OpsProtectCommand2 {


    private static final SuggestionProvider<CommandSourceStack> DEED_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(ParcelType.getNames().stream().filter(p -> !p.equalsIgnoreCase("citizen")), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PARCEL_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        // get the UUID for the name
        ServerPlayer player = source.getSource().getServer().getPlayerList().getPlayerByName(ownerName);
        List<String> parcels = new ArrayList<>();
        if (player != null) {
            parcels = ParcelRegistry.findByOwner(player.getUUID()).stream().map(Parcel::getName).toList();
        }
        return SharedSuggestionProvider.suggest(parcels, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> OWNER_NAMES = (source, builder) -> {
        PlayerList playerList = source.getSource().getServer().getPlayerList();
        List<String> names = ParcelRegistry.getOwnerIds().stream().map(id -> {
            ServerPlayer player = playerList.getPlayer(id);
            return player != null ? player.getName().getString() : "";
        }).toList();

        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> NATION_NAMES = (source, builder) -> {
        List<String> names = ParcelRegistry.getNations().stream().map((Parcel::getName)).toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> NATION_DEED_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(ParcelType.getNames().stream().filter(p -> !p.equalsIgnoreCase("personal")), builder);
    };

    /*
     * protect [deed [generate | ] | parcel [generate | remove] ] //give | list | rename | whitelist [add | remove | clear | list]]
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher
                .register(Commands.literal(CommandHelper.PROTECT).requires(source -> {
                                    return source.hasPermission(Config.GENERAL.opsPermissionLevel.get()); // only ops can use command
                                })
                                ///// DEED TOP-LEVEL OPTION /////
                                .then(Commands.literal(CommandHelper.DEED).requires(source -> {
                                                    return source.hasPermission(Config.GENERAL.opsPermissionLevel.get());
                                                })
                                                ///// GENERATE OPTION /////
                                                .then(Commands.literal(CommandHelper.GENERATE)
                                                        ///// NEW DEED /////
                                                        .then(Commands.literal(CommandHelper.NEW)
                                                                // TODO somehow this has to change because a CITIZEN deed requires a nation id
                                                                .then(Commands.argument(CommandHelper.DEED_TYPE, StringArgumentType.string())
                                                                        .suggests(DEED_TYPES)
                                                                        .then(Commands.argument(CommandHelper.X_SIZE, IntegerArgumentType.integer())
                                                                                .then(Commands.argument(CommandHelper.Y_SIZE_UP, IntegerArgumentType.integer())
                                                                                        .then(Commands.argument(CommandHelper.Y_SIZE_DOWN, IntegerArgumentType.integer())
                                                                                                .then(Commands.argument(CommandHelper.Z_SIZE, IntegerArgumentType.integer())
                                                                                                        .executes(source -> {
                                                                                                            return generateDeed(source.getSource(),
                                                                                                                    StringArgumentType.getString(source, CommandHelper.DEED_TYPE),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE)
                                                                                                            );
                                                                                                        })
                                                                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                                                .executes(source -> {
                                                                                                                    return generateDeed(source.getSource(),
                                                                                                                            StringArgumentType.getString(source, CommandHelper.DEED_TYPE),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE));
                                                                                                                })
                                                                                                        )

                                                                                                )

                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        ///// FROM PARCEL /////
                                                        .then(Commands.literal(CommandHelper.PARCEL)
                                                                .then(Commands.literal(CommandHelper.BY_OWNER)
                                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                .suggests(OWNER_NAMES)
                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                        .suggests(PARCEL_NAMES)
                                                                                        .executes(source -> {
                                                                                            return generateDeedFromParcel(source.getSource(),
                                                                                                    StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                                    StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                    "");
                                                                                        })
                                                                                        .then(Commands.argument(CommandHelper.NEW_OWNER_NAME, StringArgumentType.string())
                                                                                                .suggests(OWNER_NAMES)
                                                                                                .executes(source -> {
                                                                                                    return generateDeedFromParcel(source.getSource(),
                                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                                            StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                            StringArgumentType.getString(source, CommandHelper.NEW_OWNER_NAME));
                                                                                                })
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        .then(Commands.literal("citizen_of_nation")
                                                                .then(Commands.argument(CommandHelper.NATION_NAME, StringArgumentType.string())
                                                                        .suggests(NATION_NAMES)
                                                                        .then(Commands.argument(CommandHelper.X_SIZE, IntegerArgumentType.integer())
                                                                                .then(Commands.argument(CommandHelper.Y_SIZE_UP, IntegerArgumentType.integer())
                                                                                        .then(Commands.argument(CommandHelper.Y_SIZE_DOWN, IntegerArgumentType.integer())
                                                                                                .then(Commands.argument(CommandHelper.Z_SIZE, IntegerArgumentType.integer())
                                                                                                        .executes(source -> {
                                                                                                            return generateDeedFromNation(source.getSource(),
                                                                                                                    StringArgumentType.getString(source, CommandHelper.NATION_NAME),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE)
                                                                                                                    );
                                                                                                         })
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                )
                                ///// PARCEL TOP-LEVEL OPTION /////
                                .then(Commands.literal(CommandHelper.PARCEL).requires(source -> {
                                                    return source.hasPermission(Config.GENERAL.opsPermissionLevel.get());
                                                })
                                                ///// LIST OPTION /////
                                                .then(Commands.literal(CommandHelper.LIST)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .executes(source -> {
                                                                    return ParcelCommandDelegate.listParcels(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME));
                                                                })
                                                        )
                                                )

                                                ///// ADD OPTION /////
                                                .then(Commands.literal(CommandHelper.ADD)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.POS, BlockPosArgument.blockPos())
                                                                        .then(Commands.argument(CommandHelper.X_SIZE, IntegerArgumentType.integer())
                                                                                .then(Commands.argument(CommandHelper.Y_SIZE_UP, IntegerArgumentType.integer())
                                                                                        .then(Commands.argument(CommandHelper.Y_SIZE_DOWN, IntegerArgumentType.integer())
                                                                                                .then(Commands.argument(CommandHelper.Z_SIZE, IntegerArgumentType.integer())
                                                                                                        .then(Commands.argument(CommandHelper.DEED_TYPE, StringArgumentType.string())
                                                                                                                .suggests(DEED_TYPES)
                                                                                                                .executes(source -> {
                                                                                                                    return ParcelCommandDelegate.addParcel(source.getSource(),
                                                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                                                            BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE),
                                                                                                                            StringArgumentType.getString(source, CommandHelper.DEED_TYPE)
                                                                                                                            // TODO add Nation ID/Name
                                                                                                                    );
                                                                                                                })
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )

                                                .then(Commands.literal(CommandHelper.REMOVE)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                        .suggests(PARCEL_NAMES)
                                                                        .executes(source -> {
                                                                            return ParcelCommandDelegate.removeParcel(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
                                                                        })

                                                                )
                                                        )
                                                )
                                                ///// RENAME PARCEL /////
                                                .then(Commands.literal(CommandHelper.RENAME)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                        .suggests(PARCEL_NAMES)
                                                                        .then(Commands.argument(CommandHelper.NEW_NAME, StringArgumentType.string())
                                                                                .executes(source -> {
                                                                                    return ParcelCommandDelegate.renameParcel(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.NEW_NAME));
                                                                                })
                                                                        )

                                                                )
                                                        )
                                                )
                                                ///// TRANSFER /////
                                                .then(Commands.literal(CommandHelper.TRANSFER)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                        .suggests(PARCEL_NAMES)
                                                                        .then(Commands.argument(CommandHelper.NEW_OWNER_NAME, StringArgumentType.string())
                                                                                .suggests(OWNER_NAMES)
                                                                                .executes(source -> {
                                                                                    return ParcelCommandDelegate.transferParcel(source.getSource(),
                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.NEW_OWNER_NAME));
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                                ///// CLEAR /////
                                                .then(Commands.literal(CommandHelper.CLEAR)
                                                        .executes(source -> {
                                                            return ParcelCommandDelegate.clearAllParcels(source.getSource());
                                                        })
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .executes(source -> {
                                                                    return ParcelCommandDelegate.clearOwnerParcels(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME));
                                                                })
                                                        )
                                                )
                                                ///// WHITELIST OPTION /////
                                                .then(Commands.literal(CommandHelper.WHITELIST)
                                                        ///// WHITELIST ADD /////
                                                        .then(Commands.literal(CommandHelper.ADD)
                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                        .suggests(OWNER_NAMES)
                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                .suggests(PARCEL_NAMES)
                                                                                .executes(source -> {
                                                                                    return ParcelWhitelistCommandDelegate.addToWhitelist(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                        ///// WHITELIST LIST /////
                                                        .then(Commands.literal(CommandHelper.LIST)
                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                        .suggests(OWNER_NAMES)
                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                .suggests(PARCEL_NAMES)
                                                                                .executes(source -> {
                                                                                    return ParcelWhitelistCommandDelegate.displayWhitelist(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )


                                                .then(Commands.literal(CommandHelper.BACKUP)
                                                        .executes(source -> {
                                                            return ParcelCommandDelegate.backupParcels(source.getSource());
                                                        })
                                                )
                                )
                );


    } // end of method

    /**
     *
     * @param source
     * @param deedType
     * @param xSize
     * @param ySizeUp
     * @param ySizeDown
     * @param zSize
     * @return
     */
    private static int generateDeed(CommandSourceStack source, String deedType, int xSize, int ySizeUp, int ySizeDown, int zSize) {
        // create a relative sized Box
        Box size = new Box(new Coords(0, -ySizeDown, 0), new Coords(xSize-1, ySizeUp-1, zSize-1));

        // create a deed item
        ItemStack deed = switch (ParcelType.valueOf(deedType)) {
            case PERSONAL -> DeedFactory.createPersonalDeed(size);
            case NATION -> DeedFactory.createNationDeed(size);
            // TODO requires the NATION_ID
            case CITIZEN -> DeedFactory.createCitizenDeed(size, null);
        };

        // attempt to add the deed item to the player inventory
        try {
            if (deed != ItemStack.EMPTY) {
                source.getPlayerOrException().getInventory().add(deed);
            }
        } catch (Exception e) {
            ProtectIt.LOGGER.error("error on give -> ", e);
            source.sendSuccess(() -> Component.translatable(LangUtil.chat(" deed.generate.failure")).withStyle(ChatFormatting.RED), false);
        }

        return 1;
    }

    /**
     * @param source
     * @param ownerName
     * @param parcelName
     * @return
     */
    private static int generateDeedFromParcel(CommandSourceStack source, String ownerName, String parcelName, String newOwnerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                ItemStack deed = DeedFactory.createDeed(parcel.get().getClass(), parcel.get().getSize());
                CompoundTag tag = deed.getOrCreateTag();
                tag.putUUID(Deed.PARCEL_ID, parcel.get().getId());
                // set owner id if present
                if(StringUtils.isNotBlank(newOwnerName)) {
                    ServerPlayer newOwner = source.getServer().getPlayerList().getPlayerByName(newOwnerName);
                    if (newOwner != null) {
                        tag.putUUID(Deed.OWNER_ID, newOwner.getUUID());
                    } else {
                        CommandHelper.sendUnableToLocatePlayerMessage(source, newOwnerName);
                        return -1;
                    }
                }
                // attempt to add the deed item to the player inventory
                try {
                    if (deed != null && deed != ItemStack.EMPTY) {
                        source.getPlayerOrException().getInventory().add(deed);
                    }
                } catch (Exception e) {
                    ProtectIt.LOGGER.error("error on give -> ", e);
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED), false);

                }
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.generate.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }

        return 1;
    }

    private static int generateDeedFromNation(CommandSourceStack source, String nationName, int xSize, int ySizeUp, int ySizeDown, int zSize) {
        // create a relative sized Box
        Box size = new Box(new Coords(0, -ySizeDown, 0), new Coords(xSize-1, ySizeUp-1, zSize-1));

        Optional<Parcel> parcel = ParcelRegistry.getNations().stream().filter(p -> p.getName().equalsIgnoreCase(nationName)).findFirst();
        if (parcel.isPresent()) {
            ItemStack deed = DeedFactory.createCitizenDeed(size, ((NationParcel)parcel.get()).getNationId());
            CompoundTag tag = deed.getOrCreateTag();

            // attempt to add the deed item to the player inventory
            try {
                if (deed != ItemStack.EMPTY) {
                    source.getPlayerOrException().getInventory().add(deed);
                }
            } catch (Exception e) {
                ProtectIt.LOGGER.error("error on give -> ", e);
                CommandHelper.sendUnableToGenerateDeedMessage(source, nationName);
             }
        } else {
            // TODO can't find nation
        }
        return 1;
    }
}