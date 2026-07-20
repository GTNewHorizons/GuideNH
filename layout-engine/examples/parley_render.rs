//! Side-by-side cosmic vs parley render — same text, cosmic on top, parley
//! below, saved to target/parley_compare.png. Pass --window to also open a
//! minifb preview. Run: cargo run --example parley_render --features window-test

use std::fs;
use std::sync::Arc;

use cosmic_text::{Attrs, Buffer, Color, Family, FontSystem, Metrics, Shaping, SwashCache, Wrap};
use parley::fontique;
use parley::{
    Alignment, AlignmentOptions, FontContext, FontFamily, Layout, LayoutContext, LineHeight,
    PositionedLayoutItem, StyleProperty,
};

const FAMILY: &str = "Microsoft YaHei";
const SIZE: f32 = 24.0;
const LH: f32 = 10.0 / 9.0;
const WIDTH: usize = 940;
const BG: u32 = 0xFF1E1E1E;

#[derive(Clone, Debug, Default, PartialEq)]
struct SpanBrush(u32);

const BLOCKS: [&str; 3] = [
    "Hello World! The quick brown fox\njumps over the lazy dog.\nABCDEFGH abcdefgh 0123456789",
    "中文测试 日本語テスト 한국어\nGuideNH 是一个 Minecraft 1.7.10 的攻略书 mod，\n支持富文本排版、LaTeX 公式与函数图像。",
    "Regular Bold Italic\nLorem ipsum dolor sit amet, consectetur\nadipiscing elit, sed do eiusmod tempor.",
];

