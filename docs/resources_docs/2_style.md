# 🎨 style.css — Code Documentation

## 🗂️ Overview

`style.css` is the **complete visual stylesheet** for the PacketScope frontend. It implements a dark terminal aesthetic with neon accent colors, animated elements, and a monospace type system — giving the application the look and feel of a professional network analysis tool. Every UI component defined in `index.html` is styled here, from the animated logo rings to the packet table rows to the domain chip grid.

---

## 🏗️ File Structure Overview

```
style.css
  ├── Variables        ← Design tokens (colors, fonts, radii)
  ├── Reset            ← Box model normalization
  ├── Background Grid  ← Animated dot-grid + top glow
  ├── App Shell        ← Main content container
  ├── Header           ← Logo, rings, clock, status pill
  ├── Upload Section   ← Drop zone, pulse icon, file badge, button
  ├── Stats Row        ← Four summary stat cards
  ├── Panel            ← Reusable panel container + header
  ├── Table            ← Sticky-header scrollable packet table
  ├── Domain Grid      ← SNI domain chip cards
  └── Responsive       ← Mobile breakpoints (760px, 480px)
```

---

## 🎨 Design Tokens — CSS Variables (`:root`)

All colors, fonts, and shape values are defined as CSS custom properties so they can be reused and changed from one place.

### 🌑 Background & Surface Colors

```css
--bg:       #0d0f14;   /* Deepest background — near-black with blue tint */
--surface:  #13161e;   /* Card/panel surfaces — one step lighter than bg */
--surface2: #1a1e28;   /* Elevated surfaces — hover states, panel headers */
```

Three levels of dark surface create **visual depth** without introducing light colors.

### 🔲 Borders

```css
--border:        rgba(255,255,255,0.07);   /* Subtle default border */
--border-bright: rgba(255,255,255,0.14);   /* Slightly visible border */
```

White with low opacity rather than a fixed grey, so borders remain visible against any dark background.

### 🔤 Text Colors

```css
--text:   #e8eaf0;   /* Primary — near-white, cool-toned */
--text-2: #8b909e;   /* Secondary — muted labels and values */
--text-3: #555a6b;   /* Tertiary — disabled, placeholder, metadata */
```

Three tiers of text opacity create a clear **typographic hierarchy** without using actual opacity.

### 🌈 Neon Accent Colors

```css
--cyan:      #00e5ff;                    /* Primary accent — upload zone, protocols */
--cyan-dim:  rgba(0,229,255,0.12);       /* Tinted background fill */
--cyan-glow: rgba(0,229,255,0.25);       /* Drop shadow / box-shadow tint */

--teal:      #00ffb3;                    /* Domains panel accent */
--teal-dim:  rgba(0,255,179,0.12);

--amber:     #ffb300;                    /* Loading / warning state */
--amber-dim: rgba(255,179,0,0.12);

--pink:      #ff4ecd;                    /* Unique IPs stat card */
--pink-dim:  rgba(255,78,205,0.12);
```

Four neon colors are used systematically across the UI:

| Color | Primary Use |
|---|---|
| **Cyan** `#00e5ff` | Default accent — borders, icons, protocol badges, focus rings |
| **Teal** `#00ffb3` | Domains — domain chips, domains stat card |
| **Amber** `#ffb300` | Loading state — status pill while analysis runs |
| **Pink** `#ff4ecd` | Unique IPs stat card accent |

### 🔤 Font Families

```css
--font-display: 'Syne', sans-serif;          /* Headings, titles, button labels */
--font-mono:    'JetBrains Mono', monospace; /* Body, data, IP addresses, ports */
```

### 📐 Border Radii

```css
--radius:    10px;   /* Standard — buttons, inputs, stat cards */
--radius-lg: 16px;   /* Large — panels, upload zone */
```

---

## 🔄 Reset

```css
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
html { font-size: 14px; }
body {
    font-family: var(--font-mono);
    background: var(--bg);
    color: var(--text);
    min-height: 100vh;
    -webkit-font-smoothing: antialiased;
    overflow-x: hidden;
}
```

- `box-sizing: border-box` — Applied universally so padding and borders are included in element dimensions, preventing layout surprises.
- `font-size: 14px` on `<html>` — Sets the `rem` base. All font sizes in the stylesheet use `rem`, so scaling the base changes everything proportionally.
- `font-family: var(--font-mono)` — JetBrains Mono is the default for the entire page; the display font (Syne) is applied selectively where needed.
- `-webkit-font-smoothing: antialiased` — Renders fonts with thinner, crisper strokes on macOS/iOS.
- `overflow-x: hidden` — Prevents horizontal scrollbar from appearing if any element slightly overflows on mobile.

