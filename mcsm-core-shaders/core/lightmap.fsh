#version 330

// MCSM_visuals lightmap override. In 26.2 this program BAKES the 16x16
// lightmap texture on the GPU from the LightmapInfo UBO alone (no sampler);
// terrain then samples the result. So the override keeps vanilla's exact
// contract (same UBO instance, texCoord mapping, notGamma/parabolic maths)
// and changes one thing only: the SKY channel gets the Story Mode time-of-day
// cast - cool blue at night, warm at dusk/dawn - mixed by SkyFactor. Block
// light is untouched, so torches stay vanilla. Under Sodium this program is
// not used; no crash, just vanilla light.

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

const vec3 LIGHT_DAY   = vec3(1.08, 0.98, 0.88);   // warm MCSM key (1.9.165)
const vec3 LIGHT_NIGHT = vec3(0.32, 0.48, 1.10);   // deep blue nights
const vec3 LIGHT_DUSK  = vec3(1.12, 0.68, 0.42);   // warmer dusk/dawn cast

void main() {
    float block_level = floor(texCoord.x * 16.0) / 15.0;
    float sky_level = floor(texCoord.y * 16.0) / 15.0;

    float block_brightness = get_brightness(block_level) * lightmapInfo.BlockFactor;
    float sky_brightness = get_brightness(sky_level) * lightmapInfo.SkyFactor;

    vec3 nightVisionColor = lightmapInfo.NightVisionColor * lightmapInfo.NightVisionFactor;
    vec3 color = max(lightmapInfo.AmbientColor * 0.82, nightVisionColor); // 1.9.165 deeper shade floor

    // Add sky light - MCSM: Story tint rides the sky channel only.
    float sf = clamp(lightmapInfo.SkyFactor, 0.0, 1.0);
    float duskW = pow(1.0 - abs(2.0 * sf - 1.0), 1.4) * 1.0;
    vec3 skyTint = mix(mix(LIGHT_NIGHT, LIGHT_DAY, sf), LIGHT_DUSK, duskW);
    color += lightmapInfo.SkyLightColor * sky_brightness * skyTint * 1.12;

    // Add block light (vanilla, untouched)
    vec3 BlockLightColor = mix(lightmapInfo.BlockLightTint, vec3(1.0), 0.9 * parabolicMixFactor(block_level));
    color += BlockLightColor * block_brightness;

    // Apply boss overlay darkening effect
    color = mix(color, color * vec3(0.7, 0.6, 0.6), lightmapInfo.BossOverlayWorldDarkeningFactor);

    // Apply darkness effect scale
    color = color - vec3(lightmapInfo.DarknessScale);

    // Apply brightness
    color = clamp(color, 0.0, 1.0);
    vec3 linearColor = notGamma(color);
    color = mix(color, linearColor, lightmapInfo.BrightnessFactor);

    fragColor = vec4(color, 1.0);
}
