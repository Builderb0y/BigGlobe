package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.world.ActiveSectionTracker;
import me.cortex.voxy.common.world.WorldSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.throwables.MixinError;

@Mixin(value = WorldSection.class, remap = false)
public interface Voxy_WorldSection_ConstructorAccess {

	@Invoker("<init>")
	public static WorldSection bigglobe_construct(int lvl, int x, int y, int z, ActiveSectionTracker tracker) {
		throw new MixinError("Invoker not applied");
	}
}