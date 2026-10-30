#version 120

uniform sampler2D uMask;
uniform sampler2D uStyles;
uniform vec2 uTexel;
uniform vec2 uMaskColumns;
uniform int uRadius;

varying vec2 vUv;

const float STYLE_SLOTS = 256.0;
const float STYLE_COLUMNS = 4.0;
const float THICKNESS_COLUMN = 2.0;

vec4 mask(float u) {
    if (u < uMaskColumns.x || u >= uMaskColumns.y) {
        return vec4(0.0);
    }
    return texture2D(uMask, vec2(u, vUv.y));
}

void main() {
    vec4 hit = vec4(0.0);
    float nearest = 0.0;
    for (int d = 0; d <= uRadius; d++) {
        float offset = float(d) * uTexel.x;
        hit = mask(vUv.x - offset);
        if (hit.r == 0.0) {
            hit = mask(vUv.x + offset);
        }
        if (hit.r > 0.0) {
            nearest = float(d);
            break;
        }
    }
    if (hit.r == 0.0) {
        gl_FragColor = vec4(0.0);
    } else {
        float slot = floor(hit.r * 255.0 + 0.5);
        vec2 styleUv = vec2((THICKNESS_COLUMN + 0.5) / STYLE_COLUMNS, (slot + 0.5) / STYLE_SLOTS);
        float thickness = max(texture2D(uStyles, styleUv).r, 1.0 / 255.0);
        gl_FragColor = vec4(hit.r, hit.g, nearest / 255.0, thickness);
    }
}
