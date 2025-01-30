package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.cortex.voxy.common.world.ActiveSectionTracker;
import me.cortex.voxy.common.world.WorldSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldSection.class)
public abstract class Voxy_WorldSection_Untracked {

	@Shadow abstract boolean trySetFreed();

	@WrapWithCondition(method = "release", at = @At(value = "INVOKE", target = "Lme/cortex/voxy/common/world/ActiveSectionTracker;tryUnload(Lme/cortex/voxy/common/world/WorldSection;)V"))
	private boolean bigglobe_dontUnloadIfNotTracked(ActiveSectionTracker instance, WorldSection section) {
		if (instance != null) {
			return true;
		}
		else {
			this.trySetFreed();
			return false;
		}
	}
}