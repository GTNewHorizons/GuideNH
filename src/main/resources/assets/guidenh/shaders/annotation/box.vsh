#version 120

uniform vec4 uColorCenter;
uniform vec4 uBoxColor;
varying vec4 vColor;
varying vec2 vColorCenter;
varying vec3 vBoxPosition;
varying vec3 vBoxRay;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    vColor = gl_Color * uBoxColor;
    vBoxPosition = gl_Vertex.xyz - uColorCenter.xyz;
    vBoxRay = -vec3(gl_ModelViewMatrix[0].z, gl_ModelViewMatrix[1].z, gl_ModelViewMatrix[2].z);
    vec4 center = gl_ModelViewProjectionMatrix * uColorCenter;
    vColorCenter = center.xy / center.w * 0.5 + 0.5;
}
