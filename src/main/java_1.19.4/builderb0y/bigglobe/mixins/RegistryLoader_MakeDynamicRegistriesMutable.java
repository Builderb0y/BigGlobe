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

@Mixin(RegistryLoader.class)
public class RegistryLoader_MakeDynamicRegistriesMutable {

	@Shadow @Final @Mutable public static List<RegistryLoader.Entry<?>> DYNAMIC_REGISTRIES;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void bigglobe_makeDynamicRegistriesMutable(CallbackInfo callback) {
		if (DYNAMIC_REGISTRIES.getClass() != ArrayList.class) {
			DYNAMIC_REGISTRIES = new ArrayList<>(DYNAMIC_REGISTRIES);
		}
	}
}