#version 150

uniform sampler2D OreMask;
uniform sampler2D SceneDepth;
uniform vec4 ColorModulator;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    float visibleOre = texelFetch(OreMask, pixel, 0).r;
    float surfaceDepth = texelFetch(SceneDepth, pixel, 0).r;
    // Keep the exposed face's own coplanar outline, but reject geometry farther behind it.
    if (visibleOre > 0.5 && gl_FragCoord.z > surfaceDepth + 0.00000012) {
        discard;
    }
    if (vertexColor.a == 0.0) {
        discard;
    }
    fragColor = vertexColor * ColorModulator;
}
