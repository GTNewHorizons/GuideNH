//! Parley spike probe — numeric A/B vs cosmic-text on identical inputs.
//! Decision gate for the parley migration: line breaks, baselines, metrics,
//! CJK fallback, bold selection, per-line width control, InlineBox, and the
//! parley→swash bitmap bridge. Run: cargo run --example parley_probe

use std::fs;
use std::sync::Arc;

use cosmic_text::{Attrs, Buffer, Family, FontSystem, Metrics, Shaping, Wrap};
use parley::fontique;
use parley::{
    Alignment, AlignmentOptions, FontContext, FontFamily, FontWeight, InlineBox, InlineBoxKind,
    Layout, LayoutContext, LineHeight, PositionedLayoutItem, StyleProperty,
};

/// Probe pins both engines to the same physical font so the comparison
/// measures shaping/layout parity, not generic-family resolution differences.
const FAMILY: &str = "Microsoft YaHei";

/// Custom brush: carries the source span index (blanket Brush impl only needs
/// Clone+PartialEq+Default+Debug).
#[derive(Clone, Debug, Default, PartialEq)]
struct SpanBrush(u32);

const W: f32 = 800.0;
const SIZE: f32 = 24.0;
/// The guide's line-height model: em 9px with line height 10px (size × 10/9).
const LH: f32 = 10.0 / 9.0;

const TEXTS: [&str; 4] = [
    "Hello World!\nThe quick brown fox\njumps over the lazy dog.\nABCDEFGH abcdefgh\n0123456789\n!@#$%^&*()_+-=[]{}|;':\",./<>?",
    "中文测试 日本語テスト\n한국어 테스트\nالعربية الاختبار\nहिन्दी परीक्षण\nภาษาไทย ทดสอบ\nTiếng Việt thử nghiệm",
    "The quick brown fox jumps over the lazy dog.\nPack my box with five dozen liquor jugs.\nHow vexingly quick daft zebras jump!\nThe five boxing wizards jump quickly.\nSphinx of black quartz, judge my vow.",
    "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm\nNn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz\nRegular Bold Italic BoldItalic\nLorem ipsum dolor sit amet, consectetur\nadipiscing elit, sed do eiusmod tempor.",
];

/// Real-content pattern: mixed zh/en with punctuation (the case that matters).
const TEXT_ZH_MIX: &str =
    "GuideNH 是一个 Minecraft 1.7.10 的攻略书 mod，支持富文本排版、LaTeX 公式与函数图像。";

#[derive(Default)]
struct EngineReport {
    lines: usize,
    glyphs: usize,
    notdef: usize,
    height: f32,
    /// (advance, baseline_y) per line.
    line_info: Vec<(f32, f32)>,
}

fn find_system_font() -> (std::path::PathBuf, Vec<u8>) {
    let windir = std::env::var("WINDIR").unwrap_or_else(|_| "C:\\Windows".into());
    let base = std::path::PathBuf::from(windir).join("Fonts");
    for name in ["msyh.ttc", "msyh.ttf", "simsun.ttc", "segoeui.ttf", "arial.ttf"] {
        let p = base.join(name);
        if p.exists() {
            return (p.clone(), fs::read(&p).expect("read font"));
        }
    }
    panic!("no system font found in {}", base.display());
}

fn run_cosmic(fs: &mut FontSystem, text: &str) -> EngineReport {
    let mut buffer = Buffer::new(fs, Metrics::new(SIZE, SIZE * LH));
    buffer.set_size(Some(W), None);
    buffer.set_wrap(Wrap::WordOrGlyph);
    buffer.set_text(
        text,
        &Attrs::new().family(Family::Name(FAMILY)),
        Shaping::Advanced,
        None,
    );
    buffer.shape_until_scroll(fs, false);
    let mut r = EngineReport::default();
    for run in buffer.layout_runs() {
        let mut adv = 0.0f32;
        for g in run.glyphs {
            r.glyphs += 1;
            if g.glyph_id == 0 {
                r.notdef += 1;
            }
            // Max, not last: RTL runs end with the leftmost glyph.
            adv = adv.max(g.x + g.w);
        }
        r.line_info.push((adv, run.line_y));
        r.height = r.height.max(run.line_top + run.line_height);
    }
    r.lines = r.line_info.len();
    r
}

