package mod.gottsch.forge.protectit.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.protectit.core.ProtectIt;
import mod.gottsch.forge.protectit.core.config.Config;
import mod.gottsch.forge.protectit.core.item.DeedFactory;
import mod.gottsch.forge.protectit.core.parcel.ParcelType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.item.ItemStack;

/**
 *
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
public class OpsProtectCommand2 {
    private static final String PROTECT = "protect-ops";
    private static final String DEED = "deed";
    private static final String GENERATE = "generate";
    private static final String DEED_TYPE = "deed_type";
    private static final String X_SIZE = "x_size";
    private static final String Y_SIZE_UP = "y_size_up";
    private static final String Y_SIZE_DOWN = "y_size_down";
    private static final String Z_SIZE = "z_size";
    private static final String OWNER_NAME = "owner_name";

    private static final SuggestionProvider<CommandSourceStack> DEED_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(ParcelType.getNames(), builder);
    };

    /*
     * protect [deed [generate | ] | parcel [generate | remove] ] //give | list | rename | whitelist [add | remove | clear | list]]
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher
                .register(Commands.literal(PROTECT).requires(source -> {
                            return source.hasPermission(Config.GENERAL.opsPermissionLevel.get()); // only ops can use command
                        })
                        ///// DEED TOP-LEVEL OPTION /////
                        .then(Commands.literal(DEED)
                                ///// GENERATE OPTION /////
                                .then(Commands.literal(GENERATE)
                                        .then(Commands.argument(DEED_TYPE, StringArgumentType.string())
                                                .suggests(DEED_TYPES)
                                                .then(Commands.argument(X_SIZE, IntegerArgumentType.integer())
                                                        .then(Commands.argument(Y_SIZE_UP, IntegerArgumentType.integer())
                                                                .then(Commands.argument(Y_SIZE_DOWN, IntegerArgumentType.integer())
                                                                        .then(Commands.argument(Z_SIZE, IntegerArgumentType.integer())
                                                                                .executes(source -> {
                                                                                    return generateDeed(source.getSource(),
                                                                                            StringArgumentType.getString(source, DEED_TYPE),
                                                                                            IntegerArgumentType.getInteger(source, X_SIZE),
                                                                                            IntegerArgumentType.getInteger(source, Y_SIZE_UP),
                                                                                            IntegerArgumentType.getInteger(source, Y_SIZE_DOWN),
                                                                                            IntegerArgumentType.getInteger(source, Z_SIZE)
                                                                                            );
                                                                                })
                                                                        )
                                                                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                                                                .executes(source -> {
                                                                                    return generateDeed(source.getSource(),
                                                                                            StringArgumentType.getString(source, DEED_TYPE),
                                                                                            IntegerArgumentType.getInteger(source, X_SIZE),
                                                                                            IntegerArgumentType.getInteger(source, Y_SIZE_UP),
                                                                                            IntegerArgumentType.getInteger(source, Y_SIZE_DOWN),
                                                                                            IntegerArgumentType.getInteger(source, Z_SIZE));
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )

                );



    } // end of method

    private static int generateDeed(CommandSourceStack source, String deedType, int xSize, int ySizeUp, int ySizeDown, int zSize) {
        // create a relative sized Box
        Box size = new Box(new Coords(0, -ySizeDown, 0), new Coords(xSize, ySizeUp, zSize));

        // create a deed item
        ItemStack deed = switch (ParcelType.valueOf(deedType)) {
            case PERSONAL -> {
                yield DeedFactory.createPersonalDeed(size);
            }
            case NATION -> null;
            case CITIZEN -> null;
        };

        // attempt to add the deed item to the player inventory
        try {
            if (deed != null && deed != ItemStack.EMPTY) {
                source.getPlayerOrException().getInventory().add(deed);
            }
        } catch(Exception e) {
            ProtectIt.LOGGER.error("error on give -> ", e);
        }

        return 1;
    }
}
