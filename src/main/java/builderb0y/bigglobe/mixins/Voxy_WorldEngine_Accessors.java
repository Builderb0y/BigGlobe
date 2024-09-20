package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.world.ActiveSectionTracker;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldEngine.class)
public interface Voxy_WorldEngine_Accessors {

	@Accessor("sectionTracker")
	public abstract ActiveSectionTracker bigglobe_getTracker();
}