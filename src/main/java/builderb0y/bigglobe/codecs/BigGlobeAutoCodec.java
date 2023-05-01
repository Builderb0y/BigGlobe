package builderb0y.bigglobe.codecs;

import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.AutoCodec;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.PrimitiveCoders;
import builderb0y.autocodec.common.AutoHandler.HandlerMapper;
import builderb0y.autocodec.common.LookupFactory;
import builderb0y.autocodec.decoders.*;
import builderb0y.autocodec.decoders.AutoDecoder.DecoderFactory;
import builderb0y.autocodec.encoders.*;
import builderb0y.autocodec.encoders.AutoEncoder.EncoderFactory;
import builderb0y.autocodec.imprinters.CollectionImprinter;
import builderb0y.autocodec.imprinters.ImprinterFactoryList;
import builderb0y.autocodec.logging.*;
import builderb0y.autocodec.reflection.ReflectionManager;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.registries.*;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.OverworldBiomeLayout;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.structures.scripted.StructurePlacementScript;
import builderb0y.bigglobe.util.TagOrObject;
import builderb0y.bigglobe.util.TagOrObjectKey;
import builderb0y.scripting.parsing.ScriptTemplate;

public class BigGlobeAutoCodec {

	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Codecs");
	public static final Printer PRINTER = createPrinter(LOGGER);

	public static final AutoCoder<Identifier> IDENTIFIER_CODER = PrimitiveCoders.STRING.mapCoder(
		ReifiedType.from(Identifier.class),
		"Identifier::toString", HandlerMapper.nullSafe(Identifier::toString),
		"Identifier::new",      HandlerMapper.nullSafe(Identifier::new)
	);

