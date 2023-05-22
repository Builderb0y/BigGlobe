package builderb0y.bigglobe.features;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import net.minecraft.world.tick.QueryableTickScheduler;

import builderb0y.bigglobe.blocks.BlockStates;

public class BlockQueueStructureWorldAccess implements StructureWorldAccess {

	public final StructureWorldAccess world;
	public final BlockQueue queue;

	public BlockQueueStructureWorldAccess(StructureWorldAccess world, BlockQueue queue) {
		this.world = world;
		this.queue = queue;
	}

	public void commit() {
		this.queue.placeQueuedBlocks(this.world);
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return this.world.getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		BlockState state = this.queue.getBlockStateOrNull(pos);
		return state != null ? state : this.getWorldState(pos);
	}

	public BlockState getWorldState(BlockPos pos) {
		Chunk chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.NOISE, false);
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
	public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
		this.setBlockState(pos, state);
		return true;
	}

	@Override
	public boolean removeBlock(BlockPos pos, boolean move) {
		BlockState oldState = this.getBlockState(pos);
		BlockState newState = oldState.getFluidState().getBlockState();
		if (oldState != newState) {
			this.setBlockState(pos, newState, Block.NOTIFY_ALL);
			return true;
		}
		return false;
	}

	@Override
	public boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
		return this.removeBlock(pos, false);
	}

	@Override
	public boolean testBlockState(BlockPos pos, Predicate<BlockState> state) {
		return state.test(this.getBlockState(pos));
	}

	@Override
	public boolean testFluidState(BlockPos pos, Predicate<FluidState> state) {
		return state.test(this.getFluidState(pos));
	}

	@Override
	public long getSeed() {
		return this.world.getSeed();
	}

	@Override
	public ServerWorld toServerWorld() {
		return this.world.toServerWorld();
	}

	@Override
	public long getTickOrder() {
		return this.world.getTickOrder();
	}

	@Override
	public QueryableTickScheduler<Block> getBlockTickScheduler() {
		return this.world.getBlockTickScheduler();
	}

	@Override
	public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
		return this.world.getFluidTickScheduler();
	}

	@Override
	public WorldProperties getLevelProperties() {
		return this.world.getLevelProperties();
	}

	@Override
	public LocalDifficulty getLocalDifficulty(BlockPos pos) {
		return this.world.getLocalDifficulty(pos);
	}

	@Override
	@Nullable
	public MinecraftServer getServer() {
		return this.world.getServer();
	}

	@Override
	public ChunkManager getChunkManager() {
		return this.world.getChunkManager();
	}

	@Override
	public Random getRandom() {
		return this.world.getRandom();
	}

	@Override
	public void playSound(@Nullable PlayerEntity player, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
		this.world.playSound(player, pos, sound, category, volume, pitch);
	}

	@Override
	public void addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		this.world.addParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
	}

	@Override
	public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {
		this.world.syncWorldEvent(player, eventId, pos, data);
	}

	@Override
	public void emitGameEvent(GameEvent event, Vec3d emitterPos, Emitter emitter) {
		this.world.emitGameEvent(event, emitterPos, emitter);
	}

	@Override
	public DynamicRegistryManager getRegistryManager() {
		return this.world.getRegistryManager();
	}

	@Override
	public List<Entity> getOtherEntities(@Nullable Entity except, Box box, Predicate<? super Entity> predicate) {
		return this.world.getOtherEntities(except, box, predicate);
	}

	@Override
	public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate) {
		return this.world.getEntitiesByType(filter, box, predicate);
	}

	@Override
	public List<? extends PlayerEntity> getPlayers() {
		return this.world.getPlayers();
	}

	@Override
	@Nullable
	public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
		return this.world.getChunk(chunkX, chunkZ, leastStatus, create);
	}

	@Override
	public int getTopY(Heightmap.Type heightmap, int x, int z) {
		return this.world.getTopY(heightmap, x, z);
	}

	@Override
	public int getAmbientDarkness() {
		return this.world.getAmbientDarkness();
	}

	@Override
	public BiomeAccess getBiomeAccess() {
		return this.world.getBiomeAccess();
	}

	@Override
	public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
		return this.world.getGeneratorStoredBiome(biomeX, biomeY, biomeZ);
	}

	@Override
	public boolean isClient() {
		return this.world.isClient();
	}

	@Override
	@Deprecated
	public int getSeaLevel() {
		return this.world.getSeaLevel();
	}

	@Override
	public DimensionType getDimension() {
		return this.world.getDimension();
	}

	@Override
	public float getBrightness(Direction direction, boolean shaded) {
		return this.world.getBrightness(direction, shaded);
	}

	@Override
	public LightingProvider getLightingProvider() {
		return this.world.getLightingProvider();
	}

	@Override
	public WorldBorder getWorldBorder() {
		return this.world.getWorldBorder();
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return this.world.getEnabledFeatures();
	}
}