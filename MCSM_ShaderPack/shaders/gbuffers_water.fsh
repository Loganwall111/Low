#version 120

// ============================================================================
// MCSM gbuffers_water.fsh — bright translucent Story Mode water. The sun's cast
// shadow drifts across the surface as the sun moves through the day/night
// cycle, and the surface carries a faint animated shimmer.
// ============================================================================

precision highp float;
precision highp int;

uniform sampler2D gtexture;
uniform sampler2D shadowtex0;
uniform vec3 sunPosition;
uniform float frameTimeCounter;

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 worldPos;
varying vec4 shadowPos;
varying float vSunY;

void main() {
    vec4 tex = texture2D(gtexture, texcoord);
    tex *= color;
    if (tex.a < 0.05) {
        discard;
    }

    float blockLight = clamp((lmcoord.x - 0.03) * 1.05, 0.0, 1.0);
    float skyLight   = clamp((lmcoord.y - 0.03) * 1.05, 0.0, 1.0);

    vec3 sunLightColor = vec3(1.20, 1.10, 1.00);
    vec3 ambientColor = vec3(0.80, 0.87, 1.00);
    vec3 lighting = mix(ambientColor * 0.85, sunLightColor, pow(skyLight, 1.3));
    lighting += vec3(1.15, 0.75, 0.38) * pow(blockLight, 1.4) * 0.8;
    tex.rgb *= lighting;

    // ---- Live sun shadow sweeping the water surface ----
    float sunVis = clamp(vSunY * 14.0, 0.0, 1.0);
    if (sunVis > 0.02) {
        vec3 sp = shadowPos.xyz * 0.5 + 0.5;
        if (sp.x >= 0.0 && sp.x <= 1.0 && sp.y >= 0.0 && sp.y <= 1.0 && sp.z <= 1.0) {
            float depth = texture2D(shadowtex0, sp.xy).x;
            float shadow = (depth >= sp.z - 0.0030) ? 1.0 : 0.72;
            tex.rgb = mix(tex.rgb, tex.rgb * vec3(0.72, 0.78, 0.94), (1.0 - shadow) * sunVis * 0.80);
        }
    }

    // Faint moving shimmer so the surface reads as alive
    float shim = 0.96 + 0.05 * sin(worldPos.x * 0.31 + frameTimeCounter * 0.9) *
                          sin(worldPos.z * 0.27 - frameTimeCounter * 0.7);
    tex.rgb *= shim;

    gl_FragColor = tex;
}
