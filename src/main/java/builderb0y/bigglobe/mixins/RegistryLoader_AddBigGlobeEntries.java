package builderb0y.bigglobe.mixins;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.registry.RegistryLoader;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;

/**
fabric now has its own system for adding dynamic registries,
but it doesn't let me add my entries at a custom index.
this is important because some dynamic registries
depend on others at the time of deserialization.
as such, I'm keeping this mixin here for now.
*/
@Mixin(RegistryLoader.class)
public class RegistryLoader_AddBigGlobeEntries {

	@Shadow @Final @Mutable public static List<RegistryLoader.Entry<?>> DYNAMIC_REGISTRIES;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void bigglobe_addEntries(CallbackInfo callback) {
		if (DYNAMIC_REGISTRIES.getClass() != ArrayList.class) {
			DYNAMIC_REGISTRIES = new ArrayList<>(DYNAMIC_REGISTRIES);
		}
		BigGlobeDynamicRegistries.init();
	}
}