#version 330

layout(std140) uniform LightmapInfo {
    float SkyFactor;
    float BlockFactor;
    float NightVisionFactor;
    float DarknessScale;
    float BossOverlayWorldDarkeningFactor;
    float BrightnessFactor;
    vec3 BlockLightTint;
    vec3 SkyLightColor;
    vec3 AmbientColor;
    vec3 NightVisionColor;
} lightmapInfo;

in vec2 texCoord;

out vec4 fragColor;

float get_brightness(float level) {
    return level / (4.0 - 3.0 * level);
}

vec3 notGamma(vec3 color) {
    float maxComponent = max(max(color.x, color.y), color.z);
    float maxInverted = 1.0f - maxComponent;
    float maxScaled = 1.0f - maxInverted * maxInverted * maxInverted * maxInverted;
    return color * (maxScaled / maxComponent);
}

float parabolicMixFactor(float level) {
    return (2.0 * level - 1.0) * (2.0 * level - 1.0);
}

// ---------------------------------------------------------------------------
// Story Look lighting: vanilla 26.2 lightmap, plus (a) a soft ambient floor
// on sky-lit surfaces so outdoor shade keeps the reference's readable,
// never-black shadows (fully unlit caves stay dark), and (b) a cool
// lavender tilt in sky shadow, matching the screenshots' shadow colour.
// The LightmapInfo UBO layout is copied verbatim from vanilla.
// ---------------------------------------------------------------------------

void main() {
    // Calculate block and sky brightness levels based on texture coordinates
    float block_level = floor(texCoord.x * 16) / 15;
    float sky_level = floor(texCoord.y * 16) / 15;

    float block_brightness = get_brightness(block_level) * lightmapInfo.BlockFactor;
    // Story Mode torches throw real light: wider, hotter falloff so a torch
    // reads as a source, not a decal.
    block_brightness *= 1.0 + 0.40 * smoothstep(0.15, 0.85, block_level);
    float sky_brightness = get_brightness(sky_level) * lightmapInfo.SkyFactor;

    // Story Look: soft shadow floor, scaled by day strength and gated so
    // sky_level 0 (caves, interiors) keeps vanilla darkness.
    sky_brightness += 0.16 * lightmapInfo.SkyFactor
                    * smoothstep(0.0, 0.35, sky_level)
                    * (1.0 - sky_brightness);

    // Calculate ambient color with or without night vision
    vec3 nightVisionColor = lightmapInfo.NightVisionColor * lightmapInfo.NightVisionFactor;
    vec3 color = max(lightmapInfo.AmbientColor, nightVisionColor);

    // Add sky light, with the cool shadow tint at low sky levels
    vec3 skyTint = mix(vec3(1.0), vec3(0.93, 0.92, 1.08), (1.0 - sky_level) * 0.6);
    color += lightmapInfo.SkyLightColor * skyTint * sky_brightness;

    // Add block light
    vec3 BlockLightColor = mix(lightmapInfo.BlockLightTint, vec3(1.0), 0.9 * parabolicMixFactor(block_level));
    BlockLightColor *= vec3(1.06, 0.97, 0.86);
    color += BlockLightColor * block_brightness;

    // Apply boss overlay darkening effect
    color = mix(color, color * vec3(0.7, 0.6, 0.6), lightmapInfo.BossOverlayWorldDarkeningFactor);

    // Apply darkness effect scale
    color = color - vec3(lightmapInfo.DarknessScale);

    // Apply brightness
    color = clamp(color, 0.0, 1.0);
    vec3 notGamma = notGamma(color);
    color = mix(color, notGamma, lightmapInfo.BrightnessFactor);

    fragColor = vec4(color, 1.0);
}
