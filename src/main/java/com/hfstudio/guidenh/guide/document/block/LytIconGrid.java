package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;

public class LytIconGrid extends LytBox {

    private final List<LytBlock> slots = new ArrayList<>();

    public LytIconGrid() {
        setPadding(5);
    }

    public void addSlot(LytBlock slot) {
        slots.add(slot);
        append(slot);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int columns = Math.max(1, availableWidth / LytSlot.OUTER_SIZE);
        int rows = (slots.size() + columns - 1) / columns;
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i)
                .layout(
                    context,
                    x + i % columns * LytSlot.OUTER_SIZE,
                    y + i / columns * LytSlot.OUTER_SIZE,
                    availableWidth);
        }
        return new LytRect(x, y, columns * LytSlot.OUTER_SIZE, rows * LytSlot.OUTER_SIZE);
    }
}
