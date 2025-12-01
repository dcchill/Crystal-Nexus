package net.crystalnexus.procedures;

import net.minecraft.world.entity.Entity;

import net.crystalnexus.network.CrystalnexusModVariables;

public class JetpackJumpOnKeyPressedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double thrust = 0;
		{
			CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
			_vars.jetpackFly = true;
			_vars.syncPlayerVariables(entity);
		}
	}
}