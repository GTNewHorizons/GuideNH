package com.hfstudio.guidenh.guide.scene;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;

import lombok.Getter;
import lombok.Setter;

public class SceneSoundCue {

    @Getter
    private final GuideSoundTrigger trigger;
    @Getter
    private final GuideSoundSpec sound;
    @Nullable
    private StructureLibSceneCondition structureLibCondition;
    @Getter
    @Setter
    private boolean entered;
    @Getter
    @Setter
    private boolean hovered;

    public SceneSoundCue(GuideSoundTrigger trigger, GuideSoundSpec sound) {
        this.trigger = trigger != null ? trigger : GuideSoundTrigger.CLICK;
        this.sound = sound;
    }

    @Nullable
    public StructureLibSceneCondition getStructureLibCondition() {
        return structureLibCondition;
    }

    public void setStructureLibCondition(@Nullable StructureLibSceneCondition structureLibCondition) {
        this.structureLibCondition = structureLibCondition;
    }

    public boolean matches(GuideSoundTrigger trigger) {
        return this.trigger == trigger;
    }
}
