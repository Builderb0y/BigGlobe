package builderb0y.bigglobe.compat.dhChunkGen;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.tick.BasicTickScheduler;
import net.minecraft.world.tick.EmptyTickSchedulers;
import net.minecraft.world.tick.SimpleTickScheduler;

import builderb0y.bigglobe.versions.RegistryKeyVersions;

/** not used, intended for testing purposes only, to be removed as soon as DH's API allows what I want. */
public class FakeChunk extends Chunk {

	public static final TickSchedulers EMPTY_TICK_SCHEDULERS = new TickSchedulers(new SimpleTickScheduler<>(), new SimpleTickScheduler<>());

	public ILevelWrapper level;
	public IBiomeWrapper biome;

	public FakeChunk(
		ChunkPos pos,
		ServerWorld world,
		ILevelWrapper level,
		IBiomeWrapper biome,
		ChunkSection[] sharedSections
	) {
		super(
			pos,
			UpgradeData.NO_UPGRADE_DATA,
			world,
			world.getRegistryManager().get(RegistryKeyVersions.biome()),
			0L,
			sharedSections,
			null
		);
		this.level = level;
		this.biome = biome;
	}

	public ServerWorld getServerWorld() {
		return (ServerWorld)(this.heightLimitView);
	}

	@Nullable
	@Override
	public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
		return null;
	}

	@Override
	public void setBlockEntity(BlockEntity blockEntity) {

	}

	@Override
	public void addEntity(Entity entity) {

	}

	@Override
	public ChunkStatus getStatus() {
		return ChunkStatus.FULL;
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {

	}

	@Nullable
	@Override
	public NbtCompound getPackedBlockEntityNbt(BlockPos pos) {
		return null;
	}

	@Override
	public BasicTickScheduler<Block> getBlockTickScheduler() {
		return EmptyTickSchedulers.getReadOnlyTickScheduler();
	}

	@Override
	public BasicTickScheduler<Fluid> getFluidTickScheduler() {
		return EmptyTickSchedulers.getReadOnlyTickScheduler();
	}

	@Override
	public TickSchedulers getTickSchedulers() {
		return EMPTY_TICK_SCHEDULERS;
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return null;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return Blocks.AIR.getDefaultState();
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return Fluids.EMPTY.getDefaultState();
	}

	@Override
	public void addPendingBlockEntityNbt(NbtCompound nbt) {

	}

	@Override
	public void addStructureReference(Structure structure, long reference) {

	}

	@Override
	public boolean areSectionsEmptyBetween(int lowerHeight, int upperHeight) {
		return true;
	}

	#if MC_VERSION >= MC_1_20_0
		@Override
		public void forEachBlockMatchingPredicate(Predicate<BlockState> predicate, BiConsumer<BlockPos, BlockState> consumer) {

		}
	#else
		@Override
		public Stream<BlockPos> getLightSourcesStream() {
			return Stream.empty();
		}
	#endif

	@Override
	public #if MC_VERSION >= MC_1_20_0 int #else ChunkSection #endif getHighestNonEmptySection() {
		return #if MC_VERSION >= MC_1_20_0 -1 #else this.sectionArray[0] #endif;
	}

	@Override
	public Set<BlockPos> getBlockEntityPositions() {
		return Collections.emptySet();
	}
}