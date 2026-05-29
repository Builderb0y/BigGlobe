package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import builderb0y.bigglobe.config.BigGlobeConfig;

@Environment(EnvType.CLIENT)
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreen_MakeBigGlobeTheDefaultWorldType {

	@ModifyExpressionValue(method = "openFresh(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldCallback;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/presets/WorldPresets;NORMAL:Lnet/minecraft/resources/ResourceKey;"))
	private static ResourceKey<WorldPreset> bigglobe_getDefaultWorldPreset(ResourceKey<WorldPreset> original) {
		Identifier identifier = Identifier.tryParse(BigGlobeConfig.INSTANCE.get().defaultWorldType);
		return identifier != null ? ResourceKey.create(Registries.WORLD_PRESET, identifier) : original;
	}

	//odd that this class needs to be static when its target isn't.
	@Mixin(targets = "net/minecraft/client/gui/screens/worldselection/CreateWorldScreen$WorldTab")
	static class WorldTab_HandleUnknownWorldTypesSanely {

		@WrapOperation(method = "lambda$new$1", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/CycleButton;active:Z", opcode = Opcodes.PUTFIELD))
		private static void bigglobe_adjustWorldPreset(CycleButton<?> instance, boolean value, Operation<Void> original) {
			original.call(instance, true);
		}
	}
}