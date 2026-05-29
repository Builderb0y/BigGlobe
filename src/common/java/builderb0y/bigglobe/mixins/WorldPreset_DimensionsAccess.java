package builderb0y.bigglobe.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

@Mixin(WorldPreset.class)
public interface WorldPreset_DimensionsAccess {

	@Accessor("dimensions")
	public abstract Map<ResourceKey<LevelStem>, LevelStem> bigglobe_getDimensions();
}