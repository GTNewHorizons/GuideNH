vec3 annotationRainbow(vec2 direction, float phase) {
    float angle = dot(direction, direction) > 0.000001 ? atan(direction.y, direction.x) : 0.0;
    float hue = fract(-angle / 6.28318530718 - phase);
    vec3 rgb = clamp(abs(fract(hue + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
    return mix(vec3(1.0), rgb, 0.65);
}