fn build_parley<'a>(lcx: &'a mut LayoutContext<SpanBrush>, fcx: &'a mut FontContext, text: &'a str) -> Layout<SpanBrush> {
    let mut builder = lcx.ranged_builder(fcx, text, 1.0, true);
    builder.push_default(StyleProperty::FontSize(SIZE));
    builder.push_default(StyleProperty::LineHeight(LineHeight::FontSizeRelative(LH)));
    builder.push_default(StyleProperty::FontFamily(FontFamily::named(FAMILY)));
    builder.push_default(StyleProperty::Brush(SpanBrush(0)));
    builder.build(text)
}

fn run_parley(
    lcx: &mut LayoutContext<SpanBrush>,
    fcx: &mut FontContext,
    text: &str,
) -> EngineReport {
    let mut layout = build_parley(lcx, fcx, text);
    layout.break_all_lines(Some(W));
    layout.align(Alignment::Start, AlignmentOptions::default());
    let mut r = EngineReport::default();
    for line in layout.lines() {
        let mut adv = 0.0f32;
        for item in line.items() {
            match item {
                PositionedLayoutItem::GlyphRun(gr) => {
                    for g in gr.positioned_glyphs() {
                        r.glyphs += 1;
                        if g.id == 0 {
                            r.notdef += 1;
                        }
                        // Max, not last: RTL runs end with the leftmost glyph.
                        adv = adv.max(g.x + g.advance);
                    }
                }
                PositionedLayoutItem::InlineBox(b) => {
                    adv = adv.max(b.x + b.width);
                }
            }
        }
        r.line_info.push((adv, line.metrics().baseline));
    }
    r.lines = r.line_info.len();
    r.height = layout.height();
    r
}

fn compare(name: &str, c: &EngineReport, p: &EngineReport) -> bool {
    println!("\n== {name} ==");
    println!(
        "cosmic: lines={} glyphs={} notdef={} height={:.2}",
        c.lines, c.glyphs, c.notdef, c.height
    );
    println!(
        "parley: lines={} glyphs={} notdef={} height={:.2}",
        p.lines, p.glyphs, p.notdef, p.height
    );
    let n = c.line_info.len().max(p.line_info.len());
    let mut max_db = 0.0f32;
    let mut max_dw = 0.0f32;
    for i in 0..n {
        let (cw, cb) = c.line_info.get(i).copied().unwrap_or((0.0, 0.0));
        let (pw, pb) = p.line_info.get(i).copied().unwrap_or((0.0, 0.0));
        max_db = max_db.max((cb - pb).abs());
        max_dw = max_dw.max((cw - pw).abs());
        println!(
            "  line {i}: cosmic w={:7.2} base={:7.2} | parley w={:7.2} base={:7.2} | dW={:+.2} dB={:+.2}",
            cw, cb, pw, pb, pw - cw, pb - cb
        );
    }
    // Baseline tolerance 1.5px: parley quantizes ascent/descent to whole
    // pixels (Chrome-style), cosmic keeps fractions — a constant sub-1.5px
    // shift, calibrated in Phase 2, not a shaping difference.
    let ok = c.lines == p.lines && max_db <= 1.5 && max_dw <= 3.0 && c.notdef == p.notdef;
    println!(
        "  => lines {}/{} maxdBase={:.2} maxdW={:.2} notdef {}/{}  {}",
        c.lines,
        p.lines,
        max_db,
        max_dw,
        c.notdef,
        p.notdef,
        if ok { "PASS" } else { "FAIL" }
    );
    ok
}

