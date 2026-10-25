package builderb0y.bigglobe.particles;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;

public class BigGlobeParticles {

	public static void init() {
		SporeParticles.init();
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		SporeParticles.initClient();
	}
}