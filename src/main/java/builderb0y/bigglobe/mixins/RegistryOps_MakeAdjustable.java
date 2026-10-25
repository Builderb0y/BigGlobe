package builderb0y.bigglobe.mixins;

import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.DelegatingOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.RegistryOps.RegistryInfoLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import builderb0y.bigglobe.mixinInterfaces.AdjustableRegistryOps;

@Mixin(RegistryOps.class)
public abstract class RegistryOps_MakeAdjustable<T> extends DelegatingOps<T> implements AdjustableRegistryOps {

	@Shadow
	@Final
	private RegistryInfoLookup lookupProvider;

	public RegistryOps_MakeAdjustable(DynamicOps<T> delegate) {
		super(delegate);
	}

	@Override
	public <T_NewType> RegistryOps<T_NewType> bigglobe_changeType(DynamicOps<T_NewType> newDelegate) {
		return RegistryOps.create(newDelegate, this.lookupProvider);
	}
}