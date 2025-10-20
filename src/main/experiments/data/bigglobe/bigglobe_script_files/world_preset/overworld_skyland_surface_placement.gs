long seed = columnSeed(16x4DED4293C5E459FEUL)
double exactSkylandTop = `bigglobe:overworld/skyland_max_y`
double skylandSnow = `bigglobe:overworld/skyland_snow_y`
int boundMaxY = floorInt(exactSkylandTop)
int boundMinY = max(floorInt(`bigglobe:overworld/skyland_min_y`), boundMaxY - (seed := seed.newSeed()).nextInt(3, 7))
if (boundMaxY > boundMinY:
	SurfaceStates states = `bigglobe:overworld/skyland_surface_state`
	setBlockStates(boundMinY, boundMaxY, states.subsurfaceState)
	setBlockState(boundMaxY - 1, states.surfaceState)
)
if ((seed := seed.newSeed()).nextBoolean(skylandSnow - exactSkylandTop):
	generateSnow(boundMaxY, skylandSnow)
)