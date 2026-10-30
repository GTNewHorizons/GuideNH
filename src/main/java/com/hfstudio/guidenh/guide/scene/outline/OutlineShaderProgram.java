package com.hfstudio.guidenh.guide.scene.outline;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class OutlineShaderProgram {

    private static final String SHADER_ROOT = "/assets/guidenh/shaders/outline/";

    private final int program;
    private final Map<String, Integer> locations = new HashMap<>();

    private OutlineShaderProgram(int program) {
        this.program = program;
    }

    static OutlineShaderProgram load(String vertexName, String fragmentName) {
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
            throw new IllegalStateException("Failed to link outline shader " + fragmentName + ": " + log);
        }
        return new OutlineShaderProgram(program);
    }

    void use() {
        GL20.glUseProgram(program);
    }

    OutlineShaderProgram sampler(String name, int unit) {
        use();
        setInt(name, unit);
        return this;
    }

    void setInt(String name, int value) {
        GL20.glUniform1i(location(name), value);
    }

    void setFloat(String name, float value) {
        GL20.glUniform1f(location(name), value);
    }

    void setVec2(String name, float x, float y) {
        GL20.glUniform2f(location(name), x, y);
    }

    void setVec4(String name, float x, float y, float z, float w) {
        GL20.glUniform4f(location(name), x, y, z, w);
    }

    void delete() {
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
            throw new IllegalStateException("Failed to compile outline shader " + name + ": " + log);
        }
        return shader;
    }

    private static String readSource(String name) {
        try (InputStream stream = OutlineShaderProgram.class.getResourceAsStream(SHADER_ROOT + name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing outline shader " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read outline shader " + name, e);
        }
    }
}
