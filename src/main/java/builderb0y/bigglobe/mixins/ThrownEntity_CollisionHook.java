package builderb0y.bigglobe.mixins;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

@Mixin(ThrownEntity.class)
public abstract class ThrownEntity_CollisionHook extends ProjectileEntity {

	public ThrownEntity_CollisionHook(EntityType<? extends ProjectileEntity> entityType, World world) {
		super(entityType, world);
	}

	@Unique
	public HitResult bigglobe_getCollision() {
		return ProjectileUtil.getCollision(this, this::canHit);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/HitResult;"))
	private HitResult bigglobe_redirectGetCollision(Entity entity, Predicate<Entity> predicate) {
		return this.bigglobe_getCollision();
	}
}