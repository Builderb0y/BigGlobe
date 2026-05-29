void placeSurface(int y, SurfaceStates states:
	int limit = getBottomOfSegment(y - 1)
	for (SurfaceState layer in states.layers:
		setBlockStates(max(y - layer.depth, limit), y, layer.state)
	)
)