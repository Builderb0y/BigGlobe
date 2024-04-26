package builderb0y.bigglobe.mixinInterfaces;

import builderb0y.bigglobe.hyperspace.ClientWaypointManager;

public interface WaypointTracker {

	public abstract ClientWaypointManager bigglobe_getWaypointManager();

	public abstract void bigglobe_setWaypointManager(ClientWaypointManager data);
}