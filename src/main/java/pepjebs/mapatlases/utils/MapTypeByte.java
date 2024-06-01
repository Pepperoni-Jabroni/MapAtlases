package pepjebs.mapatlases.utils;

import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.registry.entry.RegistryEntry;

public enum MapTypeByte {
    PLAYER(MapDecorationTypes.PLAYER),
    FRAME(MapDecorationTypes.FRAME),
    RED_MARKER(MapDecorationTypes.RED_MARKER),
    BLUE_MARKER(MapDecorationTypes.BLUE_MARKER),
    TARGET_X(MapDecorationTypes.TARGET_X),
    TARGET_POINT(MapDecorationTypes.TARGET_POINT),
    PLAYER_OFF_MAP(MapDecorationTypes.PLAYER_OFF_MAP),
    PLAYER_OFF_LIMITS(MapDecorationTypes.PLAYER_OFF_LIMITS),
    MANSION(MapDecorationTypes.MANSION),
    MONUMENT(MapDecorationTypes.MONUMENT),
    BANNER_WHITE(MapDecorationTypes.BANNER_WHITE),
    BANNER_ORANGE(MapDecorationTypes.BANNER_ORANGE),
    BANNER_MAGENTA(MapDecorationTypes.BANNER_MAGENTA),
    BANNER_LIGHT_BLUE(MapDecorationTypes.BANNER_LIGHT_BLUE),
    BANNER_YELLOW(MapDecorationTypes.BANNER_YELLOW),
    BANNER_LIME(MapDecorationTypes.BANNER_LIME),
    BANNER_PINK(MapDecorationTypes.BANNER_PINK),
    BANNER_GRAY(MapDecorationTypes.BANNER_GRAY),
    BANNER_LIGHT_GRAY(MapDecorationTypes.BANNER_LIGHT_GRAY),
    BANNER_CYAN(MapDecorationTypes.BANNER_CYAN),
    BANNER_PURPLE(MapDecorationTypes.BANNER_PURPLE),
    BANNER_BLUE(MapDecorationTypes.BANNER_BLUE),
    BANNER_BROWN(MapDecorationTypes.BANNER_BROWN),
    BANNER_GREEN(MapDecorationTypes.BANNER_GREEN),
    BANNER_RED(MapDecorationTypes.BANNER_RED),
    BANNER_BLACK(MapDecorationTypes.BANNER_BLACK),
    RED_X(MapDecorationTypes.RED_X),
    VILLAGE_DESERT(MapDecorationTypes.VILLAGE_DESERT),
    VILLAGE_PLAINS(MapDecorationTypes.VILLAGE_PLAINS),
    VILLAGE_SAVANNA(MapDecorationTypes.VILLAGE_SAVANNA),
    VILLAGE_SNOWY(MapDecorationTypes.VILLAGE_SNOWY),
    VILLAGE_TAIGA(MapDecorationTypes.VILLAGE_TAIGA),
    JUNGLE_TEMPLE(MapDecorationTypes.JUNGLE_TEMPLE),
    SWAMP_HUT(MapDecorationTypes.SWAMP_HUT),
    TRIAL_CHAMBERS(MapDecorationTypes.TRIAL_CHAMBERS);
    
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
