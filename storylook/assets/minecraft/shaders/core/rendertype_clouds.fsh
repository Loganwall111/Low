#version 330

#moj_import <minecraft:fog.glsl>


in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

// 1.9.152: soft volumetric cloud deck look — not the hard blocky MC sticker.
// Soft white mass with pale-blue fringe; edges feather via alpha.

void main() {
    vec4 color = vertexColor;
    // pull toward soft story-mode white with a cool fringe
    vec3 soft = mix(vec3(0.92, 0.94, 1.00), vec3(0.78, 0.84, 0.96), 0.18);
    color.rgb = mix(color.rgb, soft, 0.92);
    // feather alpha so the plane reads as a soft deck, not a glued sticker
    float fade = 1.0 - linear_fog_value(vertexDistance, 0, FogCloudsEnd);
    color.a *= fade * 0.88;
    if (color.a < 0.02) discard;
    fragColor = color;
}
