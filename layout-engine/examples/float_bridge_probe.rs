use taffy::prelude::*;
use taffy::style::Float;

#[derive(Clone)]
struct Ctx {
    kind: u8,
    id: usize,
}

#[derive(Clone, Copy)]
struct FRect {
    x: f32,
    y: f32,
    w: f32,
    h: f32,
    right: bool,
}

#[derive(Clone, Copy)]
struct Clip {
    y_top: f32,
    y_bottom: f32,
    x: f32,
    width: f32,
}

#[derive(Clone, Copy)]
enum Kind {
    F,
    P(usize),
}

const LH: f32 = 10.0;
const GAP: f32 = 5.0;
const SPACE: f32 = 3.0;

fn clip_w(rel_y: f32, clips: &[Clip], w: f32) -> f32 {
    let mut x0: f32 = 0.0;
    let mut x1 = w;
    for c in clips {
        if c.y_bottom <= rel_y || c.y_top >= rel_y + LH {
            continue;
        }
        if c.x <= 0.0 {
            x0 = x0.max(c.x + c.width);
        } else {
            x1 = x1.min(c.x);
        }
    }
    (x1 - x0).max(1.0)
}

fn greedy(words: &[f32], mut line_w: impl FnMut(f32) -> f32, y0: f32) -> (f32, Vec<f32>) {
    let mut lines: Vec<f32> = Vec::new();
    let mut rel = 0.0;
    let mut i = 0;
    while i < words.len() {
        let lw = line_w(y0 + rel);
        let mut used = 0.0;
        let mut took = 0;
        while i + took < words.len() {
            let add = if took == 0 {
                words[i + took]
            } else {
                used + SPACE + words[i + took]
            };
            if add > lw && took > 0 {
                break;
            }
            used = add;
            took += 1;
            if used >= lw {
                break;
            }
        }
        if took == 0 {
            took = 1;
            used = words[i];
        }
        lines.push(used);
        i += took;
        rel += LH;
    }
    if lines.is_empty() {
        lines.push(0.0);
        rel = LH;
    }
    (rel, lines)
}

fn wrap_clips(words: &[f32], clips: &[Clip], w: f32) -> (f32, Vec<f32>) {
    greedy(words, |rel_y| clip_w(rel_y, clips, w), 0.0)
}

fn float_edges(abs_y: f32, floats: &[FRect], para_x: f32, para_w: f32) -> f32 {
    let mut l = para_x;
    let mut r = para_x + para_w;
    for f in floats {
        if f.y + f.h <= abs_y || f.y >= abs_y + LH {
            continue;
        }
        if f.right {
            r = r.min(f.x);
        } else {
            l = l.max(f.x + f.w);
        }
    }
    (r - l).max(1.0)
}

fn wrap_floats(words: &[f32], floats: &[FRect], para_x: f32, para_w: f32, para_abs_y: f32) -> (f32, Vec<f32>) {
    greedy(
        words,
        |abs_y| float_edges(abs_y, floats, para_x, para_w),
        para_abs_y,
    )
}

fn words(n: usize) -> Vec<f32> {
    vec![30.0; n]
}

