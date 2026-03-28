# ⚙️ app.js — Code Documentation

## 🗂️ Overview

`app.js` is the **complete frontend logic layer** of PacketScope. It handles everything that happens after the HTML page loads — file selection, drag-and-drop, API communication, result rendering, live filtering, animated counters, and XSS-safe DOM injection. It is the glue that connects the static HTML structure in `index.html` to the Spring Boot backend at `POST /analyze`.

> 🎯 **Role in the Architecture:** `app.js` is the only JavaScript file in the application. It runs entirely in the browser and communicates with the backend via the Fetch API. No frameworks or libraries are used — pure vanilla JavaScript.

---

## 🏗️ File Structure Overview

```
app.js
  ├── DOM References       ← Cache all element handles at startup
  ├── Live Clock           ← Real-time HH:MM:SS display
  ├── File Input Change    ← Respond to file picker selection
  ├── Drag & Drop          ← Drop zone events
  ├── upload()             ← Core: send file to API, coordinate rendering
  ├── setStatus()          ← Update the status pill state
  ├── renderStats()        ← Calculate and animate summary counters
  ├── animateCount()       ← Eased number count-up animation
  ├── renderTable()        ← Populate the packet table
  ├── buildRows()          ← DOM fragment construction for table rows
  ├── filterTable()        ← Live search/filter on packet data
  ├── renderDomains()      ← Populate the domain chip grid
  ├── showPanels()         ← Reveal hidden result sections
  └── esc()                ← XSS sanitization utility
```

---

## 🔧 DOM References

```javascript
const fileInput  = document.getElementById("file");
const analyzeBtn = document.getElementById("analyzeBtn");
const statusPill = document.getElementById("statusPill");
const statusText = document.getElementById("statusText");
const fileBadge  = document.getElementById("fileBadge");
const fileLabel  = document.getElementById("fileNameLabel");
const uploadZone = document.getElementById("uploadZone");
```

- All frequently accessed DOM elements are resolved **once at module load** and stored in `const` variables.
- This avoids repeated `getElementById` calls throughout the code, which would re-query the DOM every time they're used.
- These elements map directly to the IDs defined in `index.html`.

| Variable | Element | Purpose |
|---|---|---|
| `fileInput` | `#file` | The hidden `<input type="file">` |
| `analyzeBtn` | `#analyzeBtn` | The "Run Analysis" button |
| `statusPill` | `#statusPill` | The status badge container |
| `statusText` | `#statusText` | The text label inside the status pill |
| `fileBadge` | `#fileBadge` | The filename badge shown after file selection |
| `fileLabel` | `#fileNameLabel` | The `<span>` inside the file badge |
| `uploadZone` | `#uploadZone` | The `<label>` drop zone |

---

## 🕐 Live Clock

```javascript
function updateClock() {
    const el = document.getElementById("liveClock");
    if (!el) return;
    const now = new Date();
    el.textContent = now.toLocaleTimeString("en-US", { hour12: false });
}
updateClock();
setInterval(updateClock, 1000);
```

- `new Date()` — Gets the current date and time at the moment of the call.
- `toLocaleTimeString("en-US", { hour12: false })` — Formats the time as `HH:MM:SS` in 24-hour format (e.g., `"14:35:02"`).
- `updateClock()` is called immediately on load so the clock appears without a 1-second blank delay.
- `setInterval(updateClock, 1000)` — Schedules the clock to refresh every 1000ms (1 second), keeping it live.

---

## 📂 File Input Change

```javascript
fileInput.addEventListener("change", () => {
    const f = fileInput.files[0];
    if (f) {
        fileLabel.textContent = f.name;
        fileBadge.style.display = "flex";
        analyzeBtn.disabled = false;
    } else {
        fileBadge.style.display = "none";
        analyzeBtn.disabled = true;
    }
});
```

