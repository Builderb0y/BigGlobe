package builderb0y.bigglobe.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseDecoder;
import builderb0y.autocodec.annotations.UseEncoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.decoders.AutoDecoder.NamedDecoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeContext.ArrayDecodePath;
import builderb0y.autocodec.decoders.DecodeContext.ObjectDecodePath;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.bigglobe.noise.Permuter;

@UseEncoder(name = "encode", usage = MemberUsage.METHOD_IS_HANDLER)
@UseDecoder(name = "new", in = Seed.SeedDecoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
public abstract class Seed {

	public static final int
		AUTO   = 1 << 0,
		NUMBER = 1 << 1,
		STRING = 1 << 2;

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

	public abstract <T_Encoded> T_Encoded doEncode(EncodeContext<T_Encoded, Seed> context);

	public static <T_Encoded> T_Encoded encode(EncodeContext<T_Encoded, Seed> context) {
		Seed seed = context.input;
		return seed == null ? context.empty() : seed.doEncode(context);
	}

	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface SeedModes {

		@MagicConstant(flagsFromClass = Seed.class)
		public abstract int value();
	}

	public static class SeedDecoder extends NamedDecoder<Seed> {

		public final int modes;

		public SeedDecoder(int modes) {
			super("SeedDecoder");
			this.modes = modes;
		}

		public SeedDecoder(FactoryContext<Seed> context) {
			super("SeedDecoder");
			SeedModes annotation = context.type.getAnnotations().getFirst(SeedModes.class);
			this.modes = (annotation != null ? annotation.value() : -1) & (AUTO | NUMBER | STRING);
			if (this.modes == 0) throw new FactoryException("@SeedModes annotation specified no modes.");
		}

		@Override
		public <T_Encoded> @Nullable Seed decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if ((this.modes & AUTO) != 0 && context.isEmpty()) {
				return new AutoSeed(context);
			}
			Number number;
			if ((this.modes & NUMBER) != 0 && (number = context.tryAsNumber()) != null) {
				return new NumberSeed(number.longValue());
			}
			String string;
			if ((this.modes & STRING) != 0 && (string = context.tryAsString()) != null) {
				return new StringSeed(string);
			}
			throw new DecodeException(() -> {
				StringBuilder builder = context.pathToStringBuilder().append(" must be ");
				List<String> options = new ArrayList<>(3);
				if ((this.modes & AUTO) != 0) {
					options.add("null");
				}
				if ((this.modes & NUMBER) != 0) {
					options.add("a number");
				}
				if ((this.modes & STRING) != 0) {
					options.add("a string");
				}
				switch (options.size()) {
					case 0 -> throw new AssertionError("no modes");
					case 1 -> builder.append(options.get(0));
					case 2 -> builder.append(options.get(0)).append(" or ").append(options.get(1));
					case 3 -> builder.append(options.get(0)).append(", ").append(options.get(1)).append(", or ").append(options.get(2));
					default -> throw new AssertionError(options.size() + " modes?");
				}
				return builder.toString();
			});
		}
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
		public <T_Encoded> T_Encoded doEncode(EncodeContext<T_Encoded, Seed> context) {
			return context.empty();
		}
	}

	public static class NumberSeed extends Seed {

		public NumberSeed(long value) {
			super(value);
		}

		@Override
		public <T_Encoded> T_Encoded doEncode(EncodeContext<T_Encoded, Seed> context) {
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
		public <T_Encoded> T_Encoded doEncode(EncodeContext<T_Encoded, Seed> context) {
			return context.createString(this.source);
		}
	}
}