package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;

@Environment(EnvType.CLIENT)
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreen_MakeBigGlobeTheDefaultWorldType {

	@Redirect(method = "create(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/Screen;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/WorldPresets;DEFAULT:Lnet/minecraft/util/registry/RegistryKey;"))
	private static RegistryKey<WorldPreset> bigglobe_getDefaultWorldPreset() {
		return BigGlobeConfig.INSTANCE.get().makeBigGlobeDefaultWorldType ? BigGlobeMod.BIG_GLOBE_WORLD_PRESET_KEY : WorldPresets.DEFAULT;
	}
}