- Fires whenever the user selects a file via the file picker dialog.
- `fileInput.files[0]` — Accesses the first (and only) selected file from the `FileList`.
- If a file is present:
    - `fileLabel.textContent = f.name` — Displays the filename (e.g., `"capture.pcap"`) inside the badge.
    - `fileBadge.style.display = "flex"` — Makes the file badge visible.
    - `analyzeBtn.disabled = false` — Unlocks the "Run Analysis" button.
- If no file (e.g., user opens picker then cancels): the badge is hidden and the button stays disabled.

---

## 🖱️ Drag & Drop

```javascript
uploadZone.addEventListener("dragover", e => {
    e.preventDefault();
    uploadZone.classList.add("drag-over");
});
uploadZone.addEventListener("dragleave", () => uploadZone.classList.remove("drag-over"));
uploadZone.addEventListener("drop", e => {
    e.preventDefault();
    uploadZone.classList.remove("drag-over");
    const f = e.dataTransfer?.files[0];
    if (f) {
        const dt = new DataTransfer();
        dt.items.add(f);
        fileInput.files = dt.files;
        fileLabel.textContent = f.name;
        fileBadge.style.display = "flex";
        analyzeBtn.disabled = false;
    }
});
```

Three events handle the full drag-and-drop lifecycle:

| Event | Action |
|---|---|
| `dragover` | Prevents the browser's default behavior (opening the file), adds `.drag-over` CSS class to highlight the zone |
| `dragleave` | Removes `.drag-over` when the user drags the file back out |
| `drop` | Prevents default, removes `.drag-over`, and processes the dropped file |

- `e.dataTransfer?.files[0]` — Retrieves the dropped file using optional chaining (`?.`) to safely handle cases where `dataTransfer` is null.
- **DataTransfer workaround:** `fileInput.files` is read-only and cannot be set directly. A `new DataTransfer()` object is created, the file is added to it via `.items.add(f)`, and then its `.files` list is assigned to `fileInput.files`. This synchronizes the dropped file with the file input so that `upload()` can access it via `fileInput.files[0]`.
- After assignment, the same badge/button logic runs as in the `change` event handler.

---

## 🚀 `upload()` — Core Analysis Function

```javascript
async function upload() {
    const file = fileInput.files[0];
    if (!file) return;

    setStatus("loading", "Analyzing…");
    analyzeBtn.disabled = true;

    try {
        const form = new FormData();
        form.append("file", file);

        const res = await fetch("/analyze", { method: "POST", body: form });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const data = await res.json();

        renderStats(data);
        renderTable(data);
        renderDomains(data);
        showPanels();
        setStatus("done", "Done");
    } catch (err) {
        console.error(err);
        alert("Analysis failed — check the console for details.");
        setStatus("idle", "Ready");
    } finally {
        analyzeBtn.disabled = false;
    }
}
```

This is the central function that coordinates the entire analysis workflow:

#### Step-by-step:

1. **Guard clause** — `if (!file) return` exits immediately if somehow called with no file selected.
2. **UI feedback** — Sets status to `"loading"` and disables the button to prevent double-submission.
3. **Build request** — `new FormData()` constructs a multipart form body. `.append("file", file)` adds the file under the key `"file"`, matching `@RequestParam("file")` in `PacketController`.
4. **Send request** — `fetch("/analyze", { method: "POST", body: form })` sends the file to the Spring Boot backend. `await` pauses execution until the response arrives.
5. **Check response** — `if (!res.ok)` throws an error for any non-2xx HTTP status (e.g., 500, 404), routing to the `catch` block.
6. **Parse JSON** — `await res.json()` parses the response body as a `List<PacketInfo>` JSON array into a JavaScript array of objects.
7. **Render results** — Calls four render functions in sequence to populate the UI.
8. **Error handling** — The `catch` block logs the error, alerts the user, and resets the status to idle.
9. **`finally` block** — Always re-enables the button, whether the request succeeded or failed.

---

## 🚦 `setStatus(state, label)`

```javascript
function setStatus(state, label) {
    statusPill.className = "status-pill " + (state === "idle" ? "" : state);
    statusText.textContent = label;
}
```

