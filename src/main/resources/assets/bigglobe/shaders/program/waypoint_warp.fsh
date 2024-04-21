#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform mat4 ActualProjMat;

uniform int bigglobe_waypoint_count;
//satin does not expose array-type uniforms, so I need 16 different uniforms instead.
uniform vec4
	bigglobe_waypoint_0,
	bigglobe_waypoint_1,
	bigglobe_waypoint_2,
	bigglobe_waypoint_3,
	bigglobe_waypoint_4,
	bigglobe_waypoint_5,
	bigglobe_waypoint_6,
	bigglobe_waypoint_7,
	bigglobe_waypoint_8,
	bigglobe_waypoint_9,
	bigglobe_waypoint_10,
	bigglobe_waypoint_11,
	bigglobe_waypoint_12,
	bigglobe_waypoint_13,
	bigglobe_waypoint_14,
	bigglobe_waypoint_15;

in vec2 texCoord;

out vec4 fragColor;

float square(float f) {
	return f * f;
}

void warp(inout vec3 uv, in vec4 waypoint, in vec3 norm) {
	float distanceFromOriginToClosest = dot(waypoint.xyz, norm);
	if (distanceFromOriginToClosest > 0.0 || dot(waypoint.xyz, waypoint.xyz) < 1.0) {
		vec3 closest = norm * distanceFromOriginToClosest;
		float distanceFromClosestToWaypoint = distance(closest, waypoint.xyz);
		if (distanceFromClosestToWaypoint < 1.0) {
			vec4 screenSpaceWaypoint = ActualProjMat * vec4(waypoint.xyz, 1.0);
			screenSpaceWaypoint.xyz = screenSpaceWaypoint.xyz / screenSpaceWaypoint.w * 0.5 + 0.5;
			if (screenSpaceWaypoint.z < uv.z) {
				uv.xy = mix(uv.xy, screenSpaceWaypoint.xy, square(1.0 - distanceFromClosestToWaypoint) * 4.0 * waypoint.w);
			}
		}
	}
}

void main() {
	vec3 uv = vec3(texCoord, texture(DiffuseDepthSampler, texCoord).r);
	vec4 tmp = inverse(ActualProjMat) * vec4(uv * 2.0 - 1.0, 1.0);
	vec3 norm = normalize(tmp.xyz);
	//aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
	if (bigglobe_waypoint_count > 0) {
		warp(uv, bigglobe_waypoint_0, norm);
		if (bigglobe_waypoint_count > 1) {
			warp(uv, bigglobe_waypoint_1, norm);
			if (bigglobe_waypoint_count > 2) {
				warp(uv, bigglobe_waypoint_2, norm);
				if (bigglobe_waypoint_count > 3) {
					warp(uv, bigglobe_waypoint_3, norm);
					if (bigglobe_waypoint_count > 4) {
						warp(uv, bigglobe_waypoint_4, norm);
						if (bigglobe_waypoint_count > 5) {
							warp(uv, bigglobe_waypoint_5, norm);
							if (bigglobe_waypoint_count > 6) {
								warp(uv, bigglobe_waypoint_6, norm);
								if (bigglobe_waypoint_count > 7) {
									warp(uv, bigglobe_waypoint_7, norm);
									if (bigglobe_waypoint_count > 8) {
										warp(uv, bigglobe_waypoint_8, norm);
										if (bigglobe_waypoint_count > 9) {
											warp(uv, bigglobe_waypoint_9, norm);
											if (bigglobe_waypoint_count > 10) {
												warp(uv, bigglobe_waypoint_10, norm);
												if (bigglobe_waypoint_count > 11) {
													warp(uv, bigglobe_waypoint_11, norm);
													if (bigglobe_waypoint_count > 12) {
														warp(uv, bigglobe_waypoint_12, norm);
														if (bigglobe_waypoint_count > 13) {
															warp(uv, bigglobe_waypoint_13, norm);
															if (bigglobe_waypoint_count > 14) {
																warp(uv, bigglobe_waypoint_14, norm);
																if (bigglobe_waypoint_count > 15) {
																	warp(uv, bigglobe_waypoint_15, norm);
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	fragColor = texture(DiffuseSampler, uv.xy);
}