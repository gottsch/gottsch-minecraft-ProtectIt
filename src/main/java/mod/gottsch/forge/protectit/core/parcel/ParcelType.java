package mod.gottsch.forge.protectit.core.parcel;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public enum ParcelType implements StringRepresentable {
    PERSONAL,
    NATION,
    CITIZEN;

    public static List<String> getNames() {
        return EnumSet.allOf(ParcelType.class).stream().map(Enum::name).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return this.name();
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name();
    }
}
