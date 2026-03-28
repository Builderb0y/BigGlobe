package builderb0y.bigglobe.mixins;

import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.throwables.MixinError;

/**
zoglins don't have any spawn restrictions like hoglins do.
this is a problem because it means they often spawn in mid-air.
to fix this, we copy the spawn restrictions of hoglins to zoglins.
*/
@Mixin(SpawnPlacements.class)
public interface SpawnRestriction_BackingMapAccess {

	@Accessor("DATA_BY_TYPE")
	public static Map<EntityType<?>, Object /* Entry is package-private but mixins don't care about the generic type */> bigglobe_getRestrictions() {
		throw new MixinError("mixin not applied");
	}
}