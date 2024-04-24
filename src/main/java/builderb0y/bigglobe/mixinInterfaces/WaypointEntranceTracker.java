package builderb0y.bigglobe.mixinInterfaces;

import builderb0y.bigglobe.hyperspace.WaypointManager.ServerWaypointData;

public interface WaypointEntranceTracker {

	public abstract ServerWaypointData bigglobe_getWaypointEntrance();
	
	public abstract void bigglobe_setWaypointEntrance(ServerWaypointData data);
}