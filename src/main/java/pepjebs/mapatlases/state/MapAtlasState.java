package pepjebs.mapatlases.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.PersistentState;

public class MapAtlasState extends PersistentState {
    public MapAtlasState(String key) {
        super(key);
    }

    @Override
    public void fromTag(CompoundTag tag) {

    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        return null;
    }
}