fn run_scene(name: &str, avail: f32, fw: f32, fh: f32, paras: &[Vec<f32>], layout: &[Kind]) {
    println!("==== {} (avail={} fw={} fh={}) ====", name, avail, fw, fh);

    // ---- Algorithm Y: single-pass document-flow driver (Java-homologous) ----
    let mut y_floats: Vec<FRect> = Vec::new();
    let mut y_h = vec![0.0f32; paras.len()];
    let mut y_lines: Vec<Vec<f32>> = vec![Vec::new(); paras.len()];
    let mut cursor = 0.0f32;
    for k in layout {
        match k {
            Kind::F => {
                let fx = avail - fw;
                y_floats.push(FRect {
                    x: fx - GAP,
                    y: cursor,
                    w: fw + GAP,
                    h: fh + GAP,
                    right: true,
                });
            }
            Kind::P(idx) => {
                let (h, lines) = wrap_floats(&paras[*idx], &y_floats, 0.0, avail, cursor);
                y_h[*idx] = h;
                y_lines[*idx] = lines;
                cursor += h;
            }
        }
    }

    // ---- Algorithm X: two-or-more-pass taffy, clips replayed from taffy geometry ----
    let mut t: TaffyTree<Ctx> = TaffyTree::new();
    let mut node_ids: Vec<(Kind, NodeId)> = Vec::new();
    for k in layout {
        let (style, ctx) = match k {
            Kind::F => (
                Style {
                    float: Float::Right,
                    size: Size {
                        width: Dimension::length(fw),
                        height: Dimension::length(fh),
                    },
                    ..Default::default()
                },
                Ctx { kind: 0, id: usize::MAX },
            ),
            Kind::P(idx) => (
                Style {
                    size: Size {
                        width: Dimension::AUTO,
                        height: Dimension::AUTO,
                    },
                    ..Default::default()
                },
                Ctx { kind: 1, id: *idx },
            ),
        };
        let id = t.new_leaf_with_context(style, ctx).unwrap();
        node_ids.push((*k, id));
    }
    let child_ids: Vec<NodeId> = node_ids.iter().map(|(_, id)| *id).collect();
    let root = t
        .new_with_children(
            Style {
                display: Display::Block,
                size: Size {
                    width: Dimension::length(avail),
                    height: Dimension::AUTO,
                },
                ..Default::default()
            },
            &child_ids,
        )
        .unwrap();

    let n = paras.len();
    let mut clips_per_para: Vec<Vec<Clip>> = vec![Vec::new(); n];
    let mut history: Vec<Vec<f32>> = Vec::new();
    let mut x_lines: Vec<Vec<f32>> = vec![Vec::new(); n];
    let mut converged = false;
    let mut oscillate = false;
    let max_pass = 6;

    for pass in 0..max_pass {
        let mut lines_out: Vec<Vec<f32>> = vec![Vec::new(); n];
        let clips_ref = &clips_per_para;
        let paras_ref = paras;
        let lines_ref = &mut lines_out;
        t.compute_layout_with_measure(
            root,
            Size {
                width: AvailableSpace::Definite(avail),
                height: AvailableSpace::MaxContent,
            },
            move |known, available, _node_id, ctx, _style| {
                let c = match ctx {
                    Some(c) => c,
                    None => return Size::ZERO,
                };
                if c.kind == 0 {
                    return Size {
                        width: known.width.unwrap_or(0.0),
                        height: known.height.unwrap_or(0.0),
                    };
                }
                let id = c.id;
                let w = match available.width {
                    AvailableSpace::Definite(x) => x,
                    _ => avail,
                };
                let (h, lines) = wrap_clips(&paras_ref[id], &clips_ref[id], w);
                lines_ref[id] = lines;
                Size {
                    width: known.width.unwrap_or(w),
                    height: known.height.unwrap_or(h),
                }
            },
        )
        .unwrap();

        let mut float_rect: Option<(f32, f32, f32, f32)> = None;
        for (k, id) in &node_ids {
            if let Kind::F = k {
                let l = t.layout(*id).unwrap();
                float_rect = Some((l.location.x, l.location.y, l.size.width, l.size.height));
            }
        }
        let (frx, fry, _frw, frh) = float_rect.unwrap();
        let reg_x = frx - GAP;
        let reg_h = frh + GAP;

        let mut new_clips: Vec<Vec<Clip>> = vec![Vec::new(); n];
        let mut heights = vec![0.0f32; n];
        for (k, id) in &node_ids {
            if let Kind::P(idx) = k {
                let l = t.layout(*id).unwrap();
                let px = l.location.x;
                let py = l.location.y;
                let pw = l.size.width;
                let ph = l.size.height;
                heights[*idx] = ph;
                if reg_h > 0.0 && !(fry + reg_h <= py || fry >= py + ph) {
                    let y_top = fry.max(py) - py;
                    let y_bottom = (fry + reg_h).min(py + ph) - py;
                    if y_bottom > y_top {
                        new_clips[*idx].push(Clip {
                            y_top,
                            y_bottom,
                            x: reg_x - px,
                            width: ((px + pw) - reg_x).max(1.0),
                        });
                    }
                }
            }
        }
        x_lines = lines_out;
        clips_per_para = new_clips;

        let stable = history.last().map_or(false, |prev| *prev == heights);
        history.push(heights.clone());
        if pass > 0 && stable {
            converged = true;
            break;
        }
        let len = history.len();
        if len >= 3 && history[len - 1] == history[len - 3] && history[len - 1] != history[len - 2] {
            oscillate = true;
            break;
        }
    }

    println!("  X pass heights:");
    for (i, h) in history.iter().enumerate() {
        println!("    pass {}: {:?}", i, h);
    }
    println!(
        "  X status: {}{}",
        if converged { "CONVERGED" } else { "NOT-CONVERGED" },
        if oscillate { " OSCILLATE" } else { "" }
    );
    println!("  X final lines: {:?}", x_lines);
    println!("  Y heights:     {:?}", y_h);
    println!("  Y lines:       {:?}", y_lines);

    let x_final = history.last().cloned().unwrap_or_default();
    let h_match = x_final == y_h;
    let l_match = x_lines == y_lines;
    println!(
        "  VERDICT: heights {} | lines {} | oscillate {}",
        if h_match { "MATCH" } else { "DIVERGE" },
        if l_match { "MATCH" } else { "DIVERGE" },
        oscillate
    );
    println!();
}

