package mod.gottsch.forge.protectit.core.parcel;

import java.util.List;
import java.util.Optional;

public class ParcelUtil {
    private ParcelUtil() {}

    public static Optional<Parcel> findLeastSignificant(List<Parcel> parcels) {
        Parcel parcel = null;
        if (parcels.isEmpty()) {
            return Optional.empty();
        }
        else if (parcels.size() == 1) {
            parcel = parcels.get(0);
        } else {
            parcel = parcels.get(0);
            for (Parcel p : parcels) {
                if (p != parcel) {
                    if (p.getArea() < parcel.getArea()) {
                        parcel = p;
                    }
                }
            }
        }
        return Optional.ofNullable(parcel);
    }
}
