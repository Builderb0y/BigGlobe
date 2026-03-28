package builderb0y.bigglobe.mixins;

import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntity_DestroyOnLandingAccess {

	@Accessor("cancelDrop")
	public abstract boolean shouldDestroyOnLanding();

	@Accessor("cancelDrop")
	public abstract void setDestroyOnLanding(boolean destroyOnLanding);
}