package pepjebs.mapatlases.mixin;

import com.google.common.collect.Maps;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(MapState.class)
public class MapStateMemberAccessor {
    @Shadow
    Map<String, MapIcon> icons = Maps.newLinkedHashMap();

    Map<String, MapIcon> getFullIcons(){
        return icons;
    }
}
