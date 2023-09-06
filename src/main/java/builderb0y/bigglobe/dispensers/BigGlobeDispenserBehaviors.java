package builderb0y.bigglobe.dispensers;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.RockEntity;
import builderb0y.bigglobe.entities.TorchArrowEntity;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.mixins.DispenserBlock_BehaviorsAccess;

public class BigGlobeDispenserBehaviors {

	public static void init() {
		DispenserBlock.registerBehavior(BigGlobeItems.TORCH_ARROW, new ProjectileDispenserBehavior() {

			@Override
			public ProjectileEntity createProjectile(World world, Position position, ItemStack stack) {
				TorchArrowEntity entity = new TorchArrowEntity(BigGlobeEntityTypes.TORCH_ARROW, position.getX(), position.getY(), position.getZ(), world);
				entity.pickupType = PickupPermission.ALLOWED;
				return entity;
			}
		});
		DispenserBlock.registerBehavior(BigGlobeItems.ROCK, new ProjectileDispenserBehavior() {

			@Override
			public ProjectileEntity createProjectile(World world, Position position, ItemStack stack) {
				return new RockEntity(BigGlobeEntityTypes.ROCK, position.getX(), position.getY(), position.getZ(), world);
			}
		});
		DispenserBlock.registerBehavior(BigGlobeItems.SOUL_LAVA_BUCKET, DispenserBlock_BehaviorsAccess.bigglobe_getBehaviors().get(Items.LAVA_BUCKET));
	}
}