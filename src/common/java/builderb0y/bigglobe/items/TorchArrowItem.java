package builderb0y.bigglobe.items;

import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.TorchArrowEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TorchArrowItem extends ArrowItem {

	public TorchArrowItem(Item.Properties settings) {
		super(settings);
	}

	@Override
	public AbstractArrow createArrow(Level world, ItemStack stack, LivingEntity shooter, @Nullable ItemStack shotFrom) {
		return new TorchArrowEntity(BigGlobeEntityTypes.TORCH_ARROW, shooter, world, stack.copyWithCount(1), shotFrom);
	}

	@Override
	public Projectile asProjectile(Level world, Position pos, ItemStack stack, Direction direction) {
		TorchArrowEntity arrowEntity = new TorchArrowEntity(BigGlobeEntityTypes.TORCH_ARROW, pos.x(), pos.y(), pos.z(), world, stack.copyWithCount(1), null);
		arrowEntity.pickup = Pickup.ALLOWED;
		return arrowEntity;
	}
}