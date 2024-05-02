package builderb0y.bigglobe.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.world.World;

public interface SlingshotAmmunition #if MC_VERSION >= MC_1_20_5 extends ProjectileItem #endif {

	public abstract ProjectileEntity createProjectile(World world, LivingEntity user, ItemStack stack, ItemStack slingshot);
}