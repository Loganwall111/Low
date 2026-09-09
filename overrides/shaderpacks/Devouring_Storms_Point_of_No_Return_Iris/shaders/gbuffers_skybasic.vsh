#version 330 compatibility

out vec4 starData;
out vec3 viewPos;

void main() {
    gl_Position = ftransform();
    viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    // vanilla marks star geometry by having vertex colour; sky dome is untextured
    starData = vec4(gl_Color.rgb, float(gl_Color.r + gl_Color.g + gl_Color.b > 0.01));
}
