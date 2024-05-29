package pepjebs.mapatlases.utils;

import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.registry.entry.RegistryEntry;

public enum MapTypeByte {
    PLAYER(MapDecorationTypes.PLAYER),
    FRAME(MapDecorationTypes.FRAME),
    RED_MARKER(MapDecorationTypes.RED_MARKER);
    public final byte id;
    public final RegistryEntry<MapDecorationType> type;
    private MapTypeByte(RegistryEntry<MapDecorationType> type) {
        this.type = type;
        this.id = (byte)this.ordinal();
    }

    public static byte getTypeId(RegistryEntry<MapDecorationType> type) {
        for(MapTypeByte b : MapTypeByte.values()) {
            if(b.type.equals(type)) return b.id;
        }
        return (byte)Integer.MAX_VALUE;
        
    }
}
