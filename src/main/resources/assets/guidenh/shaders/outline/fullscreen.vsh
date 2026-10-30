#version 120

varying vec2 vUv;

void main() {
    gl_Position = gl_Vertex;
    vUv = gl_MultiTexCoord0.st;
}
