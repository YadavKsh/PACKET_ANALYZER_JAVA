# 🖥️ index.html — Code Documentation

## 🗂️ Overview

`index.html` is the **frontend entry point** of the PacketScope application. It defines the entire user interface structure as a single HTML page — covering the app header, file upload zone, summary statistics, packet data table, and domain list panel. All visual styling is handled by an external CSS file, and all interactivity is driven by an external JavaScript file.

> 🎯 **Role in the Architecture:** This is the only HTML page in the application. The user loads it in a browser, uploads a `.pcap` file, and the page communicates with the Spring Boot backend (`POST /analyze`) to display results — all without any page reload.

---

## 🏗️ Document-Level Setup (`<head>`)

```html
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PacketScope</title>
```

- `charset="UTF-8"` — Ensures correct rendering of all Unicode characters including the glyph symbols used in the UI (`▤`, `◈`, `⬡`, `⊕`, `⌕`).
- `viewport` meta — Makes the layout responsive and correctly scaled on mobile devices.
- `<title>PacketScope</title>` — Sets the browser tab name.

---

### 🔤 Fonts (Google Fonts)

```html
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;700&family=Syne:wght@700;800&display=swap" rel="stylesheet">
```

Two typefaces are loaded:

| Font | Weights | Intended Use |
|---|---|---|
| **JetBrains Mono** | 400, 500, 700 | Monospace font for IP addresses, ports, packet data — gives a terminal/technical aesthetic |
| **Syne** | 700, 800 | Display font for the app title and headings — bold and distinctive |

- `display=swap` — Renders fallback fonts immediately while custom fonts load, preventing invisible text (FOIT).

---

### 🎨 Stylesheet

```html
<link rel="stylesheet" href="css/style.css">
```

- All visual styling — colors, layout, animations, card styles — is externalized to `css/style.css`.
- Keeps `index.html` purely structural.

---

## 🏛️ Page Structure Overview

```
<body>
  ├── .bg-grid               ← Decorative background grid
  └── .app-shell             ← Main content container
        ├── <header>         ← App header (logo, clock, status)
        ├── <section>        ← File upload zone + analyze button
        ├── .stats-row       ← Summary stat cards (hidden initially)
        ├── <section>        ← Packet data table (hidden initially)
        └── <section>        ← Resolved domains grid (hidden initially)
```

---

## 🔍 Element-by-Element Breakdown

### 🌐 Background Grid

```html
<div class="bg-grid"></div>
```

- A purely decorative full-page background element.
- Styled via CSS to render a subtle grid pattern behind the application UI.
- Contains no content or functionality.

---

### 🖼️ App Shell

```html
<div class="app-shell">
```

- The main content wrapper that constrains max-width, centers the layout, and provides the primary padding/spacing for all UI sections.

---

### 🔝 Header

```html
<header class="app-header">
```

Split into two sides:

#### Left Side — Logo and Metadata

```html
<div class="logo">
    <div class="logo-rings">
        <span class="ring r1"></span>
        <span class="ring r2"></span>
        <span class="ring r3"></span>
    </div>
    <h1 class="app-title">PacketScope</h1>
</div>
<div class="header-meta">
    <span class="version-tag">v2.0</span>
    <span class="sep">·</span>
    <span class="live-clock" id="liveClock"></span>
</div>
```

- `.logo-rings` — Three concentric `<span>` rings (`r1`, `r2`, `r3`) styled via CSS to create an animated radar/sonar logo icon.
- `app-title` — The "PacketScope" heading rendered in the Syne display font.
- `version-tag` — Displays the static version label `v2.0`.
- `#liveClock` — An empty `<span>` populated in real time by `app.js` to display the current time (a live clock).

#### Right Side — Status Indicator

```html
<div class="status-pill" id="statusPill">
    <span class="status-dot"></span>
    <span id="statusText">Ready</span>
</div>
```

- `#statusPill` — A pill-shaped badge that displays the application's current state.
- `.status-dot` — A colored dot (CSS-animated) that changes appearance based on status.
- `#statusText` — The text label updated by `app.js` during upload and analysis (e.g., `"Ready"` → `"Analyzing..."` → `"Done"`).

