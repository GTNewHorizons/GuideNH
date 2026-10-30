#version 120

varying vec2 vUv;

void main() {
    gl_Position = ftransform();
    vUv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
}
