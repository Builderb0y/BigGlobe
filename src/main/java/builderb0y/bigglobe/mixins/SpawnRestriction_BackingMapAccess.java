package builderb0y.bigglobe.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;

/**
zoglins don't have any spawn restrictions like hoglins do.
this is a problem because it means they often spawn in mid-air.
to fix this, we copy the spawn restrictions of hoglins to zoglins.
*/
@Mixin(SpawnRestriction.class)
public interface SpawnRestriction_BackingMapAccess {

	@Accessor("RESTRICTIONS")
	public static Map<EntityType<?>, Object /* Entry is package-private but mixins don't care about the generic type */> bigglobe_getRestrictions() {
		throw new IllegalStateException("mixin not applied");
	}
}