#version 330

#if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)
#moj_import <minecraft:light.glsl>
#endif
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

#ifndef NO_OVERLAY
uniform sampler2D Sampler1;
#endif

#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

out float sphericalVertexDistance;
out float cylindricalVertexDistance;

#ifdef PER_FACE_LIGHTING
out vec4 vertexPerFaceColorBack;
out vec4 vertexPerFaceColorFront;
#else
out vec4 vertexColor;
#endif

#ifndef EMISSIVE
out vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
out vec4 overlayColor;
#endif

out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

#ifdef PER_FACE_LIGHTING
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, Normal);
    vertexPerFaceColorBack = minecraft_mix_light_separate(-light, Color);
    vertexPerFaceColorFront = minecraft_mix_light_separate(light, Color);
#elif defined(NO_CARDINAL_LIGHTING)
    vertexColor = Color;
#elif defined(REVERSE_SHADING)
    // REVERSE SHADING. Vanilla lights every entity from above-front: top faces bright, bottom
    // faces dark. At night the storm is lit by the glow BEHIND it, so that is backwards --
    // the faces turned toward you should be the dark ones and the far/underside should catch
    // the light. Negating the normal before the light calculation flips exactly that, without
    // touching brightness, tint or anything else about the model.
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, -Normal, Color);
#else
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
#endif

#ifdef STORM_SHADING
    // MODEL SHADING -- hemisphere ambient, on top of whatever lighting ran above.
    //
    // Vanilla lights an entity with two fixed directional lamps and nothing else. That is fine for
    // a cow. On a storm built from thousands of cubes it is close to flat: every face pointing the
    // same way gets the same value however deep inside a crevice it sits, so a mass forty blocks
    // across reads as a silhouette rather than as something with form.
    //
    // A hemisphere term fixes most of that for one dot product. Real ambient light is not uniform:
    // it comes overwhelmingly from the sky, so an upward face catches far more of it than a
    // downward one. Grading between the two by the normal's Y darkens undersides, overhangs and
    // the inside of every gap in the mass, which is what the eye reads as self-shadowing -- and
    // it costs one multiply per vertex, no shadow map, no extra pass, nothing to sample.
    //
    // Deliberately centred rather than a straight darkening: side faces come out near where they
    // started, so the model gains depth without simply getting dimmer.
    float upness = Normal.y * 0.5 + 0.5;      // 0 straight down, 1 straight up
    float hemisphere = mix(0.42, 1.22, upness * upness * (3.0 - 2.0 * upness));

#if defined(SUN_X) && defined(SUN_Y) && defined(SUN_Z)
    // THE SUN ITSELF. Baked in as a constant by the pipeline (see FoglessRenderTypes.sunVector):
    // the face turned toward it is bright, the face turned away is not, and it swings round as
    // the day does. This is the difference between a model that is shaded and a model that is lit.
    vec3 sunDir = normalize(vec3(SUN_X, SUN_Y, SUN_Z));
    // Wrapped rather than clamped at zero. A hard terminator on a mass of cubes gives a black
    // half and a lit half with a hard seam down the middle; wrapping carries some light round the
    // curve, which is what real bounced light does and what keeps the dark side readable.
    float ndl = dot(normalize(Normal), sunDir);
    float sun = clamp((ndl + 0.35) / 1.35, 0.0, 1.0);
    // Night takes the directional term away and leaves the ambient, so the model does not stay
    // lit from a sun that has set. SUN_Y is the sun's height; below the horizon it is negative.
    float daylight = clamp(SUN_Y * 3.0 + 0.35, 0.0, 1.0);
    hemisphere *= mix(1.0, mix(0.38, 1.62, sun), daylight);
#endif
#ifdef PER_FACE_LIGHTING
    vertexPerFaceColorBack.rgb *= hemisphere;
    vertexPerFaceColorFront.rgb *= hemisphere;
#else
    vertexColor.rgb *= hemisphere;
#endif
#endif

#ifdef STORM_RIM
    // CRISP EDGE HIGHLIGHTS (matte MCSM skin, never glossy). A tight fresnel
    // term in view space: faces turning edge-on to the camera catch a sharp
    // indigo edge light, which is what makes the dark block mass read as
    // solid and faintly luminous instead of a flat black void. No specular
    // lobe, no sweeping glint band: matte charcoal, not polished armour.
    vec3 rimViewNormal = normalize((ModelViewMat * vec4(Normal, 0.0)).xyz);
    vec3 rimViewPos = (ModelViewMat * vec4(Position, 1.0)).xyz;
    float rimLen = max(length(rimViewPos), 1e-4);
    vec3 rimViewDir = -rimViewPos / rimLen;
    float rim = pow(1.0 - abs(dot(rimViewNormal, rimViewDir)), 3.0);
    vec3 rimLight = vec3(0.10, 0.12, 0.26) * rim;
#ifdef PER_FACE_LIGHTING
    vertexPerFaceColorBack.rgb += rimLight;
    vertexPerFaceColorFront.rgb += rimLight;
#else
    vertexColor.rgb += rimLight;
#endif
#endif

#ifndef EMISSIVE
    lightMapColor = sample_lightmap(Sampler2, UV2);
#endif

#ifndef NO_OVERLAY
    overlayColor = texelFetch(Sampler1, UV1, 0);
#endif

    texCoord0 = UV0;

#ifdef APPLY_TEXTURE_MATRIX
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
#endif
}
