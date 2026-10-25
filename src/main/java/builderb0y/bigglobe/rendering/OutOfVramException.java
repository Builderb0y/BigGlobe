package builderb0y.bigglobe.rendering;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;

@Environment(EnvType.CLIENT)
public class OutOfVramException extends RuntimeException {

	public OutOfVramException() {
	}

	public OutOfVramException(String message) {
		super(message);
	}

	public OutOfVramException(Throwable cause) {
		super(cause);
	}

	public OutOfVramException(String message, Throwable cause) {
		super(message, cause);
	}
}