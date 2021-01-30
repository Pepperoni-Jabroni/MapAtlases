package pepjebs.mapatlases.config;

import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry;
import me.sargunvohra.mcmods.autoconfig1u.shadowed.blue.endless.jankson.Comment;
import pepjebs.mapatlases.MapAtlasesMod;

@Config(name = MapAtlasesMod.MOD_ID)
public class MapAtlasesConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip()
    @Comment("The maximum number of Maps (Filled & Empty combined) allowed to be inside an Atlas.")
    public int maxMapCount = 128;

    @ConfigEntry.Gui.Tooltip()
    @Comment("Scale the mini-map to a given pixel size. (Default is 64)")
    public int forceMiniMapScaling = 64;

    @ConfigEntry.Gui.Tooltip()
    @Comment("If 'true', the Mini-Map of the Active Map will be drawn on the HUD while the Atlas is on your hot-bar or off-hand.")
    public boolean drawMiniMapHUD = true;

    @ConfigEntry.Gui.Tooltip()
    @Comment("If 'true', Atlases will be able to store Empty Maps and auto-fill them as you explore.")
    public boolean enableEmptyMapEntryAndFill = true;

    @ConfigEntry.Gui.Tooltip()
    @Comment("If 'true', Atlases will require to be held in Main or Off Hands to be displayed or updated.")
    public boolean forceUseInHands = false;
}