---

### 📤 Upload Section

```html
<section class="upload-section">
```

#### Upload Zone

```html
<label class="upload-zone" id="uploadZone" for="file">
    <input type="file" id="file" class="file-input" accept=".pcap,.pcapng,.csv,.json">
```

- The entire visible upload area is a `<label>` tied to the hidden `<input type="file">` via `for="file"` — clicking anywhere on the zone opens the file picker.
- `accept=".pcap,.pcapng,.csv,.json"` — Restricts the file picker to supported capture file formats.
- The `<input>` is styled to be invisible (`file-input` class hides it); the `<label>` provides the visual drop zone.

#### Upload Icon

```html
<div class="upload-icon-wrap">
    <div class="upload-pulse"></div>
    <svg width="40" height="40" viewBox="0 0 48 48" ...>
        <path d="M24 32V16M24 16L17 23M24 16L31 23" .../>  <!-- Upload arrow -->
        <path d="M8 38C8 38 8 42 12 42H36 ..." .../>        <!-- Bottom bar -->
    </svg>
</div>
```

- `.upload-pulse` — A CSS-animated pulsing ring behind the icon to draw attention to the drop target.
- The inline SVG renders a custom upload arrow icon (upward arrow + horizontal base line) using `stroke` paths rather than a bitmap image.

#### Upload Text and File Badge

```html
<p class="upload-heading">Drop your capture file here</p>
<p class="upload-sub">or click to browse &nbsp;·&nbsp; .pcap .pcapng .csv .json</p>
<div class="file-badge" id="fileBadge" style="display:none;">
    <span class="file-badge-dot"></span>
    <span id="fileNameLabel"></span>
</div>
```

- `upload-heading` / `upload-sub` — Instructional text displayed before a file is selected.
- `#fileBadge` — Hidden by default (`display:none`). Shown by `app.js` after a file is chosen, displaying the selected filename with a colored dot badge.
- `#fileNameLabel` — Populated by `app.js` with the selected file's name (e.g., `capture.pcap`).

#### Analyze Button

```html
<button class="btn-analyze" id="analyzeBtn" onclick="upload()" disabled>
    Run Analysis <span class="btn-arrow">→</span>
</button>
```

- `disabled` by default — prevents submission before a file is selected.
- `onclick="upload()"` — Calls the `upload()` function defined in `app.js`, which sends the file to the backend `POST /analyze` endpoint.
- `.btn-arrow` — The `→` arrow is wrapped in a `<span>` so CSS can animate it independently (e.g., slide on hover).
- Enabled by `app.js` once a valid file is selected.

---

### 📊 Stats Row

```html
<div class="stats-row" id="statsRow" style="display:none;">
```

- **Hidden initially** (`display:none`) — shown by `app.js` after a successful analysis response.
- Contains four summary stat cards, each following the same structure:

```html
<div class="stat-card">
    <span class="stat-glyph">▤</span>
    <span class="stat-value" id="statTotal">0</span>
    <span class="stat-label">Packets</span>
</div>
```

| Card | ID | Glyph | Metric |
|---|---|---|---|
| Default | `#statTotal` | `▤` | Total packets analyzed |
| Teal (`c-teal`) | `#statDomains` | `◈` | Unique domains extracted |
| Amber (`c-amber`) | `#statProtocols` | `⬡` | Distinct protocols detected |
| Pink (`c-pink`) | `#statIPs` | `⊕` | Unique IP addresses seen |

- Each `stat-value` starts at `0` and is updated by `app.js` after the analysis results are received.
- Color modifier classes (`c-teal`, `c-amber`, `c-pink`) apply different accent colors via CSS.

---

### 📋 Packet Table Panel

```html
<section class="panel" id="tablePanel" style="display:none;">
```

- **Hidden initially** — shown by `app.js` after results load.

#### Panel Header

