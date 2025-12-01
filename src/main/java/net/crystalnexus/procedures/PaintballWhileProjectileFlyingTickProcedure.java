package net.crystalnexus.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.crystalnexus.CrystalnexusMod;

public class PaintballWhileProjectileFlyingTickProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity projectile) {
        if (projectile == null) return;

        // Read the stored dye string from the projectile
        String dye = projectile.getPersistentData().getString("dye");
        String color;

        // Map dye name to RGB
        switch (dye) {
            case "crystalnexus:black_paintball": color = "0.0,0.0,0.0"; break;
            case "crystalnexus:white_paintball": color = "1.0,1.0,1.0"; break;
            case "crystalnexus:gray_paintball": color = "0.8,0.8,0.8"; break;
            case "crystalnexus:light_gray_paintball": color = "0.9,0.9,0.9"; break;
            case "crystalnexus:red_paintball": color = "1.0,0.0,0.0"; break;
            case "crystalnexus:orange_paintball": color = "1.0,0.5,0.0"; break;
            case "crystalnexus:yellow_paintball": color = "1.0,1.0,0.0"; break;
            case "crystalnexus:lime_paintball": color = "0.0,1.0,0.0"; break;
            case "crystalnexus:green_paintball": color = "0.0,0.8,0.0"; break;
            case "crystalnexus:cyan_paintball": color = "0.0,0.8,0.8"; break;
            case "crystalnexus:light_blue_paintball": color = "0.0,0.8,1.0"; break;
            case "crystalnexus:blue_paintball": color = "0.0,0.0,1.0"; break;
            case "crystalnexus:purple_paintball": color = "0.8,0.0,1.0"; break;
            case "crystalnexus:magenta_paintball": color = "1.0,0.0,1.0"; break;
            case "crystalnexus:pink_paintball": color = "1.0,0.7,1.0"; break;
            default: color = "1.0,1.0,1.0"; // fallback to white
        }

        // Spawn the particle on the server
		if (world instanceof ServerLevel _level)
			_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
					("particle dust{color:[" + color + "],scale:1} ~ ~ ~ 0 0 0 0 1"));
    }
}
