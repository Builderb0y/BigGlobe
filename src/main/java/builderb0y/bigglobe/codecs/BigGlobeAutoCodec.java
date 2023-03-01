package builderb0y.bigglobe.codecs;

import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import it.unimi.dsi.fastutil.Hash;
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
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
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
import builderb0y.autocodec.util.HashStrategies.NamedHashStrategy;
import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeAutoCodec {

	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Codecs");
	public static final Printer PRINTER = createPrinter(LOGGER);

	public static final Hash.Strategy<RegistryEntry<?>> REGISTRY_ENTRY_STRATEGY = new NamedHashStrategy<>("BigGlobeAutoCodec.REGISTRY_ENTRY_STRATEGY") {

		@Override
		public int hashCode(RegistryEntry<?> o) {
			if (o == null) return 0;
			return o.getKey().hashCode();
		}

		@Override
		public boolean equals(RegistryEntry<?> a, RegistryEntry<?> b) {
			if (a == b) return true;
			if (a == null || b == null) return false;
			return a.getKey().equals(b.getKey());
		}
	};

	public static final AutoCoder<Identifier> IDENTIFIER_CODER = PrimitiveCoders.STRING.mapCoder(
		ReifiedType.from(Identifier.class),
		"Identifier::toString", HandlerMapper.nullSafe(Identifier::toString),
		"Identifier::new",      HandlerMapper.nullSafe(Identifier::new)
	);
	public static final DynamicRegistryCoders<DimensionType>           DIMENSION_TYPE_REGISTRY_CODERS           = new DynamicRegistryCoders<>(ReifiedType.from(DimensionType         .class), Registry.DIMENSION_TYPE_KEY);
	public static final DynamicRegistryCoders<ConfiguredCarver<?>>     CONFIGURED_CARVER_REGISTRY_CODERS        = new DynamicRegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredCarver .class), Registry.CONFIGURED_CARVER_KEY);
	public static final DynamicRegistryCoders<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE_REGISTRY_CODERS       = new DynamicRegistryCoders<>(ReifiedType.parameterizeWithWildcards(ConfiguredFeature.class), Registry.CONFIGURED_FEATURE_KEY);
	public static final DynamicRegistryCoders<PlacedFeature>           PLACED_FEATURE_REGISTRY_CODERS           = new DynamicRegistryCoders<>(ReifiedType.from(PlacedFeature         .class), Registry.PLACED_FEATURE_KEY);
	public static final DynamicRegistryCoders<Structure>               STRUCTURE_REGISTRY_CODERS                = new DynamicRegistryCoders<>(ReifiedType.from(Structure             .class), Registry.STRUCTURE_KEY);
	public static final DynamicRegistryCoders<StructureSet>            STRUCTURE_SET_REGISTRY_CODERS            = new DynamicRegistryCoders<>(ReifiedType.from(StructureSet          .class), Registry.STRUCTURE_SET_KEY);
	public static final DynamicRegistryCoders<StructureProcessorList>  STRUCTURE_PROCESSOR_LIST_REGISTRY_CODERS = new DynamicRegistryCoders<>(ReifiedType.from(StructureProcessorList.class), Registry.STRUCTURE_PROCESSOR_LIST_KEY);
	public static final DynamicRegistryCoders<StructurePool>           STRUCTURE_POOL_REGISTRY_CODERS           = new DynamicRegistryCoders<>(ReifiedType.from(StructurePool         .class), Registry.STRUCTURE_POOL_KEY);
	public static final DynamicRegistryCoders<Biome>                   BIOME_REGISTRY_CODERS                    = new DynamicRegistryCoders<>(ReifiedType.from(Biome                 .class), Registry.BIOME_KEY);
	public static final DynamicRegistryCoders<DensityFunction>         DENSITY_FUNCTION_REGISTRY_CODERS         = new DynamicRegistryCoders<>(ReifiedType.from(DensityFunction       .class), Registry.DENSITY_FUNCTION_KEY);
	public static final DynamicRegistryCoders<ChunkGeneratorSettings>  CHUNK_GENERATOR_SETTINGS_REGISTRY_CODERS = new DynamicRegistryCoders<>(ReifiedType.from(ChunkGeneratorSettings.class), Registry.CHUNK_GENERATOR_SETTINGS_KEY);
	public static final DynamicRegistryCoders<WorldPreset>             WORLD_PRESET_REGISTRY_CODERS             = new DynamicRegistryCoders<>(ReifiedType.from(WorldPreset           .class), Registry.WORLD_PRESET_KEY);
	public static final DynamicRegistryCoders<?>[] DYNAMIC_REGISTRY_CODERS = {
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

	public static final HardCodedRegistryCoders<Block> BLOCK_REGISTRY_CODERS = new HardCodedRegistryCoders<>(ReifiedType.from(Block.class), Registry.BLOCK, Registry.BLOCK_KEY);
	public static final HardCodedRegistryCoders<Item> ITEM_REGISTRY_CODERS = new HardCodedRegistryCoders<>(ReifiedType.from(Item.class), Registry.ITEM, Registry.ITEM_KEY);
	public static final HardCodedRegistryCoders<Fluid> FLUID_REGISTRY_CODERS = new HardCodedRegistryCoders<>(ReifiedType.from(Fluid.class), Registry.FLUID, Registry.FLUID_KEY);
	public static final HardCodedRegistryCoders<Potion> POTION_REGISTRY_CODERS = new HardCodedRegistryCoders<>(ReifiedType.from(Potion.class), Registry.POTION, Registry.POTION_KEY);
	public static final HardCodedRegistryCoders<BlockEntityType<?>> BLOCK_ENTITY_TYPE_REGISTRY_CODERS = new HardCodedRegistryCoders<>(ReifiedType.parameterizeWithWildcards(BlockEntityType.class), Registry.BLOCK_ENTITY_TYPE, Registry.BLOCK_ENTITY_TYPE_KEY);
	public static final HardCodedRegistryCoders<EntityType<?>> ENTITY_TYPE_REGISTRY_CODERS = new HardCodedRegistryCoders<>(ReifiedType.parameterizeWithWildcards(EntityType.class), Registry.ENTITY_TYPE, Registry.ENTITY_TYPE_KEY);
	public static final HardCodedRegistryCoders<?>[] HARD_CODED_REGISTRY_CODERS = {
		BLOCK_REGISTRY_CODERS,
		ITEM_REGISTRY_CODERS,
		FLUID_REGISTRY_CODERS,
		POTION_REGISTRY_CODERS,
		BLOCK_ENTITY_TYPE_REGISTRY_CODERS,
		ENTITY_TYPE_REGISTRY_CODERS
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
							for (HardCodedRegistryCoders<?> coders : HARD_CODED_REGISTRY_CODERS) {
								coders.addAllTo(this, autoCodec);
							}
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
							for (HardCodedRegistryCoders<?> coders : HARD_CODED_REGISTRY_CODERS) {
								coders.addAllTo(this, autoCodec);
							}
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

	public static class HardCodedRegistryCoders<T> {

		public final Registry<T> registry;
		public final RegistryKey<Registry<T>> registryKey;

		public final ReifiedType<T> objectType;
		public final ReifiedType<RegistryKey<T>> registryKeyType;
		public final ReifiedType<TagKey<T>> tagKeyType;

		public HardCodedRegistryCoders(ReifiedType<T> objectType, Registry<T> registry, RegistryKey<Registry<T>> registryKey) {
			this.registry = registry;
			this.registryKey = registryKey;
			this.objectType = objectType;
			this.registryKeyType = ReifiedType.parameterize(RegistryKey.class, objectType);
			this.tagKeyType = ReifiedType.parameterize(TagKey.class, objectType);
		}

		public void addAllTo(LookupFactory<? super AutoCoder<?>> lookupFactory, AutoCodec autoCodec) {
			lookupFactory.doAddGeneric(this.objectType, autoCodec.wrapDFUCodec(this.registry.getCodec(), false));
			RegistryKey<Registry<T>> registryKey = this.registryKey;
			lookupFactory.doAddGeneric(this.tagKeyType, tagKeyCoder(this.tagKeyType, this.registryKey));
			lookupFactory.doAddGeneric(this.registryKeyType, registryKeyCoder(this.registryKeyType, this.registryKey));
		}
	}

	public static class DynamicRegistryCoders<T> {

		public final RegistryKey<Registry<T>> registryKey;

		public final ReifiedType<T> objectType;
		public final ReifiedType<Registry<T>> registryType;
		public final ReifiedType<RegistryKey<T>> registryKeyType;
		public final ReifiedType<RegistryEntry<T>> entryType;
		public final ReifiedType<TagKey<T>> tagKeyType;

		public final DynamicRegistryCoder<T> registryCoder;
		public final AutoCoder<RegistryKey<T>> registryKeyCoder;
		public final DynamicRegistryEntryCoder<T> entryCoder;
		public final DynamicRegistryObjectCoder<T> objectCoder;
		public final AutoCoder<TagKey<T>> tagKeyCoder;

		public DynamicRegistryCoders(ReifiedType<T> objectType, RegistryKey<Registry<T>> registryKey) {
			this.registryKey      = registryKey;

			this.objectType       = objectType;
			this.registryType     = ReifiedType.parameterize(Registry.class, objectType);
			this.registryKeyType  = ReifiedType.parameterize(RegistryKey.class, objectType);
			this.entryType        = ReifiedType.parameterize(RegistryEntry.class, objectType);
			this.tagKeyType       = ReifiedType.parameterize(TagKey.class, objectType);

			this.registryCoder    = new DynamicRegistryCoder<>(registryKey);
			this.registryKeyCoder = registryKeyCoder(this.registryKeyType, this.registryKey);
			this.entryCoder       = new DynamicRegistryEntryCoder<>(this.entryType, this.registryCoder);
			this.objectCoder      = new DynamicRegistryObjectCoder<>(objectType, this.registryCoder);
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