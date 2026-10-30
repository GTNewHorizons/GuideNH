#version 120

uniform sampler2D uTexture;
uniform sampler2D uSceneDepth;
uniform vec2 uTexel;
uniform vec4 uDepthRect;
uniform float uSlot;
uniform float uCutout;

varying vec2 vUv;

const float ALPHA_CUTOFF = 0.1;
const float DEPTH_BIAS = 0.00002;

void main() {
    if (uCutout > 0.5 && texture2D(uTexture, vUv).a < ALPHA_CUTOFF) {
        discard;
    }
    float visible = 1.0;
    vec2 pixel = gl_FragCoord.xy;
    if (pixel.x >= uDepthRect.x && pixel.y >= uDepthRect.y && pixel.x < uDepthRect.z && pixel.y < uDepthRect.w) {
        float sceneDepth = texture2D(uSceneDepth, pixel * uTexel).r;
        visible = gl_FragCoord.z <= sceneDepth + DEPTH_BIAS ? 1.0 : 0.0;
    }
    gl_FragColor = vec4(uSlot, visible, 0.0, 1.0);
}
