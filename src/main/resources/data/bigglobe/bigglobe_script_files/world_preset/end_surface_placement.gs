if (EndSurface*(surface := `bigglobe:end/surface`).surface_depth > 0:
	if (surface.surface_depth > 1:
		setBlockStates(
			max(
				higherInt(`bigglobe:end/mountain_max_y`) - surface.surface_depth,
				floorInt(`bigglobe:end/mountain_min_y`)
			),
			floorInt(`bigglobe:end/mountain_max_y`),
			surface.subsurface_state
		)
	)
	setBlockState(floorInt(`bigglobe:end/mountain_max_y`), surface.surface_state)
)