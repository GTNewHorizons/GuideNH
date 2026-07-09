package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.Setter;

public class SceneEditorSceneModel {

    @Nullable
    private String structureSource;
    @Nullable
    private String perspectivePreset;
    @Getter
    @Setter
    private int previewWidth;
    @Getter
    @Setter
    private int previewHeight;
    @Getter
    @Setter
    private float rotationX;
    @Getter
    @Setter
    private float rotationY;
    @Getter
    @Setter
    private float rotationZ;
    @Getter
    @Setter
    private float offsetX;
    @Getter
    @Setter
    private float offsetY;
    @Getter
    @Setter
    private float zoom;
    @Getter
    @Setter
    private boolean interactive;
    @Getter
    @Setter
    private boolean showBackground;
    @Getter
    @Setter
    private boolean allowLayerSlider;
    @Getter
    @Setter
    private float centerX;
    @Getter
    @Setter
    private float centerY;
    @Getter
    @Setter
    private float centerZ;
    @Getter
    private final List<SceneEditorSceneNodeModel> sceneNodes;
    @Getter
    private final List<SceneEditorElementModel> elements;

    private SceneEditorSceneModel(@Nullable String structureSource) {
        this.structureSource = structureSource;
        this.perspectivePreset = null;
        this.previewWidth = 256;
        this.previewHeight = 192;
        this.rotationX = Float.NaN;
        this.rotationY = Float.NaN;
        this.rotationZ = Float.NaN;
        this.offsetX = Float.NaN;
        this.offsetY = Float.NaN;
        this.zoom = Float.NaN;
        this.interactive = true;
        this.showBackground = true;
        this.allowLayerSlider = false;
        this.centerX = Float.NaN;
        this.centerY = Float.NaN;
        this.centerZ = Float.NaN;
        this.sceneNodes = new ArrayList<>();
        this.elements = new ArrayList<>();
    }

    public static SceneEditorSceneModel blank() {
        return new SceneEditorSceneModel(null);
    }

    public static SceneEditorSceneModel withStructureSource(String structureSource) {
        SceneEditorSceneModel model = new SceneEditorSceneModel(null);
        model.setStructureSource(structureSource);
        return model;
    }

    public SceneEditorSceneModel copy() {
        SceneEditorSceneModel copy = new SceneEditorSceneModel(this.structureSource);
        copy.setPerspectivePreset(this.perspectivePreset);
        copy.setPreviewWidth(this.previewWidth);
        copy.setPreviewHeight(this.previewHeight);
        copy.setRotationX(this.rotationX);
        copy.setRotationY(this.rotationY);
        copy.setRotationZ(this.rotationZ);
        copy.setOffsetX(this.offsetX);
        copy.setOffsetY(this.offsetY);
        copy.setZoom(this.zoom);
        copy.setInteractive(this.interactive);
        copy.setShowBackground(this.showBackground);
        copy.setAllowLayerSlider(this.allowLayerSlider);
        copy.setCenterX(this.centerX);
        copy.setCenterY(this.centerY);
        copy.setCenterZ(this.centerZ);
        Map<UUID, SceneEditorSceneNodeModel> copiedAnnotationNodes = new LinkedHashMap<>();
        for (SceneEditorSceneNodeModel sceneNode : this.sceneNodes) {
            SceneEditorSceneNodeModel duplicatedNode = sceneNode.duplicate();
            copy.addSceneNode(duplicatedNode);
            if (duplicatedNode.getType() == SceneEditorSceneNodeType.ANNOTATION
                && duplicatedNode.getAnnotationElement() != null) {
                copiedAnnotationNodes.put(
                    sceneNode.getAnnotationElement()
                        .getId(),
                    duplicatedNode);
            }
        }
        for (SceneEditorElementModel element : this.elements) {
            if (!copiedAnnotationNodes.containsKey(element.getId())) {
                copy.addElement(element.duplicate());
            }
        }
        return copy;
    }

    @Nullable
    public String getStructureSource() {
        return structureSource;
    }

    public void setStructureSource(@Nullable String structureSource) {
        this.structureSource = structureSource;
        if (structureSource == null || structureSource.isEmpty()) {
            return;
        }

        SceneEditorSceneNodeModel importStructureNode = findFirstSceneNode(SceneEditorSceneNodeType.IMPORT_STRUCTURE);
        if (importStructureNode != null) {
            importStructureNode.setAttribute("src", structureSource);
            return;
        }

        SceneEditorSceneNodeModel node = new SceneEditorSceneNodeModel(SceneEditorSceneNodeType.IMPORT_STRUCTURE);
        node.setAttribute("src", structureSource);
        sceneNodes.addFirst(node);
    }

    @Nullable
    public String getPerspectivePreset() {
        return perspectivePreset;
    }

    public void setPerspectivePreset(@Nullable String perspectivePreset) {
        this.perspectivePreset = perspectivePreset;
    }

    public boolean hasExplicitRotationX() {
        return !Float.isNaN(rotationX);
    }

