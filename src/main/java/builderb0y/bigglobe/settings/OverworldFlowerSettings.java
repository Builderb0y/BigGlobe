package builderb0y.bigglobe.settings;

import builderb0y.bigglobe.noise.Grid2D;

/*
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
	vec2 uv = fragCoord / 4.0 + iTime * 64.0;

	vec3 color = vec3(0.0);
	vec2 gridStart = floor(uv / 64.0) * 64.0;
	vec2 inGrid = uv - gridStart;
	for (int offsetX = -64; offsetX <= 64; offsetX += 64) {
		for (int offsetY = -64; offsetY <= 64; offsetY += 64) {
			vec2 otherGridStart = gridStart + vec2(offsetX, offsetY);
			vec2 otherGridCenter = hash23(vec3(otherGridStart, 1.9835)) * 64.0 + vec2(offsetX, offsetY);
			float otherGridSeed = hash13(vec3(otherGridStart, 1.5238));
			float noise = 0.0;
			noise += (noise12(uv / 32.0, otherGridSeed) * 2.0 - 1.0);
			noise += (noise12(uv / 16.0, otherGridSeed) * 2.0 - 1.0);
			noise += (noise12(uv /  8.0, otherGridSeed) * 2.0 - 1.0);
			noise += (noise12(uv /  4.0, otherGridSeed) * 2.0 - 1.0);
			noise /= 4.0; //1.0 + 1.0 / 2.0 + 1.0 / 3.0 + 1.0 / 4.0;
			noise = abs(noise);
			noise -= distanceSquared(inGrid, otherGridCenter) / (64.0 * 64.0);
			if (noise > 0.0) {
				color += hue(hash13(vec3(otherGridStart, 1.0405))) * (noise * 4.0);
			}
		}
	}

	fragColor = vec4(color, 1.0);
}
*/
public record OverworldFlowerSettings(Grid2D noise) {}