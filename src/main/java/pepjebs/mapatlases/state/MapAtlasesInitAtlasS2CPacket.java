package pepjebs.mapatlases.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.io.IOException;

public class MapAtlasesInitAtlasS2CPacket implements Packet<ClientPlayPacketListener> {

    public static final Identifier MAP_ATLAS_INIT = new Identifier(MapAtlasesMod.MOD_ID, "map_atlas_init");
    public static final Identifier MAP_ATLAS_SYNC = new Identifier(MapAtlasesMod.MOD_ID, "map_atlas_sync");

    private MapState mapState;

    public MapAtlasesInitAtlasS2CPacket(){}

    public MapAtlasesInitAtlasS2CPacket(MapState mapState1) {
        mapState = mapState1;
    }

    @Override
    public void read(PacketByteBuf buf) {
        int mapId = buf.readInt();
        mapState = new MapState("map_" + mapId);
        mapState.fromTag(buf.readCompoundTag());
    }

    @Override
    public void write(PacketByteBuf buf) {
        CompoundTag mapAsTag = new CompoundTag();
        mapState.toTag(mapAsTag);
        buf.writeInt(MapAtlasesAccessUtils.getMapIntFromState(mapState));
        buf.writeCompoundTag(mapAsTag);
    }

    @Override
    public void apply(ClientPlayPacketListener listener) {
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().world == null) return;
            MinecraftClient.getInstance().world.putMapState(mapState);
        });
    }

    public MapState getMapState() {return this.mapState;}
}
