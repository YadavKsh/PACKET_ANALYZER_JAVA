# 📂 PcapReader — Code Documentation

## 🗂️ Overview

`PcapReader` is the **data source layer** of the Packet Analyzer application. Its sole responsibility is to open a `.pcap` file from disk, read every packet contained within it, decode each one into a structured `EthernetPacket`, and return them all as a list — without doing any analysis or parsing of the packet contents.

> 🎯 **Single Responsibility:** This class only reads and returns raw packets. All further interpretation is delegated to `PacketParser`.

---

## 🏗️ Class-Level Details

```java
public class PcapReader {
```

- A **plain Java class** with no Spring annotations — instantiated directly by both `PacketAnalyzerService` and `PacketAnalyzerMTService`.
- Uses the **pcap4j** library to read binary `.pcap` files in offline (non-live) mode.
- Designed to be **fault-tolerant** — malformed packets are skipped individually rather than aborting the entire read.

---

## 🌐 Method — `readPackets(String filePath)`

```java
public List<Packet> readPackets(String filePath)
```

| Part | Meaning |
|---|---|
| `String filePath` | Absolute path to the `.pcap` file on disk |
| `List<Packet>` | Returns all successfully decoded packets from the file |

---

## 🔄 Method Flow — Step by Step

### Step 1 — 🗃️ Initialize the Packet List

```java
List<Packet> packets = new ArrayList<>();
```

- Creates an empty list to accumulate decoded packets as they are read.
- Returned at the end of the method, even if empty (on error or empty file).

---

### Step 2 — 📂 Open the PCAP File in Offline Mode

```java
PcapHandle handle = Pcaps.openOffline(filePath);
```

- `Pcaps.openOffline(filePath)` — Opens the `.pcap` file for **reading only**, not live network capture.
- Returns a `PcapHandle` — a stateful object that maintains the current read position within the file and provides access to packets one at a time.
- If the file doesn't exist, is unreadable, or is not a valid `.pcap` file, this line throws an exception caught by the outer `catch` block.

---

### Step 3 — 🔁 Loop Through All Packets

```java
Packet packet;

while ((packet = handle.getNextPacket()) != null) {
    ...
}
```

- `handle.getNextPacket()` — Reads and returns the **next raw packet** from the file, advancing the internal file cursor.
- Returns `null` when the end of the file is reached — this is what terminates the `while` loop.
- The assignment inside the `while` condition is intentional: read one packet, check if it's `null`, and if not, enter the loop body.

---

### Step 4 — 🔍 Manually Decode Each Packet as Ethernet

```java
Packet decoded = EthernetPacket.newPacket(
        packet.getRawData(), 0, packet.length()
);
packets.add(decoded);
```

- `packet.getRawData()` — Extracts the raw byte array of the packet as read from the file.
- `EthernetPacket.newPacket(bytes, offset, length)` — **Manually decodes** the raw bytes into a structured `EthernetPacket`, starting at offset `0` and reading the full packet length.

> ⚠️ **Why manual decoding?**
> `handle.getNextPacket()` returns an `UnknownPacket` by default when using `pcap4j-core` alone — the library does not auto-register Ethernet packet factories without additional modules. Calling `EthernetPacket.newPacket()` explicitly forces correct layer-by-layer decoding (Ethernet → IP → TCP/UDP), making the packet usable by downstream components like `PacketParser`.

- `packets.add(decoded)` — Adds the correctly decoded packet to the results list.

---

### Step 5 — ⚠️ Skip Malformed Packets

```java
} catch (Exception e) {
    System.err.println("Skipping malformed packet: " + e.getMessage());
}
```

- If `EthernetPacket.newPacket()` fails for any packet (e.g., truncated data, unexpected format), the exception is caught **per packet** inside the inner `try/catch`.
- The bad packet is **silently skipped** with a warning printed to `stderr`.
- The `while` loop **continues** reading the remaining packets — one bad packet does not abort the entire file read.

---

### Step 6 — 🔒 Close the File Handle

```java
handle.close();
```

- Releases the native pcap file handle and frees associated system resources.
- Should always be called after reading is complete to prevent resource leaks.

> ⚠️ **Note:** `handle.close()` is only reached if no exception was thrown by `Pcaps.openOffline()`. If the file fails to open, the handle is never created and this line is never reached — which is safe.

---

### Step 7 — 🛡️ Outer Error Handling

```java
} catch (Exception e) {
    e.printStackTrace();
}
```

Catches failures at the file level (as opposed to the per-packet level above), including:

| Scenario | Cause |
|---|---|
| File not found | The provided `filePath` doesn't exist on disk |
| Permission denied | The process doesn't have read access to the file |
| Invalid file format | The file exists but is not a valid `.pcap` file |
| Any other I/O error | Low-level native pcap library failure |

- The stack trace is printed to the console for debugging.
- The method then falls through and returns whatever packets were collected before the error (or an empty list if the file couldn't be opened at all).

---

### ✅ Return Packets

```java
return packets;
```

- Returns the list of all successfully decoded `EthernetPacket` objects.
- Returns an **empty list** (never `null`) if the file could not be opened or contained no valid packets — safe for callers to iterate without null checks.

---

## 🔁 Complete Flow Diagram

```
filePath (String)
      |
      ▼
Pcaps.openOffline(filePath)
      |
      ├── ❌ Fails? (not found / bad format) → catch → print stack trace → return []
      |
      ▼
PcapHandle (file cursor at start)
      |
      ▼
while getNextPacket() != null
      |
      ├── getRawData() → raw bytes
      ├── EthernetPacket.newPacket(bytes, 0, length)
      |         |
      |         ├── ❌ Malformed? → skip + print warning → continue loop
      |         └── ✅ Decoded?   → packets.add(decoded)
      |
      ▼ (null returned = end of file)
handle.close()
      |
      ▼
List<Packet>  (all successfully decoded packets)
      |
      ▼
   Caller (PacketAnalyzerService / PacketAnalyzerMTService)
```

---

## 🧱 Packet Decoding — Before vs After

```
getNextPacket() returns:          EthernetPacket.newPacket() produces:

┌─────────────────────┐           ┌─────────────────────────────────┐
│   UnknownPacket     │    →→→    │       EthernetPacket            │
│   (raw bytes only,  │           │  ┌───────────────────────────┐  │
│    no structure)    │           │  │      IpV4Packet           │  │
└─────────────────────┘           │  │  ┌─────────────────────┐  │  │
                                  │  │  │  TcpPacket /        │  │  │
                                  │  │  │  UdpPacket          │  │  │
                                  │  │  └─────────────────────┘  │  │
                                  │  └───────────────────────────┘  │
                                  └─────────────────────────────────┘
```

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `PcapReader.java` |
| **Package** | `com.packetanalyzer.reader` |
| **Role** | Data source layer — reads and decodes raw packets from a `.pcap` file |
| **Input** | Absolute file path to a `.pcap` file |
| **Output** | `List<Packet>` (decoded `EthernetPacket` objects) |
| **Library Used** | pcap4j (`Pcaps`, `PcapHandle`, `EthernetPacket`) |
| **Read Mode** | Offline only — no live network capture |
| **Malformed Packets** | Skipped individually; reading continues |
| **File Errors** | Stack trace printed; empty list returned |
| **Never Returns** | `null` — always returns a list (possibly empty) |