- Replaces the pill's `className` entirely — this is cleaner than `classList.add/remove` when toggling between mutually exclusive states.
- `state === "idle" ? "" : state` — For `"idle"`, only the base `"status-pill"` class is applied (no state modifier). For `"loading"` or `"done"`, the state name is appended as a modifier class.
- `statusText.textContent` updates the label text inside the pill.

| `state` arg | Resulting class | Visual |
|---|---|---|
| `"idle"` | `"status-pill"` | Grey dot, muted text |
| `"loading"` | `"status-pill loading"` | Blinking amber dot |
| `"done"` | `"status-pill done"` | Solid teal dot |

---

## 📊 `renderStats(data)`

```javascript
function renderStats(data) {
    const domains   = new Set(data.filter(p => p.domain).map(p => p.domain));
    const protocols = new Set(data.map(p => p.protocol).filter(Boolean));
    const ips       = new Set([
        ...data.map(p => p.srcIp).filter(Boolean),
        ...data.map(p => p.dstIp).filter(Boolean)
    ]);

    animateCount("statTotal",     data.length);
    animateCount("statDomains",   domains.size);
    animateCount("statProtocols", protocols.size);
    animateCount("statIPs",       ips.size);
}
```

Calculates four summary metrics from the full `data` array (`List<PacketInfo>`):

| Metric | Calculation |
|---|---|
| **Total packets** | `data.length` — count of all items in the response |
| **Unique domains** | `Set` of all non-null `p.domain` values — deduplicates SNI hostnames |
| **Unique protocols** | `Set` of all non-null `p.protocol` values — typically `{"TCP", "UDP"}` |
| **Unique IPs** | `Set` of all `srcIp` and `dstIp` values combined — spread operator merges both arrays before deduplication |

- `filter(Boolean)` — Removes `null`, `undefined`, and empty string values before counting.
- All four values are then animated into the stat card elements via `animateCount()`.

---

## 🔢 `animateCount(id, target)`

```javascript
function animateCount(id, target) {
    const el = document.getElementById(id);
    if (!el) return;
    const duration = 600;
    const start    = Date.now();
    const tick = () => {
        const elapsed  = Date.now() - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased    = 1 - Math.pow(1 - progress, 3);
        el.textContent = Math.round(eased * target);
        if (progress < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
}
```

Animates a number from `0` to `target` over 600ms using a **cubic ease-out** curve:

- `elapsed / duration` — Linear progress from `0.0` to `1.0` over 600ms.
- `Math.min(..., 1)` — Clamps progress to never exceed `1.0`.
- `1 - Math.pow(1 - progress, 3)` — Cubic ease-out formula: starts fast, decelerates smoothly to the final value. This gives the count-up a natural, snappy feel rather than a mechanical linear increment.
- `Math.round(eased * target)` — Converts the eased float to the nearest integer for display.
- `requestAnimationFrame(tick)` — Schedules the next frame update, running at the browser's native frame rate (~60fps). Stops when `progress >= 1`.

#### Easing Curve Visualization

```
Count value
  target ┤                          ╭────
         │                     ╭────
         │                ╭────
         │           ╭────
         │      ╭────
       0 ┼──────────────────────────────→ time (600ms)
         fast start              slow finish
```

---

## 📋 `renderTable(data)` and `buildRows(data)`

```javascript
let allPackets = [];

function renderTable(data) {
    allPackets = data;
    buildRows(data);
}
```

- `allPackets` — A **module-level variable** that stores the full unfiltered dataset. This is what `filterTable()` filters against — filtering never permanently removes data, it just rebuilds rows from a subset.
- `renderTable()` caches the data and delegates row building to `buildRows()`.

