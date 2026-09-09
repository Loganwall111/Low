#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:mcsm_visuals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 mcsmWorldPos;

out vec4 fragColor;

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    // Convert our UV back up to texel coordinates and find out how far over we are from the center of each pixel
    vec2 uvTexelCoords = uv / pixelSize;
    vec2 texelCenter = round(uvTexelCoords) - 0.5f;
    vec2 texelOffset = uvTexelCoords - texelCenter;

    // Move our offset closer to the texel center based on texel size on screen
    texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
    texelOffset = clamp(texelOffset, 0.0f, 1.0f);

    uv = (texelCenter + texelOffset) * pixelSize;
    return textureGrad(source, uv, du, dv);
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);
}

// Rotated Grid Super-Sampling
vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);

    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);

    float minPixelSize = min(pixelSize.x, pixelSize.y);

    float transitionStart = minPixelSize * 1.0;
    float transitionEnd = minPixelSize * 2.0;
    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);

    float duLength = length(du);
    float dvLength = length(dv);
    float minDerivative = min(duLength, dvLength);
    float maxDerivative = max(duLength, dvLength);

    float effectiveDerivative = sqrt(minDerivative * maxDerivative);

    float mipLevelExact = max(0.0, log2(effectiveDerivative / minPixelSize));

    float mipLevelLow = floor(mipLevelExact);
    float mipLevelHigh = mipLevelLow + 1.0;
    float mipBlend = fract(mipLevelExact);

    const vec2 offsets[4] = vec2[](
    vec2(0.125, 0.375),
    vec2(-0.125, -0.375),
    vec2(0.375, -0.125),
    vec2(-0.375, 0.125)
    );

    vec4 rgssColorLow = vec4(0.0);
    vec4 rgssColorHigh = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 sampleUV = uv + offsets[i] * pixelSize;
        rgssColorLow += textureLod(source, sampleUV, mipLevelLow);
        rgssColorHigh += textureLod(source, sampleUV, mipLevelHigh);
    }
    rgssColorLow *= 0.25;
    rgssColorHigh *= 0.25;

    vec4 rgssColor = mix(rgssColorLow, rgssColorHigh, mipBlend);

    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

