package builderb0y.bigglobe.mixinInterfaces;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;

import builderb0y.bigglobe.rendering.lods.LodSystem;

public interface LodSystemHolder {

	public abstract @Nullable LodSystem bigglobe_getLodSystem();

	public abstract void bigglobe_setLodSystem(@Nullable LodSystem system);

	public static LodSystemHolder get() {
		#if MC_VERSION >= MC_26_2_0
			return (LodSystemHolder)(Minecraft.getInstance().levelExtractor);
		#else
			return (LodSystemHolder)(Minecraft.getInstance().levelRenderer);
		#endif
	}
}