package builderb0y.bigglobe.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

public interface SlingshotAmmunition extends ProjectileItem {

	public abstract Projectile createProjectile(Level world, LivingEntity user, ItemStack stack, ItemStack slingshot);
}