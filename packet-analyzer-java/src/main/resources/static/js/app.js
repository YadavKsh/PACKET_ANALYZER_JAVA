// ============================================================
//  PacketScope — app.js
// ============================================================

const fileInput  = document.getElementById("file");
const analyzeBtn = document.getElementById("analyzeBtn");
const statusPill = document.getElementById("statusPill");
const statusText = document.getElementById("statusText");
const fileBadge  = document.getElementById("fileBadge");
const fileLabel  = document.getElementById("fileNameLabel");
const uploadZone = document.getElementById("uploadZone");

// ── Live clock ────────────────────────────────────────────────
function updateClock() {
    const el = document.getElementById("liveClock");
    if (!el) return;
    const now = new Date();
    el.textContent = now.toLocaleTimeString("en-US", { hour12: false });
}
updateClock();
setInterval(updateClock, 1000);

// ── File input change ─────────────────────────────────────────
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

// ── Drag & drop ───────────────────────────────────────────────
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

// ── Upload & analyze ──────────────────────────────────────────
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

// ── Status ────────────────────────────────────────────────────
function setStatus(state, label) {
    statusPill.className = "status-pill " + (state === "idle" ? "" : state);
    statusText.textContent = label;
}

// ── Stats ─────────────────────────────────────────────────────
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

function animateCount(id, target) {
    const el = document.getElementById(id);
    if (!el) return;
    const duration = 600;
    const start    = Date.now();
    const tick = () => {
        const elapsed = Date.now() - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        el.textContent = Math.round(eased * target);
        if (progress < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
}

// ── Table ─────────────────────────────────────────────────────
let allPackets = [];

function renderTable(data) {
    allPackets = data;
    buildRows(data);
}

function buildRows(data) {
    const tbody   = document.querySelector("#table tbody");
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

function filterTable() {
    const q = document.getElementById("searchInput").value.toLowerCase();
    if (!q) { buildRows(allPackets); return; }
    const filtered = allPackets.filter(p =>
        [p.srcIp, p.dstIp, p.protocol, p.applicationProtocol, p.domain]
            .some(v => v && String(v).toLowerCase().includes(q))
    );
    buildRows(filtered);
}

// ── Domains ───────────────────────────────────────────────────
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

// ── Show result panels ────────────────────────────────────────
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

// ── XSS escape ────────────────────────────────────────────────
function esc(str) {
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}