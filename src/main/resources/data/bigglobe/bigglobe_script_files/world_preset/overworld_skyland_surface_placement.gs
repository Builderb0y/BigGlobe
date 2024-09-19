SkylandBounds bounds = `bigglobe:overworld/skyland`.bounds
int boundMaxY = higherInt(bounds.max_y)
int boundMinY = max(floorInt(bounds.min_y), boundMaxY - bounds.surface_depth)
if (boundMaxY > boundMinY:
	setBlockStates(boundMinY, boundMaxY, 'minecraft:dirt')
	long seed = columnSeed(16x4DED4293C5E459FEUL)
	setBlockState(boundMaxY - 1,
		seed.if (0.25:
			if (`bigglobe:overworld/skyland`.tree_feature != null:
				seed.newSeed().if (0.25:
					'minecraft:podzol[snowy=false]'
				)
				else (
					'bigglobe:overgrown_podzol[snowy=false]'
				)
			)
			else (
				'minecraft:grass_block[snowy=false]'
			)
		)
		else (
			'minecraft:grass_block[snowy=false]'
		)
	)
)