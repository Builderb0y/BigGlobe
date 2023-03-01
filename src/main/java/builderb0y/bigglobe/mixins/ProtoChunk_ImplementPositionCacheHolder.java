package builderb0y.bigglobe.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.chunk.BlendingData;

import builderb0y.bigglobe.chunkgen.PositionCache;
import builderb0y.bigglobe.chunkgen.PositionCache.PositionCacheHolder;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunk_ImplementPositionCacheHolder extends Chunk implements PositionCacheHolder {

	@Unique
	public PositionCache bigglobe_positionCache;

	public ProtoChunk_ImplementPositionCacheHolder(
		ChunkPos pos,
		UpgradeData upgradeData,
		HeightLimitView heightLimitView,
		Registry<Biome> biome,
		long inhabitedTime,
		@Nullable ChunkSection[] sectionArrayInitializer,
		@Nullable BlendingData blendingData
	) {
		super(
			pos,
			upgradeData,
			heightLimitView,
			biome,
			inhabitedTime,
			sectionArrayInitializer,
			blendingData
		);
	}

	@Override
	public PositionCache bigglobe_getPositionCache() {
		return this.bigglobe_positionCache;
	}

	@Override
	public void bigglobe_setPositionCache(PositionCache cache) {
		this.bigglobe_positionCache = cache;
	}
}