package net.crystalnexus.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.crystalnexus.init.CrystalnexusModDataComponents;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe.FluidAmount;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_parts")
@PrefixGameTestTemplate(false)
public final class FluidIngredientGameTests {
    @GameTest(template = "empty")
    public static void commonTagsAcceptAlternateFluidsAndPreserveConstraints(GameTestHelper helper) {
        var registry = BuiltInRegistries.FLUID;
        Map<TagKey<Fluid>, List<Holder<Fluid>>> original = new HashMap<>();
        registry.getTags().forEach(pair -> original.put(pair.getFirst(), pair.getSecond().stream().toList()));
        var tags = new HashMap<>(original);
        var nitrogen = ResourceLocation.parse("crystalnexus:nitrogen");
        tags.put(TagKey.create(Registries.FLUID, ResourceLocation.parse("c:nitrogen")),
            List.of(CrystalnexusModFluids.NITROGEN.get().builtInRegistryHolder(), Fluids.WATER.builtInRegistryHolder()));
        try {
            registry.bindTags(tags);
            var input = new FluidAmount(nitrogen, 100);
            helper.assertTrue(input.matches(new FluidStack(Fluids.WATER, 100)),
                "A different fluid in c:nitrogen must satisfy a direct nitrogen ingredient");
            helper.assertTrue(!input.matches(new FluidStack(Fluids.WATER, 99))
                    && !input.matches(new FluidStack(Fluids.LAVA, 100)),
                "Amounts and unrelated fluids must still be checked");
            var material = ResourceLocation.parse("crystalnexus:iron");
            var withMaterial = new FluidAmount(nitrogen, 100, Optional.of(material));
            var alternate = new FluidStack(Fluids.WATER, 100);
            helper.assertTrue(!withMaterial.matches(alternate), "Material components remain mandatory");
            alternate.set(CrystalnexusModDataComponents.MATERIAL.get(), material);
            helper.assertTrue(withMaterial.matches(alternate), "Tagged alternates may carry the required material");
            helper.assertTrue(input.stack().is(CrystalnexusModFluids.NITROGEN.get()),
                "Output stacks must retain the specified fluid");
            var pos = new net.minecraft.core.BlockPos(1, 1, 1);
            helper.setBlock(pos, net.crystalnexus.init.CrystalnexusModBlocks.CIRCUIT_PRESS.get());
            net.crystalnexus.block.entity.CircuitPressBlockEntity machine = helper.getBlockEntity(pos);
            helper.assertTrue(machine.getNitrogenTank().fill(new FluidStack(Fluids.WATER, 100),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) == 100,
                "The circuit press tank must admit fluids accepted by its recipes");
        } finally {
            tags.replaceAll((key, values) -> original.getOrDefault(key, List.of()));
            registry.bindTags(tags);
        }
        helper.succeed();
    }
}
