package builderb0y.bigglobe.settings;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeContext.ArrayDecodePath;
import builderb0y.autocodec.decoders.DecodeContext.ObjectDecodePath;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.bigglobe.noise.Permuter;

@UseCoder(name = "code", usage = MemberUsage.METHOD_IS_HANDLER)
public abstract class Seed {

	public final long value;

	public Seed(long value) {
		this.value = value;
	}

	public long xor(long other) {
		return this.value ^ other;
	}

	public long xor(Seed other) {
		return this.value ^ other.value;
	}

	public abstract <T_Encoded> T_Encoded encode(EncodeContext<T_Encoded, Seed> context);

	public static <T_Encoded> T_Encoded code(EncodeContext<T_Encoded, Seed> context) {
		Seed seed = context.input;
		return seed == null ? context.empty() : seed.encode(context);
	}

	public static <T_Encoded> Seed code(DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return new AutoSeed(context);
		Number number = context.tryAsNumber();
		if (number != null) return new NumberSeed(number.longValue());
		String string = context.tryAsString();
		if (string != null) return new StringSeed(string);
		throw new DecodeException(context.pathToStringBuilder().append(" must be empty, a number, or a string. Was: ").append(context.input).toString());
	}

	public static long computeSeedFromPath(DecodeContext<?> context) {
		long seed;
		if (context.parent != null) {
			seed = computeSeedFromPath(context.parent);
		}
		else {
			seed = 0L;
		}
		if (context.path instanceof ObjectDecodePath path) {
			seed = Permuter.permute(seed, path.memberName());
		}
		else if (context.path instanceof ArrayDecodePath path) {
			seed = Permuter.permute(seed, path.index());
		}
		return seed;
	}

	public static class AutoSeed extends Seed {

		public AutoSeed(DecodeContext<?> context) {
			super(computeSeedFromPath(context));
		}

		@Override
		public <T_Encoded> T_Encoded encode(EncodeContext<T_Encoded, Seed> context) {
			return context.empty();
		}
	}

	public static class NumberSeed extends Seed {

		public NumberSeed(long value) {
			super(value);
		}

		@Override
		public <T_Encoded> T_Encoded encode(EncodeContext<T_Encoded, Seed> context) {
			return context.createLong(this.value);
		}
	}

	public static class StringSeed extends Seed {

		public final String source;

		public StringSeed(String source) {
			super(Permuter.permute(0L, source));
			this.source = source;
		}

		@Override
		public <T_Encoded> T_Encoded encode(EncodeContext<T_Encoded, Seed> context) {
			return context.createString(this.source);
		}
	}
}