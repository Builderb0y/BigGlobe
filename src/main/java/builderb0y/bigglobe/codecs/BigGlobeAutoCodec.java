package builderb0y.bigglobe.codecs;

import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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
import builderb0y.autocodec.decoders.AutoDecoder.DecoderFactory;
import builderb0y.autocodec.decoders.DecoderFactoryList;
import builderb0y.autocodec.decoders.EnumDecoder;
import builderb0y.autocodec.decoders.LookupDecoderFactory;
import builderb0y.autocodec.encoders.AutoEncoder.EncoderFactory;
import builderb0y.autocodec.encoders.EncoderFactoryList;
import builderb0y.autocodec.encoders.EnumEncoder;
import builderb0y.autocodec.encoders.LookupEncoderFactory;
import builderb0y.autocodec.imprinters.CollectionImprinter;
import builderb0y.autocodec.imprinters.ImprinterFactoryList;
import builderb0y.autocodec.logging.*;
import builderb0y.autocodec.reflection.ReflectionManager;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.registry.BetterRegistry;
import builderb0y.bigglobe.registry.BetterRegistryEntry;

public class BigGlobeAutoCodec {

	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Codecs");
	public static final Printer PRINTER = createPrinter(LOGGER);

	public static final AutoCoder<Identifier> IDENTIFIER_CODER = PrimitiveCoders.STRING.mapCoder(
		ReifiedType.from(Identifier.class),
		"Identifier::toString", HandlerMapper.nullSafe(Identifier::toString),
		"Identifier::new",      HandlerMapper.nullSafe(Identifier::new)
	);

