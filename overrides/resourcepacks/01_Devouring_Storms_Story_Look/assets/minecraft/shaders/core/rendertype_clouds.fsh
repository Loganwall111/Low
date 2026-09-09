#version 330

#moj_import <minecraft:fog.glsl>


in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

// Story Look: the near cloud deck reads pure white in the reference shots;
// vanilla tints it toward the sky. Fog fade behaviour is unchanged.

void main() {
    vec4 color = vertexColor;
    color.rgb = mix(color.rgb, vec3(1.0), 0.85);
    color.a *= 1.0f - linear_fog_value(vertexDistance, 0, FogCloudsEnd);
    fragColor = color;
}
