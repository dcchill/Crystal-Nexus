package net.crystalnexus.procedures;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;

import java.util.Locale;

public class ColorBlocksProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, ItemStack itemstack) {
        if (world == null || itemstack == null) return;

        // --- Get dye color from NBT ---
        String dye = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("dye");
        if (dye == null || dye.isEmpty()) return;

        String dyeName = dye.contains(":") ? dye.split(":")[1] : dye;
        dyeName = dyeName.replace("_dye", "").replace("_paintball", "").toLowerCase(Locale.ENGLISH);

        // --- Get current block info ---
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState oldState = world.getBlockState(pos);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(oldState.getBlock());
        if (blockId == null) return;

        String namespace = blockId.getNamespace();
        String blockPath = blockId.getPath();

        // --- Detect existing color prefix/suffix ---
        String[] knownColors = {
            "white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray",
            "cyan","purple","blue","brown","green","red","black"
        };

        String baseName = blockPath;
        String existingColor = null;

        for (String c : knownColors) {
            if (blockPath.startsWith(c + "_")) {
                existingColor = c;
                baseName = blockPath.substring(c.length() + 1);
                break;
            } else if (blockPath.endsWith("_" + c)) {
                existingColor = c;
                baseName = blockPath.substring(0, blockPath.length() - c.length() - 1);
                break;
            }
        }

        // --- Build possible new block IDs ---
        ResourceLocation newIdPrefix = ResourceLocation.fromNamespaceAndPath(namespace, dyeName + "_" + baseName);
        ResourceLocation newIdSuffix = ResourceLocation.fromNamespaceAndPath(namespace, baseName + "_" + dyeName);

        // --- Determine if the block can be recolored ---
        boolean isDyed = oldState.is(BlockTags.create(ResourceLocation.parse("c:dyed")))
            || oldState.is(BlockTags.create(ResourceLocation.fromNamespaceAndPath(namespace, "dyed_blocks")))
            || (existingColor != null);

        if (!isDyed) return;

        // --- Try replacing block ---
        ResourceLocation finalId = null;
        if (BuiltInRegistries.BLOCK.containsKey(newIdPrefix)) {
            finalId = newIdPrefix;
        } else if (BuiltInRegistries.BLOCK.containsKey(newIdSuffix)) {
            finalId = newIdSuffix;
        }

        if (finalId != null) {
            BlockState newState = BuiltInRegistries.BLOCK.get(finalId).defaultBlockState();

            // --- Copy block properties ---
            for (Property<?> prop : oldState.getProperties()) {
                Property<?> targetProp = newState.getBlock().getStateDefinition().getProperty(prop.getName());
                if (targetProp != null) {
                    try {
                        newState = newState.setValue((Property) targetProp, oldState.getValue(prop));
                    } catch (Exception ignored) {}
                }
            }

            world.setBlock(pos, newState, 3);
        }
    }
}