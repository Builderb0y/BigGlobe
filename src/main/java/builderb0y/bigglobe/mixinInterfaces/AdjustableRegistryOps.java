package builderb0y.bigglobe.mixinInterfaces;

import com.mojang.serialization.DynamicOps;

import net.minecraft.registry.RegistryOps;

public interface AdjustableRegistryOps {

	/**
	vanilla has a replacement for this in some MC versions, but not others.
	*/
	public abstract <T_NewType> RegistryOps<T_NewType> bigglobe_changeType(DynamicOps<T_NewType> newDelegate);
}