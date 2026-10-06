//! Minimal Taffy flex behavior probe: Row[grow text child, fixed-size leaf],
//! mirroring the production toolbar style (align CENTER + padding 8/8/4/4)
//! and the production measure dispatch.
use taffy::prelude::*;

fn main() {
    let mut t: TaffyTree<()> = TaffyTree::new();
    let label = t
        .new_leaf(Style {
            flex_grow: 1.0,
            ..Default::default()
        })
        .unwrap();
    let button = t
        .new_leaf(Style {
            size: Size {
                width: Dimension::length(16.0),
                height: Dimension::length(16.0),
            },
            ..Default::default()
        })
        .unwrap();
    let row = t
        .new_with_children(
            Style {
                display: Display::Flex,
                flex_direction: FlexDirection::Row,
                align_items: Some(AlignItems::CENTER),
                padding: Rect {
                    left: LengthPercentage::length(8.0),
                    right: LengthPercentage::length(8.0),
                    top: LengthPercentage::length(4.0),
                    bottom: LengthPercentage::length(4.0),
                },
                size: Size {
                    width: Dimension::length(533.0),
                    height: Dimension::AUTO,
                },
                ..Default::default()
            },
            &[label, button],
        )
        .unwrap();

    t.compute_layout_with_measure(
        row,
        Size {
            width: AvailableSpace::Definite(533.0),
            height: AvailableSpace::MaxContent,
        },
        |known, _avail, node_id, _ctx, _style| {
            // Mirror production dispatch: label (node_type 1) measures text at
            // 24x23; button (node_type 0) gets ZERO then known-dims wrap.
            let measured = if node_id == NodeId::from(0usize) {
                Size {
                    width: 24.0,
                    height: 23.0,
                }
            } else {
                Size::ZERO
            };
            Size {
                width: known.width.unwrap_or(measured.width),
                height: known.height.unwrap_or(measured.height),
            }
        },
    )
    .unwrap();

    println!("label:  {:?}", t.layout(label).unwrap());
    println!("button: {:?}", t.layout(button).unwrap());
}
