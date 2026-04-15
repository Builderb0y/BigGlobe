world_traits.`bigglobe:temperature_at_surface`.isBetween[-0.25I, +0.25I] &&
world_traits.`bigglobe:foliage_at_surface` > 0.0I &&
world_traits.`bigglobe:hilliness` <= 0.25I &&
!world_traits.`bigglobe:in_river` &&
world_traits.`bigglobe:exact_surface_y` >= 16.0L &&
`bigglobe:overworld/cave`.noise(world_traits.`bigglobe:y_level_in_surface`) > 0.5I