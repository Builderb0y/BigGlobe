package builderb0y.bigglobe.mixins;

import me.shedaniel.autoconfig.ConfigData;
import org.spongepowered.asm.mixin.Mixin;

import builderb0y.bigglobe.config.BigGlobeConfig;

/**
cloth config/auto config requires that my config class
{@link BigGlobeConfig} implements {@link ConfigData},
which is a problem because it places a hard dependency on cloth config.
I would like a soft dependency instead, where big globe
can load normally when cloth config is not installed.

so, this mixin makes BigGlobeConfig implement ConfigData,
but this mixin will be disabled whenever cloth config is not installed.
that logic is controlled by {@link ClothConfigCompatibilityPlugin#shouldApplyMixin(String, String)}.
*/
@Mixin(BigGlobeConfig.class)
public class BigGlobeConfig_ImplementConfigData implements ConfigData {}