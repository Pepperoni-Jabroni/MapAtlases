package pepjebs.mapatlases.networking;

import java.util.List;
import java.util.Optional;

import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;

public record MapAtlasesSyncPacket(MapIdComponent mapId, byte scale, boolean locked, Optional<List<MapDecoration>> decorations, Optional<MapState.UpdateData> updateData) implements CustomPayload {
    public static final CustomPayload.Id<MapAtlasesSyncPacket> PACKET_ID = new Id<MapAtlasesSyncPacket>(new Identifier(MapAtlasesMod.MOD_ID, "map_atlas_sync"));

    public static final PacketCodec<RegistryByteBuf, MapAtlasesSyncPacket> PACKET_CODEC = PacketCodec.tuple(MapIdComponent.PACKET_CODEC, MapAtlasesSyncPacket::mapId, PacketCodecs.BYTE, MapAtlasesSyncPacket::scale, PacketCodecs.BOOL, MapAtlasesSyncPacket::locked, MapDecoration.CODEC.collect(PacketCodecs.toList()).collect(PacketCodecs::optional), MapAtlasesSyncPacket::decorations, MapState.UpdateData.CODEC, MapAtlasesSyncPacket::updateData, MapAtlasesSyncPacket::new);
    
    @Override
    public Id<? extends CustomPayload> getId() {
       return PACKET_ID;
    }
    
}
