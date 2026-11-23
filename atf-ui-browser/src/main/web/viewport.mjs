export const REFERENCE = Object.freeze({ width: 414, height: 896 });
export const MIN_DESKTOP_SCALE = 0.68;
export const ADAPTIVE_SURFACE = Object.freeze({ width: 393, height: 626 });
export const MENU_TYPOGRAPHY = Object.freeze({
  title: 1.5625, section: .8125, action: 1, selection: .875, helper: .875, dedication: .9375
});
export const SUMMARY_TYPOGRAPHY = Object.freeze({ progress: 1, bid: 1, progressLeading: .9375, bidLeading: .9375 });
export const SCORE_TABLE_EMPHASIS = 1;

/** One height-led fitting scale keeps all presentation roles proportional. */
export function adaptiveViewport(width, height, contentHeight, mobile = false) {
  contentHeight ??= mobile ? height : ADAPTIVE_SURFACE.height;
  if (typeof mobile !== 'boolean' || ![width, height, contentHeight].every(value => Number.isFinite(value) && value > 0))
    throw new RangeError("The usable viewport must have positive finite dimensions");
  const logicalHeight = contentHeight;
  const available = mobile ? width : ADAPTIVE_SURFACE.width;
  const scale = Math.min(height / logicalHeight, width / available);
  return { width: available, height: logicalHeight, scale, frameWidth: available * scale, frameHeight: logicalHeight * scale };
}

/** Explicit menu roles retain the approved default sizes and follow the user's root font. */
export function surfaceTypography() {
  return { ...Object.fromEntries(Object.entries(MENU_TYPOGRAPHY).map(([role, rem]) => ["--menu-" + role + "-size", rem + "rem"])),
    "--table-progress-emphasis": String(SUMMARY_TYPOGRAPHY.progress), "--table-bid-emphasis": String(SUMMARY_TYPOGRAPHY.bid),
    "--table-progress-leading": SUMMARY_TYPOGRAPHY.progressLeading + "rem", "--table-bid-leading": SUMMARY_TYPOGRAPHY.bidLeading + "rem",
    "--score-table-emphasis": String(SCORE_TABLE_EMPHASIS) };
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
