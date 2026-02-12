package net.crystalnexus.radiation;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import net.minecraft.world.phys.Vec3;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModSounds;
import net.crystalnexus.init.CrystalnexusModBlocks;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public class RadiationEvents {

    private static final int SCAN_XZ = 48;
    private static final int SCAN_Y = 24;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        var server = event.getServer();

        for (ServerLevel level : server.getAllLevels()) {

            for (ServerPlayer player : level.players()) {

                ItemStack stack = player.getMainHandItem();

                if (stack.getItem() != CrystalnexusModItems.GEIGER_COUNTER.get())
                    continue;

                RadiationResult result =
                        calculateRadiation(level, player.blockPosition());

                double totalRadiation = Math.min(result.total, 200);

                // ----------------------------
                // 🔊 Geiger Click Logic
                // ----------------------------
                if (totalRadiation > 0.01) {

                    double intensity =
                            Math.min(totalRadiation / 35.0, 1.0);

                    int interval = (int)(16 - (intensity * 14));
                    interval = Math.max(interval, 2);

                    if (level.getGameTime() % interval == 0 ||
                        level.random.nextFloat() < (0.02 * intensity)) {

                        level.playSound(
                                null,
                                player.blockPosition(),
                                CrystalnexusModSounds.GEIGER_CLICK.get(),
                                SoundSource.PLAYERS,
                                0.6F + (float)(intensity * 0.9F),
                                0.95F + level.random.nextFloat() * 0.1F
                        );
                    }
                }

                // ----------------------------
                // 🧭 Compass Display
                // ----------------------------
                if (result.strongestPos != null) {

                    String bar = buildCompassBar(player, result.strongestPos);

                    Component message = Component.literal(bar)
                            .withStyle(getColorForRadiation(totalRadiation));

                    player.displayClientMessage(message, true);
                }
            }
        }
    }

    // =========================================
    // 🧠 Radiation Calculation
    // =========================================

    private static RadiationResult calculateRadiation(ServerLevel level, BlockPos center) {

        double total = 0;
        BlockPos strongestPos = null;
        double strongestValue = 0;

        BlockPos min = center.offset(-SCAN_XZ, -SCAN_Y, -SCAN_XZ);
        BlockPos max = center.offset(SCAN_XZ, SCAN_Y, SCAN_XZ);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {

            double distance = Math.sqrt(pos.distSqr(center));
            if (distance > SCAN_XZ) continue;

            double falloff =
                    1.0 / (1.0 + (distance * distance * 0.08));

            double value = 0;

					// ----------------------------------
					//  SPECIAL RAD_PLACEHOLDER BLOCK
					// ----------------------------------
					if (level.getBlockState(pos).getBlock() ==
					        CrystalnexusModBlocks.RAD_PLACEHOLDER.get()) {
					
					    // MUCH stronger
					    value += 55.0 * falloff;
					}
					
					// ----------------------------------
					// ️ Tagged radioactive blocks
					// ----------------------------------
					else if (level.getBlockState(pos).is(
					        BlockTags.create(
					                ResourceLocation.fromNamespaceAndPath(
					                        "crystalnexus",
					                        "radioactive_blocks"
					                )))) {
					
					    value += 18.0 * falloff;
					}


            // Containers
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null && !be.isRemoved()) {

                IItemHandler handler =
                        level.getCapability(
                                Capabilities.ItemHandler.BLOCK,
                                pos,
                                null);

                if (handler == null) {
                    for (Direction dir : Direction.values()) {
                        handler = level.getCapability(
                                Capabilities.ItemHandler.BLOCK,
                                pos,
                                dir);
                        if (handler != null) break;
                    }
                }

                if (handler != null) {

                    int waste = 0;

                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack slot = handler.getStackInSlot(i);

                        if (!slot.isEmpty() &&
                            slot.getItem() ==
                                CrystalnexusModItems.BLUTONIUM_WASTE.get()) {

                            waste += slot.getCount();
                        }
                    }

                    if (waste > 0) {
                        double scaled = Math.log(waste + 1);
                        value += scaled * 14.0 * falloff;
                    }
                }
            }

            total += value;

            if (value > strongestValue) {
                strongestValue = value;
                strongestPos = pos.immutable();
            }
        }

        return new RadiationResult(total, strongestPos);
    }

    // =========================================
    // 🧭 Compass Logic
    // =========================================
	
	private static String buildCompassBar(ServerPlayer player, BlockPos target) {
	
	    int size = 15;
	    int center = size / 2;
	
	    Vec3 playerPos = player.position();
	    Vec3 look = player.getLookAngle().normalize();
	
	    Vec3 toSource = new Vec3(
	            target.getX() + 0.5 - playerPos.x,
	            0,
	            target.getZ() + 0.5 - playerPos.z
	    ).normalize();
	
	    // 🔥 Dot product check
	    double dot = look.dot(toSource);
	
	    // If facing away, blank it
	    if (dot <= 0) {
	        return "================"; //
	    }
	
	    // Cross product for left/right offset
	    double cross = look.x * toSource.z - look.z * toSource.x;
	
	    int offset = (int)(cross * center);
	    offset = Math.max(-center, Math.min(center, offset));
	
	    int pointerIndex = center + offset;
	
	    StringBuilder bar = new StringBuilder();
	
	    for (int i = 0; i < size; i++) {
	        if (i == pointerIndex) {
	            bar.append("<>");
	        } else {
	            bar.append("=");
	        }
	    }
	
	    return bar.toString();
	}


    // =========================================
    // 🎨 Color Based on Intensity
    // =========================================

    private static Style getColorForRadiation(double radiation) {

        if (radiation < 10)
            return Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)); // Green

        if (radiation < 25)
            return Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)); // Yellow

        if (radiation < 50)
            return Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00)); // Orange

        return Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555)); // Red
    }

    // =========================================
    // 📦 Result Record
    // =========================================

    private record RadiationResult(double total, BlockPos strongestPos) {}
}
