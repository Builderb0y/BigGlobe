package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;

import builderb0y.bigglobe.mixinInterfaces.PositionCache;
import builderb0y.bigglobe.mixinInterfaces.PositionCache.PositionCacheHolder;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunk_ImplementPositionCacheHolder extends Chunk implements PositionCacheHolder {

	@Unique
	public PositionCache bigglobe_positionCache;

	public ProtoChunk_ImplementPositionCacheHolder() {
		super(null, null, null, null, 0L, null, null);
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