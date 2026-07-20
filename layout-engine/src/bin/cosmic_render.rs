//! Pure cosmic-text render test. Uses glyph.physical() — the official API —
//! which handles font_id, cache_key_flags, Y-axis hinting, and subpixel offsets.

use std::fs;

use cosmic_text::{
    Attrs, Buffer, Color, Family, FontSystem, Metrics, Shaping, SwashCache,
};

#[cfg(target_os = "windows")]
extern "system" {
    fn SetProcessDPIAware() -> i32;
}

fn main() {
    #[cfg(target_os = "windows")]
    unsafe { SetProcessDPIAware(); }

    let mut font_system = FontSystem::new();
    let font_path = find_system_font();
    let font_data = fs::read(&font_path).expect("failed to read font");
    font_system.db_mut().load_font_data(font_data);
    eprintln!("font: {}", font_path.display());

    let font_size = 24.0;
    let line_height = font_size * 1.5;
    let metrics = Metrics::new(font_size, line_height);
    let mut buffer = Buffer::new(&mut font_system, metrics);
    buffer.set_size(Some(900.0), Some(700.0));

    let text = concat!(
        "Hello World! The quick brown fox\n",
        "jumps over the lazy dog.\n",
        "ABCDEFGH abcdefgh 0123456789\n",
        "!@#$%^&*()_+-=[]{}|;':\",./<>?\n",
        "\n中文测试 日本語テスト 한국어\n",
        "The five boxing wizards jump quickly.\n",
        "Sphinx of black quartz, judge my vow."
    );
    let attrs = Attrs::new().family(Family::SansSerif);
    buffer.set_text(text, &attrs, Shaping::Advanced, None);
    buffer.shape_until_scroll(&mut font_system, false);

    let (w, h): (usize, usize) = (900, 700);
    let mut pixels: Vec<u32> = vec![0xFF1E1E1Eu32; w * h];
    let mut swash_cache = SwashCache::new();
    let white = Color::rgb(255, 255, 255);

    for run in buffer.layout_runs() {
        for glyph in run.glyphs {
            // Use cosmic-text's official physical() — handles font_id,
            // cache_key_flags, Y-axis hinting truncation, and subpixel offsets.
            let pg = glyph.physical((0., run.line_y), 1.0);

            swash_cache.with_pixels(&mut font_system, pg.cache_key, white, |x, y, color| {
                let px = pg.x + x;
                let py = pg.y + y;
                if px < 0 || py < 0 || px >= w as i32 || py >= h as i32 {
                    return;
                }
                let idx = py as usize * w + px as usize;
                let src_a = (color.0 >> 24) & 0xFF;
                if src_a == 0 {
                    return;
                }
                if src_a == 255 {
                    pixels[idx] = color.0 | 0xFF000000;
                    return;
                }
                let dst = pixels[idx];
                let src_r = (color.0 >> 16) & 0xFF;
                let src_g = (color.0 >> 8) & 0xFF;
                let src_b = color.0 & 0xFF;
                let dst_r = (dst >> 16) & 0xFF;
                let dst_g = (dst >> 8) & 0xFF;
                let dst_b = dst & 0xFF;
                let r = ((src_r * src_a + dst_r * (255 - src_a)) / 255) as u32;
                let g = ((src_g * src_a + dst_g * (255 - src_a)) / 255) as u32;
                let b = ((src_b * src_a + dst_b * (255 - src_a)) / 255) as u32;
                pixels[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            });
        }
    }

    eprintln!("rendered {}x{}", w, h);

    let mut window = minifb::Window::new(
        &format!(
            "cosmic-text {}px  {}",
            font_size,
            font_path.file_name().unwrap().to_string_lossy()
        ),
        w,
        h,
        minifb::WindowOptions {
            scale: minifb::Scale::X1, // disable DPI scaling
            ..minifb::WindowOptions::default()
        },
    )
    .expect("window");

    window.limit_update_rate(Some(std::time::Duration::from_micros(16600)));
    while window.is_open() && !window.is_key_down(minifb::Key::Escape) {
        window.update_with_buffer(&pixels, w, h).unwrap_or(());
    }
}

fn find_system_font() -> std::path::PathBuf {
    let windir = std::env::var("WINDIR").unwrap_or_else(|_| "C:\\Windows".into());
    let base = std::path::PathBuf::from(windir).join("Fonts");
    for name in &[
        "msyh.ttc", "msyh.ttf", "simsun.ttc",
        "arial.ttf", "segoeui.ttf", "consola.ttf",
    ] {
        let p = base.join(name);
        if p.exists() {
            return p;
        }
    }
    panic!("No system font in {}", base.display());
}
