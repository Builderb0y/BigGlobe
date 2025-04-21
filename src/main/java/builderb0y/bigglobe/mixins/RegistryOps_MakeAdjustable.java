package builderb0y.bigglobe.mixins;

import com.mojang.serialization.DynamicOps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryOps.RegistryInfoGetter;
import net.minecraft.util.dynamic.ForwardingDynamicOps;

import builderb0y.bigglobe.mixinInterfaces.AdjustableRegistryOps;

@Mixin(RegistryOps.class)
public abstract class RegistryOps_MakeAdjustable<T> extends ForwardingDynamicOps<T> implements AdjustableRegistryOps {

	@Shadow @Final private RegistryInfoGetter registryInfoGetter;

	public RegistryOps_MakeAdjustable(DynamicOps<T> delegate) {
		super(delegate);
	}

	@Override
	public <T_NewType> RegistryOps<T_NewType> bigglobe_changeType(DynamicOps<T_NewType> newDelegate) {
		return RegistryOps.of(newDelegate, this.registryInfoGetter);
	}
}