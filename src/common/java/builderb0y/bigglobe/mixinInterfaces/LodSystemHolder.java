package builderb0y.bigglobe.mixinInterfaces;

import net.minecraft.client.renderer.LevelRenderer;

public interface LodSystemHolder {

	/*
	//todo: re-enable once rendering is re-written.
	public abstract @Nullable LodSystem bigglobe_getLodSystem();

	public abstract void bigglobe_setLodSystem(@Nullable LodSystem system);
	*/

	public static LodSystemHolder of(LevelRenderer worldRenderer) {
		return (LodSystemHolder)(worldRenderer);
	}
}