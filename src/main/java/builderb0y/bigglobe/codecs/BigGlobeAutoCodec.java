package builderb0y.bigglobe.codecs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionTypes;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootPoolEntryTypes;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.AutoCodec;
import builderb0y.autocodec.coders.*;
import builderb0y.autocodec.coders.AutoCoder.CoderFactory;
import builderb0y.autocodec.common.LookupFactory;
import builderb0y.autocodec.decoders.AutoDecoder.DecoderFactory;
import builderb0y.autocodec.decoders.*;
import builderb0y.autocodec.encoders.AutoEncoder.EncoderFactory;
import builderb0y.autocodec.encoders.*;
import builderb0y.autocodec.imprinters.CollectionImprinter;
import builderb0y.autocodec.imprinters.ImprinterFactoryList;
import builderb0y.autocodec.imprinters.MapImprinter;
import builderb0y.autocodec.logging.*;
import builderb0y.autocodec.reflection.PseudoField;
import builderb0y.autocodec.reflection.ReflectionManager;
import builderb0y.autocodec.reflection.memberViews.FieldLikeMemberView;
import builderb0y.autocodec.reflection.memberViews.PseudoFieldView;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.verifiers.FloatRangeVerifier;
import builderb0y.autocodec.verifiers.VerifierFactoryList;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.registries.*;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.WorldTrait;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.features.dispatch.FeatureDispatcher;
import builderb0y.bigglobe.noise.Grid;
import builderb0y.bigglobe.noise.Grid.GridRegistryEntryCoder;
import builderb0y.bigglobe.overriders.Overrider;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.CombinedStructureScripts;
import builderb0y.bigglobe.util.TagOrObject;
import builderb0y.bigglobe.util.TagOrObjectKey;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.parsing.input.ScriptTemplate;

public class BigGlobeAutoCodec {

	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Codecs");
	public static final Printer PRINTER = createPrinter(LOGGER);

	public static final AutoCoder<Identifier> IDENTIFIER_CODER = PrimitiveCoders.stringBased(
		"BigGlobeAutoCodec.IDENTIFIER_CODEC",
		IdentifierVersions::create,
		Identifier::toString
	);

	public static AutoCoder<Identifier> createNamespacedIdentifierCodec(String namespace) {
		return PrimitiveCoders.stringBased(
			"Identifier with default namespace '" + namespace + '\'',
			string -> toID(string, namespace),
			id -> toString(id, namespace)
		);
	}

	public static Identifier toID(String string, String defaultNamespace) {
		String namespace, path;
		int colon = string.indexOf(':');
		if (colon >= 0) {
			namespace = string.substring(0, colon);
			path = string.substring(colon + 1);
		}
		else {
			namespace = defaultNamespace;
			path = string;
		}
		return IdentifierVersions.create(namespace, path);
	}

	public static String toString(Identifier identifier, String defaultNamespace) {
		return identifier.getNamespace().equals(defaultNamespace) ? identifier.getPath() : identifier.toString();
	}

