void placeSurfaceAbove(int y, SurfaceStates states:
	int limit = getTopOfSegment(y)
	for (SurfaceState layer in states.layers:
		setBlockStates(y, min(y + layer.depth, limit), layer.state)
	)
)