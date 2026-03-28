package builderb0y.bigglobe.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class BigGlobeParticles {

	public static void init() {
		SporeParticles.init();
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		SporeParticles.initClient();
	}
}