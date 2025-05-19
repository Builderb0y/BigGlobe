package builderb0y.bigglobe.codecs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.stream.Stream;

import com.mojang.serialization.DynamicOps;
import dev.yumi.commons.Either;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestTypes;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.Mirror;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.DataOps;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.data.StringData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.registries.AbstractRegistryCoder;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.mixinInterfaces.AdjustableRegistryOps;
import builderb0y.bigglobe.versions.IdentifierVersions;

public class BlockStateCoder extends NamedCoder<BlockState> {

	public static final BlockStateCoder INSTANCE = new BlockStateCoder("BlockStateCoder.INSTANCE");

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
		StringData string = context.tryAsString();
		if (string != null) try {
			BetterRegistry<Block> blockRegistry = AbstractRegistryCoder.registry(RegistryKeys.BLOCK, context);
			BlockProperties blockProperties = decodeState(blockRegistry, string.value);
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
			DynamicOps<Data> dataOps = context.ops.compressMaps() ? DataOps.COMPRESSED : DataOps.UNCOMPRESSED;
			if (context.ops instanceof AdjustableRegistryOps registryOps) {
				dataOps = registryOps.bigglobe_changeType(dataOps);
			}
			return context.logger().unwrapLazy(
				BlockState.CODEC.parse(dataOps, context.data),
				true,
				DecodeException::new
			);
		}
	}

	public static BlockProperties decodeState(BetterRegistry<Block> blockRegistry, String input) {
		if (input.charAt(0) == '#') throw new IllegalArgumentException("Tags not allowed here");
		int openBracket = input.indexOf('[');
		Identifier blockID = IdentifierVersions.create(openBracket >= 0 ? input.substring(0, openBracket) : input);
		Block block = blockRegistry.requireById(blockID).value();
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

		public Stream<BlockState> allStates() {
			Stream<BlockState> stream = this.block.getStateManager().getStates().stream();
			return switch (this.properties.size()) {
				case 0 -> stream;
				case 1 -> {
					Map.Entry<Property<?>, Comparable<?>> entry = this.properties.entrySet().iterator().next();
					yield stream.filter((BlockState state) -> state.get(entry.getKey()).equals(entry.getValue()));
				}
				case 2 -> {
					Iterator<Map.Entry<Property<?>, Comparable<?>>> iterator = this.properties.entrySet().iterator();
					Map.Entry<Property<?>, Comparable<?>> entry1 = iterator.next(), entry2 = iterator.next();
					yield stream.filter((BlockState state) -> state.get(entry1.getKey()).equals(entry1.getValue()) && state.get(entry2.getKey()).equals(entry2.getValue()));
				}
				default -> {
					yield stream.filter((BlockState state) -> {
						for (Map.Entry<Property<?>, Comparable<?>> entry : this.properties.entrySet()) {
							if (!state.get(entry.getKey()).equals(entry.getValue())) return false;
						}
						return true;
					});
				}
			};
		}
	}

	public static TagProperties decodeTag(BetterRegistry<Block> blockRegistry, String input) {
		if (input.charAt(0) != '#') throw new IllegalArgumentException("Not a tag: " + input);
		int openBracket = input.indexOf('[');
		Identifier tagID = IdentifierVersions.create(openBracket >= 0 ? input.substring(1, openBracket) : input.substring(1));
		RegistryEntryList<Block> tag = blockRegistry.requireTag(TagKey.of(RegistryKeys.BLOCK, tagID));
		if (openBracket >= 0) {
			int closeBracket = input.indexOf(']');
			if (closeBracket != input.length() - 1) {
				throw new IllegalArgumentException("Closing ']' must be the last character in the input string: " + input);
			}
			String[] split = input.substring(openBracket + 1, closeBracket).split(",");
			Map<String, String> properties = new Object2ObjectOpenHashMap<>(split.length);
			for (String pair : split) {
				int equals = pair.indexOf('=');
				if (equals < 0) {
					throw new IllegalArgumentException("Expected '=' somewhere in " + pair);
				}
				properties.put(pair.substring(0, equals), pair.substring(equals + 1));
			}
			return new TagProperties(tagID, tag, properties);
		}
		else {
			return new TagProperties(tagID, tag, Collections.emptyMap());
		}
	}

	public static record TagProperties(Identifier id, RegistryEntryList<Block> tag, Map<String, String> properties) {

		public Stream<BlockState> collectStates() {
			return this.tag.stream().map(
				(RegistryEntry<Block> entry) -> entry.value().getStateManager()
			)
			.flatMap(
				//create specialized lambdas to handle small sizes.
				switch (this.properties.size()) {
					case 0 -> {
						yield (StateManager<Block, BlockState> manager) -> manager.getStates().stream();
					}
					case 1 -> {
						Map.Entry<String, String> entry = this.properties.entrySet().iterator().next();
						yield (StateManager<Block, BlockState> manager) -> {
							Property<?> property = manager.getProperty(entry.getKey());
							if (property == null) return Stream.empty();
							Comparable<?> value = property.parse(entry.getValue()).orElse(null);
							if (value == null) return Stream.empty();
							return manager.getStates().stream().filter((BlockState state) -> state.get(property).equals(value));
						};
					}
					case 2 -> {
						Iterator<Map.Entry<String, String>> iterator = this.properties.entrySet().iterator();
						Map.Entry<String, String> entry1 = iterator.next(), entry2 = iterator.next();
						yield (StateManager<Block, BlockState> manager) -> {
							Property<?> property1 = manager.getProperty(entry1.getKey());
							if (property1 == null) return Stream.empty();
							Comparable<?> value1 = property1.parse(entry1.getValue()).orElse(null);
							if (value1 == null) return Stream.empty();
							Property<?> property2 = manager.getProperty(entry2.getKey());
							if (property2 == null) return Stream.empty();
							Comparable<?> value2 = property2.parse(entry2.getValue()).orElse(null);
							if (value2 == null) return Stream.empty();
							return manager.getStates().stream().filter((BlockState state) -> state.get(property1).equals(value1) && state.get(property2).equals(value2));
						};
					}
					default -> {
						yield (StateManager<Block, BlockState> manager) -> {
							Object2ObjectOpenHashMap<Property<?>, Comparable<?>> properties = new Object2ObjectOpenHashMap<>(this.properties.size());
							for (Map.Entry<String, String> entry : this.properties.entrySet()) {
								Property<?> property = manager.getProperty(entry.getKey());
								if (property == null) return Stream.empty();
								Comparable<?> value = property.parse(entry.getValue()).orElse(null);
								if (value == null) return Stream.empty();
								properties.put(property, value);
							}
							return manager.getStates().stream().filter((BlockState state) -> {
								for (ObjectIterator<Object2ObjectMap.Entry<Property<?>, Comparable<?>>> iterator = properties.object2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
									Map.Entry<Property<?>, Comparable<?>> entry = iterator.next();
									if (!state.get(entry.getKey()).equals(entry.getValue())) return false;
								}
								return true;
							});
						};
					}
				}
			);
		}
	}

	public static Either<BlockProperties, TagProperties> decodeBlockOrTag(BetterRegistry<Block> blockRegistry, String input) {
		return input.charAt(0) == '#' ? Either.right(decodeTag(blockRegistry, input)) : Either.left(decodeState(blockRegistry, input));
	}

	@Override
	public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, BlockState> context) throws EncodeException {
		BlockState state = context.object;
		if (state == null) return EmptyData.INSTANCE;
		return new StringData(encodeState(state));
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