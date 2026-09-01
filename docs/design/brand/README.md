# Eyespie brand

The canonical Eyespie mark is the **Micrantha Lens**: a five-lobed *Hackelia micrantha* flower abstracted into a camera lens / aperture.

## Design intent

- masculine pastel rather than bright or playful;
- botanical first, computer-vision / optics second;
- no literal eyeball or cartoon spy imagery;
- simple enough to remain recognizable at launcher-icon sizes;
- compatible with Android adaptive/themed icons and iOS application icons;
- launch and application surfaces use the same restrained palette rather than introducing a separate splash identity.

## Palette

| Token | Value | Use |
| --- | --- | --- |
| field | `#D9E3DF` | cool sage-stone background |
| petal | `#829FC0` | dusty steel/periwinkle outer petals |
| petal-inner | `#6F8CA8` | deeper petal/aperture geometry and system accent |
| throat | `#EEE7CD` | pale floral throat / secondary surface |
| iris | `#B59C69` | restrained ochre iris / tertiary accent |
| pupil | `#263947` | deep slate optical center / high-contrast foreground |
| ink | `#314956` | primary supporting dark slate |
| white | `#F5F5F0` | highlight / primary surface |

`palette.json` is the machine-readable palette contract. Platform and Compose representations are validated against it by `scripts/validate_brand_assets.py`.

## Sources

- `eyespie-app-icon.svg` — canonical square icon including the field color.
- `eyespie-mark.svg` — transparent full-color mark.
- `eyespie-mark-monochrome.svg` — monochrome source for themed/symbolic usage.
- `eyespie-app-icon-1024.png` — store/master raster used by the iOS asset catalog.
- `palette.json` — canonical palette values and semantic roles.

Platform resources live with their platform projects rather than being duplicated here.

## Runtime application

- Compose uses `EyespieTheme`, with the field color as the application background, cream/white surfaces, deep-slate primary/foreground colors, and restrained blue/ochre accents.
- Android uses the field color for the starting window. Android 12+ explicitly uses the Micrantha launcher foreground over the same field color for the system splash; older supported Android versions retain the branded starting background.
- iOS uses `BrandLaunchBackground` for the system launch screen and intentionally does not add a separate launch image. The application icon remains the visual mark while launch stays quiet and transitions into the field-colored app shell.
- iOS `AccentColor` uses the petal-inner blue so native wrapper/system controls stay within the same palette.

Run the deterministic brand contract locally with:

```bash
mise run brand-verify
```

## Release validation boundary

Automated validation proves palette/source wiring and platform compilation. Physical launcher masks, Android themed-icon rendering, iOS home-screen rendering, and store/internal-distribution presentation remain release-surface evidence under #283; CI success is not a substitute for that physical review.