```javascript
function buildRows(data) {
    const tbody    = document.querySelector("#table tbody");
    const rowCount = document.getElementById("rowCount");
    tbody.innerHTML = "";
    rowCount.textContent = `${data.length} rows`;

    const fragment = document.createDocumentFragment();
    data.forEach((p, i) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td class="num">${i + 1}</td>
            <td>${esc(p.srcIp)}:${esc(String(p.srcPort ?? ""))}</td>
            <td>${esc(p.dstIp)}:${esc(String(p.dstPort ?? ""))}</td>
            <td class="proto-badge"><span>${esc(p.protocol ?? "")}</span></td>
            <td>${esc(p.applicationProtocol ?? "")}</td>
            <td class="${p.domain ? "domain" : ""}">${esc(p.domain ?? "")}</td>
        `;
        fragment.appendChild(tr);
    });
    tbody.appendChild(fragment);
}
```

- `tbody.innerHTML = ""` — Clears any previously rendered rows before rebuilding.
- `rowCount.textContent` — Updates the row count label (e.g., `"25 rows"` or `"8 rows"` when filtered).
- **DocumentFragment** — `document.createDocumentFragment()` creates an in-memory container. All rows are appended to this fragment first, then the fragment is inserted into the DOM in a **single operation**. This avoids triggering a browser reflow/repaint for every individual row — a critical performance optimization for large packet lists.
- `p.srcPort ?? ""` — The `??` (nullish coalescing) operator returns `""` if `srcPort` is `null` or `undefined`, preventing the string `"null"` from appearing in the table.
- `p.domain ? "domain" : ""` — Conditionally applies the `domain` CSS class (teal color) only to cells that actually have a domain value.
- Every value is wrapped in `esc()` to prevent XSS injection from packet data.

---

## 🔍 `filterTable()`

```javascript
function filterTable() {
    const q = document.getElementById("searchInput").value.toLowerCase();
    if (!q) { buildRows(allPackets); return; }
    const filtered = allPackets.filter(p =>
        [p.srcIp, p.dstIp, p.protocol, p.applicationProtocol, p.domain]
            .some(v => v && String(v).toLowerCase().includes(q))
    );
    buildRows(filtered);
}
```

- Called on every `oninput` event from the search box — filters in real time as the user types.
- `if (!q)` — If the search box is empty, restores the full unfiltered dataset and exits early.
- `.toLowerCase()` on both the query and each field value — makes the search case-insensitive.
- The filter checks five fields: `srcIp`, `dstIp`, `protocol`, `applicationProtocol`, and `domain`.
- `.some(v => ...)` — Short-circuits as soon as any field matches, avoiding unnecessary checks.
- `v && String(v).toLowerCase().includes(q)` — The `v &&` guard skips `null`/`undefined` fields before calling `.toLowerCase()`.
- `buildRows(filtered)` — Rebuilds the table with only matching rows. `allPackets` is never mutated — the full dataset is always preserved for the next filter query.

---

## 🌐 `renderDomains(data)`

```javascript
function renderDomains(data) {
    const domains = [...new Set(data.filter(p => p.domain).map(p => p.domain))];
    const grid    = document.getElementById("domains");
    const count   = document.getElementById("domainCount");
    grid.innerHTML = "";
    count.textContent = `${domains.length} domains`;

    domains.forEach((d, i) => {
        const chip = document.createElement("div");
        chip.className = "domain-chip";
        chip.style.animationDelay = `${i * 35}ms`;
        chip.innerHTML = `<span class="domain-chip-dot"></span>${esc(d)}`;
        grid.appendChild(chip);
    });
}
```

- `[...new Set(...)]` — Deduplicates domains: `filter` removes nulls, `map` extracts the domain string, `Set` removes duplicates, spread `[...]` converts back to an array.
- `grid.innerHTML = ""` — Clears previous results before re-rendering.
- `chip.style.animationDelay = ${i * 35}ms` — Each chip gets a **staggered entrance delay**: chip 0 appears immediately, chip 1 after 35ms, chip 2 after 70ms, etc. This creates a cascading reveal effect rather than all chips appearing at once.
- `chip.innerHTML` inserts the teal dot (`domain-chip-dot`) and the sanitized domain string.

---

## 👁️ `showPanels()`

```javascript
function showPanels() {
    const ids = ["statsRow", "tablePanel", "domainsPanel"];
    ids.forEach((id, i) => {
        const el = document.getElementById(id);
        if (!el) return;
        el.style.display = id === "statsRow" ? "grid" : "block";
        el.style.animationDelay = `${i * 80}ms`;
    });
    document.getElementById("tablePanel").scrollIntoView({ behavior: "smooth", block: "start" });
}
```

- Iterates the three result sections in order and makes each visible.
- `id === "statsRow" ? "grid" : "block"` — The stats row uses `display: grid` (its CSS layout type); the panels use `display: block`. Setting the wrong display type would break the layout.
- `animationDelay = ${i * 80}ms` — Staggers panel appearances by 80ms each: stats appear first, then the table, then domains — a sequenced reveal rather than a simultaneous pop.
- `scrollIntoView({ behavior: "smooth", block: "start" })` — Smoothly scrolls the page to bring the packet table into view immediately after results load, so the user doesn't have to manually scroll down.

---

## 🛡️ `esc(str)` — XSS Sanitizer

```javascript
function esc(str) {
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}
```

- Called on **every piece of packet data** before it is injected into the DOM via `innerHTML`.
- `String(str)` — Safely converts any value (including numbers, null, undefined) to a string before applying replacements.
- Replaces the four HTML-significant characters with their safe entity equivalents:

| Character | Replaced With | Risk if not escaped |
|---|---|---|
| `&` | `&amp;` | Breaks entity parsing |
| `<` | `&lt;` | Could open an HTML tag |
| `>` | `&gt;` | Could close an HTML tag |
| `"` | `&quot;` | Could break out of an attribute value |