	public static final RegistryCoders<Block>                                         BLOCK_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(Block                                 .class), RegistryVersions.block());
	public static final RegistryCoders<Item>                                           ITEM_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(Item                                  .class), RegistryVersions.item());
	public static final RegistryCoders<Fluid>                                         FLUID_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(Fluid                                 .class), RegistryVersions.fluid());
	public static final RegistryCoders<Potion>                                       POTION_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(Potion                                .class), RegistryVersions.potion());
	public static final RegistryCoders<BlockEntityType<?>>                BLOCK_ENTITY_TYPE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(BlockEntityType  .class), RegistryVersions.blockEntityType());
	public static final RegistryCoders<EntityType<?>>                           ENTITY_TYPE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(EntityType       .class), RegistryVersions.entityType());
	#if MC_VERSION >= MC_1_20_3
	public static final RegistryCoders<StatusEffect>                          STATUS_EFFECT_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(StatusEffect                          .class), RegistryVersions.statusEffect());
	#endif
	public static final RegistryCoders<DimensionType>                        DIMENSION_TYPE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(DimensionType                         .class), RegistryKeyVersions.dimensionType());
	public static final RegistryCoders<ConfiguredCarver<?>>               CONFIGURED_CARVER_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredCarver .class), RegistryKeyVersions.configuredCarver());
	public static final RegistryCoders<ConfiguredFeature<?, ?>>          CONFIGURED_FEATURE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredFeature.class), RegistryKeyVersions.configuredFeature());
	public static final RegistryCoders<PlacedFeature>                        PLACED_FEATURE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(PlacedFeature                         .class), RegistryKeyVersions.placedFeature());
	public static final RegistryCoders<Structure>                                 STRUCTURE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(Structure                             .class), RegistryKeyVersions.structure());
	public static final RegistryCoders<StructureSet>                          STRUCTURE_SET_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(StructureSet                          .class), RegistryKeyVersions.structureSet());
	public static final RegistryCoders<StructureProcessorList>     STRUCTURE_PROCESSOR_LIST_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(StructureProcessorList                .class), RegistryKeyVersions.processorList());
	public static final RegistryCoders<StructurePool>                        STRUCTURE_POOL_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(StructurePool                         .class), RegistryKeyVersions.templatePool());
	public static final RegistryCoders<Biome>                                         BIOME_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(Biome                                 .class), RegistryKeyVersions.biome());
	public static final RegistryCoders<DensityFunction>                    DENSITY_FUNCTION_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(DensityFunction                       .class), RegistryKeyVersions.densityFunction());
	public static final RegistryCoders<ChunkGeneratorSettings>     CHUNK_GENERATOR_SETTINGS_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(ChunkGeneratorSettings                .class), RegistryKeyVersions.chunkGeneratorSettings());
	public static final RegistryCoders<WorldPreset>                            WORLD_PRESET_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(WorldPreset                           .class), RegistryKeyVersions.worldPreset());
	public static final RegistryCoders<WoodPalette>                            WOOD_PALETTE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(WoodPalette                           .class), BigGlobeDynamicRegistries.              WOOD_PALETTE_REGISTRY_KEY);
	public static final RegistryCoders<ScriptTemplate>                      SCRIPT_TEMPLATE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(ScriptTemplate                        .class), BigGlobeDynamicRegistries.           SCRIPT_TEMPLATE_REGISTRY_KEY);
	public static final RegistryCoders<Grid>                                  GRID_TEMPLATE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(Grid                                  .class), BigGlobeDynamicRegistries.             GRID_TEMPLATE_REGISTRY_KEY);
	public static final RegistryCoders<ColumnEntry>                            COLUMN_ENTRY_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(ColumnEntry                           .class), BigGlobeDynamicRegistries.              COLUMN_ENTRY_REGISTRY_KEY);
	public static final RegistryCoders<VoronoiSettings>                    VORONOI_SETTINGS_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(VoronoiSettings                       .class), BigGlobeDynamicRegistries.          VORONOI_SETTINGS_REGISTRY_KEY);
	public static final RegistryCoders<DecisionTreeSettings>         DECISION_TREE_SETTINGS_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(DecisionTreeSettings                  .class), BigGlobeDynamicRegistries.    DECISION_TREE_SETTINGS_REGISTRY_KEY);
	public static final RegistryCoders<Overrider>                                 OVERRIDER_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(Overrider                             .class), BigGlobeDynamicRegistries.                 OVERRIDER_REGISTRY_KEY);
	public static final RegistryCoders<CombinedStructureScripts> SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(CombinedStructureScripts              .class), BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY);
	public static final RegistryCoders<FeatureDispatcher>                FEATURE_DISPATCHER_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(FeatureDispatcher                     .class), BigGlobeDynamicRegistries.        FEATURE_DISPATCHER_REGISTRY_KEY);
	public static final RegistryCoders<WorldTrait>                              WORLD_TRAIT_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(WorldTrait                            .class), BigGlobeDynamicRegistries.               WORLD_TRAIT_REGISTRY_KEY);
	public static final RegistryCoders<?>[]                                         DYNAMIC_REGISTRY_CODERS = {
		BLOCK_REGISTRY_CODERS,
		ITEM_REGISTRY_CODERS,
		FLUID_REGISTRY_CODERS,
		POTION_REGISTRY_CODERS,
		BLOCK_ENTITY_TYPE_REGISTRY_CODERS,
		ENTITY_TYPE_REGISTRY_CODERS,
		#if MC_VERSION >= MC_1_20_3
		STATUS_EFFECT_REGISTRY_CODERS,
		#endif
		DIMENSION_TYPE_REGISTRY_CODERS,
		CONFIGURED_CARVER_REGISTRY_CODERS,
		CONFIGURED_FEATURE_REGISTRY_CODERS,
		PLACED_FEATURE_REGISTRY_CODERS,
		STRUCTURE_REGISTRY_CODERS,
		STRUCTURE_SET_REGISTRY_CODERS,
		STRUCTURE_PROCESSOR_LIST_REGISTRY_CODERS,
		STRUCTURE_POOL_REGISTRY_CODERS,
		BIOME_REGISTRY_CODERS,
		DENSITY_FUNCTION_REGISTRY_CODERS,
		CHUNK_GENERATOR_SETTINGS_REGISTRY_CODERS,
		WORLD_PRESET_REGISTRY_CODERS,
		WOOD_PALETTE_REGISTRY_CODERS,
		SCRIPT_TEMPLATE_REGISTRY_CODERS,
		GRID_TEMPLATE_REGISTRY_CODERS,
		COLUMN_ENTRY_REGISTRY_CODERS,
		VORONOI_SETTINGS_REGISTRY_CODERS,
		DECISION_TREE_SETTINGS_REGISTRY_CODERS,
		OVERRIDER_REGISTRY_CODERS,
		SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_CODERS,
		FEATURE_DISPATCHER_REGISTRY_CODERS,
		WORLD_TRAIT_REGISTRY_CODERS,
	};

	public static final AutoCodec AUTO_CODEC = new AutoCodec() {

		@Override
		public @NotNull TaskLogger createFactoryLogger(@NotNull ReentrantLock lock) {
			return LoggingMode.get("factory").createLogger(lock);
		}

		@Override
		public @NotNull TaskLogger createEncodeLogger(@NotNull ReentrantLock lock) {
			return LoggingMode.get("encoding").createLogger(lock);
		}

		@Override
		public @NotNull TaskLogger createDecodeLogger(@NotNull ReentrantLock lock) {
			return LoggingMode.get("decoding").createLogger(lock);
		}

		@Override
		@OverrideOnly
		public @NotNull CoderFactoryList createCoders() {
			return new CoderFactoryList(this) {

				@Override
				public void setup() {
					super.setup();
					this.addFactoryToStart(UseSuperClass.Coder.Factory.INSTANCE);
					this.addFactoryBefore(LookupCoderFactory.class, GridRegistryEntryCoder.Factory.INSTANCE);
					this.getFactory(EnumCoder.Factory.class).nameGetter = StringIdentifiableEnumName.INSTANCE;
				}

				@Override
				public @NotNull CoderFactory createLookupFactory() {
					return new LookupCoderFactory() {

						@Override
						public void setup() {
							super.setup();
							this.addRaw(NbtElement.class, new AutoCoder<>() {

								@Override
								@OverrideOnly
								public <T_Encoded> @Nullable NbtElement decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
									if (context.isEmpty()) return null;
									return context.ops.convertTo(NbtOps.INSTANCE, context.input);
								}

								@Override
								@OverrideOnly
								public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, NbtElement> context) throws EncodeException {
									if (context.object == null) return context.empty();
									return NbtOps.INSTANCE.convertTo(context.ops, context.object);
								}
							});
							for (RegistryCoders<?> coders : DYNAMIC_REGISTRY_CODERS) {
								coders.addAllTo(this);
							}
							this.addRaw(DecodeContext.class, DecoderContextCoder.INSTANCE);
							this.addRaw(Identifier.class, IDENTIFIER_CODER);
							this.addRaw(BlockState.class, BlockStateCoder.INSTANCE);
							this.addRaw(BetterRegistry.Lookup.class, BetterRegistryLookupCoder.INSTANCE);
							this.addRaw(BiomeSource.class, autoCodec.wrapDFUCodec(BiomeSource.CODEC));
							this.addRaw(Structure.Config.class, autoCodec.wrapDFUCodec(Structure.Config.CODEC.codec()));
							#if MC_VERSION >= MC_1_20_2
								this.addRaw(LootPoolEntry.class, autoCodec.wrapDFUCodec(LootPoolEntryTypes.CODEC));
								this.addRaw(LootFunction.class, autoCodec.wrapDFUCodec(LootFunctionTypes.CODEC));
								this.addRaw(LootCondition.class, autoCodec.wrapDFUCodec(#if MC_VERSION >= MC_1_21_0 LootCondition.CODEC #else LootConditionTypes.CODEC #endif));
							#endif
							#if MC_VERSION >= MC_1_20_3
								this.addRaw(AbstractBlock.Settings.class, autoCodec.wrapDFUCodec(AbstractBlock.Settings.CODEC));
								this.addRaw(SaplingGenerator.class, autoCodec.wrapDFUCodec(SaplingGenerator.CODEC));
								this.addRaw(BlockSetType.class, autoCodec.wrapDFUCodec(BlockSetType.CODEC));
								this.addRaw(WoodType.class, autoCodec.wrapDFUCodec(WoodType.CODEC));
							#endif
						}
					};
				}
			};
		}

		@Override
		public @NotNull ImprinterFactoryList createImprinters() {
			return new ImprinterFactoryList(this) {

				@Override
				public void setup() {
					super.setup();
					this.addFactoryBefore(CollectionImprinter.Factory.INSTANCE, BlockStateCollectionImprinter.Factory.INSTANCE);
					this.addFactoryBefore(MapImprinter.Factory.INSTANCE, BlockStateToObjectMapImprinter.Factory.INSTANCE);
				}
			};
		}

		@Override
		public @NotNull VerifierFactoryList createVerifiers() {
			return new VerifierFactoryList(this) {

				@Override
				public void setup() {
					super.setup();
					this.addFactoryAfter(FloatRangeVerifier.Factory.INSTANCE, RandomRangeVerifier.Factory.INSTANCE);
				}
			};
		}

		@Override
		public @NotNull ReflectionManager createReflectionManager() {
			return new ReflectionManager() {

				#if MC_VERSION >= MC_1_20_3
					public static final Method BLOCK_SETTINGS_GETTER;

					static {
						try {
							BLOCK_SETTINGS_GETTER = AbstractBlock.class.getDeclaredMethod(
								FabricLoader
								.getInstance()
								.getMappingResolver()
								.mapMethodName(
									"intermediary",
									"net.minecraft.class_4970",
									"method_54095",
									"()Lnet/minecraft/class_4970$class_2251;"
								)
							);
						}
						catch (NoSuchMethodException exception) {
							throw AutoCodecUtil.rethrow(exception);
						}
					}

					@Override
					public @NotNull <T_Owner> ClassCache<T_Owner> createClassCache(@NotNull Class<T_Owner> owner) {
						ClassCache<T_Owner> cache = super.createClassCache(owner);
						if (owner == BLOCK_SETTINGS_GETTER.getDeclaringClass()) {
							cache.methods = new Method[] { BLOCK_SETTINGS_GETTER };
						}
						return cache;
					}

					@Override
					public @NotNull <T_Owner> TypeCache<T_Owner> createTypeCache(@NotNull ReifiedType<T_Owner> owner) {
						TypeCache<T_Owner> cache = super.createTypeCache(owner);
						if (owner.getRawClass() == BLOCK_SETTINGS_GETTER.getDeclaringClass()) {
							cache.fields = new FieldLikeMemberView[] {
								new PseudoFieldView<>(owner, new PseudoField(owner.getRawClass(), "settings", BLOCK_SETTINGS_GETTER, null))
							};
						}
						return cache;
					}
				#endif

				@Override
				public boolean canView(@NotNull Class<?> clazz) {
					return super.canView(clazz) && (clazz.getName().startsWith("builderb0y.") || clazz.getName().startsWith("java.util."));
				}

				@Override
				public boolean canView(@NotNull Field field) {
					return super.canView(field) && field.getDeclaringClass().getName().startsWith("builderb0y.");
				}
			};
		}
	};

	public static <T> AutoCoder<@Nullable T> forceNullable(AutoCoder<@NotNull T> coder) {
		return new AutoCoder<>() {

			@Override
			public <T_Encoded> @Nullable T decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
				return context.isEmpty() ? null : context.decodeWith(coder);
			}

			@Override
			public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, T> context) throws EncodeException {
				return context.object == null ? context.empty() : context.encodeWith(coder);
			}
		};
	}

	public static class RegistryCoders<T> {

		public final @NotNull RegistryKey<Registry<T>> registryKey;
		public final @Nullable Registry<T> registry;

		public final @NotNull ReifiedType<T> objectType;
		public final @NotNull ReifiedType<Registry<T>> registryType;
		public final @NotNull ReifiedType<RegistryKey<T>> registryKeyType;
		public final @NotNull ReifiedType<RegistryEntry<T>> registryEntryType;
		public final @NotNull ReifiedType<TagKey<T>> tagKeyType;
		public final @NotNull ReifiedType<RegistryEntryList<T>> tagType;
		public final @NotNull ReifiedType<TagOrObject<T>> tagOrObjectType;
		public final @NotNull ReifiedType<TagOrObjectKey<T>> tagOrObjectKeyType;
		public final @NotNull ReifiedType<BetterRegistry<T>> betterRegistryType;

		public final @Nullable BetterDynamicRegistryCoder<T> betterDynamicRegistryCoder;
		public final @Nullable BetterHardCodedRegistryCoder<T> betterHardCodedRegistryCoder;
		public final @Nullable DynamicRegistryEntryCoder<T> dynamicRegistryEntryCoder;
		public final @Nullable DynamicTagCoder<T> dynamicTagCoder;
		public final @Nullable HardCodedObjectCoder<T> hardCodedObjectCoder;
		public final @Nullable HardCodedRegistryEntryCoder<T> hardCodedRegistryEntryCoder;
		public final @Nullable HardCodedTagCoder<T> hardCodedTagCoder;
		public final @NotNull RegistryKeyCoder<T> registryKeyCoder;
		public final @NotNull TagKeyCoder<T> tagKeyCoder;
		public final @NotNull TagOrObjectCoder<T> tagOrObjectCoder;
		public final @NotNull TagOrObjectKeyCoder<T> tagOrObjectKeyCoder;

		public RegistryCoders(@NotNull ReifiedType<T> objectType, @NotNull RegistryKey<Registry<T>> registryKey) {
			this.                    registryKey = registryKey;
			this.                       registry = null;

			this.                     objectType = objectType;
			this.                   registryType = ReifiedType.parameterize(            Registry.class, objectType);
			this.                registryKeyType = ReifiedType.parameterize(         RegistryKey.class, objectType);
			this.              registryEntryType = ReifiedType.parameterize(       RegistryEntry.class, objectType);
			this.                     tagKeyType = ReifiedType.parameterize(              TagKey.class, objectType);
			this.                        tagType = ReifiedType.parameterize(   RegistryEntryList.class, objectType);
			this.                tagOrObjectType = ReifiedType.parameterize(         TagOrObject.class, objectType);
			this.             tagOrObjectKeyType = ReifiedType.parameterize(      TagOrObjectKey.class, objectType);
			this.             betterRegistryType = ReifiedType.parameterize(      BetterRegistry.class, objectType);

			this.     betterDynamicRegistryCoder = new BetterDynamicRegistryCoder<>(registryKey);
			this.   betterHardCodedRegistryCoder = null;
			this.      dynamicRegistryEntryCoder = new DynamicRegistryEntryCoder<>(this.betterDynamicRegistryCoder);
			this.                dynamicTagCoder = new DynamicTagCoder<>(this.betterDynamicRegistryCoder);
			this.           hardCodedObjectCoder = null;
			this.    hardCodedRegistryEntryCoder = null;
			this.              hardCodedTagCoder = null;
			this.               registryKeyCoder = new RegistryKeyCoder<>(registryKey);
			this.                    tagKeyCoder = new TagKeyCoder<>(registryKey);
			this.               tagOrObjectCoder = new TagOrObjectCoder<>(registryKey, this.dynamicTagCoder, this.dynamicRegistryEntryCoder);
			this.            tagOrObjectKeyCoder = new TagOrObjectKeyCoder<>(registryKey);
		}

		public RegistryCoders(@NotNull ReifiedType<T> objectType, @NotNull Registry<T> registry) {
			this.                    registryKey = RegistryVersions.getRegistryKey(registry);
			this.                       registry = registry;

			this.                     objectType = objectType;
			this.                   registryType = ReifiedType.parameterize(            Registry.class, objectType);
			this.                registryKeyType = ReifiedType.parameterize(         RegistryKey.class, objectType);
			this.              registryEntryType = ReifiedType.parameterize(       RegistryEntry.class, objectType);
			this.                     tagKeyType = ReifiedType.parameterize(              TagKey.class, objectType);
			this.                        tagType = ReifiedType.parameterize(   RegistryEntryList.class, objectType);
			this.                tagOrObjectType = ReifiedType.parameterize(         TagOrObject.class, objectType);
			this.             tagOrObjectKeyType = ReifiedType.parameterize(      TagOrObjectKey.class, objectType);
			this.             betterRegistryType = ReifiedType.parameterize(      BetterRegistry.class, objectType);

			this.     betterDynamicRegistryCoder = null;
			this.   betterHardCodedRegistryCoder = new BetterHardCodedRegistryCoder<>(registry);
			this.      dynamicRegistryEntryCoder = null;
			this.                dynamicTagCoder = null;
			this.           hardCodedObjectCoder = new HardCodedObjectCoder<>(registry);
			this.    hardCodedRegistryEntryCoder = new HardCodedRegistryEntryCoder<>(registry);
			this.              hardCodedTagCoder = new HardCodedTagCoder<>(registry);
			this.               registryKeyCoder = new RegistryKeyCoder<>(this.registryKey);
			this.                    tagKeyCoder = new TagKeyCoder<>(this.registryKey);
			this.               tagOrObjectCoder = new TagOrObjectCoder<>(this.registryKey, this.hardCodedTagCoder, this.hardCodedRegistryEntryCoder);
			this.            tagOrObjectKeyCoder = new TagOrObjectKeyCoder<>(this.registryKey);
		}

		public void addAllTo(LookupCoderFactory factory) {
			addTo(factory, this.      registryEntryType, this.      dynamicRegistryEntryCoder);
			addTo(factory, this.                tagType, this.                dynamicTagCoder);
			addTo(factory, this.             objectType, this.           hardCodedObjectCoder);
			addTo(factory, this.      registryEntryType, this.    hardCodedRegistryEntryCoder);
			addTo(factory, this.                tagType, this.              hardCodedTagCoder);
			addTo(factory, this.        registryKeyType, this.               registryKeyCoder);
			addTo(factory, this.             tagKeyType, this.                    tagKeyCoder);
			addTo(factory, this.        tagOrObjectType, this.               tagOrObjectCoder);
			addTo(factory, this.     tagOrObjectKeyType, this.            tagOrObjectKeyCoder);
			addTo(factory, this.     betterRegistryType, this.     betterDynamicRegistryCoder);
			addTo(factory, this.     betterRegistryType, this.   betterHardCodedRegistryCoder);
		}

		public static <T> void addTo(LookupCoderFactory factory, ReifiedType<T> type, AutoCoder<T> coder) {
			if (coder != null) factory.doAddGeneric(type, coder);
		}
	}

	public static Printer createPrinter(Logger logger) {
		return new Printer() {
			@Override public void print     (@NotNull String message) { logger.info (message); }
			@Override public void printError(@NotNull String error  ) { logger.error(error  ); }
		};
	}

	public static enum LoggingMode {

		DISABLED {

			@Override
			public TaskLogger createLogger(ReentrantLock lock) {
				return new DisabledTaskLogger();
			}
		},

		DEFAULT {

			@Override
			public TaskLogger createLogger(ReentrantLock lock) {
				return new StackContextLogger(lock, PRINTER, true);
			}
		},

		VERBOSE {

			@Override
			public TaskLogger createLogger(ReentrantLock lock) {
				return new IndentedTaskLogger(lock, PRINTER, false);
			}
		};

		public static LoggingMode get(String key) {
			String fullKey = BigGlobeMod.MODID + ".codecs.logging." + key;
			String value = System.getProperty(fullKey);
			if (value != null) {
				return switch (value.toLowerCase(Locale.ROOT)) {
					case "disabled" -> DISABLED;
					case "default"  -> DEFAULT;
					case "verbose"  -> VERBOSE;
					default -> {
						LOGGER.warn("Unrecognized logging mode: -D" + fullKey + '=' + value + ". Valid logging modes are disabled, default, and verbose.");
						yield DEFAULT;
					}
				};
			}
			else {
				return DEFAULT;
			}
		}

		public abstract TaskLogger createLogger(ReentrantLock lock);
	}
}