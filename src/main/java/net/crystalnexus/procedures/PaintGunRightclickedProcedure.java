package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModEntities;
import net.crystalnexus.entity.PaintballEntity;

public class PaintGunRightclickedProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
    if (entity == null)
        return "";

    ItemStack main = (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY);
    ItemStack off = (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY);

    // Determine which hand has the paintball
    ItemStack paintballStack = ItemStack.EMPTY;
    if (main.is(ItemTags.create(ResourceLocation.parse("crystalnexus:paintballs"))))
        paintballStack = main;
    else if (off.is(ItemTags.create(ResourceLocation.parse("crystalnexus:paintballs"))))
        paintballStack = off;

    if (itemstack.getItem() == CrystalnexusModItems.PAINT_GUN.get() && !paintballStack.isEmpty()) {

        // Get dye string from paintball item
        String dyeID = BuiltInRegistries.ITEM.getKey(paintballStack.getItem()).toString();

        // Save NBT tag “dye” onto the gun (optional)
        CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putString("dye", dyeID));

        String set_color = paintballStack.getDisplayName().getString();

        // Consume paintball unless in creative
        if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
            paintballStack.shrink(1);
        }

        // Shoot 7 paintballs
        for (int i = 0; i < 7; i++) {
            if (world instanceof ServerLevel projectileLevel) {
                PaintballEntity paintball = new PaintballEntity(CrystalnexusModEntities.PAINTBALL.get(), projectileLevel);
                paintball.setOwner(entity);
                paintball.setPos(
                        x + entity.getLookAngle().x,
                        y + 1.4 + entity.getLookAngle().y,
                        z + entity.getLookAngle().z
                );
                paintball.shoot(
                        entity.getLookAngle().x + Mth.nextDouble(RandomSource.create(), -0.01, 0.01),
                        entity.getLookAngle().y + Mth.nextDouble(RandomSource.create(), -0.01, 0.01),
                        entity.getLookAngle().z + Mth.nextDouble(RandomSource.create(), -0.01, 0.01),
                        3, 3
                );

                // Store dye on projectile
                paintball.getPersistentData().putString("dye", dyeID);

                projectileLevel.addFreshEntity(paintball);
            }
        }

        // Play splash sound
        if (world instanceof Level _level) {
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing(x, y, z),
                        BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.player.splash")),
                        SoundSource.PLAYERS, 0.3F, 1);
            } else {
                _level.playLocalSound(x, y, z,
                        BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.player.splash")),
                        SoundSource.PLAYERS, 0.3F, 1, false);
            }
        }

        // Damage the gun
        if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
            if (world instanceof ServerLevel _level) {
                itemstack.hurtAndBreak(1, _level, null, _stkprov -> {});
            }
        }

        return "Set color: " + set_color;
    }

    // Default return if gun not used
    return "Set color:";
}
}