int surfaceY = world_traits.`bigglobe:skyland_max_y`
long seed = columnSeed(16x6C744C12C6BC713CUL)
SurfaceStates states = `bigglobe:islands/skyland_surface_states`
double slopeSquared = (
	+ dx(`bigglobe:islands/skyland_top`) ^ 2
	+ dz(`bigglobe:islands/skyland_top`) ^ 2
)

int depth = floorInt(
	(seed := seed.newSeed()).nextDouble(3.0L, 7.0L)
	- (slopeSquared * 3.0L)
)

if (`bigglobe:overworld/skyland_lake_surface_states` != null:
	setBlockStates(
		surfaceY - (seed := seed.newSeed()).nextInt(3, 7),
		surfaceY,
		`bigglobe:overworld/skyland_lake_surface_states`.under
	)
)

if (depth > 0:
	if (`bigglobe:overworld/skyland_lake_surface_states` != null:
		setBlockStates(
			surfaceY - depth,
			surfaceY,
			`bigglobe:overworld/skyland_lake_surface_states`.top
		)
	)
	else (
		boolean hadBlock = false
		for (int y in -range[surfaceY - depth, surfaceY):
			BlockState state = getBlockState(y)
			if (state == null: break())
			hadBlock = (state !=. 'minecraft:air').if (
				setBlockState(y, hadBlock ? states.subsurfaceState : states.surfaceState)
			)
		)
	)
)

(seed := seed.newSeed()).if (`bigglobe:islands/skyland_snow_chance`:
	generateSnow(surfaceY, `bigglobe:islands/skyland_snow_y`)
)