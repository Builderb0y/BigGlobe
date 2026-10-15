package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.FallingBlock;

@Mixin(FallingBlock.class)
public interface FallingBlock_Access {

	@Invoker("falling")
	public abstract void bigglobe_falling(FallingBlockEntity entity);
}