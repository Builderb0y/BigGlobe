package builderb0y.bigglobe.features;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import builderb0y.bigglobe.blocks.BlockStates;

public class BlockQueueStructureWorldAccess implements WorldGenLevel {

	public final WorldGenLevel world;
	public final BlockQueue queue;

	public BlockQueueStructureWorldAccess(WorldGenLevel world, BlockQueue queue) {
		this.world = world;
		this.queue = queue;
	}

	public void commit() {
		this.queue.placeQueuedBlocks(this.world);
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		BlockEntity blockEntity = this.queue.getBlockEntity(pos);
		if (blockEntity == null) blockEntity = this.world.getBlockEntity(pos);
		return blockEntity;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		BlockState state = this.queue.getBlockStateOrNull(pos);
		return state != null ? state : this.getWorldState(pos);
	}

	public BlockState getWorldState(BlockPos pos) {
		ChunkAccess chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.NOISE, false);
		return chunk != null ? chunk.getBlockState(pos) : BlockStates.AIR;
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return this.getBlockState(pos).getFluidState();
	}

	public void setBlockState(BlockPos pos, BlockState state) {
		this.queue.queueBlock(pos, state);
	}

	@Override
	public boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
		this.setBlockState(pos, state);
		return true;
	}

	@Override
	public boolean removeBlock(BlockPos pos, boolean move) {
		BlockState oldState = this.getBlockState(pos);
		BlockState newState = oldState.getFluidState().createLegacyBlock();
		if (oldState != newState) {
			this.setBlock(pos, newState, Block.UPDATE_ALL);
			return true;
		}
		return false;
	}

	@Override
	public boolean destroyBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
		return this.removeBlock(pos, false);
	}

	@Override
	public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
		return state.test(this.getBlockState(pos));
	}

	@Override
	public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> state) {
		return state.test(this.getFluidState(pos));
	}

	@Override
	public long getSeed() {
		return this.world.getSeed();
	}

	@Override
	public ServerLevel getLevel() {
		return this.world.getLevel();
	}

	@Override
	public long nextSubTickCount() {
		return this.world.nextSubTickCount();
	}

	@Override
	public LevelTickAccess<Block> getBlockTicks() {
		return this.world.getBlockTicks();
	}

	@Override
	public LevelTickAccess<Fluid> getFluidTicks() {
		return this.world.getFluidTicks();
	}

	@Override
	public LevelData getLevelData() {
		return this.world.getLevelData();
	}

	@Override
	public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
		return this.world.getCurrentDifficultyAt(pos);
	}

	@Override
	@Nullable
	public MinecraftServer getServer() {
		return this.world.getServer();
	}

	@Override
	public ChunkSource getChunkSource() {
		return this.world.getChunkSource();
	}

	@Override
	public RandomSource getRandom() {
		return this.world.getRandom();
	}

	@Override
	public EnvironmentAttributeReader environmentAttributes() {
		return this.world.environmentAttributes();
	}

	@Override
	public void addParticle(ParticleOptions parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		this.world.addParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
	}

	@Override
	public void playSound(@Nullable Entity source, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
		this.world.playSound(source, pos, sound, category, volume, pitch);
	}

	@Override
	public void levelEvent(@Nullable Entity source, int eventId, BlockPos pos, int data) {
		this.world.levelEvent(source, eventId, pos, data);
	}

	@Override
	public void gameEvent(Holder<GameEvent> event, Vec3 emitterPos, Context emitter) {
		this.world.gameEvent(event, emitterPos, emitter);
	}

	@Override
	public RegistryAccess registryAccess() {
		return this.world.registryAccess();
	}

	@Override
	public List<Entity> getEntities(@Nullable Entity except, AABB box, Predicate<? super Entity> predicate) {
		return this.world.getEntities(except, box, predicate);
	}

	@Override
	public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AABB box, Predicate<? super T> predicate) {
		return this.world.getEntities(filter, box, predicate);
	}

	@Override
	public List<? extends Player> players() {
		return this.world.players();
	}

	@Override
	@Nullable
	public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
		return this.world.getChunk(chunkX, chunkZ, leastStatus, create);
	}

	@Override
	public int getHeight(Heightmap.Types heightmap, int x, int z) {
		return this.world.getHeight(heightmap, x, z);
	}

	@Override
	public int getSkyDarken() {
		return this.world.getSkyDarken();
	}

	@Override
	public BiomeManager getBiomeManager() {
		return this.world.getBiomeManager();
	}

	@Override
	public Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
		return this.world.getUncachedNoiseBiome(biomeX, biomeY, biomeZ);
	}

	@Override
	public boolean isClientSide() {
		return this.world.isClientSide();
	}

	@Override
	@Deprecated
	public int getSeaLevel() {
		return this.world.getSeaLevel();
	}

	@Override
	public DimensionType dimensionType() {
		return this.world.dimensionType();
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return this.world.getLightEngine();
	}

	@Override
	public WorldBorder getWorldBorder() {
		return this.world.getWorldBorder();
	}

	@Override
	public FeatureFlagSet enabledFeatures() {
		return this.world.enabledFeatures();
	}
}