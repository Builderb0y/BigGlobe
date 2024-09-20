package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.world.ActiveSectionTracker;
import me.cortex.voxy.common.world.WorldSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.throwables.MixinError;

@Mixin(WorldSection.class)
public interface Voxy_WorldSection_Accessors {

	@Invoker("<init>")
	public static WorldSection bigglobe_create(int level, int x, int y, int z, ActiveSectionTracker tracker) {
		throw new MixinError("Mixin failed to apply");
	}

	@Invoker("trySetFreed")
	public abstract boolean bigglobe_trySetFreed();
}