---

## 🌐 Background Grid

```css
.bg-grid {
    position: fixed;
    inset: 0;
    background-image: radial-gradient(circle, rgba(255,255,255,0.06) 1px, transparent 1px);
    background-size: 28px 28px;
    pointer-events: none;
    z-index: 0;
}
```

- `position: fixed; inset: 0` — Pins the grid to the full viewport, so it never scrolls.
- `radial-gradient(...) 1px` — Renders a tiny soft dot at each 28×28px tile intersection, creating a subtle grid of dots over the background.
- `pointer-events: none` — Completely transparent to mouse/touch events — never interferes with interactive elements above it.
- `z-index: 0` — Sits behind all content (`.app-shell` uses `z-index: 1`).

### Top Glow Effect (Pseudo-element)

```css
.bg-grid::before {
    top: -120px; left: 50%;
    width: 700px; height: 400px;
    background: radial-gradient(ellipse at center, rgba(0,229,255,0.07) 0%, transparent 70%);
}
```

- A large, very faint cyan ellipse positioned above the top of the viewport.
- Creates a subtle "glow source" effect at the top of the page, as if the UI is lit from above by a neon light.

---

## 🏛️ App Shell

```css
.app-shell {
    position: relative;
    z-index: 1;
    max-width: 1160px;
    margin: 0 auto;
    padding: 0 28px 80px;
}
```

- `z-index: 1` — Stacks content above the `z-index: 0` background grid.
- `max-width: 1160px; margin: 0 auto` — Centers the layout and caps its width for readability on wide screens.
- `padding: 0 28px 80px` — Horizontal gutters + generous bottom padding for breathing room.

---

## 🔝 Header

### Logo Rings Animation

```css
.ring {
    position: absolute;
    border-radius: 50%;
    border: 1.5px solid var(--cyan);
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    animation: ringPulse 2.4s ease-in-out infinite;
}
.r1 { width: 10px;  height: 10px;  animation-delay: 0s;   }
.r2 { width: 20px;  height: 20px;  animation-delay: 0.3s; opacity: 0.6; }
.r3 { width: 30px;  height: 30px;  animation-delay: 0.6s; opacity: 0.3; }

@keyframes ringPulse {
    0%, 100% { opacity: var(--op, 1); transform: translate(-50%,-50%) scale(1); }
    50%       { opacity: 0.1;         transform: translate(-50%,-50%) scale(1.1); }
}
```

- Three concentric circles (10px / 20px / 30px) centered on the same point using `translate(-50%, -50%)`.
- Each ring animates with a staggered delay (`0s`, `0.3s`, `0.6s`) — creating a **sonar/radar pulse** effect.
- `--op` CSS variable per ring sets the max opacity so outer rings naturally appear fainter.
- `scale(1.1)` at 50% keyframe creates a subtle breathing/expansion effect alongside the fade.

### App Title Gradient Text

```css
.app-title {
    background: linear-gradient(90deg, #fff 0%, var(--cyan) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}
```

- A horizontal gradient from white to cyan is applied as the text fill — a standard CSS technique for gradient-colored text.
- `-webkit-text-fill-color: transparent` is required to make the background gradient visible through the text.

### Status Pill States

```css
.status-pill.loading { border-color: rgba(255,179,0,0.3); color: var(--amber); }
.status-pill.loading .status-dot {
    background: var(--amber);
    animation: blink 0.7s ease-in-out infinite;
    box-shadow: 0 0 8px var(--amber);
}
.status-pill.done { border-color: rgba(0,255,179,0.3); color: var(--teal); }
.status-pill.done .status-dot {
    background: var(--teal);
    box-shadow: 0 0 8px var(--teal);
}
```

The pill has three visual states, toggled by `app.js` adding/removing CSS classes:

| State | Class | Color | Dot behavior |
|---|---|---|---|
| Idle | *(none)* | Muted grey | Static grey dot |
| Analyzing | `.loading` | Amber | Blinking dot with amber glow |
| Complete | `.done` | Teal | Solid teal dot with teal glow |

```css
@keyframes blink {
    0%, 100% { opacity: 1; }
    50%       { opacity: 0.2; }
}
```

- The `blink` animation fades the status dot in and out at 0.7s to signal active work.

