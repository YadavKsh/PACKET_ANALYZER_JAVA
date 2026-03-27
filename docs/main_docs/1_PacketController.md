# 📦 PacketController — Code Documentation

## 🗂️ Overview

`PacketController` is a **Spring Boot REST Controller** that acts as the entry point for the Packet Analyzer application's backend API. It exposes an HTTP endpoint that accepts a PCAP (packet capture) file uploaded by a client (e.g., a web frontend), processes it through the analyzer service, and returns structured packet information.

---

## 🏗️ Class-Level Details

```java
@RestController
@CrossOrigin
public class PacketController {
```

| Annotation / Keyword | Purpose |
|---|---|
| `@RestController` | Marks this class as a REST API controller; all methods return JSON by default |
| `@CrossOrigin` | Enables Cross-Origin Resource Sharing (CORS), allowing frontend apps on different origins/ports to call this API |

---

## 🔧 Field

```java
private final PacketAnalyzerMTService service = new PacketAnalyzerMTService();
```

- Instantiates a **multi-threaded packet analyzer service** (`MT` = Multi-Threaded).
- Declared `final` — the service instance cannot be reassigned after initialization.
- This service contains the actual logic for parsing PCAP files.

---

## 🌐 API Endpoint — `/analyze`

```java
@PostMapping("/analyze")
public List<PacketInfo> analyze(@RequestParam("file") MultipartFile file)
```

| Part | Meaning |
|---|---|
| `@PostMapping("/analyze")` | Maps HTTP `POST` requests to the `/analyze` URL |
| `List<PacketInfo>` | Returns a list of parsed packet data objects as JSON |
| `@RequestParam("file")` | Expects a file parameter named `"file"` in the multipart form-data request body |
| `MultipartFile file` | Spring's abstraction for a file uploaded via HTTP |

---

## 🔄 Method Flow — Step by Step

### Step 1 — 💾 Save the Uploaded File Temporarily

```java
File tempFile = File.createTempFile("upload", ".pcap");

try (FileOutputStream fos = new FileOutputStream(tempFile)) {
    fos.write(file.getBytes());
}
```

- `File.createTempFile("upload", ".pcap")` — Creates a temporary file on disk with a name like `upload12345.pcap` in the system's temp directory.
- `file.getBytes()` — Reads the raw bytes of the uploaded file from the HTTP request.
- `FileOutputStream` — Writes those bytes to the temporary file on disk so the service can access it via a file path.
- The `try-with-resources` block ensures the stream is **automatically closed** after writing, even if an error occurs.

---

### Step 2 — 🔬 Analyze the PCAP File

```java
List<PacketInfo> result = service.analyze(tempFile.getAbsolutePath());
```

- `tempFile.getAbsolutePath()` — Gets the full file system path to the saved temp file (e.g., `/tmp/upload12345.pcap`).
- `service.analyze(...)` — Passes the path to the multi-threaded service, which parses the PCAP file and returns a list of `PacketInfo` objects (each representing one captured network packet).

---

### Step 3 — 🗑️ Clean Up the Temp File

```java
tempFile.delete();
```

- Deletes the temporary PCAP file from disk after analysis is complete.
- Prevents temp file accumulation and avoids unnecessary disk usage.

---

### ✅ Return Result

```java
return result;
```

- Returns the list of `PacketInfo` objects.
- Spring automatically serializes this list to **JSON** and sends it as the HTTP response.

---

### ⚠️ Error Handling

```java
} catch (Exception e) {
    e.printStackTrace();
    return List.of();
}
```

- Catches any exception that occurs during file saving, analysis, or deletion.
- `e.printStackTrace()` — Logs the full stack trace to the console for debugging.
- `List.of()` — Returns an **empty immutable list** as a safe fallback response instead of crashing or returning an error code.

> ⚠️ **Note:** This is a broad catch-all. In production, you'd typically return proper HTTP error responses (e.g., `ResponseEntity` with a `500` status) rather than silently returning an empty list.

---

## 🔁 Complete Request Flow Diagram

```
Client (Frontend)
      |
      |  POST /analyze  (multipart file upload)
      ▼
PacketController.analyze()
      |
      ├── 1. Save uploaded file → temp .pcap file on disk
      ├── 2. Pass file path → PacketAnalyzerMTService.analyze()
      ├── 3. Delete temp file
      |
      ▼
  List<PacketInfo>  (returned as JSON)
      |
      ▼
Client (Frontend)
```

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `PacketController.java` |
| **Package** | `com.packetanalyzer.controller` |
| **Role** | REST API entry point |
| **Endpoint** | `POST /analyze` |
| **Input** | Multipart PCAP file upload |
| **Output** | `List<PacketInfo>` as JSON |
| **Service Used** | `PacketAnalyzerMTService` (multi-threaded) |
| **Error Behavior** | Returns empty list on failure |