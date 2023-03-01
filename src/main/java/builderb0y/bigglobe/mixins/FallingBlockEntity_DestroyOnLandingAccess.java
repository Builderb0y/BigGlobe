package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.FallingBlockEntity;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntity_DestroyOnLandingAccess {

	@Accessor("destroyedOnLanding")
	public abstract boolean shouldDestroyOnLanding();

	@Accessor("destroyedOnLanding")
	public abstract void setDestroyOnLanding(boolean destroyOnLanding);
}