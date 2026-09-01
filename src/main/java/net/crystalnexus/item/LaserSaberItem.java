package net.crystalnexus.item;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.List;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public class LaserSaberItem extends SwordItem {
    public static final int CAPACITY = 100_000;
    public static final int MAX_TRANSFER = 5_000;
    public static final int FE_PER_TICK = 20;
    private static final String POWERED = "LaserSaberPowered";
    private static final TagKey<Item> BATTERIES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "battery"));
    private static final ItemAttributeModifiers POWERED_ATTRIBUTES = ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED,
                    new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND)
            .build();
    private static final Tier TIER = new Tier() {
        @Override public int getUses() { return 0; }
        @Override public float getSpeed() { return 1.0F; }
        @Override public float getAttackDamageBonus() { return 0.0F; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
        @Override public int getEnchantmentValue() { return 15; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    };

    public LaserSaberItem() {
        super(TIER, new Item.Properties().stacksTo(1).attributes(ItemAttributeModifiers.EMPTY));
    }

    public static boolean isPowered(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(POWERED);
    }

    public static int bladeColor(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, 0xFFFFFFFF);
    }

    private static void setPowered(ItemStack stack, boolean powered) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(POWERED, powered));
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                powered ? POWERED_ATTRIBUTES : ItemAttributeModifiers.EMPTY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack saber = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            boolean poweringOn = !isPowered(saber);
            if (poweringOn && !player.getAbilities().instabuild
                    && !consumeEnergy(player, saber, FE_PER_TICK, true)) {
                player.displayClientMessage(Component.literal("Out of power").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(saber);
            }
            setPowered(saber, poweringOn);
        }
        return InteractionResultHolder.sidedSuccess(saber, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !isPowered(stack) || !(entity instanceof Player player)
                || player.getAbilities().instabuild) return;

        if (!consumeEnergy(player, stack, FE_PER_TICK, true)) {
            setPowered(stack, false);
            player.displayClientMessage(Component.literal("Laser Saber ran out of power")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        consumeEnergy(player, stack, FE_PER_TICK, false);
    }

    private static boolean consumeEnergy(Player player, ItemStack saber, int amount, boolean simulate) {
        int available = extract(saber, amount, true);
        for (ItemStack battery : player.getInventory().items) {
            if (battery != saber && battery.is(BATTERIES)) {
                available += extract(battery, amount - available, true);
                if (available >= amount) break;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (available < amount && offhand != saber && offhand.is(BATTERIES)) {
            available += extract(offhand, amount - available, true);
        }
        if (available < amount || simulate) return available >= amount;

        int remaining = amount - extract(saber, amount, false);
        for (ItemStack battery : player.getInventory().items) {
            if (remaining <= 0) break;
            if (battery != saber && battery.is(BATTERIES)) remaining -= extract(battery, remaining, false);
        }
        if (remaining > 0 && offhand != saber && offhand.is(BATTERIES)) {
            remaining -= extract(offhand, remaining, false);
        }
        return remaining <= 0;
    }

    private static int extract(ItemStack stack, int amount, boolean simulate) {
        if (amount <= 0 || stack.isEmpty()) return 0;
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM, null);
        return energy == null ? 0 : energy.extractEnergy(amount, simulate);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * BatteryData.getEnergy(stack) / CAPACITY);
    }

    @Override public int getBarColor(ItemStack stack) { return bladeColor(stack); }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(isPowered(stack) ? "On" : "Off")
                .withStyle(isPowered(stack) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltip.add(Component.literal("Energy: " + String.format("%,d / %,d FE",
                BatteryData.getEnergy(stack), CAPACITY)).withStyle(ChatFormatting.AQUA));
    }

    @SubscribeEvent
    public static void preventUnpoweredDamage(AttackEntityEvent event) {
        ItemStack weapon = event.getEntity().getMainHandItem();
        if (weapon.is(CrystalnexusModItems.LASER_SABER.get()) && !isPowered(weapon)) {
            event.setCanceled(true);
        }
    }

    @EventBusSubscriber(modid = CrystalnexusMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void removeDurability(ModifyDefaultComponentsEvent event) {
            event.modify(CrystalnexusModItems.LASER_SABER.get(),
                    builder -> builder.remove(DataComponents.MAX_DAMAGE));
        }
    }
}
