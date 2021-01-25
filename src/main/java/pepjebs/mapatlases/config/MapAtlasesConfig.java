package pepjebs.mapatlases.config;

import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import me.sargunvohra.mcmods.autoconfig1u.shadowed.blue.endless.jankson.Comment;
import pepjebs.mapatlases.MapAtlasesMod;

@Config(name = MapAtlasesMod.MOD_ID)
public class MapAtlasesConfig implements ConfigData {

    @Comment("The maximum number of Maps (Filled & Empty combined) allowed to be inside an Atlas.")
    public int maxMapCount = 128;

    @Comment("Scale the mini-map to a given pixel size. (Default is 64)")
    public int forceMiniMapScaling = 64;

    @Comment("If 'true', the Mini-Map of the Active Map will be drawn on the HUD while the Atlas is on your hot-bar or off-hand.")
    public boolean drawMiniMapHUD = true;
}
