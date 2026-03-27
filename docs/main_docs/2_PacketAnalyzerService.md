# ⚙️ PacketAnalyzerService — Code Documentation

## 🗂️ Overview

`PacketAnalyzerService` is the **single-threaded service layer** of the Packet Analyzer application. It acts as the bridge between the raw PCAP file reader and the packet parser — orchestrating the two-step process of reading raw packets from a file and transforming them into structured, usable data.

> 🧵 **Note:** This is the single-threaded implementation. A separate `PacketAnalyzerMTService` handles multi-threaded processing (used by the controller).

---

## 🏗️ Class-Level Details

```java
public class PacketAnalyzerService {
```

- No Spring annotations (`@Service`, etc.) — this is a **plain Java class**, instantiated manually.
- Designed as a **single-threaded** processor; packets are read and parsed sequentially, one at a time.

---

## 🔧 Fields

```java
private final PcapReader reader = new PcapReader();
private final PacketParser parser = new PacketParser();
```

| Field | Type | Role |
|---|---|---|
| `reader` | `PcapReader` | Reads raw packets from a `.pcap` file and returns them as a list of `Packet` objects |
| `parser` | `PacketParser` | Takes a raw `Packet` object and converts it into a structured `PacketInfo` model |

- Both are declared `final` — initialized once and never reassigned.
- Both are instantiated directly (no dependency injection).

---

## 🌐 Method — `analyze(String filePath)`

```java
public List<PacketInfo> analyze(String filePath)
```

| Part | Meaning |
|---|---|
| `String filePath` | Absolute path to the `.pcap` file on disk |
| `List<PacketInfo>` | Returns a list of successfully parsed packet info objects |

---

## 🔄 Method Flow — Step by Step

### Step 1 — 📖 Read Raw Packets from File

```java
List<Packet> packets = reader.readPackets(filePath);
```

- Calls `PcapReader.readPackets()` with the provided file path.
- Returns a `List<Packet>` — raw **pcap4j `Packet` objects** containing low-level network data (headers, payloads, etc.).
- The `PcapReader` abstracts away all the complexity of opening and reading the binary `.pcap` format.

---

### Step 2 — 🔬 Parse Each Packet

```java
List<PacketInfo> results = new ArrayList<>();

for (Packet packet : packets) {
    PacketInfo info = parser.parse(packet);

    if (info != null) {
        results.add(info);
    }
}
```

- `new ArrayList<>()` — Initializes an empty list to collect parsed results.
- The `for` loop iterates over every raw `Packet` **one by one** (sequential, single-threaded).
- `parser.parse(packet)` — Converts each raw `Packet` into a structured `PacketInfo` object (e.g., source IP, destination IP, protocol, port, etc.).
- `if (info != null)` — Guards against packets the parser could not handle (unknown or malformed). Only valid results are collected.
- `results.add(info)` — Appends each successfully parsed packet to the results list.

---

### ✅ Return Result

```java
return results;
```

- Returns the fully populated `List<PacketInfo>` to the caller (typically `PacketController` or `PacketAnalyzerMTService`).

---

## 🔁 Complete Flow Diagram

```
filePath (String)
      |
      ▼
PcapReader.readPackets()
      |
      ▼
List<Packet>  (raw pcap4j packets)
      |
      ▼
 for each Packet
      |
      ├── PacketParser.parse(packet)
      |         |
      |         ├── returns null?  → ❌ skip
      |         └── returns info?  → ✅ add to results
      |
      ▼
List<PacketInfo>  (structured results)
      |
      ▼
   Caller (PacketController / MTService)
```

---

## ⚖️ Single-Threaded vs Multi-Threaded

| Aspect | `PacketAnalyzerService` (this file) | `PacketAnalyzerMTService` |
|---|---|---|
| Processing | Sequential, one packet at a time | Parallel, multiple packets concurrently |
| Performance | Slower on large files | Faster on large files |
| Complexity | Simple `for` loop | Thread pool / parallel streams |
| Used by | Standalone / testing | `PacketController` (production) |

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `PacketAnalyzerService.java` |
| **Package** | `com.packetanalyzer.service` |
| **Role** | Single-threaded service layer |
| **Input** | File path to a `.pcap` file |
| **Output** | `List<PacketInfo>` (structured packet data) |
| **Dependencies** | `PcapReader`, `PacketParser` |
| **Null Handling** | Skips packets that fail to parse |
| **Threading** | Single-threaded (sequential) |