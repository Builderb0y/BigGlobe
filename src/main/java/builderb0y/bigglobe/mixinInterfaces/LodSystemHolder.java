package builderb0y.bigglobe.mixinInterfaces;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.rendering.lods.LodSystem;

public interface LodSystemHolder {

	public abstract @Nullable LodSystem bigglobe_getLodSystem();

	public abstract void bigglobe_setLodSystem(@Nullable LodSystem system);
}