package com.hfstudio.guidenh.guide.document.block;

import lombok.Getter;
import lombok.Setter;

/**
 * A box that just aligns its content along the vertical or horizontal axis.
 */
@Getter
@Setter
public abstract class LytAxisBox extends LytBox {

    private int gap;

    private AlignItems alignItems = AlignItems.START;

}
