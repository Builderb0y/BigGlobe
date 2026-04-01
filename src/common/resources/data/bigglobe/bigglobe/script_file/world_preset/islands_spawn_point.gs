!`bigglobe:islands/is_floating` &&
!`bigglobe:islands/is_volcano` &&
world_traits.`bigglobe:temperature_at_surface`.isBetween[-0.25I, +0.25I] &&
world_traits.`bigglobe:exact_surface_y` > 16.0L &&
`bigglobe:islands/cave_noise`(world_traits.`bigglobe:y_level_in_surface`) > 0.5I