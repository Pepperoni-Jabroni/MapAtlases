package pepjebs.mapatlases.mixin;

import net.minecraft.item.map.MapIcon;

import java.util.Map;

public interface MapStateIntrfc {
    Map<String, MapIcon> getFullIcons();
}
