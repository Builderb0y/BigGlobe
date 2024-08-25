package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.world.WorldSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldSection.class)
public interface Voxy_WorldSection_DataGetter {

	@Accessor("data")
	public abstract long[] bigglobe_getData();
}