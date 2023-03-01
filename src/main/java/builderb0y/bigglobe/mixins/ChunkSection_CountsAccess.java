package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public interface ChunkSection_CountsAccess {

	@Accessor("nonEmptyBlockCount")
	public abstract short bigglobe_getNonEmptyBlockCount();

	@Accessor("randomTickableBlockCount")
	public abstract short bigglobe_getRandomTickableBlockCount();

	@Accessor("nonEmptyFluidCount")
	public abstract short bigglobe_getRandomTickableFluidCount();

	@Accessor("nonEmptyBlockCount")
	public abstract void bigglobe_setNonEmptyBlockCount(short count);

	@Accessor("randomTickableBlockCount")
	public abstract void bigglobe_setRandomTickableBlockCount(short count);

	@Accessor("nonEmptyFluidCount")
	public abstract void bigglobe_setRandomTickableFluidCount(short count);
}