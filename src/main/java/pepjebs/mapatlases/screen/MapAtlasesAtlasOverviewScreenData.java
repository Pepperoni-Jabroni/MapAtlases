package pepjebs.mapatlases.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Pair;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record MapAtlasesAtlasOverviewScreenData(ItemStack atlas, String centerMapId, int atlasScale, Map<Integer, Pair<String,List<Integer>>> idsToCenters) {
    static PacketCodec<RegistryByteBuf, Map<Integer, Pair<String,List<Integer>>>> mapCodec = PacketCodecs.map(HashMap::new, PacketCodecs.INTEGER,
    pairCodec(PacketCodecs.STRING, PacketCodecs.INTEGER.collect(PacketCodecs.toList())));

    public static final PacketCodec<RegistryByteBuf, MapAtlasesAtlasOverviewScreenData> PACKET_CODEC = PacketCodec.tuple(ItemStack.PACKET_CODEC, MapAtlasesAtlasOverviewScreenData::atlas, PacketCodecs.STRING, MapAtlasesAtlasOverviewScreenData::centerMapId, PacketCodecs.INTEGER, MapAtlasesAtlasOverviewScreenData::atlasScale, mapCodec, MapAtlasesAtlasOverviewScreenData::idsToCenters, MapAtlasesAtlasOverviewScreenData::new);

    

    private static <Buffer, F, S> PacketCodec<Buffer, Pair<F, S>> pairCodec(PacketCodec<? super Buffer, F> firstCodec,
                                                                            PacketCodec<? super Buffer, S> secondCodec) {
        return PacketCodec.tuple(firstCodec, Pair::getFirst, secondCodec, Pair::getSecond, Pair::of);
    }
    
}


