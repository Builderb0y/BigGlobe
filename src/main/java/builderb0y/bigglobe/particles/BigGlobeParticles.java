package builderb0y.bigglobe.particles;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;

public class BigGlobeParticles {

	public static void init() {
		SporeParticles.init();
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static void initClient() {
		SporeParticles.initClient();
	}
}