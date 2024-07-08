package builderb0y.bigglobe.items;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.TorchArrowEntity;

public class TorchArrowItem extends ArrowItem {

	public TorchArrowItem(Item.Settings settings) {
		super(settings);
	}

	#if MC_VERSION >= MC_1_21_0

		@Override
		public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter, @Nullable ItemStack shotFrom) {
			return new TorchArrowEntity(BigGlobeEntityTypes.TORCH_ARROW, shooter, world, stack.copyWithCount(1), shotFrom);
		}

		@Override
		public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
			TorchArrowEntity arrowEntity = new TorchArrowEntity(BigGlobeEntityTypes.TORCH_ARROW, pos.getX(), pos.getY(), pos.getZ(), world, stack.copyWithCount(1), null);
			arrowEntity.pickupType = PickupPermission.ALLOWED;
			return arrowEntity;
		}
	#else

		@Override
		public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter) {
			return new TorchArrowEntity(BigGlobeEntityTypes.TORCH_ARROW, world, shooter);
		}
	#endif
}