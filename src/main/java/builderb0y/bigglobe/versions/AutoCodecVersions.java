package builderb0y.bigglobe.versions;

import java.util.function.Supplier;

import builderb0y.autocodec.constructors.ConstructException;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.imprinters.ImprintException;
import builderb0y.autocodec.verifiers.VerifyException;

public class AutoCodecVersions {

	public static EncodeException newEncodeException(Supplier<String> message) {
		return new EncodeException(message);
	}

	public static DecodeException newDecodeExceptions(Supplier<String> message) {
		return new DecodeException(message);
	}

	public static ConstructException newConstructException(Supplier<String> message) {
		return new ConstructException(message);
	}

	public static ImprintException newImprintException(Supplier<String> message) {
		return new ImprintException(message);
	}

	public static VerifyException newVerifyException(Supplier<String> message) {
		return new VerifyException(message);
	}
}