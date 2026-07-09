package builderb0y.bigglobe.codecs;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import builderb0y.autocodec.AutoCodec;
import builderb0y.autocodec.coders.*;
import builderb0y.autocodec.coders.AutoCoder.CoderFactory;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.DataOps;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.imprinters.CollectionImprinter;
import builderb0y.autocodec.imprinters.ImprinterFactoryList;
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
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.codecs.registries.BetterRegistryCoder;
import builderb0y.bigglobe.codecs.registries.BetterRegistryLookupCoder;
import builderb0y.bigglobe.codecs.registries.DelayedEntryListCoder;
import builderb0y.bigglobe.codecs.registries.RegistryEntryCoder;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSpec;
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
import builderb0y.bigglobe.sounds.SoundModifier;
import builderb0y.bigglobe.spawning.SpawnTweaker;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure.CombinedStructureScripts;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.TextCoding;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.parsing.input.ScriptTemplate;

public class BigGlobeAutoCodec {

	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Codecs");
	public static final Printer PRINTER = createPrinter(LOGGER);

	public static final AutoCoder<Identifier> IDENTIFIER_CODER = PrimitiveCoders.stringBased(
		"BigGlobeAutoCodec.IDENTIFIER_CODER",
		IdentifierVersions::create,
		Identifier::toString
	);

