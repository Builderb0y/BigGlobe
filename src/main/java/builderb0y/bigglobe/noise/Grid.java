package builderb0y.bigglobe.noise;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.coders.KeyDispatchCoder;
import builderb0y.autocodec.coders.PrimitiveCoders;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

/**
the common superinterface of all grids.
since grids can be anywhere from 1 to 3 dimensions,
their get() methods take different numbers of parameters.
as such, those methods cannot all be pushed into Grid.
however, the minimum and maximum values don't depend on the dimensionality.
so, that's what we provide here.

the minimum and maximum values of a grid could be used to apply dynamic bias to those values in
such a way that the "real" minimum or maximum value never exceeds a certain hard-coded value.
*/
@UseCoder(name = "CODER", in = Grid.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface Grid {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public MethodInfo minValue, maxValue, getDimensions;
	}

	public static final AutoCoder<Grid> CODER = new KeyDispatchCoder<>(ReifiedType.from(Grid.class), PrimitiveCoders.INT, "dimensions") {

		@Override
		public @Nullable Integer getKey(@NotNull Grid object) {
			return object.getDimensions();
		}

		@Override
		public @Nullable AutoCoder<? extends Grid> getCoder(@NotNull Integer dimensions) {
			return switch (dimensions.intValue()) {
				case 1 -> Grid1D.REGISTRY;
				case 2 -> Grid2D.REGISTRY;
				case 3 -> Grid3D.REGISTRY;
				default -> null;
			};
		}
	};

	/**
	enabled by JUnit. MUST NOT BE ENABLED FROM ANYWHERE ELSE!
	when true, sub-interfaces do not create a Registry for their implementations.
	this aids in testing, since loading Registry-related classes would cause an
	error in a testing environment (cause not bootstrapped), but disabling Registry
	creation in a normal environment would also crash when creating an AutoCoder for grids.

	this field is a MutableBoolean instead of a boolean for the sole reason that
	fields in interfaces are implicitly final, which is undesired for this use case.
	*/
	@TestOnly
	public static final MutableBoolean TESTING = new MutableBoolean(false);

	public abstract double minValue();

	public abstract double maxValue();

	public abstract int getDimensions();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static class GridRegistryEntryCoder<G extends Grid> extends NamedCoder<RegistryEntry<G>> {

		public static final EncoderFactory ENCODER_FACTORY = GridRegistryEntryCoder::tryCreate;
		public static final DecoderFactory DECODER_FACTORY = GridRegistryEntryCoder::tryCreate;

		public final AutoCoder<RegistryEntry<Grid>> fallback;
		public final int dimensions;

		public GridRegistryEntryCoder(@NotNull ReifiedType<RegistryEntry<G>> handledType, AutoCoder<RegistryEntry<Grid>> fallback, int dimensions) {
			super(handledType);
			this.fallback = fallback;
			this.dimensions = dimensions;
		}

		@Override
		public <T_Encoded> @Nullable RegistryEntry<G> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			RegistryEntry<Grid> entry = context.decodeWith(this.fallback);
			if (entry.value().getDimensions() == this.dimensions) {
				return (RegistryEntry<G>)(entry);
			}
			else {
				throw new DecodeException(() -> "Requested " + this.dimensions + " dimensions, but " + UnregisteredObjectException.getID(entry) + " is of " + entry.value().getDimensions() + " dimensions.");
			}
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntry<G>> context) throws EncodeException {
			return (
				(T_Encoded)(
					(
						(EncodeContext)(
							context
						)
					)
					.encodeWith(this.fallback)
				)
			);
		}

		public static <T_HandledType> @Nullable AutoCoder<?> tryCreate(@NotNull FactoryContext<T_HandledType> context) {
			ReifiedType<?> gridType = context.type.resolveParameter(RegistryEntry.class);
			Class<?> rawType;
			if (gridType != null && (rawType = gridType.getRawClass()) != null && Grid.class.isAssignableFrom(rawType)) {
				AutoCoder<RegistryEntry<Grid>> fallback = context.type(ReifiedType.<RegistryEntry<Grid>>parameterize(RegistryEntry.class, ReifiedType.from(Grid.class))).forceCreateCoder();
				if (rawType == Grid1D.class) {
					return new GridRegistryEntryCoder(context.type, fallback, 1);
				}
				else if (rawType == Grid2D.class) {
					return new GridRegistryEntryCoder(context.type, fallback, 2);
				}
				else if (rawType == Grid3D.class) {
					return new GridRegistryEntryCoder(context.type, fallback, 3);
				}
			}
			return null;
		}
	}
}