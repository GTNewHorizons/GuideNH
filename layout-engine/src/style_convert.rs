use crate::fb::Style as FbStyle;
use taffy::prelude::*;
use taffy::style::{Clear, Float, Overflow};

/// Convert FlatBuffer Dimension → Taffy Dimension
fn fb_dim(unit: u8, value: f32) -> Dimension {
    match unit {
        1 => Dimension::length(value),
        2 => Dimension::percent(value / 100.0),
        _ => Dimension::AUTO,
    }
}

/// Convert FlatBuffer Dimension → LengthPercentageAuto (for margin)
fn fb_to_lpa(unit: u8, value: f32, auto: bool) -> LengthPercentageAuto {
    if auto {
        return LengthPercentageAuto::AUTO;
    }
    match unit {
        1 => LengthPercentageAuto::length(value),
        2 => LengthPercentageAuto::percent(value / 100.0),
        _ => LengthPercentageAuto::AUTO,
    }
}

/// Convert FlatBuffer Dimension → LengthPercentage (for padding/border)
fn fb_to_lp(unit: u8, value: f32) -> LengthPercentage {
    match unit {
        1 => LengthPercentage::length(value),
        2 => LengthPercentage::percent(value / 100.0),
        _ => LengthPercentage::length(0.0),
    }
}

/// Convert a FlatBuffer byte to Dimension (for simple getters)
fn dim_from_opt(opt: Option<crate::fb::Dimension>) -> (u8, f32) {
    match opt {
        Some(d) => (d.unit() as u8, d.value()),
        None => (0, 0.0),
    }
}

/// Full conversion: FlatBuffer Style → Taffy Style
pub fn flat_style_to_taffy(fb: &FbStyle) -> Style {
    let (gw, gv) = dim_from_opt(fb.gap_w());
    let (gh, hv) = dim_from_opt(fb.gap_h());
    let (sw, sv) = dim_from_opt(fb.size_w());
    let (sh, shv) = dim_from_opt(fb.size_h());
    let (mnw, mnwv) = dim_from_opt(fb.min_w());
    let (mnh, mnhv) = dim_from_opt(fb.min_h());
    let (mxw, mxwv) = dim_from_opt(fb.max_w());
    let (mxh, mxhv) = dim_from_opt(fb.max_h());
    let (fbv, fbwv) = dim_from_opt(fb.flex_basis());
    let (it, itv) = dim_from_opt(fb.inset_top());
    let (ir, irv) = dim_from_opt(fb.inset_right());
    let (ib, ibv) = dim_from_opt(fb.inset_bottom());
    let (il, ilv) = dim_from_opt(fb.inset_left());

    Style {
        display: match fb.display() {
            1 => Display::Grid,
            2 => Display::Block,
            3 => Display::None,
            _ => Display::Flex,
        },
        flex_direction: match fb.flex_direction() {
            1 => FlexDirection::Column,
            _ => FlexDirection::Row,
        },
        flex_wrap: match fb.flex_wrap() {
            1 => FlexWrap::Wrap,
            _ => FlexWrap::NoWrap,
        },
        align_items: match fb.align_items() {
            1 => Some(AlignItems::CENTER),
            2 => Some(AlignItems::FLEX_END),
            3 => Some(AlignItems::STRETCH),
            _ => Some(AlignItems::FLEX_START),
        },
        align_self: match fb.align_self() {
            1 => Some(AlignSelf::FLEX_START),
            2 => Some(AlignSelf::CENTER),
            3 => Some(AlignSelf::FLEX_END),
            4 => Some(AlignSelf::STRETCH),
            _ => None,
        },
        justify_content: match fb.justify_content() {
            1 => Some(JustifyContent::CENTER),
            2 => Some(JustifyContent::FLEX_END),
            3 => Some(JustifyContent::SPACE_BETWEEN),
            4 => Some(JustifyContent::SPACE_AROUND),
            5 => Some(JustifyContent::SPACE_EVENLY),
            _ => Some(JustifyContent::FLEX_START),
        },
        gap: Size { width: fb_to_lp(gw, gv), height: fb_to_lp(gh, hv) },
        size: Size {
            width: fb_dim(sw, sv),
            height: fb_dim(sh, shv),
        },
        min_size: Size {
            width: fb_dim(mnw, mnwv),
            height: fb_dim(mnh, mnhv),
        },
        max_size: Size {
            width: fb_dim(mxw, mxwv),
            height: fb_dim(mxh, mxhv),
        },
        aspect_ratio: if fb.aspect_ratio() > 0.0 {
            Some(fb.aspect_ratio())
        } else {
            None
        },
        margin: Rect {
            left: fb_to_lpa(1, fb.margin_left(), fb.margin_auto_left()),
            right: fb_to_lpa(1, fb.margin_right(), fb.margin_auto_right()),
            top: fb_to_lpa(1, fb.margin_top(), fb.margin_auto_top()),
            bottom: fb_to_lpa(1, fb.margin_bottom(), fb.margin_auto_bottom()),
        },
        padding: Rect {
            left: fb_to_lp(1, fb.padding_left()),
            right: fb_to_lp(1, fb.padding_right()),
            top: fb_to_lp(1, fb.padding_top()),
            bottom: fb_to_lp(1, fb.padding_bottom()),
        },
        border: Rect {
            left: fb_to_lp(1, fb.border_left()),
            right: fb_to_lp(1, fb.border_right()),
            top: fb_to_lp(1, fb.border_top()),
            bottom: fb_to_lp(1, fb.border_bottom()),
        },
        overflow: taffy::geometry::Point {
            x: match fb.overflow() {
                1 => Overflow::Hidden,
                2 => Overflow::Scroll,
                _ => Overflow::Visible,
            },
            y: match fb.overflow() {
                1 => Overflow::Hidden,
                2 => Overflow::Scroll,
                _ => Overflow::Visible,
            },
        },
        flex_grow: fb.flex_grow(),
        flex_shrink: fb.flex_shrink(),
        flex_basis: fb_dim(fbv, fbwv),
        position: match fb.position() {
            1 => Position::Absolute,
            _ => Position::Relative,
        },
        inset: Rect {
            left: fb_to_lpa(it, itv, false),
            right: fb_to_lpa(ir, irv, false),
            top: fb_to_lpa(ib, ibv, false),
            bottom: fb_to_lpa(il, ilv, false),
        },
        // Float and Clear are feature-gated behind float_layout
        // which is enabled in our Cargo.toml
        float: if fb.float() == 1 {
            Float::Left
        } else if fb.float() == 2 {
            Float::Right
        } else {
            Float::None
        },
        clear: match fb.clear() {
            1 => Clear::Left,
            2 => Clear::Right,
            3 => Clear::Both,
            _ => Clear::None,
        },
        ..Default::default()
    }
}
