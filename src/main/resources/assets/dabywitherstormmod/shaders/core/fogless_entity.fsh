#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

// How much of vanilla's fog to apply. 0.0 = fully fogless (crisp), 0.5 = half fog. Set by the
// pipeline via a shader define; defaults to fogless if the pipeline forgets to set it.
#ifndef FOG_MIX
#define FOG_MIX 0.0
#endif

uniform sampler2D Sampler0;

#ifdef DISSOLVE
uniform sampler2D DissolveMaskSampler;
#endif

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif

#ifndef EMISSIVE
in vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
in vec4 overlayColor;
#endif

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

#ifdef PER_FACE_LIGHTING
    vec4 faceVertexColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
#else
    vec4 faceVertexColor = vertexColor;
#endif

#ifdef DISSOLVE
    if (faceVertexColor.a < texture(DissolveMaskSampler, texCoord0).a) {
        discard;
    }
    // The dissolve effect entirely replaces translucency
    faceVertexColor.a = 1.0;
#endif

    color *= faceVertexColor * ColorModulator;
#ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
#endif
#ifdef CHARCOAL_SKIN
    // MATTE CHARCOAL-INDIGO SKIN: near-black texels grade toward a dark
    // blue-violet charcoal instead of pure black, so the block mass keeps
    // detail in shadow with a soft dim sheen and no glossy highlight.
    float skinLuma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    float charcoal = 1.0 - smoothstep(0.0, 0.22, skinLuma);
    color.rgb = mix(color.rgb, color.rgb * vec3(0.72, 0.78, 1.12) + vec3(0.012, 0.012, 0.030), charcoal);
#endif
#ifndef EMISSIVE
    color *= lightMapColor;
#endif

    fragColor = mix(color, apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor), FOG_MIX);
}