	public static final DynamicRegistryCoders<Block>                   BLOCK_REGISTRY_CODERS                    = new DynamicRegistryCoders<>(ReifiedType.from( Block.class), RegistryKeys.BLOCK);
	public static final DynamicRegistryCoders<Item>                    ITEM_REGISTRY_CODERS                     = new DynamicRegistryCoders<>(ReifiedType.from(  Item.class), RegistryKeys.ITEM);
	public static final DynamicRegistryCoders<Fluid>                   FLUID_REGISTRY_CODERS                    = new DynamicRegistryCoders<>(ReifiedType.from( Fluid.class), RegistryKeys.FLUID);
	public static final DynamicRegistryCoders<Potion>                  POTION_REGISTRY_CODERS                   = new DynamicRegistryCoders<>(ReifiedType.from(Potion.class), RegistryKeys.POTION);
	public static final DynamicRegistryCoders<BlockEntityType<?>>      BLOCK_ENTITY_TYPE_REGISTRY_CODERS        = new DynamicRegistryCoders<>(ReifiedType.parameterizeWithWildcards(BlockEntityType.class), RegistryKeys.BLOCK_ENTITY_TYPE);
	public static final DynamicRegistryCoders<EntityType<?>>           ENTITY_TYPE_REGISTRY_CODERS              = new DynamicRegistryCoders<>(ReifiedType.parameterizeWithWildcards(EntityType.class), RegistryKeys.ENTITY_TYPE);
	public static final DynamicRegistryCoders<DimensionType>           DIMENSION_TYPE_REGISTRY_CODERS           = new DynamicRegistryCoders<>(ReifiedType.from(DimensionType         .class), RegistryKeys.DIMENSION_TYPE);
	public static final DynamicRegistryCoders<ConfiguredCarver<?>>     CONFIGURED_CARVER_REGISTRY_CODERS        = new DynamicRegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredCarver .class), RegistryKeys.CONFIGURED_CARVER);
	public static final DynamicRegistryCoders<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE_REGISTRY_CODERS       = new DynamicRegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredFeature.class), RegistryKeys.CONFIGURED_FEATURE);
	public static final DynamicRegistryCoders<PlacedFeature>           PLACED_FEATURE_REGISTRY_CODERS           = new DynamicRegistryCoders<>(ReifiedType.from(PlacedFeature         .class), RegistryKeys.PLACED_FEATURE);
	public static final DynamicRegistryCoders<Structure>               STRUCTURE_REGISTRY_CODERS                = new DynamicRegistryCoders<>(ReifiedType.from(Structure             .class), RegistryKeys.STRUCTURE);
	public static final DynamicRegistryCoders<StructureSet>            STRUCTURE_SET_REGISTRY_CODERS            = new DynamicRegistryCoders<>(ReifiedType.from(StructureSet          .class), RegistryKeys.STRUCTURE_SET);
	public static final DynamicRegistryCoders<StructureProcessorList>  STRUCTURE_PROCESSOR_LIST_REGISTRY_CODERS = new DynamicRegistryCoders<>(ReifiedType.from(StructureProcessorList.class), RegistryKeys.PROCESSOR_LIST);
	public static final DynamicRegistryCoders<StructurePool>           STRUCTURE_POOL_REGISTRY_CODERS           = new DynamicRegistryCoders<>(ReifiedType.from(StructurePool         .class), RegistryKeys.TEMPLATE_POOL);
	public static final DynamicRegistryCoders<Biome>                   BIOME_REGISTRY_CODERS                    = new DynamicRegistryCoders<>(ReifiedType.from(Biome                 .class), RegistryKeys.BIOME);
	public static final DynamicRegistryCoders<DensityFunction>         DENSITY_FUNCTION_REGISTRY_CODERS         = new DynamicRegistryCoders<>(ReifiedType.from(DensityFunction       .class), RegistryKeys.DENSITY_FUNCTION);
	public static final DynamicRegistryCoders<ChunkGeneratorSettings>  CHUNK_GENERATOR_SETTINGS_REGISTRY_CODERS = new DynamicRegistryCoders<>(ReifiedType.from(ChunkGeneratorSettings.class), RegistryKeys.CHUNK_GENERATOR_SETTINGS);
	public static final DynamicRegistryCoders<WorldPreset>             WORLD_PRESET_REGISTRY_CODERS             = new DynamicRegistryCoders<>(ReifiedType.from(WorldPreset           .class), RegistryKeys.WORLD_PRESET);
	public static final DynamicRegistryCoders<?>[] DYNAMIC_REGISTRY_CODERS = {
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
		WORLD_PRESET_REGISTRY_CODERS
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
							for (DynamicRegistryCoders<?> coders : DYNAMIC_REGISTRY_CODERS) {
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
							for (DynamicRegistryCoders<?> coders : DYNAMIC_REGISTRY_CODERS) {
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

	public static class DynamicRegistryCoders<T> {

		public final RegistryKey<Registry<T>> registryKey;

		public final ReifiedType<T> objectType;
		public final ReifiedType<BetterRegistry<T>> registryType;
		public final ReifiedType<RegistryKey<T>> registryKeyType;
		public final ReifiedType<BetterRegistryEntry<T>> entryType;
		public final ReifiedType<TagKey<T>> tagKeyType;

		public final BetterRegistryCoder<T> registryCoder;
		public final AutoCoder<RegistryKey<T>> registryKeyCoder;
		public final BetterRegistryEntryCoder<T> entryCoder;
		public final RegistryObjectCoder<T> objectCoder;
		public final AutoCoder<TagKey<T>> tagKeyCoder;

		public DynamicRegistryCoders(ReifiedType<T> objectType, RegistryKey<Registry<T>> registryKey) {
			this.registryKey      = registryKey;

			this.objectType       = objectType;
			this.registryType     = ReifiedType.parameterize(BetterRegistry.class, objectType);
			this.registryKeyType  = ReifiedType.parameterize(RegistryKey.class, objectType);
			this.entryType        = ReifiedType.parameterize(BetterRegistryEntry.class, objectType);
			this.tagKeyType       = ReifiedType.parameterize(TagKey.class, objectType);

			this.registryCoder    = new BetterRegistryCoder<>(registryKey);
			this.registryKeyCoder = registryKeyCoder(this.registryKeyType, this.registryKey);
			this.entryCoder       = new BetterRegistryEntryCoder<>(this.entryType, this.registryCoder, registryKey);
			this.objectCoder      = new RegistryObjectCoder<>(objectType, this.registryCoder);
			this.tagKeyCoder      = tagKeyCoder(this.tagKeyType, this.registryKey);
		}

		public void addAllTo(LookupFactory<? super AutoCoder<?>> factory) {
			factory.doAddGeneric(this.   registryType, this.   registryCoder);
			factory.doAddGeneric(this.registryKeyType, this.registryKeyCoder);
			factory.doAddGeneric(this.      entryType, this.      entryCoder);
			factory.doAddGeneric(this.     objectType, this.     objectCoder);
			factory.doAddGeneric(this.     tagKeyType, this.     tagKeyCoder);
		}
	}

	public static <T> AutoCoder<RegistryKey<T>> registryKeyCoder(ReifiedType<RegistryKey<T>> type, RegistryKey<Registry<T>> registryKey) {
		return IDENTIFIER_CODER.mapCoder(
			type,
			"RegistryKey::getValue", HandlerMapper.nullSafe(RegistryKey::getValue),
			"RegistryKey::of",       HandlerMapper.nullSafe(id -> RegistryKey.of(registryKey, id))
		);
	}

	public static <T> AutoCoder<TagKey<T>> tagKeyCoder(ReifiedType<TagKey<T>> type, RegistryKey<Registry<T>> registryKey) {
		return IDENTIFIER_CODER.mapCoder(
			type,
			"TagKey::id", HandlerMapper.nullSafe(TagKey::id),
			"TagKey::of", HandlerMapper.nullSafe(id -> TagKey.of(registryKey, id))
		);
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