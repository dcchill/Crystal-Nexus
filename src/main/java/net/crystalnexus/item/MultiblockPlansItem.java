package net.crystalnexus.item;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.multiblock.MultiblockPlanTemplates;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MultiblockPlansItem extends Item {
    private static final String TEMPLATE = "multiblockPlanTemplate";
    private static final String CONTROLLER = "multiblockPlanController";
    private static final String DIMENSION = "multiblockPlanDimension";

    public MultiblockPlansItem() { super(new Properties().stacksTo(1)); }

    public static ResourceLocation previewTemplate(ItemStack stack) { return ResourceLocation.tryParse(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(TEMPLATE)); }
    public static BlockPos previewController(ItemStack stack) { CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); return tag.contains(CONTROLLER) ? BlockPos.of(tag.getLong(CONTROLLER)) : null; }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        BlockPos controller = context.getClickedPos();
        ResourceLocation template = MultiblockPlanTemplates.templateFor(server.level().getBlockState(controller).getBlock());
        if (template == null) return InteractionResult.PASS;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (template.toString().equals(tag.getString(TEMPLATE)) && controller.asLong() == tag.getLong(CONTROLLER)
                && server.level().dimension().location().toString().equals(tag.getString(DIMENSION))) {
            build(server, stack, controller, template);
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> { data.putString(TEMPLATE, template.toString()); data.putLong(CONTROLLER, controller.asLong()); data.putString(DIMENSION, server.level().dimension().location().toString()); });
            server.displayClientMessage(Component.literal("Multiblock plan loaded — right-click the controller again to build.").withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.CONSUME;
    }

    private static void build(ServerPlayer player, ItemStack stack, BlockPos controller, ResourceLocation template) {
        List<MultiblockPlanTemplates.PlanBlock> plan = MultiblockPlanTemplates.read(player.serverLevel(), controller, template);
        if (plan.isEmpty()) { player.displayClientMessage(Component.literal("Could not read this multiblock template.").withStyle(ChatFormatting.RED), true); return; }
        Map<Item, Integer> needed = new LinkedHashMap<>();
        for (var entry : plan) {
            BlockState wanted = entry.state();
            if (wanted.isAir() || wanted.is(Blocks.STRUCTURE_VOID) || entry.pos().equals(controller)) continue;
            BlockState present = player.serverLevel().getBlockState(entry.pos());
            if (!present.isAir() && present.getBlock() != wanted.getBlock() && !present.canBeReplaced()) {
                player.displayClientMessage(Component.literal("Blocked at " + entry.pos().toShortString()).withStyle(ChatFormatting.RED), true); return;
            }
            if (present.getBlock() != wanted.getBlock() && wanted.getBlock().asItem() != Items.AIR) needed.merge(wanted.getBlock().asItem(), 1, Integer::sum);
        }
        if (!player.isCreative()) for (var entry : needed.entrySet()) if (count(player, entry.getKey()) < entry.getValue()) {
            player.displayClientMessage(Component.literal("Missing " + entry.getValue() + "x " + entry.getKey().getName(new ItemStack(entry.getKey())).getString()).withStyle(ChatFormatting.RED), true); return;
        }
        if (!player.isCreative()) needed.forEach((item, count) -> take(player, item, count));
        animateBuild(player, plan.stream().filter(entry -> !entry.pos().equals(controller) && !entry.state().isAir() && !entry.state().is(Blocks.STRUCTURE_VOID)).toList());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> { data.remove(TEMPLATE); data.remove(CONTROLLER); data.remove(DIMENSION); });
        player.displayClientMessage(Component.literal("Multiblock built.").withStyle(ChatFormatting.GREEN), true);
    }

    private static int count(ServerPlayer player, Item item) { return player.getInventory().items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static void take(ServerPlayer player, Item item, int count) { for (ItemStack stack : player.getInventory().items) if (stack.is(item)) { int n = Math.min(count, stack.getCount()); stack.shrink(n); if ((count -= n) == 0) break; } player.getInventory().setChanged(); }

    private static void animateBuild(ServerPlayer player, List<MultiblockPlanTemplates.PlanBlock> blocks) {
        var level = player.serverLevel();
        for (int i = 0; i < blocks.size(); i += 8) {
            int from = i, to = Math.min(i + 8, blocks.size());
            CrystalnexusMod.queueServerWork(i / 8 + 1, () -> {
                for (var block : blocks.subList(from, to)) {
                    Item item = block.state().getBlock().asItem();
                    if (item != Items.AIR) {
                        Vec3 start = player.position().add((level.random.nextDouble() - 0.5D) * 0.5D, 0.8D + level.random.nextDouble() * 0.35D, (level.random.nextDouble() - 0.5D) * 0.5D);
                        Vec3 target = Vec3.atCenterOf(block.pos());
                        ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(item));
                        for (int step = 0; step < 5; step++) {
                            Vec3 point = start.lerp(target, step / 4.0D);
                            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                        }
                    }
                    level.setBlock(block.pos(), block.state(), Block.UPDATE_ALL);
                }
            });
        }
    }
}
