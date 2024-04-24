package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.hyperspace.WaypointManager.ServerWaypointData;
import builderb0y.bigglobe.mixinInterfaces.WaypointEntranceTracker;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntity_TrackWaypointEntrance implements WaypointEntranceTracker {

	public ServerWaypointData bigglobe_waypointEntrance;

	@Override
	public ServerWaypointData bigglobe_getWaypointEntrance() {
		return this.bigglobe_waypointEntrance;
	}

	@Override
	public void bigglobe_setWaypointEntrance(ServerWaypointData entrance) {
		this.bigglobe_waypointEntrance = entrance;
	}
}