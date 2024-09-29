int adjustTint(int color:
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

	return(packI(roundInt(red), roundInt(green), roundInt(blue)))
)