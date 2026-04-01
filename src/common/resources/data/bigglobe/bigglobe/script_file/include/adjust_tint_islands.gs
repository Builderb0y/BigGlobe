int adjustTintIslands(int color:
	float*(
		red   = color.redI
		green = color.greenI
		blue  = color.blueI
		magicalness = world_traits.`bigglobe:magicalness`
		adjustedMagicalness = 0.5I - 0.5I / (magicalness ^ 2 + 1.0I)
	)

	if (magicalness > 0.0I:
		red   = mixLinear(red,   255.0I, adjustedMagicalness * 0.5I)
		green = mixLinear(green, 255.0I, adjustedMagicalness       )
		blue  = mixLinear(blue,  255.0I, adjustedMagicalness * 1.5I)
	)
	else (
		red   *= 1.0I - adjustedMagicalness * 0.5I
		green *= 1.0I - adjustedMagicalness
		blue  *= 1.0I - adjustedMagicalness * 1.5I
	)

	if (`bigglobe:islands/is_volcano`:
		float frac = unmixSmooth(1.0L, 0.75L, `bigglobe:islands/island`.soft_distance_squared).as(float)
		red   = mixLinear(red,   63.0I, frac)
		green = mixLinear(green, 63.0I, frac)
		blue  = mixLinear(blue,  63.0I, frac)
	)

	return(packI(roundInt(red), roundInt(green), roundInt(blue)))
)