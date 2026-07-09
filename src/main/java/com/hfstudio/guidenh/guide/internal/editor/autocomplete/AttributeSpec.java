package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import lombok.Getter;

@Getter
public class AttributeSpec {

    private final String name;
    private final AttrType type;
    private final Class<? extends Enum<?>> enumClass;

    public AttributeSpec(String name, AttrType type) {
        this(name, type, null);
    }

    public AttributeSpec(String name, AttrType type, Class<? extends Enum<?>> enumClass) {
        this.name = name;
        this.type = type;
        this.enumClass = enumClass;
    }

}
