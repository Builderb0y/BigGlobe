package builderb0y.bigglobe.mixins;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface Entity_CurrentIdGetter {

	@Accessor("ENTITY_COUNTER")
	public static AtomicInteger bigglobe_getCurrentID() {
		throw new IllegalStateException("mixin not applied");
	}
}