fn blend(px: &mut [u32], w: usize, x: i32, y: i32, argb: u32) {
    if x < 0 || y < 0 || x >= w as i32 || y >= (px.len() / w) as i32 {
        return;
    }
    let a = ((argb >> 24) & 0xFF) as u32;
    if a == 0 {
        return;
    }
    let idx = y as usize * w + x as usize;
    if a == 255 {
        px[idx] = argb;
        return;
    }
    let dst = px[idx];
    let (sr, sg, sb) = ((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    let (dr, dg, db) = ((dst >> 16) & 0xFF, (dst >> 8) & 0xFF, dst & 0xFF);
    let (r, g, b) = (
        (sr * a + dr * (255 - a)) / 255,
        (sg * a + dg * (255 - a)) / 255,
        (sb * a + db * (255 - a)) / 255,
    );
    px[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
}

/// Render one cosmic-text block; returns height consumed.
fn render_cosmic(
    fs: &mut FontSystem,
    sc: &mut SwashCache,
    text: &str,
    px: &mut [u32],
    w: usize,
    x0: i32,
    y0: i32,
) -> i32 {
    let mut buffer = Buffer::new(fs, Metrics::new(SIZE, SIZE * LH));
    buffer.set_size(Some((w - 20) as f32), None);
    buffer.set_wrap(Wrap::WordOrGlyph);
    buffer.set_text(text, &Attrs::new().family(Family::Name(FAMILY)), Shaping::Advanced, None);
    buffer.shape_until_scroll(fs, false);
    let white = Color::rgb(255, 255, 255);
    let mut max_bottom = 0.0f32;
    for run in buffer.layout_runs() {
        max_bottom = max_bottom.max(run.line_top + run.line_height);
        for glyph in run.glyphs {
            let pg = glyph.physical((0., run.line_y), 1.0);
            sc.with_pixels(fs, pg.cache_key, white, |x, y, color| {
                // color.0 is ARGB with coverage-modulated alpha — pass through.
                blend(px, w, x0 + pg.x + x, y0 + pg.y + y, color.0);
            });
        }
    }
    max_bottom as i32
}

/// Render one parley block; returns height consumed.
fn render_parley(
    fcx: &mut FontContext,
    lcx: &mut LayoutContext<SpanBrush>,
    text: &str,
    px: &mut [u32],
    w: usize,
    x0: i32,
    y0: i32,
) -> i32 {
    let mut builder = lcx.ranged_builder(fcx, text, 1.0, true);
    builder.push_default(StyleProperty::FontSize(SIZE));
    builder.push_default(StyleProperty::LineHeight(LineHeight::FontSizeRelative(LH)));
    builder.push_default(StyleProperty::FontFamily(FontFamily::named(FAMILY)));
    builder.push_default(StyleProperty::Brush(SpanBrush(0)));
    let mut layout: Layout<SpanBrush> = builder.build(text);
    layout.break_all_lines(Some((w - 20) as f32));
    layout.align(Alignment::Start, AlignmentOptions::default());

    for line in layout.lines() {
        for item in line.items() {
            if let PositionedLayoutItem::GlyphRun(gr) = item {
                let fd = gr.run().font();
                let Some(font_ref) = swash::FontRef::from_index(fd.data.data(), fd.index as usize)
                else {
                    continue;
                };
                let mut ctx = swash::scale::ScaleContext::new();
                let mut scaler = ctx.builder(font_ref).size(SIZE).hint(true).build();
                for g in gr.positioned_glyphs() {
                    let Some(img) = swash::scale::Render::new(&[
                        swash::scale::Source::ColorOutline(0),
                        swash::scale::Source::ColorBitmap(swash::scale::StrikeWith::BestFit),
                        swash::scale::Source::Outline,
                    ])
                    .format(swash::zeno::Format::Alpha)
                    .render(&mut scaler, g.id as u16) else {
                        continue;
                    };
                    // g.y is the baseline; placement.left/top offset the bitmap
                    let gx = (x0 as f32 + g.x) as i32 + img.placement.left;
                    let gy = (y0 as f32 + g.y) as i32 - img.placement.top;
                    for row in 0..img.placement.height as i32 {
                        for col in 0..img.placement.width as i32 {
                            let a = img.data[(row * img.placement.width as i32 + col) as usize];
                            blend(px, w, gx + col, gy + row, ((a as u32) << 24) | 0xFFFFFF);
                        }
                    }
                }
            }
        }
    }
    layout.height() as i32
}

fn main() {
    let windir = std::env::var("WINDIR").unwrap_or_else(|_| "C:\\Windows".into());
    let base = std::path::PathBuf::from(windir).join("Fonts");
    let font_path = ["msyh.ttc", "msyh.ttf", "segoeui.ttf", "arial.ttf"]
        .iter()
        .map(|n| base.join(n))
        .find(|p| p.exists())
        .expect("no system font");
    let font_data = fs::read(&font_path).expect("read font");
    eprintln!("font: {}", font_path.display());

    let mut cfs = FontSystem::new();
    cfs.db_mut().load_font_data(font_data.clone());
    let mut scache = SwashCache::new();
    let mut fcx = FontContext::new();
    fcx.collection.register_fonts(fontique::Blob::new(Arc::new(font_data)), None);
    let mut lcx: LayoutContext<SpanBrush> = LayoutContext::new();

    let height = 3000usize;
    let mut px = vec![BG; WIDTH * height];
    let mut y = 10i32;
    for text in BLOCKS {
        // cosmic on top
        y += render_cosmic(&mut cfs, &mut scache, text, &mut px, WIDTH, 10, y);
        y += 6;
        // separator
        for x in 0..WIDTH {
            blend(&mut px, WIDTH, x as i32, y, 0xFF884400);
        }
        y += 2;
        // parley below
        y += render_parley(&mut fcx, &mut lcx, text, &mut px, WIDTH, 10, y);
        y += 30;
    }
    let used = (y + 10) as usize;

    // Save PNG
    let mut rgba = Vec::with_capacity(WIDTH * used * 4);
    for p in &px[..WIDTH * used] {
        rgba.push(((p >> 16) & 0xFF) as u8);
        rgba.push(((p >> 8) & 0xFF) as u8);
        rgba.push((p & 0xFF) as u8);
        rgba.push((p >> 24) as u8);
    }
    let out = std::path::Path::new("target/parley_compare.png");
    image::save_buffer(
        out,
        &rgba,
        WIDTH as u32,
        used as u32,
        image::ExtendedColorType::Rgba8,
    )
    .expect("save png");
    eprintln!("saved {}", out.display());

    if std::env::args().any(|a| a == "--window") {
        let mut window = minifb::Window::new(
            "cosmic (top) vs parley (bottom)",
            WIDTH,
            used.min(1400),
            minifb::WindowOptions { scale: minifb::Scale::X1, ..Default::default() },
        )
        .expect("window");
        window.limit_update_rate(Some(std::time::Duration::from_micros(16600)));
        while window.is_open() && !window.is_key_down(minifb::Key::Escape) {
            window.update_with_buffer(&px[..WIDTH * used.min(1400)], WIDTH, used.min(1400)).unwrap_or(());
        }
    }
}
