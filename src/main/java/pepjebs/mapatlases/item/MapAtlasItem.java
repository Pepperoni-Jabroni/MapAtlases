package pepjebs.mapatlases.item;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerLifecycleEvents;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreenHandler;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pepjebs.mapatlases.lifecycle.MapAtlasesServerLifecycleEvents.MAP_ATLAS_ACTIVE_STATE_CHANGE;

public class MapAtlasItem extends Item implements ExtendedScreenHandlerFactory {

    public static final String EMPTY_MAP_NBT = "empty";
    public static final String MAP_LIST_NBT = "maps";

    public static final BooleanProperty HAS_ATLAS = BooleanProperty.of("has_atlas");

    public MapAtlasItem(Settings settings) {
        super(settings);
    }

    public static int getMaxMapCount() {
        if (MapAtlasesMod.CONFIG != null) {
            return MapAtlasesMod.CONFIG.maxMapCount;
        }
        return 128;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        if (world != null && world.isClient) {
            int mapSize = MapAtlasesAccessUtils.getMapCountFromItemStack(stack);
            int empties = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(stack);
            if (getMaxMapCount() != -1 && mapSize + empties >= getMaxMapCount()) {
                tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.atlas.tooltip_full"))
                        .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
            }
            tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.atlas.tooltip_1", mapSize))
                    .formatted(Formatting.GRAY));
            if (MapAtlasesMod.CONFIG == null || MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill) {
                // If there's no maps & no empty maps, the atlas is "inactive", so display how many empty maps
                // they *would* receive if they activated the atlas
                if (mapSize + empties == 0 && MapAtlasesMod.CONFIG != null) {
                    empties = MapAtlasesMod.CONFIG.pityActivationMapCount;
                }
                tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.atlas.tooltip_2", empties))
                        .formatted(Formatting.GRAY));
            }
            MapState mapState = world.getMapState(MapAtlasesClient.currentMapStateId);
            if (mapState == null) return;
            tooltip.add(MutableText.of(
                        new TranslatableTextContent("item.map_atlases.atlas.tooltip_3", 1 << mapState.scale)
                    )
                    .formatted(Formatting.GRAY));
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        openHandledAtlasScreen(world, player);
        return TypedActionResult.consume(player.getStackInHand(hand));
    }

    public void openHandledAtlasScreen(World world, PlayerEntity player) {
        player.openHandledScreen(this);
        world.playSound(player.getX(), player.getY(), player.getZ(), MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT,
                SoundCategory.PLAYERS, 1.0F, 1.0F, false);
    }

    @Override
    public Text getDisplayName() {
        return MutableText.of(new TranslatableTextContent(getTranslationKey()));
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) {
            atlas = getAtlasFromLookingLectern(player);
        }
        Map<Integer, List<Integer>> idsToCenters = new HashMap<>();
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(player.world, atlas);
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            idsToCenters.put(
                    MapAtlasesAccessUtils.getMapIntFromString(state.getKey()),
                    Arrays.asList(state.getValue().centerX, state.getValue().centerZ));
        }
        return new MapAtlasesAtlasOverviewScreenHandler(syncId, inv, idsToCenters, atlas);
    }

    public ItemStack getAtlasFromLookingLectern(PlayerEntity player) {
        HitResult h = player.raycast(10, 1, false);
        if (h.getType() == HitResult.Type.BLOCK) {
            BlockEntity e = player.getWorld().getBlockEntity(new BlockPos(h.getPos()));
            if (e instanceof LecternBlockEntity) {
                ItemStack book = ((LecternBlockEntity) e).getBook();
                if (book.getItem() == MapAtlasesMod.MAP_ATLAS) {
                    return book;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void sendPlayerLecternAtlasData(ServerPlayerEntity serverPlayerEntity, ItemStack atlas){
        // Send player all MapStates
        var states =
                MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(serverPlayerEntity.world, atlas);
        double minDist = Double.MAX_VALUE;
        String activeId = null;
        for (var state : states.entrySet()) {
            state.getValue().getPlayerSyncData(serverPlayerEntity);
            double distance =
                    MapAtlasesAccessUtils.distanceBetweenMapStateAndPlayer(state.getValue(), serverPlayerEntity);
            if (distance < minDist) {
                minDist = distance;
                activeId = state.getKey();
            }
            MapAtlasesServerLifecycleEvents.relayMapStateSyncToPlayerClient(state, serverPlayerEntity);
        }
        // Tell player active MapState for location
        if (activeId != null) {
            PacketByteBuf p = new PacketByteBuf(Unpooled.buffer());
            p.writeString(activeId);
            serverPlayerEntity.networkHandler.sendPacket(new CustomPayloadS2CPacket(
                    MAP_ATLAS_ACTIVE_STATE_CHANGE, p));
        }
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(serverPlayerEntity);
        if (atlas.isEmpty()) {
            atlas = getAtlasFromLookingLectern(serverPlayerEntity);
            sendPlayerLecternAtlasData(serverPlayerEntity, atlas);
        }
        if (atlas.isEmpty()) return;
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(
                serverPlayerEntity.world, atlas);
        packetByteBuf.writeItemStack(atlas);
        packetByteBuf.writeInt(mapInfos.size());
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            packetByteBuf.writeInt(MapAtlasesAccessUtils.getMapIntFromString(state.getKey()));
            packetByteBuf.writeInt(state.getValue().centerX);
            packetByteBuf.writeInt(state.getValue().centerZ);
        }
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null || context.getWorld() == null
                || context.getStack() == null || context.getBlockPos() == null)
            return super.useOnBlock(context);
        BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
        if (blockState.isOf(Blocks.LECTERN)) {
            boolean didPut = LecternBlock.putBookIfAbsent(
                    context.getPlayer(),
                    context.getWorld(),
                    context.getBlockPos(),
                    blockState,
                    context.getStack()
            );
            if (!didPut) {
                return ActionResult.PASS;
            }
            blockState = context.getWorld().getBlockState(context.getBlockPos());
            LecternBlock.setHasBook(context.getWorld(), context.getBlockPos(), blockState, true);
            context.getWorld().setBlockState(context.getBlockPos(), blockState.with(HAS_ATLAS, true));
            return ActionResult.success(context.getWorld().isClient);
        } if (blockState.isIn(BlockTags.BANNERS)) {
            if (!context.getWorld().isClient) {
                Map<String, MapState> currentDimMapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(
                        context.getWorld(), context.getStack());
                MapState mapState = MapAtlasesAccessUtils.getActiveAtlasMapStateServer(
                        currentDimMapInfos, (ServerPlayerEntity) context.getPlayer()).getValue();
                if (mapState == null)
                    return ActionResult.FAIL;
                boolean didAdd = mapState.addBanner(context.getWorld(), context.getBlockPos());
                if (!didAdd)
                    return ActionResult.FAIL;
            }
            return ActionResult.success(context.getWorld().isClient);
        } else {
            return super.useOnBlock(context);
        }
    }
}
