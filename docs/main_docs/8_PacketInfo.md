# 📦 PacketInfo — Code Documentation

## 🗂️ Overview

`PacketInfo` is the **Data Transfer Object (DTO)** of the Packet Analyzer application. It represents a single network packet in a clean, structured form after all raw byte parsing is complete. Rather than passing around low-level pcap4j `Packet` objects throughout the application, `PacketInfo` holds only the human-readable fields that are actually useful — IP addresses, protocol, ports, application protocol, and optionally a domain name.

> 🎯 **Role in the Architecture:** `PacketInfo` is the output type of `PacketParser`, the return type of all `analyze()` service methods, and the JSON payload returned by `PacketController` to the frontend.

---

## 🏗️ Class-Level Details

```java
public class PacketInfo {
```

- A **plain Java POJO** (Plain Old Java Object) — no Spring annotations, no pcap4j dependencies.
- Acts as a **DTO**: carries data between layers without exposing internal implementation details.
- **Fully mutable** — fields can be set individually via setters (used by `PacketParser`) or all at once via the parameterized constructor.
- Spring's Jackson library automatically serializes instances of this class to **JSON** when returned from `PacketController`.

---

## 🗂️ Fields

```java
private String srcIp;
private String dstIp;
private String protocol;
private int srcPort;
private int dstPort;
private String applicationProtocol;
private String domain;
```

| Field | Type | Description | Example Value | Always Set? |
|---|---|---|---|---|
| `srcIp` | `String` | Source IP address | `"192.168.1.2"` | ✅ For all IP packets |
| `dstIp` | `String` | Destination IP address | `"142.250.183.78"` | ✅ For all IP packets |
| `protocol` | `String` | Transport layer protocol | `"TCP"` or `"UDP"` | ⚠️ Only TCP or UDP packets |
| `srcPort` | `int` | Source port number | `52341` | ⚠️ Only TCP or UDP packets |
| `dstPort` | `int` | Destination port number | `443` | ⚠️ Only TCP or UDP packets |
| `applicationProtocol` | `String` | Detected application layer protocol | `"HTTP"`, `"HTTPS"`, `"DNS"` | ⚠️ Only if port matches known protocol |
| `domain` | `String` | Hostname extracted from TLS SNI | `"github.com"` | ⚠️ Only for TLS ClientHello packets |

- All `String` fields default to `null` if not set.
- `srcPort` and `dstPort` default to `0` (Java's default for `int`) if not set.

---

## 🔧 Constructors

### Default Constructor

```java
public PacketInfo() {
}
```

- Creates an empty `PacketInfo` with all fields at their defaults (`null` / `0`).
- Used by `PacketParser`, which builds up the object incrementally via individual setter calls.

---

### Parameterized Constructor

```java
public PacketInfo(String srcIp, String dstIp, String protocol,
                  int srcPort, int dstPort,
                  String applicationProtocol, String domain)
```

- Sets all seven fields in one call.
- Useful for testing or anywhere a fully-formed `PacketInfo` needs to be created in a single expression.

| Parameter | Maps To |
|---|---|
| `srcIp` | `this.srcIp` |
| `dstIp` | `this.dstIp` |
| `protocol` | `this.protocol` |
| `srcPort` | `this.srcPort` |
| `dstPort` | `this.dstPort` |
| `applicationProtocol` | `this.applicationProtocol` |
| `domain` | `this.domain` |

---

## 🔁 Getters and Setters

Each field has a standard getter and setter pair. No logic is applied — they are pure accessors.

| Method | Type | Description |
|---|---|---|
| `getSrcIp()` / `setSrcIp(String)` | `String` | Source IP address |
| `getDstIp()` / `setDstIp(String)` | `String` | Destination IP address |
| `getProtocol()` / `setProtocol(String)` | `String` | Transport protocol (`"TCP"` / `"UDP"`) |
| `getSrcPort()` / `setSrcPort(int)` | `int` | Source port number |
| `getDstPort()` / `setDstPort(int)` | `int` | Destination port number |
| `getApplicationProtocol()` / `setApplicationProtocol(String)` | `String` | App-layer protocol label |
| `getDomain()` / `setDomain(String)` | `String` | SNI-extracted hostname |

> 🔗 **Jackson integration:** Spring's JSON serializer calls all `get*()` methods automatically when converting a `PacketInfo` to JSON. This means every non-null field appears in the API response without any extra configuration.

---

## 🖨️ `toString()` — Human-Readable Output

```java
@Override
public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("PacketInfo {\n");
    sb.append("  ").append(srcIp).append(" → ").append(dstIp).append("\n");
    sb.append("  ").append(protocol).append(" ").append(srcPort).append(" → ").append(dstPort).append("\n");

    if (applicationProtocol != null) {
        sb.append("  App: ").append(applicationProtocol).append("\n");
    }

    if (domain != null) {
        sb.append("  Domain: ").append(domain).append("\n");
    }

    sb.append("}");

    return sb.toString();
}
```

- Overrides Java's default `Object.toString()` to produce a **readable multi-line summary**.
- Uses `StringBuilder` for efficient string concatenation (avoids creating multiple intermediate `String` objects).
- `applicationProtocol` and `domain` are printed **conditionally** — they are only included when non-null, keeping the output clean for packets where these fields weren't detected.

### Example Output

**Full packet (HTTPS with SNI):**
```
PacketInfo {
  192.168.1.2 → 142.250.183.78
  TCP 52341 → 443
  App: HTTPS
  Domain: github.com
}
```

**Minimal packet (DNS, no domain extracted):**
```
PacketInfo {
  10.0.0.5 → 8.8.8.8
  UDP 45231 → 53
  App: DNS
}
```

---

## 🔁 PacketInfo in the Application Data Flow

```
PcapReader
    └── returns List<Packet> (raw EthernetPackets)
              |
              ▼
        PacketParser.parse()
              |
              ├── new PacketInfo()           ← default constructor
              ├── info.setSrcIp(...)
              ├── info.setDstIp(...)
              ├── info.setProtocol(...)
              ├── info.setSrcPort(...)
              ├── info.setDstPort(...)
              ├── info.setApplicationProtocol(...)
              └── info.setDomain(...)        ← optional (SNI only)
              |
              ▼
        PacketInfo (populated)
              |
              ▼
  PacketAnalyzerService / PacketAnalyzerMTService
        └── returns List<PacketInfo>
              |
              ▼
        PacketController
        └── Spring serializes each PacketInfo → JSON
              |
              ▼
        Frontend / API Consumer

        Example JSON output:
        {
          "srcIp": "192.168.1.2",
          "dstIp": "142.250.183.78",
          "protocol": "TCP",
          "srcPort": 52341,
          "dstPort": 443,
          "applicationProtocol": "HTTPS",
          "domain": "github.com"
        }
```

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `PacketInfo.java` |
| **Package** | `com.packetanalyzer.model` |
| **Role** | DTO — structured representation of a parsed network packet |
| **Fields** | 7 total: 2 IPs, 1 protocol, 2 ports, 1 app protocol, 1 domain |
| **Constructors** | Default (empty) + Parameterized (all fields) |
| **Mutability** | Fully mutable via getters and setters |
| **Populated By** | `PacketParser` |
| **Consumed By** | `PacketAnalyzerService`, `PacketAnalyzerMTService`, `PacketController` |
| **JSON Serialization** | Automatic via Spring Jackson (no annotations needed) |
| **Optional Fields** | `protocol`, `srcPort`, `dstPort`, `applicationProtocol`, `domain` |
| **toString()** | Multi-line human-readable summary; optional fields printed conditionally |