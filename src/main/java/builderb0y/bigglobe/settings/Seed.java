package builderb0y.bigglobe.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeContext.ArrayDecodePath;
import builderb0y.autocodec.decoders.DecodeContext.ObjectDecodePath;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed.SeedCoder;

@UseCoder(name = "new", in = SeedCoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
public class Seed {

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

	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface SeedModes {

		@MagicConstant(flagsFromClass = Seed.class)
		public abstract int value();
	}

	public static class SeedCoder extends NamedCoder<Seed> {

		public final int modes;

		public SeedCoder(int modes) {
			super("SeedCoder");
			this.modes = modes;
		}

		public SeedCoder(FactoryContext<Seed> context) {
			super("SeedCoder");
			SeedModes annotation = context.type.getAnnotations().getFirst(SeedModes.class);
			this.modes = (annotation != null ? annotation.value() : -1) & (AUTO | NUMBER | STRING);
			if (this.modes == 0) throw new FactoryException("@SeedModes annotation specified no modes.");
		}

		public <T_Encoded> long recursiveDecodeSeed(DecodeContext<T_Encoded> context, String key, boolean last) throws DecodeException {
			DecodeContext<T_Encoded> value = context.getMember(key);
			if ((this.modes & AUTO) != 0 && value.isEmpty()) {
				long seed;
				if (context.parent != null) {
					seed = this.recursiveDecodeSeed(context.parent, key, false);
				}
				else {
					seed = 0L;
				}
				if (context.path instanceof ObjectDecodePath objectDecodePath) {
					seed = Permuter.permute(seed, objectDecodePath.memberName());
				}
				else if (context.path instanceof ArrayDecodePath arrayDecodePath) {
					seed = Permuter.permute(seed, arrayDecodePath.index());
				}
				if (last) {
					seed = Permuter.permute(seed, key);
				}
				return seed;
			}
			String string;
			if ((this.modes & STRING) != 0 && (string = value.tryAsString()) != null) {
				return Permuter.permute(0L, string);
			}
			Number number;
			if ((this.modes & NUMBER) != 0 && (number = value.tryAsNumber()) != null) {
				return number.longValue();
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

		@Override
		public <T_Encoded> @Nullable Seed decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			String key = ((ObjectDecodePath)(context.path)).memberName();
			return new Seed(
				this.recursiveDecodeSeed(
					Objects.requireNonNull(context.parent, "context.parent"),
					key,
					true
				)
			);
		}

		@OverrideOnly
		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, Seed> context) throws EncodeException {
			Seed seed = context.object;
			return seed == null ? context.empty() : context.createLong(seed.value);
		}
	}
}