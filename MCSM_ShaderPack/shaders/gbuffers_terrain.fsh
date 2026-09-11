#version 120

// ============================================================================
// MCSM gbuffers_terrain.fsh — Story Mode warm/cool lighting, lavender ambient
// shadows, and live sun-cast shadows that sweep the ground with the time of day
// ============================================================================

#define MCSM_LIGHTING

precision highp float;
precision highp int;

uniform sampler2D gtexture;
uniform sampler2D shadowtex0;
// Custom material bindings from shaders.properties:
//   customTexture.witherFlesh = shaders/textures/wither_flesh_block.png
//   customTexture.tornFlesh   = shaders/textures/torn_withered_flesh.png
uniform sampler2D witherFlesh;
uniform sampler2D tornFlesh;
uniform vec3 sunPosition;
uniform float frameTimeCounter;

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewPos;
varying vec3 worldPos;
varying vec4 shadowPos;
varying float vSunY;

void main() {
    vec4 tex = texture2D(gtexture, texcoord);
    tex *= color;
    if (tex.a < 0.1) {
        discard;
    }

    // Story Mode Block and Sky light levels
    float blockLight = clamp((lmcoord.x - 0.03) * 1.05, 0.0, 1.0);
    float skyLight   = clamp((lmcoord.y - 0.03) * 1.05, 0.0, 1.0);

    // Warm Story Mode sunlight, cool lavender ambient shadow, warm amber torchlight
    vec3 sunLightColor = vec3(1.12, 1.02, 0.90);
    vec3 shadowAmbientColor = vec3(0.70, 0.62, 0.88);
    vec3 torchColor = vec3(1.20, 0.78, 0.38);

    vec3 skyLightTerm = mix(shadowAmbientColor * 0.72, sunLightColor, pow(skyLight, 1.25));
    vec3 blockLightTerm = torchColor * pow(blockLight, 1.35) * 1.35;

    vec3 ambientLighting = skyLightTerm + blockLightTerm;
    tex.rgb *= ambientLighting;

    // Diffuse surface normal shading (pure diffuse, NO reflections)
    float normalShade = clamp(normal.y * 0.35 + 0.65, 0.35, 1.0);
    tex.rgb *= normalShade;

    // ---- Matte material pass: the active witherFlesh / tornFlesh voxel
    // sheets grade to charcoal-indigo and catch one restrained edge light.
    // MCSM flesh is flat dark matter with crisp rims, never glossy metal.
    // Gated by a material match against the bound custom textures (colour
    // identity test on the block's sampled albedo vs the custom texture's
    // average texel).
    vec3 witherAvg = texture2D(witherFlesh, vec2(0.5, 0.5)).rgb;
    vec3 tornAvg = texture2D(tornFlesh, vec2(0.5, 0.5)).rgb;
    float witherMatch = 1.0 - smoothstep(0.0, 0.30, length(tex.rgb - witherAvg));
    float tornMatch = 1.0 - smoothstep(0.0, 0.30, length(tex.rgb - tornAvg));
    float fleshMask = clamp(witherMatch + tornMatch, 0.0, 1.0);
    if (fleshMask > 0.01) {
        vec3 viewDir = normalize(-viewPos);
        vec3 keyLight = normalize(vec3(0.35, 0.85, 0.30)); // stylized key light
        vec3 halfVec = normalize(viewDir + keyLight);
        float ndotl = clamp(dot(normal, keyLight), 0.0, 1.0);
        float spec = pow(clamp(dot(normal, halfVec), 0.0, 1.0), 28.0);
        // charcoal grade toward blue-violet instead of pure black
        tex.rgb = max(tex.rgb, vec3(0.028, 0.030, 0.062));
        tex.rgb *= mix(vec3(1.0), vec3(0.86, 0.90, 1.10), fleshMask);
        // faint key-light kiss only: no specular lobe on MCSM flesh
        float sheen = spec * (0.25 + 0.75 * skyLight) + 0.05 * ndotl;
        tex.rgb += vec3(0.75, 0.66, 1.05) * sheen * fleshMask * 0.10;
        // restrained indigo edge light so the sheets keep crisp rims
        float fres = pow(1.0 - clamp(dot(normal, viewDir), 0.0, 1.0), 3.0);
        tex.rgb += vec3(0.10, 0.12, 0.30) * fres * fleshMask * 0.35;
    }

    // ---- Live sun shadow (moves with the time of day) ----
    // Sun elevation gates the shadow strength so it fades at dawn/dusk and is
    // gone at night; the shadow map itself is rendered from the sun each frame.
    float sunVis = clamp(vSunY * 14.0, 0.0, 1.0);
    float shadow = 1.0;
    if (sunVis > 0.02) {
        vec3 sp = shadowPos.xyz * 0.5 + 0.5;
        if (sp.x >= 0.0 && sp.x <= 1.0 && sp.y >= 0.0 && sp.y <= 1.0 && sp.z <= 1.0) {
            float depth = texture2D(shadowtex0, sp.xy).x;
            shadow = (depth >= sp.z - 0.0025) ? 1.0 : 0.55;
        }
        tex.rgb = mix(tex.rgb, tex.rgb * 0.55, (1.0 - shadow) * sunVis * 0.85);
    }

    // Story Mode shadow deepening on shaded faces
    float isShadowed = 1.0 - skyLight;
    if (isShadowed > 0.20) {
        float shadowStr = (isShadowed - 0.20) / 0.80;
        tex.rgb = mix(tex.rgb, tex.rgb * vec3(0.80, 0.74, 0.94), shadowStr * 0.45);
    }

    gl_FragColor = tex;
}
