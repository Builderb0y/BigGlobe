package builderb0y.bigglobe.mixins;

import java.util.concurrent.atomic.AtomicInteger;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

#if MC_VERSION >= MC_26_2_0
	@Mixin(ServerLevel.class)
#else
	@Mixin(Entity.class)
#endif
public interface Entity_CurrentIdGetter {

	@Accessor("ENTITY_COUNTER")
	public static AtomicInteger bigglobe_getCurrentID() {
		throw new IllegalStateException("mixin not applied");
	}
}