	public static AutoCoder<Identifier> createNamespacedIdentifierCodec(String namespace) {
		return PrimitiveCoders.stringBased(
			"Identifier with default namespace '" + namespace + '\'',
			(String string) -> toID(string, namespace),
			(Identifier id) -> toString(id, namespace)
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

	public static final RegistryCoders<Block>                                         BLOCK_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Block                   .class),                Registries.BLOCK                              );
	public static final RegistryCoders<Item>                                           ITEM_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Item                    .class),                Registries.ITEM                               );
	public static final RegistryCoders<Fluid>                                         FLUID_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Fluid                   .class),                Registries.FLUID                              );
	public static final RegistryCoders<Potion>                                       POTION_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Potion                  .class),                Registries.POTION                             );
	public static final RegistryCoders<BlockEntityType<?>>                BLOCK_ENTITY_TYPE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(BlockEntityType         .class),                Registries.BLOCK_ENTITY_TYPE                  );
	public static final RegistryCoders<EntityType<?>>                           ENTITY_TYPE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(EntityType              .class),                Registries.ENTITY_TYPE                        );
	public static final RegistryCoders<MobEffect>                             STATUS_EFFECT_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (MobEffect               .class),                Registries.MOB_EFFECT                         );
	public static final RegistryCoders<ParticleType<?>>                       PARTICLE_TYPE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(ParticleType            .class),                Registries.PARTICLE_TYPE                      );
	public static final RegistryCoders<DimensionType>                        DIMENSION_TYPE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (DimensionType           .class),                Registries.DIMENSION_TYPE                     );
	public static final RegistryCoders<ConfiguredWorldCarver<?>>          CONFIGURED_CARVER_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredWorldCarver   .class),                Registries.CONFIGURED_CARVER                  );
	public static final RegistryCoders<ConfiguredFeature<?, ?>>          CONFIGURED_FEATURE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredFeature       .class),                Registries.CONFIGURED_FEATURE                 );
	public static final RegistryCoders<PlacedFeature>                        PLACED_FEATURE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (PlacedFeature           .class),                Registries.PLACED_FEATURE                     );
	public static final RegistryCoders<Structure>                                 STRUCTURE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Structure               .class),                Registries.STRUCTURE                          );
	public static final RegistryCoders<StructureType<?>>                     STRUCTURE_TYPE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(StructureType           .class),                Registries.STRUCTURE_TYPE                     );
	public static final RegistryCoders<StructureSet>                          STRUCTURE_SET_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (StructureSet            .class),                Registries.STRUCTURE_SET                      );
	public static final RegistryCoders<StructureProcessorList>     STRUCTURE_PROCESSOR_LIST_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (StructureProcessorList  .class),                Registries.PROCESSOR_LIST                     );
	public static final RegistryCoders<StructureTemplatePool>                STRUCTURE_POOL_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (StructureTemplatePool   .class),                Registries.TEMPLATE_POOL                      );
	public static final RegistryCoders<Biome>                                         BIOME_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Biome                   .class),                Registries.BIOME                              );
	public static final RegistryCoders<DensityFunction>                    DENSITY_FUNCTION_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (DensityFunction         .class),                Registries.DENSITY_FUNCTION                   );
	public static final RegistryCoders<NoiseGeneratorSettings>     CHUNK_GENERATOR_SETTINGS_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (NoiseGeneratorSettings  .class),                Registries.NOISE_SETTINGS                     );
	public static final RegistryCoders<WorldPreset>                            WORLD_PRESET_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (WorldPreset             .class),                Registries.WORLD_PRESET                       );
	public static final RegistryCoders<WoodPalette>                            WOOD_PALETTE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (WoodPalette             .class), BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY          );
	public static final RegistryCoders<ScriptTemplate>                      SCRIPT_TEMPLATE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (ScriptTemplate          .class), BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY       );
	public static final RegistryCoders<Grid>                                  GRID_TEMPLATE_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Grid                    .class), BigGlobeDynamicRegistries.NOISE_SOURCE_REGISTRY_KEY          );
	public static final RegistryCoders<ElementSpec>                            ELEMENT_SPEC_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (ElementSpec             .class), BigGlobeDynamicRegistries.ELEMENT_SPEC_REGISTRY_KEY          );
	public static final RegistryCoders<ColumnEntry>                            COLUMN_ENTRY_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (ColumnEntry             .class), BigGlobeDynamicRegistries.COLUMN_VALUE_REGISTRY_KEY          );
	public static final RegistryCoders<DecisionTreeSpec>             DECISION_TREE_SETTINGS_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (DecisionTreeSpec        .class), BigGlobeDynamicRegistries.DECISION_TREE_REGISTRY_KEY         );
	public static final RegistryCoders<Overrider>                                 OVERRIDER_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Overrider               .class), BigGlobeDynamicRegistries.OVERRIDER_REGISTRY_KEY             );
	public static final RegistryCoders<CombinedStructureScripts> SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (CombinedStructureScripts.class), BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PIECE_REGISTRY_KEY);
	public static final RegistryCoders<FeatureDispatcher>                FEATURE_DISPATCHER_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (FeatureDispatcher       .class), BigGlobeDynamicRegistries.FEATURE_DISPATCHER_REGISTRY_KEY    );
	public static final RegistryCoders<WorldTrait>                              WORLD_TRAIT_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (WorldTrait              .class), BigGlobeDynamicRegistries.WORLD_TRAIT_REGISTRY_KEY           );
	public static final RegistryCoders<Layer>                                         LAYER_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (Layer                   .class), BigGlobeDynamicRegistries.TERRAIN_LAYER_REGISTRY_KEY         );
	public static final RegistryCoders<SpawnTweaker>                            EXTRA_SPAWN_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (SpawnTweaker            .class), BigGlobeDynamicRegistries.MOB_SPAWN_TWEAKER_REGISTRY_KEY     );
	public static final RegistryCoders<SoundModifier>                        SOUND_MODIFIER_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from                     (SoundModifier           .class), BigGlobeDynamicRegistries.SOUND_MODIFIER_REGISTRY_KEY        );
	public static final RegistryCoders<?>[]                                         DYNAMIC_REGISTRY_CODERS = {
		BLOCK_REGISTRY_CODERS,
		ITEM_REGISTRY_CODERS,
		FLUID_REGISTRY_CODERS,
		POTION_REGISTRY_CODERS,
		BLOCK_ENTITY_TYPE_REGISTRY_CODERS,
		ENTITY_TYPE_REGISTRY_CODERS,
		STATUS_EFFECT_REGISTRY_CODERS,
		PARTICLE_TYPE_REGISTRY_CODERS,
		DIMENSION_TYPE_REGISTRY_CODERS,
		CONFIGURED_CARVER_REGISTRY_CODERS,
		CONFIGURED_FEATURE_REGISTRY_CODERS,
		PLACED_FEATURE_REGISTRY_CODERS,
		STRUCTURE_REGISTRY_CODERS,
		STRUCTURE_TYPE_REGISTRY_CODERS,
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
		ELEMENT_SPEC_REGISTRY_CODERS,
		COLUMN_ENTRY_REGISTRY_CODERS,
		DECISION_TREE_SETTINGS_REGISTRY_CODERS,
		OVERRIDER_REGISTRY_CODERS,
		SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_CODERS,
		FEATURE_DISPATCHER_REGISTRY_CODERS,
		WORLD_TRAIT_REGISTRY_CODERS,
		LAYER_REGISTRY_CODERS,
		EXTRA_SPAWN_REGISTRY_CODERS,
		SOUND_MODIFIER_REGISTRY_CODERS,
	};

	public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
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
					this.removeFactory(UseCoderFactory.INSTANCE);
					this.addFactoryAfter(DefaultEmptyCoder.Factory.INSTANCE, UseCoderFactory.INSTANCE);
					this.addFactoryToStart(UseSuperClass.Coder.Factory.INSTANCE);
					this.addFactoryBefore(LookupCoderFactory.class, GridRegistryEntryCoder.Factory.INSTANCE);
					this.addFactoryAfter(LookupCoderFactory.class, RegistryEntryCoder.Factory.INSTANCE);
					this.getFactory(EnumCoder.Factory.class).nameGetter = StringIdentifiableEnumName.INSTANCE;
				}

				@Override
				public @NotNull CoderFactory createLookupFactory() {
					return new LookupCoderFactory() {

						@Environment(EnvType.CLIENT)
						public void setupClient() {
							this.addRaw(BlockStateModel.Unbaked.class, autoCodec.wrapDFUCodec(BlockStateModel.Unbaked.CODEC));
						}

						@Override
						public void setup() {
							super.setup();
							if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
								this.setupClient();
							}
							this.addRaw(Tag.class, new NamedCoder<>("NbtElementCoder") {

								@Override
								@OverrideOnly
								public <T_Encoded> @Nullable Tag decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
									if (context.isEmpty()) return null;
									return DataOps.UNCOMPRESSED.convertTo(NbtOps.INSTANCE, context.data);
								}

								@Override
								@OverrideOnly
								public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, Tag> context) throws EncodeException {
									if (context.object == null) return EmptyData.INSTANCE;
									return NbtOps.INSTANCE.convertTo(DataOps.UNCOMPRESSED, context.object);
								}
							});
							this.addRaw(UUID.class, UUIDCoder.INSTANCE);
							for (RegistryCoders<?> coders : DYNAMIC_REGISTRY_CODERS) {
								coders.addAllTo(this);
							}
							this.addGeneric(
								ReifiedType.parameterize(ResourceKey.class, ReifiedType.from(Level.class)),
								PrimitiveCoders.stringBased(
									"AutoCoder<RegistryKey<World>>",
									(String string) -> ResourceKey.create(Registries.DIMENSION, IdentifierVersions.create(string)),
									(ResourceKey<Level> key) -> key.identifier().toString()
								)
							);
							this.addRaw(Data.class, DataCoder.INSTANCE);
							this.addRaw(DecodeContext.class, DecoderContextCoder.INSTANCE);
							this.addRaw(Identifier.class, IDENTIFIER_CODER);
							this.addRaw(BlockState.class, BlockStateCoder.INSTANCE);
							this.addRaw(BetterRegistry.Lookup.class, BetterRegistryLookupCoder.INSTANCE);
							this.addRaw(BiomeSource.class, autoCodec.wrapDFUCodec(BiomeSource.CODEC));
							this.addRaw(Structure.StructureSettings.class, autoCodec.wrapDFUCodec(Structure.StructureSettings.CODEC.codec()));

							this.addRaw(LootPoolEntryContainer.class, autoCodec.wrapDFUCodec(LootPoolEntries.CODEC));
							this.addRaw(LootItemFunction.class, autoCodec.wrapDFUCodec(LootItemFunctions.ROOT_CODEC));
							this.addRaw(LootItemCondition.class, autoCodec.wrapDFUCodec(LootItemCondition.DIRECT_CODEC));

							this.addRaw(BlockBehaviour.Properties.class, autoCodec.wrapDFUCodec(BlockBehaviour.Properties.CODEC));
							this.addRaw(TreeGrower.class, autoCodec.wrapDFUCodec(TreeGrower.CODEC));
							this.addRaw(BlockSetType.class, autoCodec.wrapDFUCodec(BlockSetType.CODEC));
							this.addRaw(WoodType.class, autoCodec.wrapDFUCodec(WoodType.CODEC));

							this.addRaw(BoundingBox.class, autoCodec.wrapDFUCodec(BoundingBox.CODEC));
							this.addRaw(Component.class, TextCoding.CODER);
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

				public static final Method BLOCK_SETTINGS_GETTER;

				static {
					try {
						BLOCK_SETTINGS_GETTER = BlockBehaviour.class.getDeclaredMethod("properties");
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

				@Override
				public boolean canView(@NotNull Class<?> clazz) {
					return super.canView(clazz) && (clazz.getName().startsWith("builderb0y.") || clazz.getName().startsWith("java.util."));
				}

				@Override
				public boolean canView(@NotNull Field field) {
					return super.canView(field) && field.getDeclaringClass().getName().startsWith("builderb0y.");
				}

				@Override
				public MethodHandles.@NotNull Lookup getLookup(@NotNull Class<?> in) {
					return LOOKUP;
				}
			};
		}
	};
	public static final AutoCodec SILENT_CODEC = new AutoCodec() {

		@Override
		@OverrideOnly
		public @NotNull TaskLogger createDefaultLogger(@NotNull ReentrantLock lock) {
			return new DisabledTaskLogger();
		}

		@Override
		@OverrideOnly
		public @NotNull CoderFactoryList createCoders() {
			return new CoderFactoryList(this) {

				@Override
				public void setup() {
					super.setup();
					this.getFactory(EnumCoder.Factory.class).nameGetter = StringIdentifiableEnumName.INSTANCE;
				}
			};
		}
	};

	public static class RegistryCoders<T> {

		public final @NotNull ResourceKey<Registry<T>> registryKey;

		public final @NotNull ReifiedType<T> objectType;
		public final @NotNull ReifiedType<Registry<T>> registryType;
		public final @NotNull ReifiedType<BetterRegistry<T>> betterRegistryType;
		public final @NotNull ReifiedType<Holder<T>> registryEntryType;
		public final @NotNull ReifiedType<DelayedEntryList<T>> delayedTagType;

		public final @NotNull BetterRegistryCoder<T> betterRegistryCoder;
		public final @NotNull DelayedEntryListCoder<T> delayedEntryListCoder;

		public RegistryCoders(@NotNull ReifiedType<T> objectType, @NotNull ResourceKey<Registry<T>> registryKey) {
			this.registryKey = registryKey;

			this.objectType = objectType;
			this.registryType = ReifiedType.parameterize(Registry.class, objectType);
			this.registryEntryType = ReifiedType.parameterize(Holder.class, objectType);
			this.delayedTagType = ReifiedType.parameterize(DelayedEntryList.class, objectType);
			this.betterRegistryType = ReifiedType.parameterize(BetterRegistry.class, objectType);

			this.betterRegistryCoder = new BetterRegistryCoder<>(this.betterRegistryType, registryKey);
			this.delayedEntryListCoder = new DelayedEntryListCoder<>(this.delayedTagType, registryKey);
		}

		public void addAllTo(LookupCoderFactory factory) {
			factory.addGeneric(this.betterRegistryType, this.betterRegistryCoder);
			factory.addGeneric(this.delayedTagType, this.delayedEntryListCoder);
			RegistryEntryCoder.Factory.INSTANCE.register(this.objectType, this.registryKey);
		}
	}

	public static Printer createPrinter(Logger logger) {
		return new Printer() {

			@Override
			public void print(@NotNull String message) {
				logger.info(message);
			}

			@Override
			public void printError(@NotNull String error) {
				logger.error(error);
			}
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
					case "default" -> DEFAULT;
					case "verbose" -> VERBOSE;
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