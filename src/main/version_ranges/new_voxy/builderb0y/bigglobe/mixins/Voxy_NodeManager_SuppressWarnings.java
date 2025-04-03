package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.cortex.voxy.client.core.rendering.hierachical.NodeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;

@Mixin(NodeManager.class)
public class Voxy_NodeManager_SuppressWarnings {

	@WrapWithCondition(method = "makeLeafChildRequest", at = @At(value = "INVOKE", target = "Lme/cortex/voxy/common/Logger;warn([Ljava/lang/Object;)V"), remap = false)
	private boolean bigglobe_suppressWarningsForGeneratingStorageBackend(Object[] loggerArgs) {
		return AbstractVoxyWorldGenerator.RUNNING_COUNT.get() == 0;
	}
}