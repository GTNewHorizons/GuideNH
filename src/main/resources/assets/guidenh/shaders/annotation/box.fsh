#version 120

uniform vec4 uViewport;
uniform float uPhase;
uniform vec2 uColorOffsets;
uniform vec4 uBoxShape;
uniform int uFilterBoxSurface;
uniform int uRainbow;
varying vec4 vColor;
varying vec2 vColorCenter;
varying vec3 vBoxPosition;
varying vec3 vBoxRay;

#include <annotation_color>
#include <annotation_box_surface>

void main() {
    if (vColor.a <= 0.0) discard;
    if (uFilterBoxSurface != 0 && !annotationBoxNearestSurface(vBoxPosition, vBoxRay, uBoxShape.xyz, uBoxShape.w)) discard;
    if (uRainbow == 0) {
        gl_FragColor = vColor;
        return;
    }
    vec2 uv = (gl_FragCoord.xy - uViewport.xy) / uViewport.zw;
    vec2 direction = (uv - vColorCenter) * uViewport.zw;
    direction.y = -direction.y;
    vec3 color = clamp(annotationRainbow(direction, uPhase) + uColorOffsets.x, 0.0, 1.0);
    color = clamp(color + uColorOffsets.y, 0.0, 1.0);
    gl_FragColor = vec4(color * vColor.rgb, vColor.a);
}
