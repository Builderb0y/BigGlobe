package builderb0y.bigglobe.codecs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
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
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.registries.AbstractRegistryCoder;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.mixinInterfaces.AdjustableRegistryOps;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BlockStateCoder extends NamedCoder<BlockState> {

	public static Map<Identifier, Block> RENAMED_BLOCKS = new Object2ObjectOpenHashMap<>(2);

	static {

		RENAMED_BLOCKS.put(BigGlobeMod.mcID("chain"), Blocks.IRON_CHAIN);

		RENAMED_BLOCKS.put(BigGlobeMod.mcID("grass"), Blocks.SHORT_GRASS);
	}

	public static final BlockStateCoder INSTANCE = new BlockStateCoder("BlockStateCoder.INSTANCE");

	public BlockStateCoder(String toString) {
		super(toString);
	}

	public static <T_Encoded> void verifyNormal(VerifyContext<T_Encoded, BlockState> context) throws VerifyException {
		BlockState state = context.object;
		if (state != null && (state.hasBlockEntity() || PoiTypes.forState(state).isPresent())) {
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
		if (string != null) {
			BetterRegistry<Block> blockRegistry = AbstractRegistryCoder.registry(Registries.BLOCK, context);
			return (
				withMissingErrors(decodeState(blockRegistry, string.value))
					.unwrapLazy(context.logger()::logErrorLazy, DecodeException::new)
					.state()
			);
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

	public static Block blockOnly(BetterRegistry<Block> blockRegistry, String input) {
		Identifier identifier = IdentifierVersions.create(input);
		Block block = RENAMED_BLOCKS.get(identifier);
		return block != null ? block : blockRegistry.requireByName(input).value();
	}

	public static Result<BlockProperties> decodeStateWithMissingErrors(BetterRegistry<Block> blockRegistry, String input) {
		return withMissingErrors(decodeState(blockRegistry, input));
	}

	public static Result<BlockProperties> decodeState(BetterRegistry<Block> blockRegistry, String input) {
		return switch (input) {
			default -> {
				if (input.charAt(0) == '#') yield new Result<>((StringBuilder builder) -> builder.append("Tags not allowed here"));
				int openBracket = input.indexOf('[');
				Identifier blockID = IdentifierVersions.create(openBracket >= 0 ? input.substring(0, openBracket) : input);
				Block block = RENAMED_BLOCKS.get(blockID);
				if (block == null) block = blockRegistry.requireById(blockID).value();
				BlockState state = block.defaultBlockState();
				if (openBracket >= 0) {
					if (block.getStateDefinition().getProperties().isEmpty()) {
						yield new Result<>(new BlockProperties(block.defaultBlockState()), (StringBuilder builder) -> builder.append("Block ").append(blockID).append(" has no properties, but input string specified an opening '[' anyway."));
					}
					List<Consumer<StringBuilder>> errors = null;
					int closeBracket = input.indexOf(']', openBracket + 1);
					if (closeBracket != input.length() - 1) {
						errors = new ArrayList<>();
						if (closeBracket >= 0) {
							errors.add((StringBuilder builder) -> builder.append("Closing ']' is not at the end of the input string: ").append(input).append(", all input after it will be ignored"));
						}
						else {
							errors.add((StringBuilder builder) -> builder.append("Closing ']' not found in input string: ").append(input).append(", attempting to parse everything until the end of the string"));
							closeBracket = input.length();
						}
					}
					String[] split = input.substring(openBracket + 1, closeBracket).split(",");
					Map<Property<?>, Comparable<?>> properties = new Object2ObjectOpenHashMap<>(split.length);
					for (String pair : split) {
						int equals = pair.indexOf('=');
						if (equals < 0) {
							if (errors == null) errors = new ArrayList<>(split.length);
							errors.add((StringBuilder builder) -> builder.append("Expected '=' somewhere in ").append(pair));
							continue;
						}
						String propertyName = pair.substring(0, equals);
						Property property = block.getStateDefinition().getProperty(propertyName);
						if (property == null) {
							if (errors == null) errors = new ArrayList<>(split.length);
							errors.add((StringBuilder builder) -> builder.append("Block ").append(blockID).append(" has no such property named ").append(propertyName).append(" for input ").append(input));
							continue;
						}
						String valueString = pair.substring(equals + 1);
						Comparable value = (Comparable)(property.getValue(valueString).orElse(null));
						if (value == null) {
							if (errors == null) errors = new ArrayList<>(split.length);
							errors.add((StringBuilder builder) -> builder.append("Value ").append(valueString).append(" is not applicable for property ").append(propertyName).append(" for input ").append(input));
							continue;
						}
						state = state.setValue(property, value);
						properties.put(property, value);
					}
					yield new Result<>(new BlockProperties(blockID, block, state, properties), concat(errors));
				}
				else {
					yield new Result<>(new BlockProperties(blockID, block, state, Collections.emptyMap()));
				}
			}
		};
	}

	public static BetterRegistry<Block> findBlockRegistry() {
		MinecraftServer server = BigGlobeMod.currentServer;
		if (server != null) {
			return new BetterHardCodedRegistry<>(RegistryVersions.getRegistry(server.registryAccess(), Registries.BLOCK));
		}
		else if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			return findBlockRegistryClient();
		}
		else {
			return new BetterHardCodedRegistry<>(BuiltInRegistries.BLOCK);
		}
	}

	@Environment(EnvType.CLIENT)
	public static BetterRegistry<Block> findBlockRegistryClient() {
		ClientLevel world = Minecraft.getInstance().level;
		return new BetterHardCodedRegistry<>(world != null ? RegistryVersions.getRegistry(world.registryAccess(), Registries.BLOCK) : BuiltInRegistries.BLOCK);
	}

	public static boolean isEnabled(Block block) {
		MinecraftServer server = BigGlobeMod.currentServer;
		if (server != null) {
			return block.isEnabled(server.getWorldData().enabledFeatures());
		}
		else if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			return isEnabledClient(block);
		}
		else {
			return true;
		}
	}

	@Environment(EnvType.CLIENT)
	public static boolean isEnabledClient(Block block) {
		ClientLevel world = Minecraft.getInstance().level;
		return world == null || block.isEnabled(world.enabledFeatures());
	}

	public static Result<BlockProperties> withMissingErrors(Result<BlockProperties> result) {
		BlockProperties block = result.value;
		if (block != null) {
			Set<Property<?>> missing = block.missing();
			if (missing.isEmpty()) {
				return result;
			}
			else {
				Consumer<StringBuilder> missingErrors = (StringBuilder builder) -> builder.append("Block ").append(block.id).append(" is missing properties: ").append(missing);
				Consumer<StringBuilder> errors = result.errors;
				if (errors != null) {
					return new Result<>(
						block, (StringBuilder builder) -> {
						errors.accept(builder);
						builder.append("; ");
						missingErrors.accept(builder);
					}
					);
				}
				else {
					return new Result<>(block, missingErrors);
				}
			}
		}
		else {
			return result;
		}
	}

	public static interface BlockStateSource {

		public abstract Stream<BlockState> getMatchingStates();
	}

	public static record BlockProperties(
		Identifier id,
		Block block,
		BlockState state,
		Map<Property<?>, Comparable<?>> properties,
		boolean enabled
	)
		implements BlockStateSource {

		public BlockProperties(Identifier id, Block block, BlockState state, Map<Property<?>, Comparable<?>> properties) {
			this(id, block, state, properties, isEnabled(block));
		}

		public BlockProperties(BlockState state) {
			this(UnregisteredObjectException.getID(state.typeHolder()), state.getBlock(), state, state.getValues().collect(Collectors.toMap(Property.Value::property, Property.Value::value)), isEnabled(state.getBlock()));
		}

		public Set<Property<?>> missing() {
			Collection<Property<?>> properties = this.block.getStateDefinition().getProperties();
			if (properties.size() == this.properties.size()) return Collections.emptySet();
			Set<Property<?>> set = new HashSet<>(properties);
			set.removeAll(this.properties.keySet());
			return set;
		}

		@Override
		public Stream<BlockState> getMatchingStates() {
			Stream<BlockState> stream = this.block.getStateDefinition().getPossibleStates().stream();
			return switch (this.properties.size()) {
				case 0 -> stream;
				case 1 -> {
					Map.Entry<Property<?>, Comparable<?>> entry = this.properties.entrySet().iterator().next();
					yield stream.filter((BlockState state) -> state.getValue(entry.getKey()).equals(entry.getValue()));
				}
				case 2 -> {
					Iterator<Map.Entry<Property<?>, Comparable<?>>> iterator = this.properties.entrySet().iterator();
					Map.Entry<Property<?>, Comparable<?>> entry1 = iterator.next(), entry2 = iterator.next();
					yield stream.filter((BlockState state) -> state.getValue(entry1.getKey()).equals(entry1.getValue()) && state.getValue(entry2.getKey()).equals(entry2.getValue()));
				}
				default -> {
					yield stream.filter((BlockState state) -> {
						for (Map.Entry<Property<?>, Comparable<?>> entry : this.properties.entrySet()) {
							if (!state.getValue(entry.getKey()).equals(entry.getValue())) return false;
						}
						return true;
					});
				}
			};
		}
	}

	public static Result<TagProperties> decodeTag(BetterRegistry<Block> blockRegistry, String input) {
		if (input.charAt(0) != '#') return new Result<>((StringBuilder builder) -> builder.append("Not a tag: ").append(input));
		int openBracket = input.indexOf('[');
		Identifier tagID = IdentifierVersions.create(openBracket >= 0 ? input.substring(1, openBracket) : input.substring(1));
		HolderSet<Block> tag = blockRegistry.requireTag(TagKey.create(Registries.BLOCK, tagID));
		if (openBracket >= 0) {
			List<Consumer<StringBuilder>> errors = null;
			int closeBracket = input.indexOf(']', openBracket + 1);
			if (closeBracket != input.length() - 1) {
				errors = new ArrayList<>();
				if (closeBracket >= 0) {
					errors.add((StringBuilder builder) -> builder.append("Closing ']' is not at the end of the input string: ").append(input).append(", all input after it will be ignored"));
				}
				else {
					errors.add((StringBuilder builder) -> builder.append("Closing ']' not found in input string: ").append(input).append(", attempting to parse everything until the end of the string"));
					closeBracket = input.length();
				}
			}
			String[] split = input.substring(openBracket + 1, closeBracket).split(",");
			Map<String, String> properties = new Object2ObjectOpenHashMap<>(split.length);
			for (String pair : split) {
				int equals = pair.indexOf('=');
				if (equals < 0) {
					if (errors == null) errors = new ArrayList<>(split.length);
					errors.add((StringBuilder builder) -> builder.append("Expected '=' somewhere in ").append(pair));
					continue;
				}
				properties.put(pair.substring(0, equals), pair.substring(equals + 1));
			}
			return new Result<>(new TagProperties(tagID, tag, properties), concat(errors));
		}
		else {
			return new Result<>(new TagProperties(tagID, tag, Collections.emptyMap()));
		}
	}

	public static Consumer<StringBuilder> concat(List<Consumer<StringBuilder>> errors) {
		return errors == null ? null : (StringBuilder builder) -> {
			for (Consumer<StringBuilder> error : errors) {
				error.accept(builder);
				builder.append("; ");
			}
			builder.setLength(builder.length() - 2);
		};
	}

	public static record TagProperties(
		Identifier id,
		HolderSet<Block> tag,
		Map<String, String> properties
	)
		implements BlockStateSource {

		@Override
		public Stream<BlockState> getMatchingStates() {
			return this.tag.stream().map(
					(Holder<Block> entry) -> entry.value().getStateDefinition()
				)
					.flatMap(
						//create specialized lambdas to handle small sizes.
						switch (this.properties.size()) {
							case 0 -> {
								yield (StateDefinition<Block, BlockState> manager) -> manager.getPossibleStates().stream();
							}
							case 1 -> {
								Map.Entry<String, String> entry = this.properties.entrySet().iterator().next();
								yield (StateDefinition<Block, BlockState> manager) -> {
									Property<?> property = manager.getProperty(entry.getKey());
									if (property == null) return Stream.empty();
									Comparable<?> value = property.getValue(entry.getValue()).orElse(null);
									if (value == null) return Stream.empty();
									return manager.getPossibleStates().stream().filter((BlockState state) -> state.getValue(property).equals(value));
								};
							}
							case 2 -> {
								Iterator<Map.Entry<String, String>> iterator = this.properties.entrySet().iterator();
								Map.Entry<String, String> entry1 = iterator.next(), entry2 = iterator.next();
								yield (StateDefinition<Block, BlockState> manager) -> {
									Property<?> property1 = manager.getProperty(entry1.getKey());
									if (property1 == null) return Stream.empty();
									Comparable<?> value1 = property1.getValue(entry1.getValue()).orElse(null);
									if (value1 == null) return Stream.empty();
									Property<?> property2 = manager.getProperty(entry2.getKey());
									if (property2 == null) return Stream.empty();
									Comparable<?> value2 = property2.getValue(entry2.getValue()).orElse(null);
									if (value2 == null) return Stream.empty();
									return manager.getPossibleStates().stream().filter((BlockState state) -> state.getValue(property1).equals(value1) && state.getValue(property2).equals(value2));
								};
							}
							default -> {
								yield (StateDefinition<Block, BlockState> manager) -> {
									Object2ObjectOpenHashMap<Property<?>, Comparable<?>> properties = new Object2ObjectOpenHashMap<>(this.properties.size());
									for (Map.Entry<String, String> entry : this.properties.entrySet()) {
										Property<?> property = manager.getProperty(entry.getKey());
										if (property == null) return Stream.empty();
										Comparable<?> value = property.getValue(entry.getValue()).orElse(null);
										if (value == null) return Stream.empty();
										properties.put(property, value);
									}
									return manager.getPossibleStates().stream().filter((BlockState state) -> {
										for (ObjectIterator<Object2ObjectMap.Entry<Property<?>, Comparable<?>>> iterator = properties.object2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
											Map.Entry<Property<?>, Comparable<?>> entry = iterator.next();
											if (!state.getValue(entry.getKey()).equals(entry.getValue())) return false;
										}
										return true;
									});
								};
							}
						}
					);
		}
	}

	public static Result<BlockOrTagProperties> decodeBlockOrTag(BetterRegistry<Block> blockRegistry, String input) {
		return (
			input.charAt(0) == '#'
				? decodeTag(blockRegistry, input).map(BlockOrTagProperties::tag)
				: decodeState(blockRegistry, input).map(BlockOrTagProperties::block)
		);
	}

	public static record Result<T>(@Nullable T value, @Nullable Consumer<StringBuilder> errors) {

		public Result(T value) {
			this(value, null);
		}

		public Result(Consumer<StringBuilder> errors) {
			this(null, errors);
		}

		public String collectErrorsEager() {
			if (this.errors == null) return null;
			StringBuilder builder = new StringBuilder();
			this.errors.accept(builder);
			return builder.toString();
		}

		public @Nullable Supplier<String> collectErrorsLazy() {
			return this.errors == null ? null : this::collectErrorsEager;
		}

		public <X extends Throwable> T unwrapEager(
			Consumer<String> partialSuccess,
			Function<String, X> completeFailure
		)
			throws X {
			if (this.value != null) {
				if (this.errors != null) {
					partialSuccess.accept(this.collectErrorsEager());
				}
				return this.value;
			}
			else {
				throw completeFailure.apply(this.collectErrorsEager());
			}
		}

		public <X extends Throwable> T unwrapLazy(
			Consumer<Supplier<String>> partialSuccess,
			Function<Supplier<String>, X> completeFailure
		)
			throws X {
			if (this.value != null) {
				if (this.errors != null) {
					partialSuccess.accept(this.collectErrorsLazy());
				}
				return this.value;
			}
			else {
				throw completeFailure.apply(this.collectErrorsLazy());
			}
		}

		public @Nullable T unwrapNullableEager(Consumer<String> messageConsumer) {
			if (this.errors != null) messageConsumer.accept(this.collectErrorsEager());
			return this.value;
		}

		public @Nullable T unwrapNullableLazy(Consumer<Supplier<String>> messageConsumer) {
			if (this.errors != null) messageConsumer.accept(this.collectErrorsLazy());
			return this.value;
		}

		@SuppressWarnings("unchecked")
		public <T2> Result<T2> map(Function<T, T2> mapper) {
			return this.value == null ? (Result<T2>)(this) : new Result<>(mapper.apply(this.value), this.errors);
		}
	}

	public static record BlockOrTagProperties(
		@Nullable BlockProperties blockResult,
		@Nullable TagProperties tagResult
	)
		implements BlockStateSource {

		public static BlockOrTagProperties block(BlockProperties blockResult) {
			return new BlockOrTagProperties(blockResult, null);
		}

		public static BlockOrTagProperties tag(TagProperties tagResult) {
			return new BlockOrTagProperties(null, tagResult);
		}

		@Override
		public Stream<BlockState> getMatchingStates() {
			return (this.blockResult != null ? this.blockResult : this.tagResult).getMatchingStates();
		}
	}

	@Override
	public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, BlockState> context) throws EncodeException {
		BlockState state = context.object;
		if (state == null) return EmptyData.INSTANCE;
		return new StringData(encodeState(state));
	}

	public static String encodeState(BlockState state) {
		StringBuilder builder = new StringBuilder(64);
		Optional<ResourceKey<Block>> key = state.typeHolder().unwrapKey();
		if (key.isPresent()) builder.append(key.get().identifier());
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
		builder.append(property.getName()).append('=').append(property.getName(state.getValue(property)));
	}
}