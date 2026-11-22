export const REFERENCE = Object.freeze({ width: 414, height: 896 });
export const MIN_DESKTOP_SCALE = 0.68;
export const ADAPTIVE_SURFACE = Object.freeze({ maxWidth: 768 });
export const MENU_TYPOGRAPHY = Object.freeze({
  title: 2.34, section: 1.3125, action: 1.5, selection: 1.125, helper: 1.21875, dedication: 1.40625
});
export const SUMMARY_TYPOGRAPHY = Object.freeze({ progress: 1.5, bid: 1.35, progressLeading: 1.40625, bidLeading: 1.359375 });

/** Content stays in CSS pixels; text zoom never becomes a whole-scene transform. */
export function adaptiveViewport(width, height) {
  if (![width, height].every(value => Number.isFinite(value) && value > 0))
    throw new RangeError("The usable viewport must have positive finite dimensions");
  const available = Math.min(width, ADAPTIVE_SURFACE.maxWidth);
  return { width: available, height, scale: 1, frameWidth: available, frameHeight: height };
}

/** Explicit menu roles retain the approved default sizes and follow the user's root font. */
export function surfaceTypography() {
  return { ...Object.fromEntries(Object.entries(MENU_TYPOGRAPHY).map(([role, rem]) => ["--menu-" + role + "-size", rem + "rem"])),
    "--table-progress-emphasis": String(SUMMARY_TYPOGRAPHY.progress), "--table-bid-emphasis": String(SUMMARY_TYPOGRAPHY.bid),
    "--table-progress-leading": SUMMARY_TYPOGRAPHY.progressLeading + "rem", "--table-bid-leading": SUMMARY_TYPOGRAPHY.bidLeading + "rem" };
}

export function isPhone(userAgent) {
  return /Android|iPhone|iPod/.test(userAgent);
}

export function fitTextWidth(size, measured, available) {
  return size * Math.min(1, Math.max(1, available) / Math.max(1, measured));
}

export function viewportGeometry(width, height, mobile) {
  if (![width, height].every(value => Number.isFinite(value) && value > 0)) {
    throw new RangeError("The usable viewport must have positive finite dimensions");
  }
  if (mobile) return { width, height, scale: 1, frameWidth: width, frameHeight: height };

  // Keep the readable desktop floor when width permits. When it does not, fit both axes.
  const fitScale = Math.min(height / REFERENCE.height, width / REFERENCE.width);
  const readableFloorFits = width >= REFERENCE.width * MIN_DESKTOP_SCALE && height >= REFERENCE.height * MIN_DESKTOP_SCALE;
  const scale = fitScale >= MIN_DESKTOP_SCALE || !readableFloorFits ? fitScale : MIN_DESKTOP_SCALE;
  return { ...REFERENCE, scale, frameWidth: REFERENCE.width * scale, frameHeight: REFERENCE.height * scale };
}
/** CSS pixels remain logical units; desktop zoom is handled by the viewport adapter. */
export function textScale(fontSize) {
  const scale = Number.parseFloat(fontSize) / 16;
  return Number.isFinite(scale) && scale >= .5 && scale <= 3 ? scale : 1;
}
