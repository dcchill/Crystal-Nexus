package net.crystalnexus.procedures;

import net.minecraft.world.entity.Entity;

import net.crystalnexus.network.CrystalnexusModVariables;

public class JetpackJumpOnKeyReleasedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		{
			CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
			_vars.jetpackFly = false;
			_vars.syncPlayerVariables(entity);
		}
	}
}