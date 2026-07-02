package com.hfstudio.guidenh.guide.document.flow;

import lombok.Getter;
import lombok.Setter;

/**
 * Line-Break that also clears floats.
 */
@Getter
@Setter
public class LytFlowBreak extends LytFlowContent {

    private boolean clearLeft;
    private boolean clearRight;

}
