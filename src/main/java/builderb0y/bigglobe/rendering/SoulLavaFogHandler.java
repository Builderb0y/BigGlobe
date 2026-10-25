package builderb0y.bigglobe.rendering;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;

@Environment(EnvType.CLIENT)
public class SoulLavaFogHandler {

	/**
	yes, it is kind of stupid to have an entire class just for this one field.
	I would've put it inside {@link LavaFogEnvironment} via mixin,
	but it would need to be public and static for that, which is not allowed.
	*/
	public static boolean inSoulLava;
}