package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class OutOfVramException extends RuntimeException {

	public OutOfVramException() {}

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