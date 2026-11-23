# Whist 0.24.0

Whist 0.24.0 separates portable presentation contracts from Android and Web adapters and fixes fractional mobile viewport parsing.

## Changes
- Keep shared geometry, text roles and phase continuity in the portable presentation core.
- Normalize mobile CSS height before the integer geometry boundary while preserving invalid-input errors.
- Reuse unchanged component owners and preserve original native execution provenance.

## Validation
17 admitted component selections cover 1,583 invocations; seven installed-APK sanity flows passed on Android API 28. The public Web sanity passed two hands with 16 steps, 142 assertions and four inline captures.

## Audit
[Extent qualification audit](https://github.com/crististefanita/whist/blob/305ae4b74a4dacc6a00863b5f40d0cc822ba9888/release/0.24.0/reports/README.md)