---

## 📤 Upload Section

### Drop Zone

```css
.upload-zone {
    border: 1.5px dashed var(--border-bright);
    transition: border-color 0.25s, background 0.25s;
}
.upload-zone:hover,
.upload-zone.drag-over {
    border-color: var(--cyan);
    background: var(--surface2);
}
.upload-zone::before {
    background: radial-gradient(ellipse at 50% 0%, var(--cyan-dim) 0%, transparent 70%);
    opacity: 0;
    transition: opacity 0.3s;
}
.upload-zone:hover::before,
.upload-zone.drag-over::before { opacity: 1; }
```

- Default state: dashed grey border on a dark surface.
- On hover or drag-over: border turns cyan, background shifts to `surface2`, and a **radial cyan glow** fades in from the top of the zone via `::before`.
- `.drag-over` class is added by `app.js` when a file is dragged over the zone, matching the hover appearance.

### Upload Pulse Animation

```css
.upload-pulse {
    position: absolute; inset: 0;
    border-radius: 50%;
    border: 1.5px solid var(--cyan);
    animation: pulsate 2s ease-out infinite;
}
@keyframes pulsate {
    0%   { transform: scale(0.85); opacity: 0.6; }
    100% { transform: scale(1.3);  opacity: 0; }
}
```

- A ring that continuously scales from 85% → 130% while fading to transparent.
- Creates a continuous outward pulse effect around the upload icon, drawing the eye to the interactive area.

### Analyze Button

```css
.btn-analyze {
    background: var(--cyan);
    color: #000;
    transition: background 0.2s, box-shadow 0.2s, transform 0.1s;
}
.btn-analyze:hover:not(:disabled) {
    background: #33ecff;
    box-shadow: 0 0 28px var(--cyan-glow), 0 0 60px rgba(0,229,255,0.1);
}
.btn-analyze:active:not(:disabled) { transform: scale(0.97); }
.btn-analyze:disabled { opacity: 0.25; cursor: not-allowed; }
.btn-analyze:hover:not(:disabled) .btn-arrow { transform: translateX(4px); }
```

- **Default:** Solid cyan background, black text — maximum contrast.
- **Hover:** Slightly lighter cyan + a layered double `box-shadow` glow (close and distant) — creates a neon bloom effect.
- **Active (click):** Scales down to `0.97` — physical press feedback.
- **Disabled:** `opacity: 0.25` and `cursor: not-allowed` — visually indicates the button isn't clickable until a file is selected.
- **Arrow animation:** The `→` arrow slides 4px right on hover — a subtle directional affordance.

---

## 📊 Stats Row

### Card Layout

```css
.stats-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
}
```

- CSS Grid with four equal-width columns — automatically responsive via the `@media` breakpoints.

### Accent Top Border per Card

```css
.stat-card::before {
    content: '';
    position: absolute; top: 0; left: 0; right: 0;
    height: 2px;
    background: var(--cyan);
}
.stat-card.c-teal::before  { background: var(--teal); }
.stat-card.c-amber::before { background: var(--amber); }
.stat-card.c-pink::before  { background: var(--pink); }
```

- A 2px colored line is painted across the top of each card using a `::before` pseudo-element.
- Each color modifier class (`c-teal`, `c-amber`, `c-pink`) overrides the default cyan.
- This gives each stat card a unique identity without changing its overall structure.

### Hover Glow per Card Color

```css
.stat-card:hover              { border-color: rgba(0,229,255,0.3);  box-shadow: 0 4px 24px rgba(0,229,255,0.06); }
.stat-card.c-teal:hover       { border-color: rgba(0,255,179,0.3);  box-shadow: 0 4px 24px rgba(0,255,179,0.06); }
.stat-card.c-amber:hover      { border-color: rgba(255,179,0,0.3);  box-shadow: 0 4px 24px rgba(255,179,0,0.06); }
.stat-card.c-pink:hover       { border-color: rgba(255,78,205,0.3); box-shadow: 0 4px 24px rgba(255,78,205,0.06); }
```

- Each card's hover border and glow matches its own accent color — a consistent color-semantic system.

### Entrance Animation

```css
@keyframes fadeSlideUp {
    from { opacity: 0; transform: translateY(12px); }
    to   { opacity: 1; transform: translateY(0); }
}
```

- Applied to `.stat-card` and `.panel` elements when they appear.
- Slides elements up 12px while fading in — a subtle entrance that avoids a jarring pop.

