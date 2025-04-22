package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator.WorldType;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.WorldPreset;

import builderb0y.bigglobe.config.BigGlobeConfig;

@Environment(EnvType.CLIENT)
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreen_MakeBigGlobeTheDefaultWorldType {

	#if MC_VERSION >= MC_1_21_2
		@ModifyExpressionValue(method = "show(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/gui/screen/world/CreateWorldCallback;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/WorldPresets;DEFAULT:Lnet/minecraft/registry/RegistryKey;"))
	#else
		@ModifyExpressionValue(method = "create(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/Screen;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/WorldPresets;DEFAULT:Lnet/minecraft/registry/RegistryKey;"))
	#endif
	private static RegistryKey<WorldPreset> bigglobe_getDefaultWorldPreset(RegistryKey<WorldPreset> original) {
		Identifier identifier = Identifier.tryParse(BigGlobeConfig.INSTANCE.get().defaultWorldType);
		return identifier != null ? RegistryKey.of(RegistryKeys.WORLD_PRESET, identifier) : original;
	}

	//odd that this class needs to be static when its target isn't.
	@Mixin(targets = "net/minecraft/client/gui/screen/world/CreateWorldScreen$WorldTab")
	static class WorldTab_HandleUnknownWorldTypesSanely {

		@WrapOperation(method = "method_48673", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/widget/CyclingButtonWidget;active:Z"))
		private void bigglobe_adjustWorldPreset(CyclingButtonWidget<?> instance, boolean value, Operation<Void> original) {
			original.call(instance, true);
		}
	}
}