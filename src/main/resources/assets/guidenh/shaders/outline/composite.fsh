#version 120

uniform sampler2D uSpans;
uniform sampler2D uStyles;
uniform vec2 uTexel;
uniform vec2 uSpanRows;
uniform int uRadius;

varying vec2 vUv;

const float STYLE_SLOTS = 256.0;
const float STYLE_COLUMNS = 4.0;
const float THICKNESS_SCALE = 8.0;

vec4 span(float dy) {
    float v = vUv.y + dy * uTexel.y;
    if (v < uSpanRows.x || v >= uSpanRows.y) {
        return vec4(0.0);
    }
    return texture2D(uSpans, vec2(vUv.x, v));
}

float coverage(vec4 hit, float dy) {
    if (hit.a == 0.0) {
        return 0.0;
    }
    float dx = hit.b * 255.0;
    float thickness = hit.a * 255.0 / THICKNESS_SCALE;
    return clamp(thickness + 1.0 - sqrt(dx * dx + dy * dy), 0.0, 1.0);
}

void main() {
    vec4 best = span(0.0);
    if (best.a > 0.0 && best.b == 0.0) {
        discard;
    }
    float bestCoverage = coverage(best, 0.0);
    for (int d = 1; d <= uRadius; d++) {
        if (bestCoverage >= 1.0) {
            break;
        }
        float dy = float(d);
        vec4 below = span(-dy);
        float belowCoverage = coverage(below, dy);
        if (belowCoverage > bestCoverage) {
            bestCoverage = belowCoverage;
            best = below;
        }
        vec4 above = span(dy);
        float aboveCoverage = coverage(above, dy);
        if (aboveCoverage > bestCoverage) {
            bestCoverage = aboveCoverage;
            best = above;
        }
    }
    if (bestCoverage <= 0.0) {
        discard;
    }
    float slot = floor(best.r * 255.0 + 0.5);
    float column = best.g > 0.5 ? 0.0 : 1.0;
    vec4 color = texture2D(uStyles, vec2((column + 0.5) / STYLE_COLUMNS, (slot + 0.5) / STYLE_SLOTS));
    gl_FragColor = vec4(color.rgb, color.a * bestCoverage);
}
