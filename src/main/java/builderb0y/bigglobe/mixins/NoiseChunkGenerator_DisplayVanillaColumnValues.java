package builderb0y.bigglobe.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.VanillaWorldColumn;
import builderb0y.bigglobe.mixinInterfaces.ColumnValueDisplayer;

@Mixin(NoiseChunkGenerator.class)
public class NoiseChunkGenerator_DisplayVanillaColumnValues implements ColumnValueDisplayer {

	@Unique
	public ColumnValue<?>[] bigglobe_displayedColumnValues;

	@Override
	public ColumnValue<?>[] bigglobe_getDisplayedColumnValues() {
		return this.bigglobe_displayedColumnValues;
	}

	@Override
	public void bigglobe_setDisplayedColumnValues(ColumnValue<?>[] displayedColumnValues) {
		this.bigglobe_displayedColumnValues = displayedColumnValues;
	}

	@Inject(method = "getDebugHudText", at = @At("RETURN"))
	private void bigglobe_displayVanillaColumnValues(List<String> text, NoiseConfig noiseConfig, BlockPos pos, CallbackInfo callback) {
		this.bigglobe_appendText(text, new VanillaWorldColumn(0L, (ChunkGenerator)(Object)(this), noiseConfig, pos.getX(), pos.getZ()), pos.getY());
	}
}