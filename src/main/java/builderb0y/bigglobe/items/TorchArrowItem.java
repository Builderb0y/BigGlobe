package builderb0y.bigglobe.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.TorchArrowEntity;

public class TorchArrowItem extends ArrowItem {

	public TorchArrowItem(Settings settings) {
		super(settings);
	}

	@Override
	public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter) {
		return new TorchArrowEntity(BigGlobeEntityTypes.TORCH_ARROW, world, shooter);
	}
}