package builderb0y.bigglobe.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.WorldPreset;

@Mixin(WorldPreset.class)
public interface WorldPreset_DimensionsAccess {

	@Accessor("dimensions")
	public abstract Map<RegistryKey<DimensionOptions>, DimensionOptions> bigglobe_getDimensions();
}