package pepjebs.mapatlases.networking;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;

public record MapAtlasesOpenGUIPacket(ItemStack atlas) implements CustomPayload {
    public static final CustomPayload.Id<MapAtlasesOpenGUIPacket> PACKET_ID = new Id<MapAtlasesOpenGUIPacket>(new Identifier(MapAtlasesMod.MOD_ID, "open_gui"));

    public static final PacketCodec<RegistryByteBuf, MapAtlasesOpenGUIPacket> PACKET_CODEC = PacketCodec.tuple(ItemStack.PACKET_CODEC, (a) -> {return a.atlas();}, MapAtlasesOpenGUIPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
    
}
