"""
Remove AI-generated watermark ("AI生成 / WORKBUDDY") from ImageGen output images.
Uses OpenCV inpainting (Telea algorithm) for seamless content-aware removal.

Watermark location: bottom-right corner.
Handles both RGBA (transparent PNG) and RGB (opaque) images.
"""

import os
import sys
import cv2
import numpy as np

# ── Configuration ──────────────────────────────────────────────
# Watermark bounding box (from bottom-right corner) - generous size
MARGIN_RIGHT = 0    # px from right edge
MARGIN_BOTTOM = 0   # px from bottom edge
BOX_WIDTH = 200     # watermark area width (generous)
BOX_HEIGHT = 80     # watermark area height (generous)

# Inpainting algorithm
INPAINT_METHOD = cv2.INPAINT_TELEA
INPAINT_RADIUS = 7


def remove_watermark(image_path: str, output_path: str = None) -> bool:
    """Remove watermark from an image file. Returns True if modified."""
    img = cv2.imread(image_path, cv2.IMREAD_UNCHANGED)
    if img is None:
        print(f"  [ERROR] Cannot read: {image_path}")
        return False

    h, w = img.shape[:2]
    has_alpha = img.shape[2] == 4 if len(img.shape) == 3 else False

    # Calculate watermark region (bottom-right)
    x1 = max(0, w - BOX_WIDTH - MARGIN_RIGHT)
    y1 = max(0, h - BOX_HEIGHT - MARGIN_BOTTOM)
    x2 = w - MARGIN_RIGHT
    y2 = h - MARGIN_BOTTOM

    print(f"  Size: {w}x{h}, alpha={has_alpha}, box: ({x1},{y1})-({x2},{y2})")

    # Create mask (white = area to inpaint)
    mask = np.zeros((h, w), dtype=np.uint8)
    mask[y1:y2, x1:x2] = 255

    # For RGBA images, process color and alpha separately
    if has_alpha:
        bgr = img[:, :, :3]
        alpha = img[:, :, 3]
        cleaned_bgr = cv2.inpaint(bgr, mask, INPAINT_RADIUS, INPAINT_METHOD)
        cleaned_alpha = cv2.inpaint(alpha, mask, INPAINT_RADIUS, INPAINT_METHOD)
        result = cv2.merge([cleaned_bgr[:, :, 0],
                            cleaned_bgr[:, :, 1],
                            cleaned_bgr[:, :, 2],
                            cleaned_alpha])
    else:
        result = cv2.inpaint(img, mask, INPAINT_RADIUS, INPAINT_METHOD)

    # Verify modification using pixel comparison (not file size!)
    orig_img = cv2.imread(image_path, cv2.IMREAD_UNCHANGED)
    diff = np.abs(orig_img.astype(int) - result.astype(int))
    changed_pixels = np.any(diff > 0, axis=2).sum()
    max_diff = diff.max()

    out_path = output_path or image_path
    cv2.imwrite(out_path, result)

    modified = changed_pixels > 0
    status = "[MODIFIED]" if modified else "[NO CHANGE]"
    print(f"  {status} {changed_pixels}px changed, max_diff={max_diff}")
    return modified


def main():
    base_dir = r"U:\xiaoswz-reader\assets"

    char_dir = os.path.join(base_dir, "character")
    bg_dir = os.path.join(base_dir, "background")

    all_files = []
    for d in [char_dir, bg_dir]:
        if os.path.isdir(d):
            all_files.extend([
                os.path.join(d, f) for f in sorted(os.listdir(d))
                if f.lower().endswith('.png')
            ])

    print(f"Processing {len(all_files)} asset files\n")
    print("=" * 60)

    modified_count = 0
    for filepath in all_files:
        fname = os.path.basename(filepath)
        print(f"\n{fname}:")
        if remove_watermark(filepath):
            modified_count += 1

    print("\n" + "=" * 60)
    print(f"DONE: {modified_count}/{len(all_files)} files had watermarks removed")


if __name__ == "__main__":
    main()
