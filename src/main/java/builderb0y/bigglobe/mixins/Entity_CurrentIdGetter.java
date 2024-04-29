package builderb0y.bigglobe.mixins;

import java.util.concurrent.atomic.AtomicInteger;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public interface Entity_CurrentIdGetter {

	@Accessor("CURRENT_ID")
	public static AtomicInteger bigglobe_getCurrentID() {
		throw new IllegalStateException("mixin not applied");
	}
}