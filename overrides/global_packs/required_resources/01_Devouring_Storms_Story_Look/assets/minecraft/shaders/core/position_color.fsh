#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;

out vec4 fragColor;

// Story Look: the sunrise/sunset tint quad arrives here as vanilla orange.
// The reference dawn is pink-lavender, never orange, so orange hues are
// remapped while every other use (chunk borders, sleep overlay) passes
// through byte-identical.

void main() {
    vec4 color = vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    vec4 c = color * ColorModulator;
    if (c.g > c.r * 1.02 && c.g > c.b * 0.95 && c.g > 0.15) {
        // the story dawn quad arrives teal-green on some phases; the
        // reference dawn is pink-lavender, so remap it too
        float lg = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
        c.rgb = mix(c.rgb, lg * vec3(1.28, 0.88, 1.10), 0.85);
    }
    if (c.r > c.b * 1.15 && c.r > 0.25) {
        float l = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
        c.rgb = mix(c.rgb, l * vec3(1.28, 0.88, 1.10), 0.85);
    }
    fragColor = c;
}