---

## 📋 Table

### Sticky Header

```css
thead { position: sticky; top: 0; z-index: 2; }
th { background: var(--bg); }
```

- `position: sticky; top: 0` — The column headers remain visible while scrolling through long packet lists.
- `background: var(--bg)` on `th` — Without a solid background, the sticky header would be transparent and row content would show through it while scrolling.

### Scrollable Container

```css
.table-scroll {
    overflow-x: auto;
    max-height: 480px;
    overflow-y: auto;
}
```

- `max-height: 480px` — Caps the table at ~480px, enabling vertical scrolling for large packet sets without the table taking over the full page.
- `overflow-x: auto` — Enables horizontal scrolling for wide tables on small screens.

### Custom Scrollbar

```css
.table-scroll::-webkit-scrollbar       { width: 5px; height: 5px; }
.table-scroll::-webkit-scrollbar-track { background: transparent; }
.table-scroll::-webkit-scrollbar-thumb { background: var(--border-bright); border-radius: 10px; }
```

- Replaces the default OS scrollbar with a slim (5px), dark, rounded one that fits the aesthetic.

### Row Hover

```css
tbody tr:hover td {
    background: var(--surface2);
    color: var(--text);
}
```

- Highlights an entire row on hover by setting cell backgrounds to `surface2` and brightening text to full `--text` color.

### Typed Table Cells

```css
td.num    { color: var(--text-3); font-size: 0.7rem; }   /* Row index */
td.domain { color: var(--teal);   font-weight: 500; }     /* Domain name */
td.proto  { color: var(--cyan); }                         /* Protocol */
td.proto-badge span {
    background: var(--cyan-dim);
    border: 1px solid rgba(0,229,255,0.18);
    border-radius: 4px;
    padding: 2px 8px;
}
```

- Different `td` classes apply semantic color coding to specific columns — domain cells are teal, protocol cells are cyan.
- `proto-badge` renders the protocol as a small pill/badge rather than plain text.

---

## 🌐 Domain Grid

```css
.domain-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 10px;
    padding: 18px 22px;
}
```

- `auto-fill` + `minmax(240px, 1fr)` — Automatically determines how many columns fit, with each chip being at minimum 240px wide. Adapts fluidly to any container width without media queries.

### Domain Chip

```css
.domain-chip {
    color: var(--teal);
    border: 1px solid var(--border-bright);
    border-radius: 8px;
    transition: border-color 0.2s, background 0.2s, transform 0.15s;
    animation: fadeSlideUp 0.3s ease both;
}
.domain-chip:hover {
    border-color: var(--teal);
    background: var(--teal-dim);
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(0,255,179,0.06);
}
```

- Each chip slides up on entrance via `fadeSlideUp`.
- On hover: border turns teal, background fills with the teal-tinted dim color, and the chip lifts 1px — a subtle card-lift effect.

---

## 📱 Responsive Breakpoints

```css
@media (max-width: 760px) {
    .app-shell      { padding: 0 16px 48px; }
    .upload-section { flex-direction: column; }
    .btn-analyze    { width: 100%; justify-content: center; padding: 16px; }
    .stats-row      { grid-template-columns: repeat(2, 1fr); }
    .header-meta    { display: none; }
}

@media (max-width: 480px) {
    .stats-row { grid-template-columns: 1fr 1fr; }
}
```

| Breakpoint | Changes Applied |
|---|---|
| `≤ 760px` | Upload zone + button stack vertically; stats go to 2-column grid; header meta (version + clock) hidden to save space |
| `≤ 480px` | Stats maintain 2-column grid (already set at 760px — this reinforces it for very small screens) |

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `style.css` |
| **Theme** | Dark terminal — near-black backgrounds, neon accents, monospace type |
| **Color System** | 4 neon accents (cyan, teal, amber, pink) + 3 text tiers + 3 surface depths |
| **Typography** | JetBrains Mono (data/body), Syne (headings/buttons) |
| **Animations** | `ringPulse` (logo), `pulsate` (upload icon), `blink` (status dot), `fadeSlideUp` (cards/panels) |
| **Layout** | CSS Grid (stats, domains), Flexbox (header, upload section, cards) |
| **Table** | Sticky header, capped height with scroll, custom scrollbar, color-coded cell types |
| **Responsive** | Two breakpoints — 760px and 480px |
| **Design Tokens** | All colors, fonts, and radii in `:root` CSS variables for easy theming |