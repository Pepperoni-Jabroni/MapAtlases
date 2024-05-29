package pepjebs.mapatlases.networking;

import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;

public class MapAtlasesInitAtlasPacket implements CustomPayload {
    public static final CustomPayload.Id<MapAtlasesInitAtlasPacket> PACKET_ID = new Id<MapAtlasesInitAtlasPacket>(new Identifier(MapAtlasesMod.MOD_ID, "map_atlas_init"));

    public static final PacketCodec<RegistryByteBuf, MapAtlasesInitAtlasPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeString(value.mapId);
        buf.writeNbt(value.mapState.writeNbt(new NbtCompound(), BuiltinRegistries.createWrapperLookup()));
        
    }, (buf) -> {
        return new MapAtlasesInitAtlasPacket(buf.readString(), buf.readNbt());
    });

    public final String mapId;
    public final MapState mapState;

    public MapAtlasesInitAtlasPacket(String mapId, NbtCompound nbt) {
        this.mapId = mapId;
        if (nbt == null) {
            MapAtlasesMod.LOGGER.warn("Null MapState NBT received by client");
            mapState = null;
        } else {
            mapState = MapState.fromNbt(nbt, BuiltinRegistries.createWrapperLookup());
        }
    }

    public MapAtlasesInitAtlasPacket(String mapId1, MapState mapState1) {
        mapId = mapId1;
        mapState = mapState1;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}