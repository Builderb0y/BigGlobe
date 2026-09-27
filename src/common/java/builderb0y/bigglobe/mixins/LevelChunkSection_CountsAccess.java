package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.chunk.LevelChunkSection;

@Mixin(LevelChunkSection.class)
public interface LevelChunkSection_CountsAccess {

	@Accessor("nonEmptyBlockCount")
	public abstract void bigglobe_setNonEmptyBlockCount(short count);

	@Accessor("tickingBlockCount")
	public abstract void bigglobe_setTickingBlockCount(short count);

	@Accessor("fluidCount")
	public abstract void bigglobe_setFluidCount(short count);

	@Accessor("tickingFluidCount")
	public abstract void bigglobe_setTickingFluidCount(short count);
}
