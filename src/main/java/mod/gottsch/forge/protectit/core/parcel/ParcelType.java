package mod.gottsch.forge.protectit.core.parcel;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public enum ParcelType {
    PERSONAL,
    NATION,
    CITIZEN;

    public static List<String> getNames() {
        return EnumSet.allOf(ParcelType.class).stream().map(Enum::name).collect(Collectors.toList());
    }
}
