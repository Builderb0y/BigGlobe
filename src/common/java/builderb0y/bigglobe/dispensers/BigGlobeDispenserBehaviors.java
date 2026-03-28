package builderb0y.bigglobe.dispensers;

import builderb0y.bigglobe.items.BigGlobeItems;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

public class BigGlobeDispenserBehaviors {

	public static void init() {
		DispenserBlock.registerBehavior(BigGlobeItems.TORCH_ARROW, new ProjectileDispenseBehavior(BigGlobeItems.TORCH_ARROW));
		DispenserBlock.registerBehavior(BigGlobeItems.ROCK, new ProjectileDispenseBehavior(BigGlobeItems.ROCK));
		DispenserBlock.registerBehavior(BigGlobeItems.SOUL_LAVA_BUCKET, DispenserBlock.DISPENSER_REGISTRY.get(Items.LAVA_BUCKET));
	}
}