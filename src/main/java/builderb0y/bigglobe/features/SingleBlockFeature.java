package builderb0y.bigglobe.features;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.ConstructImprintDecoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.decoders.RecordDecoder;
import builderb0y.autocodec.encoders.CollectionEncoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.encoders.MultiFieldEncoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.SingleBlockFeature.Config;
import builderb0y.bigglobe.mixins.PlantBlock_CanPlantOnTopAccess;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class SingleBlockFeature extends Feature<Config> implements RawFeature<Config> {

	public static final Predicate<BlockState>
		IS_REPLACEABLE  = BlockStateVersions::isReplaceable,
		NOT_REPLACEABLE = state -> !BlockStateVersions.isReplaceable(state),
		IS_AIR          = BlockState::isAir,
		IS_SOURCE_WATER = state -> state == BlockStates.WATER,
		HAS_WATER       = state -> state.getFluidState().getFluid() == Fluids.WATER;

	public SingleBlockFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public SingleBlockFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static Predicate<BlockState> matchWaterlogged(BlockState state) {
		return state.getFluidState().getFluid() == Fluids.WATER ? IS_SOURCE_WATER : IS_AIR;
	}

	public static BlockState[] getStates(BlockState state) {
		if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
			return new BlockState[] {
				state.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER),
				state.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
			};
		}
		else {
			return new BlockState[] { state };
		}
	}

	public static boolean checkFluids(BlockView world, BlockPos origin, BlockState[] states, Predicate<BlockState> replace) {
		for (int offsetY = 0, length = states.length; offsetY < length; offsetY++) {
			BlockState oldState = world.getBlockState(origin.up(offsetY));
			if (!replace.test(oldState)) return false;
			BlockState newState = states[offsetY];
			FluidState fluidState = oldState.getFluidState();
			if (fluidState.isEmpty()) {
				if (newState.contains(Properties.WATERLOGGED)) {
					newState = newState.with(Properties.WATERLOGGED, Boolean.FALSE);
				}
				else if (!states[offsetY].getFluidState().isEmpty()) {
					return false;
				}
			}
			else if (fluidState.getFluid() == Fluids.WATER) {
				if (newState.contains(Properties.WATERLOGGED)) {
					newState = newState.with(Properties.WATERLOGGED, Boolean.TRUE);
				}
				else if (states[offsetY].getFluidState().getFluid() != Fluids.WATER) {
					return false;
				}
			}
			else {
				return true;
			}
			states[offsetY] = newState;
		}
		return true;
	}

	public static boolean place(
		WorldAccess world,
		BlockPos pos,
		BlockState place,
		Predicate<BlockState> replace
	) {
		if (place.getBlock() instanceof PlantBlock_CanPlantOnTopAccess plantBlock) {
			//skip light level check that some plants normally perform.
			BlockPos downPos = pos.down();
			BlockState downState = world.getBlockState(downPos);
			if (!plantBlock.bigglobe_canPlantOnTop(downState, world, downPos)) return false;
		}
		else {
			if (!place.canPlaceAt(world, pos)) return false;
		}
		BlockState[] states = getStates(place);
		if (!checkFluids(world, pos, states, replace)) return false;
		for (int offsetY = 0, length = states.length; offsetY < length; offsetY++) {
			world.setBlockState(pos.up(offsetY), states[offsetY], Block.NOTIFY_ALL);
		}
		return true;
	}

	public static boolean placeEarly(
		Chunk chunk,
		BlockPos pos,
		BlockState place,
		Predicate<BlockState> replace
	) {
		BlockState[] states = getStates(place);
		if (!checkFluids(chunk, pos, states, replace)) return false;
		for (int offsetY = 0, length = states.length; offsetY < length; offsetY++) {
			chunk.setBlockState(pos.up(offsetY), states[offsetY], false);
		}
		return true;
	}

	public static boolean place(WorldAccess world, BlockPos pos, RandomGenerator random, Config config) {
		return place(
			world,
			pos,
			config.place.get(random.nextInt(config.place.size())),
			config
		);
	}

	public static boolean place(WorldAccess world, BlockPos pos, long seed, Config config) {
		return place(
			world,
			pos,
			config.place.get(Permuter.nextBoundedInt(seed, config.place.size())),
			config
		);
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		Config config = context.getConfig();
		return place(
			context.getWorld(),
			context.getOrigin(),
			config.getState(context.getRandom()),
			config
		);
	}

	@Override
	public boolean generate(WorldWrapper world, Config config, BlockPos pos) {
		return placeEarly(((ChunkDelegator)(world.world)).chunk, pos, config.getState(world.random), config);
	}

	@UseCoder(name = "new", in = ConfigCoder.class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
	public static record Config(
		@VerifyNotEmpty List<BlockState> place,
		@VerifyNotEmpty @VerifyNullable Set<BlockState> replace
	)
	implements FeatureConfig, Predicate<BlockState> {

		public BlockState getState(RandomGenerator random) {
			return Permuter.choose(random, this.place);
		}

		public BlockState getState(Random random) {
			return this.place.get(random.nextInt(this.place.size()));
		}

		public BlockState getState(long seed) {
			return Permuter.choose(seed, this.place);
		}

		@Override
		public boolean test(BlockState state) {
			return (
				this.replace != null
				? this.replace.contains(state)
				: BlockStateVersions.isReplaceable(state)
			);
		}
	}

	public static class ConfigCoder extends NamedCoder<Config> {

		public static final ReifiedType<List<BlockState>> BLOCKSTATE_LIST_REIFIED_TYPE = new ReifiedType<@SingletonArray List<BlockState>>() {};

		public final AutoCoder<Config> withReplace;
		public final AutoCoder<List<BlockState>> placeOnly;

		public ConfigCoder(FactoryContext<Config> context) {
			super("ConfigCoder");
			this.withReplace = AutoCoder.of(
				context.forceCreateEncoder(MultiFieldEncoder.Factory.INSTANCE),
				context.forceCreateDecoder(RecordDecoder.Factory.INSTANCE)
			);
			FactoryContext<List<BlockState>> replaceContext = context.type(BLOCKSTATE_LIST_REIFIED_TYPE);
			this.placeOnly = AutoCoder.of(
				replaceContext.forceCreateEncoder(CollectionEncoder.Factory.INSTANCE),
				replaceContext.forceCreateDecoder(ConstructImprintDecoder.Factory.INSTANCE)
			);
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, Config> context) throws EncodeException {
			if (context.input == null) return context.empty();
			if (context.input.replace == null) {
				return context.input(context.input.place).encodeWith(this.placeOnly);
			}
			else {
				return context.encodeWith(this.withReplace);
			}
		}

		@Override
		public <T_Encoded> @Nullable Config decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			if (context.isString()) {
				return new Config(context.decodeWith(this.placeOnly), null);
			}
			else {
				return context.decodeWith(this.withReplace);
			}
		}
	}
}