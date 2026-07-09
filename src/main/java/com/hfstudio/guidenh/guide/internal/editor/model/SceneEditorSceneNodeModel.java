package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class SceneEditorSceneNodeModel {

    @Getter
    private final SceneEditorSceneNodeType type;
    @Getter
    private final Map<String, String> attributes;
    @Getter
    private final List<SceneEditorElementModel> templateElements;
    @Nullable
    private SceneEditorElementModel annotationElement;
    @Nullable
    private String opaqueText;

    public SceneEditorSceneNodeModel(SceneEditorSceneNodeType type) {
        this.type = type;
        this.attributes = new LinkedHashMap<>();
        this.templateElements = new ArrayList<>();
        this.annotationElement = null;
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    @Nullable
    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public void addTemplateElement(SceneEditorElementModel element) {
        templateElements.add(element);
    }

    @Nullable
    public SceneEditorElementModel getAnnotationElement() {
        return annotationElement;
    }

    public void setAnnotationElement(@Nullable SceneEditorElementModel annotationElement) {
        this.annotationElement = annotationElement;
    }

    @Nullable
    public String getOpaqueText() {
        return opaqueText;
    }

    public void setOpaqueText(@Nullable String opaqueText) {
        this.opaqueText = opaqueText;
    }

    public SceneEditorSceneNodeModel duplicate() {
        SceneEditorSceneNodeModel duplicate = new SceneEditorSceneNodeModel(type);
        duplicate.attributes.putAll(this.attributes);
        for (SceneEditorElementModel element : templateElements) {
            duplicate.templateElements.add(element.duplicate());
        }
        if (annotationElement != null) {
            duplicate.annotationElement = annotationElement.duplicate();
        }
        duplicate.opaqueText = this.opaqueText;
        return duplicate;
    }
}
