#version 150

uniform mat4 ProjMatInverse;
uniform mat4 ModelViewInverse;
uniform vec3 cameraPosition;
uniform float time;

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

vec3 smoothify(vec3 v) {
	return v * v * (v * -2.0 + 3.0);
}

vec2 hash21(float p) {
	vec3 p3 = fract(vec3(p) * vec3(0.1031, 0.1030, 0.0973));
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.xx + p3.yz) * p3.zy);
}

vec3 unitVec(float seed) {
	vec2 surface = hash21(seed) * vec2(2.0, TAU) - vec2(1.0, 0.0);
	float r = sqrt(1.0 - surface.x * surface.x);
	return vec3(vec2(cos(surface.y), sin(surface.y)) * r, surface.x);
}

vec2 hash23(vec3 p3) {
	p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.xx + p3.yz) * p3.zy);
}

vec2 noise23(vec3 coord) {
	vec3  fractCoord = fract(coord);
	vec3  floorCoord = coord - fractCoord;
	vec3   ceilCoord = floorCoord + vec3(1.0);
	vec3 smoothCoord = smoothify(fractCoord);

	return mix(
		mix(
			mix(
				hash23(vec3(floorCoord.x, floorCoord.y, floorCoord.z)),
				hash23(vec3(floorCoord.x, floorCoord.y,  ceilCoord.z)),
				smoothCoord.z
			),
			mix(
				hash23(vec3(floorCoord.x,  ceilCoord.y, floorCoord.z)),
				hash23(vec3(floorCoord.x,  ceilCoord.y,  ceilCoord.z)),
				smoothCoord.z
			),
			smoothCoord.y
		),
		mix(
			mix(
				hash23(vec3( ceilCoord.x, floorCoord.y, floorCoord.z)),
				hash23(vec3( ceilCoord.x, floorCoord.y,  ceilCoord.z)),
				smoothCoord.z
			),
			mix(
				hash23(vec3( ceilCoord.x,  ceilCoord.y, floorCoord.z)),
				hash23(vec3( ceilCoord.x,  ceilCoord.y,  ceilCoord.z)),
				smoothCoord.z
			),
			smoothCoord.y
		),
		smoothCoord.x
	);
}

vec2 backgroundNoise(vec3 norm) {
	vec2 noise = noise23(norm);
	return vec2(abs(noise.x - 0.5), noise.y);
}

vec3 smoothHue(float h) {
	return sqrt(normalize(square(cos(h * TAU - vec3(0.0, 1.0, 2.0) * (TAU / 3.0)) * 0.5 + 0.5)));
}

vec4 hash42(vec2 p) {
	vec4 p4 = fract(p.xyxy * vec4(0.1031, 0.1030, 0.0973, 0.1099));
	p4 += dot(p4, p4.wzxy + 33.33);
	return fract((p4.xxyz + p4.yzzw) * p4.zywx);
}

void main() {
	vec4 tmp = ProjMatInverse * vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
	vec3 norm = normalize(mat3(ModelViewInverse) * tmp.xyz);
	vec3 offsetNorm = norm + cameraPosition * 0.015625;

	vec2 noise = vec2(0.0);
	noise += backgroundNoise(offsetNorm *  2.0) * 0.5;
	noise += backgroundNoise(offsetNorm *  4.0) * 0.25;
	noise += backgroundNoise(offsetNorm *  8.0) * 0.125;
	noise += backgroundNoise(offsetNorm * 16.0) * 0.0625;
	noise += backgroundNoise(offsetNorm * 32.0) * 0.03125;
	noise += backgroundNoise(offsetNorm * 64.0) * 0.015625;

	vec3 color = mix(smoothHue(noise.y * 0.5 + 0.375), vec3(1.0), noise.x) * noise.x;

	vec3 starSum = vec3(0.0);

	for (int planeIndex = 1; planeIndex <= 16; planeIndex++) {
		vec3 axis1 = unitVec( float(planeIndex));
		vec3 axis2 = unitVec(-float(planeIndex));
		axis2 = normalize(axis2 - axis1 * dot(axis1, axis2));
		vec2 planePos = vec2(dot(norm, axis1), dot(norm, axis2));
		float planeIntensity = 1.0 - dot(planePos, planePos);
		vec2 scaledPlanePos = planePos * 16.0;
		vec2 fractPos = fract(scaledPlanePos);
		vec2 floorPos = scaledPlanePos - fractPos;
		vec4 starData = hash42(floorPos);
		vec2  starPos = mix(starData.xy, vec2(0.5), starData.z);
		float starIntensity = max(1.0 - 2.0 * distance(fractPos, starPos) / starData.z, 0.0);
		starIntensity = square(square(square(starIntensity)));
		starIntensity *= sin(time + starData.w * TAU) * 0.5 + 0.5;
		vec3 starColor = exp2((starData.z * 8.0 - 7.0) * (vec3(1.0, 2.0, 4.0) / 4.0));
		starSum += starColor * starIntensity * planeIntensity;
	}
	starSum *= noise.x;

	color += starSum * 4.0;
	float distanceFromOrigin = length(cameraPosition);
	color *= exp2((dot(norm, cameraPosition) / distanceFromOrigin * -0.5 - 0.5) * distanceFromOrigin * 0.03125);

	fragColor = vec4(color, 1.0);
}