	public static final RegistryCoders<Block>                           BLOCK_REGISTRY_CODERS                      = new RegistryCoders<>(ReifiedType.from(Block                 .class), Registries.BLOCK);
	public static final RegistryCoders<Item>                            ITEM_REGISTRY_CODERS                       = new RegistryCoders<>(ReifiedType.from(Item                  .class), Registries.ITEM);
	public static final RegistryCoders<Fluid>                           FLUID_REGISTRY_CODERS                      = new RegistryCoders<>(ReifiedType.from(Fluid                 .class), Registries.FLUID);
	public static final RegistryCoders<Potion>                          POTION_REGISTRY_CODERS                     = new RegistryCoders<>(ReifiedType.from(Potion                .class), Registries.POTION);
	public static final RegistryCoders<BlockEntityType<?>>              BLOCK_ENTITY_TYPE_REGISTRY_CODERS          = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(BlockEntityType  .class), Registries.BLOCK_ENTITY_TYPE);
	public static final RegistryCoders<EntityType<?>>                   ENTITY_TYPE_REGISTRY_CODERS                = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(EntityType       .class), Registries.ENTITY_TYPE);
	public static final RegistryCoders<DimensionType>                   DIMENSION_TYPE_REGISTRY_CODERS             = new RegistryCoders<>(ReifiedType.from(DimensionType         .class), RegistryKeys.DIMENSION_TYPE);
	public static final RegistryCoders<ConfiguredCarver<?>>             CONFIGURED_CARVER_REGISTRY_CODERS          = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredCarver .class), RegistryKeys.CONFIGURED_CARVER);
	public static final RegistryCoders<ConfiguredFeature<?, ?>>         CONFIGURED_FEATURE_REGISTRY_CODERS         = new RegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredFeature.class), RegistryKeys.CONFIGURED_FEATURE);
	public static final RegistryCoders<PlacedFeature>                   PLACED_FEATURE_REGISTRY_CODERS             = new RegistryCoders<>(ReifiedType.from(PlacedFeature                  .class), RegistryKeys.PLACED_FEATURE);
	public static final RegistryCoders<Structure>                       STRUCTURE_REGISTRY_CODERS                  = new RegistryCoders<>(ReifiedType.from(Structure                      .class), RegistryKeys.STRUCTURE);
	public static final RegistryCoders<StructureSet>                    STRUCTURE_SET_REGISTRY_CODERS              = new RegistryCoders<>(ReifiedType.from(StructureSet                   .class), RegistryKeys.STRUCTURE_SET);
	public static final RegistryCoders<StructureProcessorList>          STRUCTURE_PROCESSOR_LIST_REGISTRY_CODERS   = new RegistryCoders<>(ReifiedType.from(StructureProcessorList         .class), RegistryKeys.PROCESSOR_LIST);
	public static final RegistryCoders<StructurePool>                   STRUCTURE_POOL_REGISTRY_CODERS             = new RegistryCoders<>(ReifiedType.from(StructurePool                  .class), RegistryKeys.TEMPLATE_POOL);
	public static final RegistryCoders<Biome>                           BIOME_REGISTRY_CODERS                      = new RegistryCoders<>(ReifiedType.from(Biome                          .class), RegistryKeys.BIOME);
	public static final RegistryCoders<DensityFunction>                 DENSITY_FUNCTION_REGISTRY_CODERS           = new RegistryCoders<>(ReifiedType.from(DensityFunction                .class), RegistryKeys.DENSITY_FUNCTION);
	public static final RegistryCoders<ChunkGeneratorSettings>          CHUNK_GENERATOR_SETTINGS_REGISTRY_CODERS   = new RegistryCoders<>(ReifiedType.from(ChunkGeneratorSettings         .class), RegistryKeys.CHUNK_GENERATOR_SETTINGS);
	public static final RegistryCoders<WorldPreset>                     WORLD_PRESET_REGISTRY_CODERS               = new RegistryCoders<>(ReifiedType.from(WorldPreset                    .class), RegistryKeys.WORLD_PRESET);
	public static final RegistryCoders<WoodPalette>                     WOOD_PALETTE_REGISTRY_CODERS               = new RegistryCoders<>(ReifiedType.from(WoodPalette                    .class), BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY);
	public static final RegistryCoders<ScriptTemplate>                  SCRIPT_TEMPLATE_REGISTRY_CODERS            = new RegistryCoders<>(ReifiedType.from(ScriptTemplate                 .class), BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY);
	public static final RegistryCoders<StructurePlacementScript.Holder> SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_CODERS = new RegistryCoders<>(ReifiedType.from(StructurePlacementScript.Holder.class), BigGlobeDynamicRegistries.SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY);
	public static final RegistryCoders<LocalNetherSettings>             LOCAL_NETHER_SETTINGS_REGISTRY_CODERS      = new RegistryCoders<>(ReifiedType.from(LocalNetherSettings            .class), BigGlobeDynamicRegistries.LOCAL_NETHER_SETTINGS_REGISTRY_KEY);
	public static final RegistryCoders<OverworldBiomeLayout>            OVERWORLD_BIOME_LAYOUT_REGISTRY_CODERS     = new RegistryCoders<>(ReifiedType.from(OverworldBiomeLayout           .class), BigGlobeDynamicRegistries.OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY);
	public static final RegistryCoders<?>[]                             DYNAMIC_REGISTRY_CODERS = {
		BLOCK_REGISTRY_CODERS,
		ITEM_REGISTRY_CODERS,
		FLUID_REGISTRY_CODERS,
		POTION_REGISTRY_CODERS,
		BLOCK_ENTITY_TYPE_REGISTRY_CODERS,
		ENTITY_TYPE_REGISTRY_CODERS,
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
		SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_CODERS,
		LOCAL_NETHER_SETTINGS_REGISTRY_CODERS,
		OVERWORLD_BIOME_LAYOUT_REGISTRY_CODERS
	};

	@SuppressWarnings("OverrideOnly") //it should allow super calls. that should be a thing.
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
		public @NotNull EncoderFactoryList createEncoders() {
			return new EncoderFactoryList(this) {

				@Override
				public void setup() {
					super.setup();
					this.getFactory(EnumEncoder.Factory.class).nameGetter = StringIdentifiableEnumName.INSTANCE;
				}

				@Override
				public @NotNull EncoderFactory createLookupFactory() {
					AutoCodec autoCodec = this.autoCodec;
					return new LookupEncoderFactory() {

						@Override
						public void setup() {
							super.setup();
							this.addRaw(Identifier.class, IDENTIFIER_CODER);
							this.addRaw(BlockState.class, BlockStateCoder.INSTANCE);
							for (RegistryCoders<?> coders : DYNAMIC_REGISTRY_CODERS) {
								coders.addAllTo(this);
							}
							this.addRaw(Structure.Config.class, autoCodec.wrapDFUEncoder(Structure.Config.CODEC.codec(), false));
						}
					};
				}
			};
		}

		@Override
		public @NotNull DecoderFactoryList createDecoders() {
			return new DecoderFactoryList(this) {

				@Override
				public void setup() {
					super.setup();
					this.getFactory(EnumDecoder.Factory.class).nameGetter = StringIdentifiableEnumName.INSTANCE;
				}

				@Override
				public @NotNull DecoderFactory createLookupFactory() {
					AutoCodec autoCodec = this.autoCodec;
					return new LookupDecoderFactory() {

						@Override
						public void setup() {
							super.setup();
							this.addRaw(Identifier.class, IDENTIFIER_CODER);
							this.addRaw(BlockState.class, BlockStateCoder.INSTANCE);
							for (RegistryCoders<?> coders : DYNAMIC_REGISTRY_CODERS) {
								coders.addAllTo(this);
							}
							this.addRaw(Structure.Config.class, autoCodec.wrapDFUDecoder(Structure.Config.CODEC.codec(), false));
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
		public @NotNull ReflectionManager createReflectionManager() {
			return new ReflectionManager() {

				@Override
				public boolean canView(@NotNull Class<?> clazz) {
					return clazz.getName().startsWith("builderb0y.");
				}
			};
		}
	};

	public static class RegistryCoders<T> {

		public final @NotNull RegistryKey<Registry<T>> registryKey;
		public final @Nullable Registry<T> registry;

		public final @NotNull ReifiedType<T> objectType;
		public final @NotNull ReifiedType<Registry<T>> registryType;
		public final @NotNull ReifiedType<RegistryEntryLookup<T>> registryEntryLookupType;
		public final @NotNull ReifiedType<RegistryWrapper<T>> registryWrapperType;
		public final @NotNull ReifiedType<RegistryKey<T>> registryKeyType;
		public final @NotNull ReifiedType<RegistryEntry<T>> registryEntryType;
		public final @NotNull ReifiedType<TagKey<T>> tagKeyType;
		public final @NotNull ReifiedType<RegistryEntryList<T>> tagType;
		public final @NotNull ReifiedType<TagOrObject<T>> tagOrObjectType;
		public final @NotNull ReifiedType<TagOrObjectKey<T>> tagOrObjectKeyType;

		public final @Nullable DynamicRegistryCoder<T> dynamicRegistryCoder;
		public final @Nullable DynamicRegistryEntryCoder<T> dynamicRegistryEntryCoder;
		public final @Nullable DynamicRegistryWrapperCoder<T> dynamicRegistryWrapperCoder;
		public final @Nullable DynamicTagCoder<T> dynamicTagCoder;
		public final @Nullable HardCodedObjectCoder<T> hardCodedObjectCoder;
		public final @Nullable HardCodedRegistryEntryCoder<T> hardCodedRegistryEntryCoder;
		public final @Nullable HardCodedTagCoder<T> hardCodedTagCoder;
		public final @NotNull RegistryKeyCoder<T> registryKeyCoder;
		public final @NotNull TagKeyCoder<T> tagKeyCoder;
		public final @NotNull TagOrObjectCoder<T> tagOrObjectCoder;
		public final @NotNull TagOrObjectKeyCoder<T> tagOrObjectKeyCoder;

		public RegistryCoders(@NotNull ReifiedType<T> objectType, @NotNull RegistryKey<Registry<T>> registryKey) {
			this.registryKey                 = registryKey;
			this.registry                    = null;

			this.                 objectType = objectType;
			this.               registryType = ReifiedType.parameterize(           Registry.class, objectType);
			this.    registryEntryLookupType = ReifiedType.parameterize(RegistryEntryLookup.class, objectType);
			this.        registryWrapperType = ReifiedType.parameterize(    RegistryWrapper.class, objectType);
			this.            registryKeyType = ReifiedType.parameterize(        RegistryKey.class, objectType);
			this.          registryEntryType = ReifiedType.parameterize(      RegistryEntry.class, objectType);
			this.                 tagKeyType = ReifiedType.parameterize(             TagKey.class, objectType);
			this.                    tagType = ReifiedType.parameterize(  RegistryEntryList.class, objectType);
			this.            tagOrObjectType = ReifiedType.parameterize(        TagOrObject.class, objectType);
			this.         tagOrObjectKeyType = ReifiedType.parameterize(     TagOrObjectKey.class, objectType);

			this.       dynamicRegistryCoder = new DynamicRegistryCoder<>(registryKey);
			this.  dynamicRegistryEntryCoder = new DynamicRegistryEntryCoder<>(this.dynamicRegistryCoder);
			this.dynamicRegistryWrapperCoder = new DynamicRegistryWrapperCoder<>(registryKey);
			this.            dynamicTagCoder = new DynamicTagCoder<>(this.dynamicRegistryCoder);
			this.       hardCodedObjectCoder = null;
			this.hardCodedRegistryEntryCoder = null;
			this.          hardCodedTagCoder = null;
			this.           registryKeyCoder = new RegistryKeyCoder<>(registryKey);
			this.                tagKeyCoder = new TagKeyCoder<>(registryKey);
			this.           tagOrObjectCoder = new TagOrObjectCoder<>(registryKey, this.dynamicTagCoder, this.dynamicRegistryEntryCoder);
			this.        tagOrObjectKeyCoder = new TagOrObjectKeyCoder<>(registryKey);
		}

		@SuppressWarnings("unchecked")
		public RegistryCoders(@NotNull ReifiedType<T> objectType, @NotNull Registry<T> registry) {
			this.registryKey                 = (RegistryKey<Registry<T>>)(registry.getKey());
			this.registry                    = registry;

			this.                 objectType = objectType;
			this.               registryType = ReifiedType.parameterize(           Registry.class, objectType);
			this.    registryEntryLookupType = ReifiedType.parameterize(RegistryEntryLookup.class, objectType);
			this.        registryWrapperType = ReifiedType.parameterize(    RegistryWrapper.class, objectType);
			this.            registryKeyType = ReifiedType.parameterize(        RegistryKey.class, objectType);
			this.          registryEntryType = ReifiedType.parameterize(      RegistryEntry.class, objectType);
			this.                 tagKeyType = ReifiedType.parameterize(             TagKey.class, objectType);
			this.                    tagType = ReifiedType.parameterize(  RegistryEntryList.class, objectType);
			this.            tagOrObjectType = ReifiedType.parameterize(        TagOrObject.class, objectType);
			this.         tagOrObjectKeyType = ReifiedType.parameterize(     TagOrObjectKey.class, objectType);

			this.       dynamicRegistryCoder = null;
			this.  dynamicRegistryEntryCoder = null;
			this.dynamicRegistryWrapperCoder = null;
			this.            dynamicTagCoder = null;
			this.       hardCodedObjectCoder = new HardCodedObjectCoder<>(registry);
			this.hardCodedRegistryEntryCoder = new HardCodedRegistryEntryCoder<>(registry);
			this.          hardCodedTagCoder = new HardCodedTagCoder<>(registry);
			this.           registryKeyCoder = new RegistryKeyCoder<>(this.registryKey);
			this.                tagKeyCoder = new TagKeyCoder<>(this.registryKey);
			this.           tagOrObjectCoder = new TagOrObjectCoder<>(this.registryKey, this.hardCodedTagCoder, this.hardCodedRegistryEntryCoder);
			this.        tagOrObjectKeyCoder = new TagOrObjectKeyCoder<>(this.registryKey);
		}

		public void addAllTo(LookupFactory<? super AutoCoder<?>> factory) {
			addTo(factory, this.registryEntryLookupType, this.       dynamicRegistryCoder);
			addTo(factory, this.      registryEntryType, this.  dynamicRegistryEntryCoder);
			addTo(factory, this.    registryWrapperType, this.dynamicRegistryWrapperCoder);
			addTo(factory, this.                tagType, this.            dynamicTagCoder);
			addTo(factory, this.             objectType, this.       hardCodedObjectCoder);
			addTo(factory, this.      registryEntryType, this.hardCodedRegistryEntryCoder);
			addTo(factory, this.                tagType, this.          hardCodedTagCoder);
			addTo(factory, this.        registryKeyType, this.           registryKeyCoder);
			addTo(factory, this.             tagKeyType, this.                tagKeyCoder);
			addTo(factory, this.        tagOrObjectType, this.           tagOrObjectCoder);
			addTo(factory, this.     tagOrObjectKeyType, this.        tagOrObjectKeyCoder);
		}

		public static <T> void addTo(LookupFactory<? super AutoCoder<?>> factory, ReifiedType<T> type, AutoCoder<T> coder) {
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