```html
<div class="panel-title">
    <span class="panel-dot"></span>
    Packet Stream
</div>
<div class="search-wrap">
    <span class="search-ico">⌕</span>
    <input type="text" class="search-input" id="searchInput"
           placeholder="Filter packets..." oninput="filterTable()">
</div>
<span class="row-count" id="rowCount"></span>
```

- `#searchInput` — A live filter input; every keystroke triggers `filterTable()` in `app.js` which shows/hides table rows matching the query.
- `#rowCount` — Displays the count of currently visible rows (e.g., `"18 / 25 packets"`), updated by `app.js` as filters are applied.

#### Table Structure

```html
<table id="table">
    <thead>
        <tr>
            <th>#</th>
            <th>Source</th>
            <th>Destination</th>
            <th>Protocol</th>
            <th>Application</th>
            <th>Domain</th>
        </tr>
    </thead>
    <tbody></tbody>
</table>
```

- The `<thead>` defines six fixed columns matching the fields of `PacketInfo`.
- The `<tbody>` is **empty** in the HTML — it is fully populated at runtime by `app.js` after the API response is received.

| Column | Source Field |
|---|---|
| `#` | Row index |
| `Source` | `PacketInfo.srcIp` + `srcPort` |
| `Destination` | `PacketInfo.dstIp` + `dstPort` |
| `Protocol` | `PacketInfo.protocol` |
| `Application` | `PacketInfo.applicationProtocol` |
| `Domain` | `PacketInfo.domain` |

---

### 🌐 Domains Panel

```html
<section class="panel" id="domainsPanel" style="display:none;">
```

- **Hidden initially** — shown by `app.js` if any domains were extracted via SNI.

```html
<div class="panel-title">
    <span class="panel-dot teal"></span>
    Resolved Domains
</div>
<span class="domain-total" id="domainCount"></span>
<div class="domain-grid" id="domains"></div>
```

- `#domainCount` — Shows the total count of unique domains found (e.g., `"4 domains"`).
- `#domains` — An empty `<div>` populated by `app.js` with a grid of domain cards, one per unique domain extracted from HTTPS SNI fields.

---

### 📜 Scripts

```html
<script src="js/app.js"></script>
```

- Loaded at the **bottom of `<body>`** — ensures the entire DOM is parsed and all elements exist before `app.js` tries to reference them by ID.
- Contains all application logic: file selection handling, API communication, table rendering, live clock, filtering, and stats calculation.

---

## 🔁 Page Lifecycle — From Load to Results

```
Page loads
      |
      ├── Fonts loaded (JetBrains Mono, Syne)
      ├── style.css applied
      ├── app.js executed
      |       └── Live clock starts (#liveClock)
      |
      ▼
User selects or drops a .pcap file
      |
      ├── app.js detects file input change
      ├── #fileBadge shown with filename
      └── #analyzeBtn enabled
      |
      ▼
User clicks "Run Analysis"
      |
      ├── upload() called → POST /analyze (multipart)
      ├── #statusText → "Analyzing..."
      |
      ▼
Backend returns List<PacketInfo> (JSON)
      |
      ├── #statsRow shown → stat values populated
      ├── #tablePanel shown → <tbody> rows rendered
      ├── #domainsPanel shown → domain cards rendered
      └── #statusText → "Done"
```

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `index.html` |
| **Role** | Single-page frontend UI for the PacketScope application |
| **Fonts** | JetBrains Mono (data/code), Syne (headings) |
| **Styling** | Externalized to `css/style.css` |
| **Logic** | Externalized to `js/app.js` |
| **File Upload** | `<label>` + hidden `<input type="file">` — supports `.pcap`, `.pcapng`, `.csv`, `.json` |
| **Initially Hidden** | Stats row, packet table, domains panel — all revealed by `app.js` after results arrive |
| **Table Rendering** | `<tbody>` is empty in HTML; populated entirely at runtime by `app.js` |
| **Live Elements** | `#liveClock`, `#statusText`, `#fileBadge`, `#rowCount`, `#domainCount` — all JS-driven |
| **Backend Integration** | `upload()` in `app.js` sends file to Spring Boot `POST /analyze` |