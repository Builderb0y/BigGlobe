#version 150

layout(std140) uniform Uniforms {
	mat4 inverseModelView;
	mat4 inverseProjection;
	vec4 cameraPosition;
	vec4 beamOrigin;
	float beamDirectionSeed;
	float beamTime;
	float time;
};

in vec2 texCoord;

out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = PI * 2.0;

float square(float f) {
	return f * f;
}

vec2 square(vec2 v) {
	return v * v;
}

vec3 square(vec3 v) {
	return v * v;
}

float smoothify(float f) {
	return f * f * (f * -2.0 + 3.0);
}

vec2 smoothify(vec2 v) {
	return v * v * (v * -2.0 + 3.0);
}

vec3 smoothify(vec3 v) {
	return v * v * (v * -2.0 + 3.0);
}

float lengthSquared(vec2 v) {
	return dot(v, v);
}

float lengthSquared(vec3 v) {
	return dot(v, v);
}

vec4 hash41(float p) {
	vec4 p4 = fract(vec4(p) * vec4(0.1031, 0.1013, 0.0973, 0.1099));
	p4 += dot(p4, p4.wzxy + 33.33);
	return fract((p4.xxyz + p4.yzzw) * p4.zywx);
}

vec3 hash33(vec3 p3) {
	p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
	p3 += dot(p3, p3.yxz + 33.33);
	return fract((p3.xxy + p3.yxx) * p3.zyx);
}

vec2 cornerNoise(vec3 coord, float speed) {
	vec3 hash = hash33(coord);
	float phase = time * speed * hash.x + hash.y;
	//due to the fact that coord is pre-dithered
	//before this function is called,
	//coord, and therefore hash, and therefore phase,
	//is very likely to be divergent.
	//so, use ALU over SFU for this.
	float wave = smoothify(abs(fract(phase) * 2.0 - 1.0));
	return vec2(wave, hash.z);
}

vec2 volumetricNoise(vec3 coord, float speed) {
	vec3  fractCoord = fract(coord);
	vec3  floorCoord = coord - fractCoord;
	vec3   ceilCoord = floorCoord + vec3(1.0);
	vec3 smoothCoord = smoothify(fractCoord);

	return mix(
		mix(
			mix(
				cornerNoise(vec3(floorCoord.x, floorCoord.y, floorCoord.z), speed),
				cornerNoise(vec3(floorCoord.x, floorCoord.y,  ceilCoord.z), speed),
				smoothCoord.z
			),
			mix(
				cornerNoise(vec3(floorCoord.x,  ceilCoord.y, floorCoord.z), speed),
				cornerNoise(vec3(floorCoord.x,  ceilCoord.y,  ceilCoord.z), speed),
				smoothCoord.z
			),
			smoothCoord.y
		),
		mix(
			mix(
				cornerNoise(vec3( ceilCoord.x, floorCoord.y, floorCoord.z), speed),
				cornerNoise(vec3( ceilCoord.x, floorCoord.y,  ceilCoord.z), speed),
				smoothCoord.z
			),
			mix(
				cornerNoise(vec3( ceilCoord.x,  ceilCoord.y, floorCoord.z), speed),
				cornerNoise(vec3( ceilCoord.x,  ceilCoord.y,  ceilCoord.z), speed),
				smoothCoord.z
			),
			smoothCoord.y
		),
		smoothCoord.x
	);
}

vec3 smoothHue(float h) {
	return sqrt(normalize(square(cos(h * TAU - vec3(0.0, 1.0, 2.0) * (TAU / 3.0)) * 0.5 + 0.5)));
}

float ign(vec2 coord) {
	return fract(dot(coord, vec2(3.555713358, 0.3092692451)));
}

const vec3[] beamDirections = vec3[](
	vec3(1.0, 0.0, 0.0),
	vec3(0.0, 1.0, 0.0),
	vec3(0.0, 0.0, 1.0),
	vec3(-1.0,  0.0,  0.0),
	vec3( 0.0, -1.0,  0.0),
	vec3( 0.0,  0.0, -1.0),
	vec3( 0.5773502692,  0.5773502692,  0.5773502692),
	vec3(-0.5773502692,  0.5773502692,  0.5773502692),
	vec3( 0.5773502692, -0.5773502692,  0.5773502692),
	vec3(-0.5773502692, -0.5773502692,  0.5773502692),
	vec3( 0.5773502692,  0.5773502692, -0.5773502692),
	vec3(-0.5773502692,  0.5773502692, -0.5773502692),
	vec3( 0.5773502692, -0.5773502692, -0.5773502692),
	vec3(-0.5773502692, -0.5773502692, -0.5773502692)
);

void main() {
	vec4 tmp = inverseProjection * vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
	vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);
	vec3 offsetNorm = norm * (square(ign(floor(gl_FragCoord.xy))) * 32.0) + cameraPosition.xyz;

	vec2 noise = vec2(0.0);
	noise += volumetricNoise(offsetNorm * 0.125, 0.0625) * 0.5;
	noise += volumetricNoise(offsetNorm * 0.25,  0.125 ) * 0.25;
	noise += volumetricNoise(offsetNorm * 0.5,   0.1875) * 0.125;
	noise += volumetricNoise(offsetNorm,         0.25  ) * 0.0625;
	noise += volumetricNoise(offsetNorm * 2.0,   0.3125) * 0.03125;
	noise += volumetricNoise(offsetNorm * 4.0,   0.375 ) * 0.015625;
	noise.x = 0.25 / (square(noise.x * 16.0 - 8.0) + 1.0);
	vec3 color = smoothHue(noise.y * 0.5 + 0.375);

	if (beamDirectionSeed >= 0.0) {
		vec3 relativeOrigin = cameraPosition.xyz - beamOrigin.xyz;
		vec3 beamDirection = beamDirections[int(beamDirectionSeed * float(beamDirections.length()))];
		vec3 proj = -relativeOrigin + beamDirection * dot(beamDirection, relativeOrigin);
		float projDist = length(proj);
		float alignment = dot(norm, beamDirection);
		float beamDistance = (
			dot(proj, norm) > 0.0
			? (
				square(determinant(mat3(proj, beamDirection, norm)))
				/
				(
					(1.0 - alignment * alignment)
					*
					projDist
				)
			)
			//projDist ^ 2 / projDist = projDist
			: projDist
		);
		float fractTime = clamp(beamTime * 4.0 + alignment / (64.0 * sqrt(1.0 - alignment * alignment)), 0.0, 1.0);
		float extraIntensity = sqrt(fractTime) * square(1.0 - fractTime) * (1.0 / (sqrt(0.2) * 0.64));
		extraIntensity = extraIntensity / (beamDistance / max(extraIntensity, 0.0001) + 1.0);
		noise.x = mix(noise.x, 1.0, extraIntensity);
		color = sqrt(mix(color * color, vec3(1.0, 0.75, 0.0), extraIntensity));
	}
	color = noise.x * mix(vec3(noise.x), vec3(2.0 - noise.x), color);
	fragColor = vec4(color, 1.0);
}