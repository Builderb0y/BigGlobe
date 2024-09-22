int surfaceY = world_traits.`bigglobe:skyland_max_y`
long seed = columnSeed(16x6C744C12C6BC713CUL)
IslandSurfaceStates states = `bigglobe:islands/skyland_surface_states`
double slopeSquared = (
	+ `bigglobe:islands/dx_height_offset` ^ 2
	+ `bigglobe:islands/dz_height_offset` ^ 2
)

int depth = floorInt(
	(seed := seed.newSeed()).nextDouble(3.0L, 7.0L)
	- (slopeSquared * 3.0L)
)

boolean hadBlock = false
for (int y in -range[surfaceY - depth, surfaceY):
	BlockState state = getBlockState(y)
	if (state == null: break())
	hadBlock = (state !=. 'minecraft:air').if (
		setBlockState(y, hadBlock ? states.subsurfaceState : states.surfaceState)
	)
)