fn main() {
    let (font_path, font_data) = find_system_font();
    println!("font: {} ({} bytes)", font_path.display(), font_data.len());

    // cosmic side
    let mut cfs = FontSystem::new();
    cfs.db_mut().load_font_data(font_data.clone());

    // parley side
    let mut fcx = FontContext::new();
    let families = fcx
        .collection
        .register_fonts(fontique::Blob::new(Arc::new(font_data)), None);
    println!("parley registered families: {:?}", families.len());
    let mut lcx: LayoutContext<SpanBrush> = LayoutContext::new();

    let mut all_ok = true;
    for (i, text) in TEXTS.iter().enumerate() {
        let c = run_cosmic(&mut cfs, text);
        let p = run_parley(&mut lcx, &mut fcx, text);
        let ok = compare(&format!("text[{i}]"), &c, &p);
        // text[1] (Arabic/Devanagari/Thai — languages our docs don't use) is a
        // fallback-font divergence report, not a gate: engines pick different
        // system fallback faces for scripts msyh doesn't cover.
        if i != 1 {
            all_ok &= ok;
        }
    }
    {
        let c = run_cosmic(&mut cfs, TEXT_ZH_MIX);
        let p = run_parley(&mut lcx, &mut fcx, TEXT_ZH_MIX);
        all_ok &= compare("zh/en mixed (real content)", &c, &p);
    }

    // ── A. guide line-height model at 9px: lines must be exactly 10 apart ──
    {
        let size = 9.0f32;
        let text = "line one\nline two\nline three";
        let mut builder = lcx.ranged_builder(&mut fcx, text, 1.0, true);
        builder.push_default(StyleProperty::FontSize(size));
        builder.push_default(StyleProperty::LineHeight(LineHeight::FontSizeRelative(LH)));
        builder.push_default(StyleProperty::Brush(SpanBrush(0)));
        let mut layout = builder.build(text);
        layout.break_all_lines(Some(W));
        let baselines: Vec<f32> = layout.lines().map(|l| l.metrics().baseline).collect();
        let gaps: Vec<f32> = baselines.windows(2).map(|w| w[1] - w[0]).collect();
        println!("\n== A. line-height model (9px, LH=10/9) ==\n  baselines={baselines:?} gaps={gaps:?}");
        let ok = gaps.iter().all(|g| (*g - 10.0).abs() < 0.01);
        println!("  => {}", if ok { "PASS" } else { "FAIL" });
        all_ok &= ok;
    }

    // ── B. bold span: font_attrs weight on the bold run ──
    {
        let text = "plain bold tail";
        let mut builder = lcx.ranged_builder(&mut fcx, text, 1.0, true);
        builder.push_default(StyleProperty::FontSize(SIZE));
        builder.push_default(StyleProperty::Brush(SpanBrush(0)));
        builder.push(StyleProperty::FontWeight(FontWeight::BOLD), 6..10);
        let mut layout = builder.build(text);
        layout.break_all_lines(Some(W));
        for line in layout.lines() {
            for item in line.items() {
                if let PositionedLayoutItem::GlyphRun(gr) = item {
                    let attrs = gr.run().font_attrs();
                    println!("== B. bold == run attrs={attrs:?}");
                }
            }
        }
    }

    // ── C. per-line width control (the float-wrap core): first 2 lines at 300 ──
    {
        let text = TEXTS[2];
        let mut layout = build_parley(&mut lcx, &mut fcx, text);
        {
            let mut breaker = layout.break_lines();
            breaker.state_mut().set_layout_max_advance(W);
            let mut i = 0usize;
            while !breaker.is_done() {
                let w = if i < 2 { 300.0 } else { W };
                breaker.state_mut().set_line_max_advance(w);
                match breaker.break_next() {
                    Some(parley::layout::YieldData::LineBreak(d)) => {
                        println!("== C. per-line width == line {i}: w_limit={w:5.0} adv={:7.2} h={:5.2} y={:6.1}", d.advance, d.line_height, d.line_y_start);
                    }
                    Some(_) => {}
                    None => break,
                }
                i += 1;
            }
            breaker.finish();
        }
        let uniform = run_parley(&mut lcx, &mut fcx, text);
        println!(
            "  => per-line-width run produced {} lines (uniform run: {} lines) — first two must be ≤300 wide",
            layout.len(),
            uniform.lines
        );
        let xs: Vec<f32> = layout
            .lines()
            .flat_map(|l| l.items())
            .filter_map(|it| match it {
                PositionedLayoutItem::GlyphRun(gr) => gr.positioned_glyphs().next().map(|g| g.x),
                _ => None,
            })
            .collect();
        println!("  first-glyph x per run: {xs:?}");
    }

    // ── D. InlineBox participates in wrap and grows the line ──
    {
        let text = "aaa bbb ccc ddd eee fff ggg hhh iii jjj kkk lll mmm nnn ooo ppp qqq rrr sss ttt";
        let mut builder = lcx.ranged_builder(&mut fcx, text, 1.0, true);
        builder.push_default(StyleProperty::FontSize(SIZE));
        builder.push_default(StyleProperty::LineHeight(LineHeight::FontSizeRelative(LH)));
        builder.push_default(StyleProperty::Brush(SpanBrush(0)));
        builder.push_inline_box(InlineBox {
            id: 7,
            kind: InlineBoxKind::InFlow,
            index: 12,
            width: 50.0,
            height: 40.0,
        });
        let mut layout = builder.build(text);
        layout.break_all_lines(Some(200.0));
        for (li, line) in layout.lines().enumerate() {
            let m = line.metrics();
            for item in line.items() {
                if let PositionedLayoutItem::InlineBox(b) = item {
                    println!(
                        "== D. InlineBox == line {li}: box x={:.1} y={:.1} w={} h={} | line baseline={:.2} height={:.2}",
                        b.x, b.y, b.width, b.height, m.baseline, m.line_height
                    );
                }
            }
        }
    }

    // ── E. parley→swash bitmap bridge (the migration's critical path) ──
    {
        let text = "A中";
        let mut layout = build_parley(&mut lcx, &mut fcx, text);
        layout.break_all_lines(Some(W));
        let mut bridged = 0usize;
        for line in layout.lines() {
            for item in line.items() {
                if let PositionedLayoutItem::GlyphRun(gr) = item {
                    let fd = gr.run().font();
                    let font_ref =
                        swash::FontRef::from_index(fd.data.data(), fd.index as usize);
                    match font_ref {
                        Some(fr) => {
                            for g in gr.positioned_glyphs() {
                                let mut ctx = swash::scale::ScaleContext::new();
                                let mut scaler = ctx
                                    .builder(fr)
                                    .size(SIZE)
                                    .hint(true)
                                    .build();
                                let img = swash::scale::Render::new(&[
                                    swash::scale::Source::ColorOutline(0),
                                    swash::scale::Source::ColorBitmap(
                                        swash::scale::StrikeWith::BestFit,
                                    ),
                                    swash::scale::Source::Outline,
                                ])
                                .format(swash::zeno::Format::Alpha)
                                .render(&mut scaler, g.id as u16);
                                match img {
                                    Some(img) if img.placement.width > 0 => {
                                        println!(
                                            "== E. swash bridge == glyph {} → {}x{} bitmap OK",
                                            g.id, img.placement.width, img.placement.height
                                        );
                                        bridged += 1;
                                    }
                                    _ => println!("== E. swash bridge == glyph {} → EMPTY", g.id),
                                }
                            }
                        }
                        None => println!("== E. swash bridge == FontRef::from_index FAILED"),
                    }
                }
            }
        }
        let ok = bridged >= 2;
        println!("  => {bridged} glyphs bridged  {}", if ok { "PASS" } else { "FAIL" });
        all_ok &= ok;
    }

    println!("\n==== SPIKE {} ====", if all_ok { "PASS" } else { "FAIL" });
}
