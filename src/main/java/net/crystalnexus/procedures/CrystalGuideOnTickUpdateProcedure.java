package net.crystalnexus.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.crystalnexus.jei_recipes.BeamReactionRecipeRecipe;
import net.crystalnexus.init.CrystalnexusModBlocks;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class CrystalGuideOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double crystalCount = 0;
		double yOffset = 0;
		double xOffset = 0;
		double cookTime = 0;
		double zOffset = 0;
		double T = 0;
		double Zo = 0;
		double Yo = 0;
		double Za = 0;
		double Xo = 0;
		double Ya = 0;
		double Xa = 0;
		if (("up").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 2;
			zOffset = 0;
		}
		if (("down").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = -2;
			zOffset = 0;
		}
		if (("north").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 0;
			zOffset = -2;
		}
		if (("south").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 0;
			zOffset = 2;
			if ((world.getBlockState(BlockPos.containing(x, y, z + 4))).getBlock() == CrystalnexusModBlocks.CRYSTAL_GUIDE.get()) {
				if (!world.getEntitiesOfClass(EndCrystal.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((x - xOffset), (y - yOffset), (z - zOffset))).inflate(2 / 2d), e -> true).isEmpty() == true) {
					if (!world.getEntitiesOfClass(EndCrystal.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((x + 0), (y + 0), (z + 6))).inflate(2 / 2d), e -> true).isEmpty() == true) {
						if (("north").equals(getBlockNBTString(world, BlockPos.containing(x, y, z + 4), "rotation"))) {
							if (world instanceof ServerLevel _level)
								_level.sendParticles(ParticleTypes.ELECTRIC_SPARK, (x + xOffset + 0.5), (y + yOffset + 0.5), (z + zOffset + 0.5), 5, 0.1, 0.1, 0.1, 0.1);
							{
								final Vec3 _center = new Vec3((x + xOffset), (y + yOffset), (z + zOffset));
								for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
										.toList()) {
									if (entityiterator instanceof ItemEntity) {
										if (!(Blocks.AIR.asItem() == (new Object() {
											public ItemStack getResult() {
												if (world instanceof Level _lvl) {
													net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
													List<BeamReactionRecipeRecipe> recipes = rm.getAllRecipesFor(BeamReactionRecipeRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
													for (BeamReactionRecipeRecipe recipe : recipes) {
														NonNullList<Ingredient> ingredients = recipe.getIngredients();
														if (!ingredients.get(0).test((entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY)))
															continue;
														return recipe.getResultItem(null);
													}
												}
												return ItemStack.EMPTY;
											}
										}.getResult()).getItem())) {
											for (int index0 = 0; index0 < (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getCount(); index0++) {
												if (world instanceof ServerLevel _level) {
													ItemEntity entityToSpawn = new ItemEntity(_level, (x + xOffset + 0.5), (y + yOffset + 0.5), (z + zOffset + 0.5), (new Object() {
														public ItemStack getResult() {
															if (world instanceof Level _lvl) {
																net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
																List<BeamReactionRecipeRecipe> recipes = rm.getAllRecipesFor(BeamReactionRecipeRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
																for (BeamReactionRecipeRecipe recipe : recipes) {
																	NonNullList<Ingredient> ingredients = recipe.getIngredients();
																	if (!ingredients.get(0).test((entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY)))
																		continue;
																	return recipe.getResultItem(null);
																}
															}
															return ItemStack.EMPTY;
														}
													}.getResult()));
													entityToSpawn.setPickUpDelay(10);
													entityToSpawn.setUnlimitedLifetime();
													_level.addFreshEntity(entityToSpawn);
												}
											}
											if (!entityiterator.level().isClientSide())
												entityiterator.discard();
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (("east").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 2;
			yOffset = 0;
			zOffset = 0;
			if ((world.getBlockState(BlockPos.containing(x + 4, y, z))).getBlock() == CrystalnexusModBlocks.CRYSTAL_GUIDE.get()) {
				if (!world.getEntitiesOfClass(EndCrystal.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((x - xOffset), (y - yOffset), (z - zOffset))).inflate(2 / 2d), e -> true).isEmpty() == true) {
					if (!world.getEntitiesOfClass(EndCrystal.class, new AABB(Vec3.ZERO, Vec3.ZERO).move(new Vec3((x + 6), (y + 0), (z + 0))).inflate(2 / 2d), e -> true).isEmpty() == true) {
						if (("west").equals(getBlockNBTString(world, BlockPos.containing(x + 4, y, z), "rotation"))) {
							if (world instanceof ServerLevel _level)
								_level.sendParticles(ParticleTypes.ELECTRIC_SPARK, (x + xOffset + 0.5), (y + yOffset + 0.5), (z + zOffset + 0.5), 5, 0.1, 0.1, 0.1, 0.1);
							{
								final Vec3 _center = new Vec3((x + xOffset), (y + yOffset), (z + zOffset));
								for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
										.toList()) {
									if (!(Blocks.AIR.asItem() == (new Object() {
										public ItemStack getResult() {
											if (world instanceof Level _lvl) {
												net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
												List<BeamReactionRecipeRecipe> recipes = rm.getAllRecipesFor(BeamReactionRecipeRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
												for (BeamReactionRecipeRecipe recipe : recipes) {
													NonNullList<Ingredient> ingredients = recipe.getIngredients();
													if (!ingredients.get(0).test((entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY)))
														continue;
													return recipe.getResultItem(null);
												}
											}
											return ItemStack.EMPTY;
										}
									}.getResult()).getItem())) {
										for (int index1 = 0; index1 < (entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getCount(); index1++) {
											if (world instanceof ServerLevel _level) {
												ItemEntity entityToSpawn = new ItemEntity(_level, (x + xOffset + 0.5), (y + yOffset + 0.5), (z + zOffset + 0.5), (new Object() {
													public ItemStack getResult() {
														if (world instanceof Level _lvl) {
															net.minecraft.world.item.crafting.RecipeManager rm = _lvl.getRecipeManager();
															List<BeamReactionRecipeRecipe> recipes = rm.getAllRecipesFor(BeamReactionRecipeRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
															for (BeamReactionRecipeRecipe recipe : recipes) {
																NonNullList<Ingredient> ingredients = recipe.getIngredients();
																if (!ingredients.get(0).test((entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY)))
																	continue;
																return recipe.getResultItem(null);
															}
														}
														return ItemStack.EMPTY;
													}
												}.getResult()));
												entityToSpawn.setPickUpDelay(10);
												entityToSpawn.setUnlimitedLifetime();
												_level.addFreshEntity(entityToSpawn);
											}
										}
										if (!entityiterator.level().isClientSide())
											entityiterator.discard();
									}
								}
							}
						}
					}
				}
			}
		}
		if (("west").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = -2;
			yOffset = 0;
			zOffset = 0;
		}
		{
			final Vec3 _center = new Vec3((x - xOffset), (y - yOffset), (z - zOffset));
			for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
				if (entityiterator instanceof EndCrystal) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
										.withSuppressedOutput(),
								("data modify entity @e[type=minecraft:end_crystal,limit=1,sort=nearest] beam_target set value [I; " + Math.round(x) + ", " + Math.round(y - 2) + ", " + Math.round(z) + "]"));
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("crystal", true);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				}
			}
		}
		if (getBlockNBTLogic(world, BlockPos.containing(x, y, z), "crystal") == true) {
			{
				final Vec3 _center = new Vec3((x + xOffset), (y + yOffset), (z + zOffset));
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (!(entityiterator instanceof ItemEntity)) {
						entityiterator.hurt(new DamageSource(world.holderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crystalnexus:crystal_beam")))), 15);
					}
				}
			}
			{
				BlockPos _pos = BlockPos.containing(x + xOffset, y + yOffset, z + zOffset);
				Block.dropResources(world.getBlockState(_pos), world, BlockPos.containing(x + xOffset, y + yOffset, z + zOffset), null);
				world.destroyBlock(_pos, false);
			}
			if (world instanceof Level _level)
				_level.updateNeighborsAt(BlockPos.containing(x + xOffset, y + yOffset, z + zOffset), _level.getBlockState(BlockPos.containing(x + xOffset, y + yOffset, z + zOffset)).getBlock());
			if (!world.isClientSide()) {
				BlockPos _bp = BlockPos.containing(x, y, z);
				BlockEntity _blockEntity = world.getBlockEntity(_bp);
				BlockState _bs = world.getBlockState(_bp);
				if (_blockEntity != null)
					_blockEntity.getPersistentData().putBoolean("crystal", false);
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 3);
			}
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.ENCHANTED_HIT, (x + xOffset + 0.5), (y + yOffset + 0.5), (z + zOffset + 0.5), 1, 0.1, 0.1, 0.1, 0);
		}
	}

	private static String getBlockNBTString(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getString(tag);
		return "";
	}

	private static boolean getBlockNBTLogic(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getBoolean(tag);
		return false;
	}
}