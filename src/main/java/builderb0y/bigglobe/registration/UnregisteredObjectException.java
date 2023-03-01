package builderb0y.bigglobe.registration;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

/**
sometimes thrown when the {@link Identifier} or {@link RegistryKey} is
queried for an object which has not been registered to a {@link Registry}.
*/
public class UnregisteredObjectException extends RuntimeException {

	public UnregisteredObjectException() {}

	public UnregisteredObjectException(String message) {
		super(message);
	}

	public UnregisteredObjectException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnregisteredObjectException(Throwable cause) {
		super(cause);
	}
}