    public boolean hasExplicitRotationY() {
        return !Float.isNaN(rotationY);
    }

    public boolean hasExplicitRotationZ() {
        return !Float.isNaN(rotationZ);
    }

    public boolean hasExplicitOffsetX() {
        return !Float.isNaN(offsetX);
    }

    public boolean hasExplicitOffsetY() {
        return !Float.isNaN(offsetY);
    }

    public boolean hasExplicitZoom() {
        return !Float.isNaN(zoom);
    }

    public boolean hasExplicitCenterX() {
        return !Float.isNaN(centerX);
    }

    public boolean hasExplicitCenterY() {
        return !Float.isNaN(centerY);
    }

    public boolean hasExplicitCenterZ() {
        return !Float.isNaN(centerZ);
    }

    public boolean hasExplicitCenter() {
        return hasExplicitCenterX() || hasExplicitCenterY() || hasExplicitCenterZ();
    }

    public void addSceneNode(SceneEditorSceneNodeModel sceneNode) {
        sceneNodes.add(sceneNode);
        if (sceneNode.getType() == SceneEditorSceneNodeType.IMPORT_STRUCTURE && this.structureSource == null) {
            String src = sceneNode.getAttribute("src");
            if (src != null && !src.isEmpty()) {
                this.structureSource = src;
            }
        }
        if (sceneNode.getType() == SceneEditorSceneNodeType.ANNOTATION && sceneNode.getAnnotationElement() != null) {
            elements.add(sceneNode.getAnnotationElement());
        }
    }

    public void addElement(SceneEditorElementModel element) {
        elements.add(element);
        SceneEditorSceneNodeModel node = new SceneEditorSceneNodeModel(SceneEditorSceneNodeType.ANNOTATION);
        node.setAnnotationElement(element);
        sceneNodes.add(node);
    }

    public Optional<SceneEditorElementModel> getElement(UUID elementId) {
        for (SceneEditorElementModel element : elements) {
            if (element.getId()
                .equals(elementId)) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }

    public boolean removeElement(UUID elementId) {
        boolean removed = false;
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i)
                .getId()
                .equals(elementId)) {
                elements.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) {
            return false;
        }

        for (int i = 0; i < sceneNodes.size(); i++) {
            SceneEditorSceneNodeModel node = sceneNodes.get(i);
            if (node.getType() != SceneEditorSceneNodeType.ANNOTATION || node.getAnnotationElement() == null) {
                continue;
            }
            if (node.getAnnotationElement()
                .getId()
                .equals(elementId)) {
                sceneNodes.remove(i);
                break;
            }
        }
        return true;
    }

    public boolean moveElement(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= elements.size()
            || toIndex < 0
            || toIndex >= elements.size()
            || fromIndex == toIndex) {
            return false;
        }
        SceneEditorElementModel element = elements.remove(fromIndex);
        elements.add(toIndex, element);
        reorderAnnotationSceneNodes();
        return true;
    }

    @Nullable
    private SceneEditorSceneNodeModel findFirstSceneNode(SceneEditorSceneNodeType type) {
        for (SceneEditorSceneNodeModel sceneNode : sceneNodes) {
            if (sceneNode.getType() == type) {
                return sceneNode;
            }
        }
        return null;
    }

    private void reorderAnnotationSceneNodes() {
        if (sceneNodes.isEmpty()) {
            return;
        }

        LinkedHashMap<UUID, SceneEditorSceneNodeModel> annotationNodesById = new LinkedHashMap<>();
        List<SceneEditorSceneNodeModel> unattachedAnnotationNodes = new ArrayList<>();
        for (SceneEditorSceneNodeModel sceneNode : sceneNodes) {
            if (sceneNode.getType() != SceneEditorSceneNodeType.ANNOTATION) {
                continue;
            }
            if (sceneNode.getAnnotationElement() == null) {
                unattachedAnnotationNodes.add(sceneNode);
                continue;
            }
            annotationNodesById.put(
                sceneNode.getAnnotationElement()
                    .getId(),
                sceneNode);
        }

        if (annotationNodesById.isEmpty() && unattachedAnnotationNodes.isEmpty()) {
            return;
        }

        List<SceneEditorSceneNodeModel> orderedAnnotationNodes = new ArrayList<>();
        for (SceneEditorElementModel element : elements) {
            SceneEditorSceneNodeModel annotationNode = annotationNodesById.remove(element.getId());
            if (annotationNode != null) {
                orderedAnnotationNodes.add(annotationNode);
            }
        }
        orderedAnnotationNodes.addAll(annotationNodesById.values());
        orderedAnnotationNodes.addAll(unattachedAnnotationNodes);

        int annotationIndex = 0;
        for (int i = 0; i < sceneNodes.size(); i++) {
            if (sceneNodes.get(i)
                .getType() == SceneEditorSceneNodeType.ANNOTATION) {
                sceneNodes.set(i, orderedAnnotationNodes.get(annotationIndex++));
            }
        }
    }
}
