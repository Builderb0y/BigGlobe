float calcFrac(ScriptStructurePiece piece:
	float relativeX = x - piece.midX
	float relativeZ = z - piece.midZ
	float maxFrac = 0.0I
	for (NbtCompound side in NbtList(piece.data.sides):
		float sideX = side.x
		float sideZ = side.z
		float dot = relativeX * sideX + relativeZ * sideZ
		float div = sideX ^ 2 + sideZ ^ 2
		maxFrac = max(maxFrac, dot / div)
	)
	return(maxFrac)
)

double calcSurfaceY(ScriptStructurePiece piece, float frac:
	int startY = piece.data.startY
	float curve = float(1.0L - sqrt(1.0I - (2.0I - frac) ^ 2))
	return(frac == 1.0I ? startY + 16.5L : mixLinear(world_traits.`bigglobe:automatic_exact_surface_y`(piece.minY), startY + 0.5L, double(curve)))
)