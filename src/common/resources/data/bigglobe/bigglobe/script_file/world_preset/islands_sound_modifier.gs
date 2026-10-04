if (listener.underwater:
	return('bigglobe:lowpass')
)
double*(x = sound.x, y = sound.y, z = sound.z)
if (sound.relative:
	x += listener.eyeX
	y += listener.eyeY
	z += listener.eyeZ
)
int*(ix = floorInt(x), iz = floorInt(z))
if (y > world_traits.`bigglobe:exact_surface_y`(ix, iz) - 16.0L:
	return(null)
)
if (y > world_traits.`bigglobe:chasm_max_y`(ix, iz):
	return('bigglobe:reverb_small')
)
if (y > world_traits.`bigglobe:deep_dark_max_y`(ix, iz):
	return('bigglobe:reverb_medium')
)
if (y > world_traits.`bigglobe:core_max_y`(ix, iz):
	return('bigglobe:reverb_large')
)
return('bigglobe:reverb_medium')