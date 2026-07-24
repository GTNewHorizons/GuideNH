use guide_layout_engine::parley_text::ParleyFonts;

fn main() {
    let mut parley = ParleyFonts::new();

    let text = "    indented line";
    let layout = parley.layout_paragraph(text, 9.0, 10.0 / 9.0, Some(500.0));

    let mut total_advance = 0.0f32;
    let mut glyph_count = 0;
    for line in layout.lines() {
        for item in line.items() {
            if let parley::PositionedLayoutItem::GlyphRun(gr) = item {
                for g in gr.positioned_glyphs() {
                    glyph_count += 1;
                    total_advance = g.x + g.advance;
                }
            }
        }
    }

    let single_space = parley.layout_paragraph(" indented line", 9.0, 10.0 / 9.0, Some(500.0));
    let mut single_advance = 0.0f32;
    let mut single_count = 0;
    for line in single_space.lines() {
        for item in line.items() {
            if let parley::PositionedLayoutItem::GlyphRun(gr) = item {
                for g in gr.positioned_glyphs() {
                    single_count += 1;
                    single_advance = g.x + g.advance;
                }
            }
        }
    }

    println!("4-space:  glyphs={} advance={:.2}", glyph_count, total_advance);
    println!("1-space:  glyphs={} advance={:.2}", single_count, single_advance);
    println!(
        "diff={:.2} (expect ~3 space widths if NOT collapsed)",
        total_advance - single_advance
    );

    let one_space = parley.layout_paragraph(" ", 9.0, 10.0 / 9.0, Some(500.0));
    let mut space_w = 0.0f32;
    for line in one_space.lines() {
        for item in line.items() {
            if let parley::PositionedLayoutItem::GlyphRun(gr) = item {
                for g in gr.positioned_glyphs() {
                    space_w = g.advance;
                }
            }
        }
    }
    println!("single space advance={:.2}", space_w);
    println!(
        "VERDICT: {}",
        if (total_advance - single_advance - 3.0 * space_w).abs() < 1.0 {
            "SPACES PRESERVED (not collapsed)"
        } else {
            "SPACES COLLAPSED"
        }
    );

    let nl_text = "line1\nline2";
    let nl_layout = parley.layout_paragraph(nl_text, 9.0, 10.0 / 9.0, Some(500.0));
    let nl_lines = nl_layout.lines().count();
    println!("\nnewline test: \"line1\\nline2\" → {} line(s)", nl_lines);
    println!(
        "VERDICT: {}",
        if nl_lines == 1 {
            "\\n FOLDED TO SPACE (need breaks mechanism)"
        } else {
            "\\n TREATED AS LINE BREAK"
        }
    );
}