fn probe_float_display() {
    println!("==== float under FLEX vs BLOCK container ====");
    let avail = 400.0;
    let fw = 120.0;
    let fh = 80.0;

    for (label, display) in [("FLEX", Display::Flex), ("BLOCK", Display::Block)] {
        let mut t: TaffyTree<Ctx> = TaffyTree::new();
        let f = t
            .new_leaf_with_context(
                Style {
                    float: Float::Right,
                    size: Size {
                        width: Dimension::length(fw),
                        height: Dimension::length(fh),
                    },
                    ..Default::default()
                },
                Ctx { kind: 0, id: 0 },
            )
            .unwrap();
        let p = t
            .new_leaf_with_context(
                Style::default(),
                Ctx { kind: 1, id: 0 },
            )
            .unwrap();
        let root = t
            .new_with_children(
                Style {
                    display,
                    flex_direction: FlexDirection::Column,
                    size: Size {
                        width: Dimension::length(avail),
                        height: Dimension::AUTO,
                    },
                    ..Default::default()
                },
                &[f, p],
            )
            .unwrap();
        t.compute_layout_with_measure(
            root,
            Size {
                width: AvailableSpace::Definite(avail),
                height: AvailableSpace::MaxContent,
            },
            |known, _a, _n, ctx, _s| {
                let c = match ctx {
                    Some(c) => c,
                    None => return Size::ZERO,
                };
                if c.kind == 1 {
                    return Size { width: 200.0, height: 30.0 };
                }
                Size {
                    width: known.width.unwrap_or(0.0),
                    height: known.height.unwrap_or(0.0),
                }
            },
        )
        .unwrap();
        let fl = t.layout(f).unwrap();
        let pl = t.layout(p).unwrap();
        println!(
            "  {} -> float loc=({:.0},{:.0}) size=({:.0},{:.0}) | para loc=({:.0},{:.0})",
            label, fl.location.x, fl.location.y, fl.size.width, fl.size.height, pl.location.x,
            pl.location.y
        );
    }
    println!("  (float active = para.y stays at 0 and float.x right-aligned; float ignored under flex = para.y pushed below float height)");
    println!();
}

fn main() {
    probe_float_display();
    run_scene(
        "S1 float-first, bottom lands inside para2",
        400.0,
        120.0,
        75.0,
        &[words(36), words(36), words(36)],
        &[Kind::F, Kind::P(0), Kind::P(1), Kind::P(2)],
    );
    run_scene(
        "S2 float after a short paragraph",
        400.0,
        120.0,
        75.0,
        &[words(4), words(36), words(36)],
        &[Kind::P(0), Kind::F, Kind::P(1), Kind::P(2)],
    );
}
