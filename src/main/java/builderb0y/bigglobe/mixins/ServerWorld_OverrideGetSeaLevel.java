package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

@Mixin(ServerWorld.class)
public abstract class ServerWorld_OverrideGetSeaLevel extends World {

	public ServerWorld_OverrideGetSeaLevel() {
		#if MC_VERSION <= MC_1_19_2
			super(null, null, null, null, false, false, 0L, 0);
		#else
			super(null, null, null, null, null, false, false, 0L, 0);
		#endif
	}

	@Override
	@Unique(silent = true)
	public int getSeaLevel() {
		//default implementation delegates to super.
		//a different mixin is used to inject into this implementation.
		return super.getSeaLevel();
	}
}