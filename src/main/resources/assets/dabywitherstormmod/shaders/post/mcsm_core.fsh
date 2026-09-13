#version 330

// MCSM Core Engine ambient pass.
//
// This pass is intentionally a translucent screen-space overlay. It does not
// read and write the same scene texture, which keeps the framebuffer legal on
// all 26.2 render backends. The terrain/entity core shaders perform their own
// material and linear-fog work; this pass supplies the final phase-dependent
// cinematic grade without reintroducing an infinite sky layer.

layout(std140) uniform McsmCoreConfig {
    // x = u_StormPhase, y = active, z = time of day, w = distance fade
    vec4 u_StormPhase;
    // xyz = absolute u_StormPos, w = distance fade
    vec4 u_StormPos;
};

out vec4 fragColor;

vec3 phase5(float t) {
    vec3 core = vec3(10.0 / 255.0, 17.0 / 255.0, 24.0 / 255.0);
    vec3 mid = vec3(29.0 / 255.0, 51.0 / 255.0, 72.0 / 255.0);
    vec3 outer = vec3(80.0 / 255.0, 105.0 / 255.0, 135.0 / 255.0);
    return mix(mix(core, mid, smoothstep(0.0, 0.5, t)), outer,
            smoothstep(0.5, 1.0, t));
}

vec3 phase55(float t) {
    vec3 core = vec3(5.0 / 255.0, 2.0 / 255.0, 8.0 / 255.0);
    vec3 mid = vec3(42.0 / 255.0, 18.0 / 255.0, 61.0 / 255.0);
    vec3 outer = vec3(75.0 / 255.0, 30.0 / 255.0, 94.0 / 255.0);
    vec3 horizon = vec3(125.0 / 255.0, 75.0 / 255.0, 145.0 / 255.0);
    return mix(mix(core, mid, smoothstep(0.0, 0.4, t)),
            mix(outer, horizon, smoothstep(0.7, 1.0, t)),
            smoothstep(0.4, 0.7, t));
}

vec3 phase6(float t) {
    vec3 zenith = vec3(16.0 / 255.0, 10.0 / 255.0, 26.0 / 255.0);
    vec3 upper = vec3(51.0 / 255.0, 28.0 / 255.0, 61.0 / 255.0);
    vec3 lower = vec3(138.0 / 255.0, 83.0 / 255.0, 97.0 / 255.0);
    vec3 horizon = vec3(196.0 / 255.0, 122.0 / 255.0, 90.0 / 255.0);
    return t > 0.68
        ? mix(upper, zenith, smoothstep(0.68, 1.0, t))
        : (t > 0.30
            ? mix(lower, upper, smoothstep(0.30, 0.68, t))
            : mix(horizon, lower, smoothstep(0.0, 0.30, t)));
}

void main() {
    float phase = u_StormPhase.x;
    float stormActive = u_StormPhase.y;
    float distanceFade = clamp(u_StormPos.w, 0.0, 1.0);

    // A single phase deck drives the whole scene, while the entity-attached
    // saucer supplies the detailed world-space radial gradient behind the boss.
    vec3 grade;
    if (phase < 5.5) {
        grade = mix(phase5(0.72), phase55(0.72), smoothstep(5.0, 5.5, phase));
    } else {
        grade = mix(phase55(0.72), phase6(0.48), smoothstep(5.9, 6.05, phase));
    }

    // Filmic mid-tone lift with crushed shadow floor. The small alpha keeps
    // vanilla terrain, clouds, and entities visible beneath the grade.
    float luminance = dot(grade, vec3(0.2126, 0.7152, 0.0722));
    vec3 film = (grade - 0.5) * 1.12 + 0.5;
    film = max(film, vec3(0.008));
    film *= 0.78 + luminance * 0.32;
    float alpha = stormActive * distanceFade * 0.105;
    fragColor = vec4(film, alpha);
}
