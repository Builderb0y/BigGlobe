package builderb0y.bigglobe.codecs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestTypes;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.Mirror;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.registries.BetterHardCodedRegistryCoder;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BlockStateCoder extends NamedCoder<BlockState> {

	public static final BlockStateCoder INSTANCE = new BlockStateCoder("BlockStateCoder.INSTANCE");

	public static final AutoCoder<BetterRegistry<Block>> BLOCK_REGISTRY_CODER = new BetterHardCodedRegistryCoder<>(RegistryVersions.block());

	public BlockStateCoder(String toString) {
		super(toString);
	}

	public static <T_Encoded> void verifyNormal(VerifyContext<T_Encoded, BlockState> context) throws VerifyException {
		BlockState state = context.object;
		if (state != null && (state.hasBlockEntity() || PointOfInterestTypes.getTypeForState(state).isPresent())) {
			throw new VerifyException(() -> {
				StringBuilder message = new StringBuilder("For technical reasons, ");
				context.appendPathTo(message);
				return message.append(" cannot have a BlockEntity or be a point of interest. (was ").append(state).append(')').toString();
			});
		}
	}

	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	@UseVerifier(name = "verifyNormal", in = BlockStateCoder.class, usage = MemberUsage.METHOD_IS_HANDLER)
	@Mirror(UseVerifier.class)
	public static @interface VerifyNormal {}

	@Override
	public <T_Encoded> @Nullable BlockState decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		String string = context.tryAsString();
		if (string != null) try {
			BetterRegistry<Block> blockRegistry = context.decodeWith(BLOCK_REGISTRY_CODER);
			BlockProperties blockProperties = decodeState(blockRegistry, string);
			Set<Property<?>> missing = blockProperties.missing();
			if (!missing.isEmpty()) {
				context.logger().logErrorLazy(() -> "Missing properties: " + missing);
			}
			return blockProperties.state();
		}
		catch (RuntimeException exception) {
			throw new DecodeException(exception);
		}
		else {
			return context.logger().unwrapLazy(
				BlockState.CODEC.parse(context.ops, context.input),
				true,
				DecodeException::new
			);
		}
	}

	public static BlockProperties decodeState(BetterRegistry<Block> blockRegistry, String input) {
		int openBracket = input.indexOf('[');
		Identifier blockID = new Identifier(openBracket >= 0 ? input.substring(0, openBracket) : input);
		Block block = blockRegistry.getOrCreateEntry(RegistryKey.of(RegistryKeyVersions.block(), blockID)).value();
		BlockState state = block.getDefaultState();
		if (openBracket >= 0) {
			if (block.getStateManager().getProperties().isEmpty()) {
				throw new IllegalArgumentException("Block " + blockID + " has no properties, but input string specified an opening '[' anyway.");
			}
			int closeBracket = input.indexOf(']');
			if (closeBracket != input.length() - 1) {
				throw new IllegalArgumentException("Closing ']' must be the last character in the input string: " + input);
			}
			String[] split = input.substring(openBracket + 1, closeBracket).split(",");
			Map<Property<?>, Comparable<?>> properties = new Object2ObjectOpenHashMap<>(split.length);
			for (String pair : split) {
				int equals = pair.indexOf('=');
				if (equals < 0) {
					throw new IllegalArgumentException("Expected '=' somewhere in " + pair);
				}
				String propertyName = pair.substring(0, equals);
				Property property = block.getStateManager().getProperty(propertyName);
				if (property == null) {
					throw new IllegalArgumentException("Block " + blockID + " has no such property named " + propertyName + " for input " + input);
				}
				String valueString = pair.substring(equals + 1);
				Comparable value = (Comparable)(property.parse(valueString).orElse(null));
				if (value == null) {
					throw new IllegalArgumentException("Value " + valueString + " is not applicable for property " + propertyName + " for input " + input);
				}
				state = state.with(property, value);
				properties.put(property, value);
			}
			return new BlockProperties(blockID, block, state, properties);
		}
		else {
			return new BlockProperties(blockID, block, state, Collections.emptyMap());
		}
	}

	public static record BlockProperties(Identifier id, Block block, BlockState state, Map<Property<?>, Comparable<?>> properties) {

		public Set<Property<?>> missing() {
			Collection<Property<?>> properties = this.block.getStateManager().getProperties();
			if (properties.size() == this.properties.size()) return Collections.emptySet();
			Set<Property<?>> set = new HashSet<>(properties);
			set.removeAll(this.properties.keySet());
			return set;
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BlockState> context) throws EncodeException {
		BlockState state = context.input;
		if (state == null) return context.empty();
		return context.createString(encodeState(state));
	}

	public static String encodeState(BlockState state) {
		StringBuilder builder = new StringBuilder(64);
		Optional<RegistryKey<Block>> key = state.getRegistryEntry().getKey();
		if (key.isPresent()) builder.append(key.get().getValue());
		else throw new EncodeException(() -> "Unregistered block: " + state);
		Collection<Property<?>> properties = state.getProperties();
		if (!properties.isEmpty()) {
			builder.append('[');
			Iterator<Property<?>> iterator = properties.iterator();
			appendProperty(builder, state, iterator.next());
			while (iterator.hasNext()) {
				appendProperty(builder.append(','), state, iterator.next());
			}
			builder.append(']');
		}
		return builder.toString();
	}

	public static <T extends Comparable<T>> void appendProperty(StringBuilder builder, BlockState state, Property<T> property) {
		builder.append(property.getName()).append('=').append(property.name(state.get(property)));
	}
}