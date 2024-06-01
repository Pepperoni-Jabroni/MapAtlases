package pepjebs.mapatlases.mixin;

import com.google.common.collect.Maps;

import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pepjebs.mapatlases.utils.MapStateIntrfc;

import java.util.Map;

@Mixin(value = MapState.class, priority = 1100)
public class MapStateMixin implements MapStateIntrfc {
    @Shadow
    Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();

    public Map<String, MapDecoration> getFullIcons(){
        return decorations;
    }
}
