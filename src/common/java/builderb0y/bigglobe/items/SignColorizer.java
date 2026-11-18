package builderb0y.bigglobe.items;

import net.minecraft.world.level.block.entity.SignBlockEntity;

public interface SignColorizer {

	//called by mixin, since minecraft made the method below
	//this one static in 26.3, so it can no longer be overridden.
	public void setColors(SignBlockEntity sign);
}