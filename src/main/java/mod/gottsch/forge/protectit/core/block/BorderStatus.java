package mod.gottsch.forge.protectit.core.block;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Mark Gottschling on Apr 2, 2024
 *
 */
public enum BorderStatus implements StringRepresentable {
    GOOD("good"),
    BAD("bad");

    private final String name;

    BorderStatus(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
};