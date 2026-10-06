package net.crystalnexus.item;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.multiblock.MultiblockPlanTemplates;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.reactor.ReactorPlanner;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
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
    private static final String REACTOR = "generatedReactorPlan";

    public MultiblockPlansItem() { super(new Properties().stacksTo(1)); }

    public static ResourceLocation previewTemplate(ItemStack stack) { return ResourceLocation.tryParse(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(TEMPLATE)); }
    public static BlockPos previewController(ItemStack stack) { CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); return tag.contains(CONTROLLER) ? BlockPos.of(tag.getLong(CONTROLLER)) : null; }

    public static boolean hasGeneratedReactor(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(REACTOR);
    }

    public static void saveReactor(ItemStack stack, ReactorPlanner.Build build, int speed) {
        if (!(stack.getItem() instanceof MultiblockPlansItem) || !build.layout().valid || speed < 0 || speed > 100)
            throw new IllegalArgumentException("A valid reactor and Multiblock Plans item are required");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            CompoundTag plan = new CompoundTag();
            plan.putInt("size", build.size());
            plan.putByteArray("columns", ReactorPlanner.columns(build));
            plan.putInt("insertion", 100 - speed);
            data.put(REACTOR, plan);
            data.remove(TEMPLATE);
            data.remove(CONTROLLER);
            data.remove(DIMENSION);
        });
    }

    public static List<MultiblockPlanTemplates.PlanBlock> readPlan(Level level, BlockPos controller, ItemStack stack) {
        if (!hasGeneratedReactor(stack)) {
            ResourceLocation template = previewTemplate(stack);
            return template == null ? List.of() : MultiblockPlanTemplates.read(level, controller, template);
        }
        if (!level.getBlockState(controller).is(CrystalnexusModBlocks.REACTOR_COMPUTER.get())) return List.of();
        CompoundTag plan = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(REACTOR);
        try {
            var build = ReactorPlanner.fromColumns(plan.getInt("size"), plan.getByteArray("columns"));
            return MultiblockPlanTemplates.align(level.getBlockState(controller), controller, build.blocks().entrySet().stream()
                    .map(entry -> new MultiblockPlanTemplates.PlanBlock(entry.getKey(), entry.getValue())).toList());
        } catch (IllegalArgumentException ignored) {
            return List.of();
        }
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (hasGeneratedReactor(stack)) {
            int size = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(REACTOR).getInt("size");
            tooltip.add(Component.literal("Generated reactor: " + size + "x" + size + "x" + size).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Right-click a Reactor Computer to preview, then again to build.").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        BlockPos controller = context.getClickedPos();
        if (hasGeneratedReactor(stack) && !server.level().getBlockState(controller).is(CrystalnexusModBlocks.REACTOR_COMPUTER.get()))
            return InteractionResult.PASS;
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
        List<MultiblockPlanTemplates.PlanBlock> plan = readPlan(player.serverLevel(), controller, stack);
        if (plan.isEmpty()) { player.displayClientMessage(Component.literal("Could not read this multiblock template.").withStyle(ChatFormatting.RED), true); return; }
        Map<Item, Integer> needed = new LinkedHashMap<>();
        for (var entry : plan) {
            BlockState wanted = entry.state();
            if (hasGeneratedReactor(stack) && wanted.isAir() && !player.serverLevel().getBlockState(entry.pos()).isAir()) {
                player.displayClientMessage(Component.literal("Clear the planned air space at " + entry.pos().toShortString()).withStyle(ChatFormatting.RED), true);
                return;
            }
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
        int insertion = hasGeneratedReactor(stack)
                ? stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(REACTOR).getInt("insertion") : -1;
        animateBuild(player, plan.stream().filter(entry -> !entry.pos().equals(controller) && !entry.state().isAir() && !entry.state().is(Blocks.STRUCTURE_VOID)).toList(), insertion);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> { data.remove(TEMPLATE); data.remove(CONTROLLER); data.remove(DIMENSION); });
        player.displayClientMessage(Component.literal("Multiblock built.").withStyle(ChatFormatting.GREEN), true);
    }

    private static int count(ServerPlayer player, Item item) { return player.getInventory().items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static void take(ServerPlayer player, Item item, int count) { for (ItemStack stack : player.getInventory().items) if (stack.is(item)) { int n = Math.min(count, stack.getCount()); stack.shrink(n); if ((count -= n) == 0) break; } player.getInventory().setChanged(); }

    private static void animateBuild(ServerPlayer player, List<MultiblockPlanTemplates.PlanBlock> blocks, int insertion) {
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
                    if (insertion >= 0 && level.getBlockEntity(block.pos()) instanceof net.crystalnexus.block.entity.ReactorControlRodBlockEntity rod)
                        rod.setInsertion(insertion);
                }
            });
        }
    }
}