void main() {
    // Vanilla 26.2 nearest/RGSS sampling path preserved exactly; MCSM pulls the
    // raw texel first so the crisp alpha policy runs on texture data.
    vec4 texColor = (UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, 1.0f / TextureSize) : sampleNearest(Sampler0, texCoord0, 1.0f / TextureSize));

    // MCSM stylization rule (spec §4): pixel-perfect blocky edges, no soft blur.
    if (texColor.a < 0.1) {
        discard;
    }

    vec4 color = texColor * vertexColor;
    color = mix(FogColor * vec4(1, 1, 1, color.a), color, ChunkVisibility);

    // MCSM 1.9.107: fake emissive material response for torches, lava, redstone,
    // sea-lantern/beacon-style pixels and other bright accents. Core shaders do
    // not know block IDs here, so this keys off the sampled texel colour; it
    // makes the source block glow vividly without washing the whole scene.
    float mxC = max(color.r, max(color.g, color.b));
    float mnC = min(color.r, min(color.g, color.b));
    float satC = mxC - mnC;
    float warmEmit = smoothstep(0.46, 0.95, color.r) * smoothstep(0.18, 0.75, color.g) * (1.0 - smoothstep(0.50, 0.92, color.b));
    float coolEmit = smoothstep(0.50, 0.95, max(color.g, color.b)) * smoothstep(0.12, 0.55, color.r);
    float redEmit  = smoothstep(0.45, 0.95, color.r) * (1.0 - smoothstep(0.25, 0.55, color.g));
    float emit = clamp(max(max(warmEmit, coolEmit), redEmit) * smoothstep(0.16, 0.55, satC) * texColor.a, 0.0, 1.0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

    float mcsmP = mcsm_phase(FogSkyEnd, FogColor, FogRenderDistanceEnd);

    // ================= MCSM v8: ALWAYS-ON ground lighting ==================
    // Sun/moon shading and cloud shadows used to live below the storm gate,
    // so they only existed once the Wither Storm was out. They now run in
    // ordinary vanilla play too, which is what makes the world read like the
    // Story Mode reference frames before anything has gone wrong.
    vec3 nrm = normalize(cross(dFdx(mcsmWorldPos), dFdy(mcsmWorldPos)));
    vec3 camW = vec3(CameraBlockPos) + CameraOffset;
    if (dot(nrm, camW - mcsmWorldPos) < 0.0) {
        nrm = -nrm;
    }
    float upFace = clamp(nrm.y, 0.0, 1.0);
    vec3  sunT   = mcsm_sun_true(GameTime);
    float clockS = mcsm_clock(GameTime);

    // directional key light (sun by day, dim moon by night)
    float ndlA   = dot(nrm, sunT.y >= 0.0 ? sunT : -sunT);
    float crispA = mix(ndlA, step(-0.05, ndlA), 0.75);
    float keyA   = sunT.y >= 0.0 ? 1.0 : 0.42;
    color.rgb *= mix(1.0, clamp(0.58 + 0.42 * crispA, 0.0, 1.0), keyA);

    // clouds / cinematic tree-like bands cast moving shape onto the ground
    color.rgb *= mcsm_cloud_shadow(mcsmWorldPos, sunT, clockS, upFace);

    // local emissive lift after shadows: torches/glow blocks keep their colour
    // and read like little Story Mode light sources.
    color.rgb += color.rgb * emit * 0.55 + vec3(1.0, 0.72, 0.38) * warmEmit * emit * 0.22
               + vec3(0.35, 0.95, 1.00) * coolEmit * emit * 0.16
               + vec3(1.00, 0.12, 0.08) * redEmit  * emit * 0.18;

    // late-phase sun burns hotter and spills warm light onto up-facing ground
    float sunUpA = clamp(sunT.y * 3.0, 0.0, 1.0);
    float intenA = mcsm_sun_intensity(mcsmP);
    color.rgb += mcsm_sun_glow_color(mcsmP) * (intenA - 1.0) * 0.11 * upFace * sunUpA;

    if (!mcsm_fog_active(mcsmP)) {
        color.rgb = mcsm_story_grade(color.rgb);
        fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
        return;
    }

    // ---- MCSM faked moving shadows (spec §4, no depth map) ----
    // 26.2 terrain carries no Normal attribute, so reconstruct the face
    // normal per pixel from the world-position derivatives: constant on each
    // axis-aligned block face => crisp, blocky shading by construction.
    // (base sun/moon key + cloud shadows already applied above for every
    //  frame; the storm pass only adds its own occlusion on top.)
    vec3 n = nrm;
    vec3 camWorld = camW;

    // ---- MCSM storm occlusion: the ground under the storm column goes
    // dark, ringed by a faint rim of its glare colour (the pack's "shadow on
    // the ground", no depth map). Strongest on up-facing faces.
    vec4 mcsmAim = mcsm_boss_dir(camWorld);
    if (mcsmAim.w > 0.5) {
        vec3 toStorm = mcsmWorldPos - mcsmAim.xyz;
        toStorm.y = 0.0;
        float sdist = length(toStorm);
        float column = 1.0 - 0.48 * exp(-pow(sdist / 95.0, 2.0));
        float rim = 0.18 * exp(-pow(max(sdist - 130.0, 0.0) / 45.0, 2.0)) * n.y;
        float upf = clamp(0.4 + 0.6 * n.y, 0.0, 1.0);
        color.rgb *= mix(1.0, column, upf);
        color.rgb += mcsm_blob_color(mcsmP, mcsm_clock(GameTime)) * rim * 0.35;
    }

    // ---- multi-phase fog: colour blend + "denser teal" density layer ----
    vec3 fogRGB = mcsm_fog_color(mcsmP, FogColor.rgb);
    float fogv = clamp(total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, mcsm_rd_start(), FogRenderDistanceEnd) * mcsm_fog_density(mcsmP), 0.0, 1.0);
    fragColor = vec4(mcsm_story_grade(mix(color.rgb, fogRGB, fogv * FogColor.a)), color.a);
}
