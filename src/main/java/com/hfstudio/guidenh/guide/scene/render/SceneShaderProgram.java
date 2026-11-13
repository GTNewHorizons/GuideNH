package com.hfstudio.guidenh.guide.scene.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class SceneShaderProgram {

    private static final String SHADER_ROOT = "/assets/guidenh/shaders/";

    private final int program;
    private final Map<String, Integer> locations = new HashMap<>();

    protected SceneShaderProgram(int program) {
        this.program = program;
    }

    public static SceneShaderProgram load(String vertexName, String fragmentName) {
        int vertex = compile(GL20.GL_VERTEX_SHADER, vertexName);
        int fragment;
        try {
            fragment = compile(GL20.GL_FRAGMENT_SHADER, fragmentName);
        } catch (RuntimeException e) {
            GL20.glDeleteShader(vertex);
            throw e;
        }
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        GL20.glDetachShader(program, vertex);
        GL20.glDetachShader(program, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 4096);
            GL20.glDeleteProgram(program);
            throw new IllegalStateException("Failed to link scene shader " + fragmentName + ": " + log);
        }
        return new SceneShaderProgram(program);
    }

    public void use() {
        GL20.glUseProgram(program);
    }

    public SceneShaderProgram sampler(String name, int unit) {
        use();
        setInt(name, unit);
        return this;
    }

    public void setInt(String name, int value) {
        GL20.glUniform1i(location(name), value);
    }

    public void setFloat(String name, float value) {
        GL20.glUniform1f(location(name), value);
    }

    public void setVec2(String name, float x, float y) {
        GL20.glUniform2f(location(name), x, y);
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        GL20.glUniform4f(location(name), x, y, z, w);
    }

    public void delete() {
        GL20.glDeleteProgram(program);
    }

    private int location(String name) {
        Integer location = locations.get(name);
        if (location == null) {
            location = GL20.glGetUniformLocation(program, name);
            locations.put(name, location);
        }
        return location;
    }

    private static int compile(int type, String name) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, readSource(name));
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Failed to compile scene shader " + name + ": " + log);
        }
        return shader;
    }

    private static String readSource(String name) {
        try (InputStream stream = SceneShaderProgram.class.getResourceAsStream(SHADER_ROOT + name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing scene shader " + name);
            }
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (source.contains("#include <annotation_color>")) {
                source = source.replace("#include <annotation_color>", readSource("annotation/rainbow.glsl"));
            }
            if (source.contains("#include <annotation_box_surface>")) {
                source = source.replace("#include <annotation_box_surface>", readSource("annotation/box_surface.glsl"));
            }
            return source;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read scene shader " + name, e);
        }
    }
}
