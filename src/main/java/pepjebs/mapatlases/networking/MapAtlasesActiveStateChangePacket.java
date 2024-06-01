package pepjebs.mapatlases.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;

public record MapAtlasesActiveStateChangePacket(String activeMapId) implements CustomPayload {

    public static final CustomPayload.Id<MapAtlasesActiveStateChangePacket> PACKET_ID = new Id<MapAtlasesActiveStateChangePacket>(new Identifier(
        MapAtlasesMod.MOD_ID, "active_state_change"));

    public static final PacketCodec<RegistryByteBuf, MapAtlasesActiveStateChangePacket> PACKET_CODEC = PacketCodec.tuple(PacketCodecs.STRING, MapAtlasesActiveStateChangePacket::activeMapId, MapAtlasesActiveStateChangePacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
    
}