> 🔒 **Why this matters:** Packet data originates from the network — source IPs, domain names, and protocols could theoretically contain malicious strings crafted by an attacker to inject HTML or JavaScript. Using `esc()` on all values before DOM insertion prevents **Cross-Site Scripting (XSS)** attacks, regardless of what the packet data contains.

---

## 🔁 Complete Application Event Flow

```
Page loads
      │
      ├── DOM elements cached
      └── Clock starts (updateClock every 1s)

User selects / drops a file
      │
      ├── fileInput "change" event  or  uploadZone "drop" event
      ├── fileBadge shown with filename
      └── analyzeBtn enabled

User clicks "Run Analysis"
      │
      └── upload() called
            ├── setStatus("loading", "Analyzing…")
            ├── analyzeBtn disabled
            ├── FormData built with file
            ├── fetch POST /analyze (await)
            │         │
            │         ▼ (Spring Boot processes PCAP)
            │         │
            │   List<PacketInfo> JSON response
            │
            ├── renderStats(data)     → animateCount() × 4
            ├── renderTable(data)     → buildRows() → DocumentFragment → tbody
            ├── renderDomains(data)   → domain chips with staggered delay
            ├── showPanels()          → reveal + smooth scroll
            └── setStatus("done", "Done")

User types in search box
      │
      └── filterTable()
            ├── filter allPackets against query
            └── buildRows(filtered)
```

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `app.js` |
| **Role** | Complete frontend logic — file handling, API calls, DOM rendering, filtering |
| **Framework** | None — pure vanilla JavaScript |
| **API Communication** | `fetch()` with `FormData` → `POST /analyze` |
| **Response Format** | JSON array of `PacketInfo` objects |
| **Table Rendering** | `DocumentFragment` for batched DOM insertion (performance) |
| **Filtering** | Client-side, real-time, case-insensitive across 5 fields |
| **Count Animation** | Cubic ease-out over 600ms via `requestAnimationFrame` |
| **Staggered Reveals** | Domain chips (35ms steps), panels (80ms steps) |
| **XSS Protection** | `esc()` applied to all packet data before `innerHTML` injection |
| **Drag & Drop** | `DataTransfer` workaround to sync dropped file with `<input>` |
| **State Management** | `allPackets` module-level variable preserves unfiltered dataset |