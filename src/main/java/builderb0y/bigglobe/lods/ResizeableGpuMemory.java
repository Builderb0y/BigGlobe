package builderb0y.bigglobe.lods;

import builderb0y.bigglobe.BigGlobeMod;

public abstract class ResizeableGpuMemory extends GpuMemory {

	public ResizeableGpuMemory(long capacity, int binder, int bindQuery) {
		super(capacity, binder, bindQuery);
	}

	public void resize(long newCapacity) {
		if (this.capacity < newCapacity) {
			newCapacity = Math.max(newCapacity, this.capacity << 1);
			BigGlobeMod.LOGGER.info("Re-sizing LOD " + this.getClass().getSimpleName() + " from " + this.capacity + " bytes to " + newCapacity + " bytes");
			this.close();
			this.capacity = newCapacity;
			this.glID = this.nAllocate(false);
		}
	}
}