package builderb0y.bigglobe.util;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import builderb0y.bigglobe.platform.fabric.fabric.api.event.Event;
import builderb0y.bigglobe.platform.fabric.fabric.api.event.EventFactory;
import net.minecraft.client.multiplayer.ClientLevel;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public class ClientWorldEvents {

	public static final Event<WorldChanged> WORLD_CHANGED = EventFactory.createArrayBacked(
		WorldChanged.class, (WorldChanged[] events) -> {
			return (ClientLevel woldWorld, ClientLevel newWorld) -> {
				for (WorldChanged event : events) {
					event.worldChanged(woldWorld, newWorld);
				}
			};
		}
	);

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static interface WorldChanged {

		public abstract void worldChanged(ClientLevel oldWorld, ClientLevel newWorld);
	}
}