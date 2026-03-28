package builderb0y.bigglobe.features;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.coders.EncoderDecoderCoder;
import builderb0y.autocodec.coders.RecordCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.SingleBlockFeature.Config;
import builderb0y.bigglobe.mixins.PlantBlock_CanPlantOnTopAccess;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.ChunkVersions;

public class SingleBlockFeature extends Feature<Config> implements RawFeature<Config> {

	public static final Predicate<BlockState>
		IS_REPLACEABLE = BlockStateVersions::isReplaceable,
		NOT_REPLACEABLE = state -> !BlockStateVersions.isReplaceable(state),
		IS_AIR = BlockState::isAir,
		IS_SOURCE_WATER = state -> state == BlockStates.WATER,
		HAS_WATER = state -> state.getFluidState().getType() == Fluids.WATER;

	public SingleBlockFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public SingleBlockFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static Predicate<BlockState> matchWaterlogged(BlockState state) {
		return state.getFluidState().getType() == Fluids.WATER ? IS_SOURCE_WATER : IS_AIR;
	}

	public static BlockState[] getStates(BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return new BlockState[] {
				state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER),
				state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
			};
		}
		else {
			return new BlockState[] { state };
		}
	}

	public static int
		CAN_PLACE = 1,
		REPLACE_BELOW = 2,
		REPLACE_ABOVE = 4;

	public static int checkFluids(BlockGetter world, BlockPos origin, BlockState[] states, Predicate<BlockState> replace) {
		int result = CAN_PLACE;
		for (int offsetY = 0, length = states.length; offsetY < length; offsetY++) {
			BlockState oldState = world.getBlockState(origin.above(offsetY));
			if (!replace.test(oldState)) return 0;
			DoubleBlockHalf half = oldState.getOptionalValue(BlockStateProperties.DOUBLE_BLOCK_HALF).orElse(null);
			if (half != null) switch (half) {
				case LOWER -> {
					if (offsetY + 1 >= length && oldState.getBlock() == world.getBlockState(origin.above(offsetY + 1)).getBlock()) result |= REPLACE_ABOVE;
				}
				case UPPER -> {
					if (offsetY - 1 < 0 && oldState.getBlock() == world.getBlockState(origin.above(offsetY - 1)).getBlock()) result |= REPLACE_BELOW;
				}
			}
			BlockState newState = states[offsetY];
			FluidState fluidState = oldState.getFluidState();
			if (fluidState.isEmpty()) {
				if (newState.hasProperty(BlockStateProperties.WATERLOGGED)) {
					newState = newState.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
				}
				else if (!states[offsetY].getFluidState().isEmpty()) {
					return 0;
				}
			}
			else if (fluidState.getType() == Fluids.WATER) {
				if (newState.hasProperty(BlockStateProperties.WATERLOGGED)) {
					newState = newState.setValue(BlockStateProperties.WATERLOGGED, Boolean.TRUE);
				}
				else if (states[offsetY].getFluidState().getType() != Fluids.WATER) {
					return 0;
				}
			}
			else {
				return 0;
			}
			states[offsetY] = newState;
		}
		return result;
	}

	public static boolean place(
		LevelAccessor world,
		BlockPos pos,
		BlockState place,
		Predicate<BlockState> replace
	) {
		if (place.getBlock() instanceof PlantBlock_CanPlantOnTopAccess plantBlock) {
			//skip light level check that some plants normally perform.
			BlockPos downPos = pos.below();
			BlockState downState = world.getBlockState(downPos);
			if (!plantBlock.bigglobe_canPlantOnTop(downState, world, downPos)) return false;
		}
		else {
			if (!place.canSurvive(world, pos)) return false;
		}
		BlockState[] states = getStates(place);
		int flags = checkFluids(world, pos, states, replace);
		if ((flags & CAN_PLACE) == 0) return false;
		for (int offsetY = 0, length = states.length; offsetY < length; offsetY++) {
			world.setBlock(pos.above(offsetY), states[offsetY], Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
		}
		if ((flags & REPLACE_BELOW) != 0) {
			BlockPos down = pos.below();
			world.setBlock(down, world.getFluidState(down).createLegacyBlock(), Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
		}
		if ((flags & REPLACE_ABOVE) != 0) {
			BlockPos up = pos.above(states.length);
			world.setBlock(up, world.getFluidState(up).createLegacyBlock(), Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
		}
		return true;
	}

	public static boolean placeEarly(
		ChunkAccess chunk,
		BlockPos pos,
		BlockState place,
		Predicate<BlockState> replace
	) {
		BlockState[] states = getStates(place);
		int flags = checkFluids(chunk, pos, states, replace);
		if ((flags & CAN_PLACE) == 0) return false;
		for (int offsetY = 0, length = states.length; offsetY < length; offsetY++) {
			ChunkVersions.setBlockState(chunk, pos.above(offsetY), states[offsetY], Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
		if ((flags & REPLACE_BELOW) != 0) {
			BlockPos down = pos.below();
			ChunkVersions.setBlockState(chunk, down, chunk.getFluidState(down).createLegacyBlock(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
		if ((flags & REPLACE_ABOVE) != 0) {
			BlockPos up = pos.above(states.length);
			ChunkVersions.setBlockState(chunk, up, chunk.getFluidState(up).createLegacyBlock(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
		return true;
	}

	public static boolean place(LevelAccessor world, BlockPos pos, RandomGenerator random, Config config) {
		return place(
			world,
			pos,
			config.place.get(random.nextInt(config.place.size())),
			config
		);
	}

	public static boolean place(LevelAccessor world, BlockPos pos, long seed, Config config) {
		return place(
			world,
			pos,
			config.place.get(Permuter.nextBoundedInt(seed, config.place.size())),
			config
		);
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		Config config = context.config();
		return place(
			context.level(),
			context.origin(),
			config.getState(context.random()),
			config
		);
	}

	@Override
	public boolean generate(WorldWrapper world, Config config, BlockPos pos) {
		return placeEarly(((ChunkDelegator)(world.world)).chunk, pos, config.getState(world.random), config);
	}

	@UseCoder(name = "new", in = ConfigCoder.class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
	public static record Config(
		@VerifyNotEmpty @SingletonArray List<BlockState> place,
		@VerifyNotEmpty @SingletonArray @VerifyNullable Set<BlockState> replace
	)
		implements FeatureConfiguration, Predicate<BlockState> {

		public BlockState getState(RandomGenerator random) {
			return Permuter.choose(random, this.place);
		}

		public BlockState getState(RandomSource random) {
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

		public static final ReifiedType<List<BlockState>> BLOCKSTATE_LIST_REIFIED_TYPE = new ReifiedType<@SingletonArray List<BlockState>>() {

		};

		public final AutoCoder<Config> withReplace;
		public final AutoCoder<List<BlockState>> placeOnly;

		public ConfigCoder(FactoryContext<Config> context) {
			super("ConfigCoder");
			this.withReplace = context.forceCreateCoder(RecordCoder.Factory.INSTANCE);
			FactoryContext<List<BlockState>> replaceContext = context.type(BLOCKSTATE_LIST_REIFIED_TYPE);
			this.placeOnly = replaceContext.forceCreateCoder(EncoderDecoderCoder.Factory.INSTANCE);
		}

		@Override
		public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, Config> context) throws EncodeException {
			if (context.object == null) return EmptyData.INSTANCE;
			if (context.object.replace == null) {
				return context.object(context.object.place).encodeWith(this.placeOnly);
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