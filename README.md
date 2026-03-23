# 🔬 Packet Analyzer — Deep Packet Inspection Engine (Java)

> A modular network packet analyzer and Deep Packet Inspection (DPI) engine built in Java. Processes `.pcap` files to extract structured insights from raw network traffic — identifying applications, parsing protocols, and extracting domain names from TLS handshakes. Inspired by real-world tools like Wireshark.

---

## 📋 Table of Contents

- [What is Deep Packet Inspection?](#-what-is-deep-packet-inspection)
- [What This Project Does](#-what-this-project-does)
- [Architecture](#-architecture)
- [Module Breakdown](#-module-breakdown)
- [The Network Stack — Background](#-the-network-stack--background)
- [How a Packet Flows Through the System](#-how-a-packet-flows-through-the-system)
- [SNI Extraction — The Core of DPI](#-sni-extraction--the-core-of-dpi)
- [Tech Stack](#-tech-stack)
- [Setup & Installation](#-setup--installation)
- [Running the Project](#-running-the-project)
- [Sample Output](#-sample-output)
- [Key Learnings](#-key-learnings)
- [Future Enhancements](#-future-enhancements)

---

## 🔍 What is Deep Packet Inspection?

A basic firewall looks only at a packet's envelope — the source IP, destination IP, and port. **Deep Packet Inspection (DPI)** opens the envelope and reads what's inside.

```
Basic Firewall:     Sees only → [IP: 142.250.183.78] [Port: 443]
DPI Engine:         Also sees → [App: YouTube] [Domain: www.youtube.com]
```

DPI is used in the real world by ISPs to throttle BitTorrent, by enterprises to block social media, by parental controls to filter inappropriate content, and by security systems to detect malware — all by inspecting traffic that appears identical at the surface level.

The key insight that makes DPI possible on HTTPS traffic: even though the data is encrypted, the **domain name is transmitted in plaintext** during the TLS handshake, in a field called **SNI (Server Name Indication)**. This project extracts that field.

---

## 🎯 What This Project Does

```
Input: .pcap file (captured network traffic)
              ↓
        PcapReader          ← Reads binary file, validates format
              ↓
        PacketParser        ← Ethernet → IP → TCP/UDP headers
              ↓
        SNIExtractor        ← TLS Client Hello → domain name
              ↓
        PacketInfo          ← Structured data object per packet
              ↓
Output: Protocol breakdown, identified applications, detected domains
```

Given a `.pcap` file (the standard format Wireshark uses to save captured traffic), the engine reads every packet, parses each layer of the network stack, performs Deep Packet Inspection on TLS traffic to extract domain names, classifies packets by application (YouTube, Facebook, Google, DNS, etc.), and outputs a structured traffic summary.

---

## 🧱 Architecture

The project follows a clean, single-responsibility modular design where each component depends only on the layer below it:

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│                   Entry point, orchestration                 │
└──────────────────────────────┬───────────────────────────────┘
                               │
              ┌────────────────▼────────────────┐
              │          PcapReader              │
              │  Reads raw bytes from .pcap      │
              │  Validates file format           │
              └────────────────┬────────────────┘
                               │  RawPacket
              ┌────────────────▼────────────────┐
              │         PacketParser             │
              │  Parses Ethernet header          │
              │  Parses IP header                │
              │  Parses TCP / UDP header         │
              └────────────────┬────────────────┘
                               │  ParsedPacket
              ┌────────────────▼────────────────┐
              │         SNIExtractor             │
              │  Inspects TCP payload            │
              │  Parses TLS Client Hello         │
              │  Extracts SNI hostname           │
              └────────────────┬────────────────┘
                               │  PacketInfo (complete)
              ┌────────────────▼────────────────┐
              │          PacketInfo              │
              │  Structured data model           │
              │  IPs, ports, protocol, app, SNI  │
              └─────────────────────────────────┘
```

Changes to one layer don't ripple upward. `SNIExtractor` has no knowledge of `PcapReader`. `PacketParser` has no knowledge of `SNIExtractor`. Each component is independently testable.

---

## 📁 Module Breakdown

```
packet-analyzer/
├── src/
│   └── main/java/com/packetanalyzer/
│       ├── Main.java                  ← Entry point, orchestrates the pipeline
│       ├── reader/
│       │   └── PcapReader.java        ← Reads .pcap files, validates format
│       ├── parser/
│       │   └── PacketParser.java      ← Parses headers layer by layer
│       ├── inspector/
│       │   └── SNIExtractor.java      ← TLS handshake inspection, SNI extraction
│       └── model/
│           └── PacketInfo.java        ← Data model for a fully-parsed packet
├── docs/                              ← Component documentation
├── resources/
│   └── sample.pcap                    ← Sample capture for testing
└── pom.xml
```

| Module | Responsibility |
|---|---|
| `PcapReader` | Opens `.pcap` files, validates the magic number, reads packet headers and raw bytes one at a time |
| `PacketParser` | Decodes raw bytes into Ethernet, IP, and TCP/UDP fields. Handles network byte-order conversion |
| `SNIExtractor` | Inspects TCP payloads for TLS Client Hello messages and extracts the SNI hostname field |
| `PacketInfo` | Plain data class holding all extracted fields: IPs, ports, protocol, application type, domain name |

---

## 🌐 The Network Stack — Background

Every network packet is a **Russian nesting doll** — headers wrapped inside headers, each layer added by a different level of the network stack:

```
┌──────────────────────────────────────────────────────────────────┐
│ Ethernet Header (14 bytes)                                       │
│  Source MAC, Destination MAC, EtherType (0x0800 = IPv4)          │
│ ┌──────────────────────────────────────────────────────────────┐ │
│ │ IP Header (20 bytes)                                         │ │
│ │  Source IP, Destination IP, Protocol (TCP=6 / UDP=17), TTL   │ │
│ │ ┌──────────────────────────────────────────────────────────┐ │ │
│ │ │ TCP Header (20 bytes)                                    │ │ │
│ │ │  Source Port, Destination Port, Seq/Ack Numbers, Flags   │ │ │
│ │ │ ┌──────────────────────────────────────────────────────┐ │ │ │
│ │ │ │ Payload (Application Data)                           │ │ │ │
│ │ │ │  e.g. TLS Client Hello containing SNI hostname       │ │ │ │
│ │ │ └──────────────────────────────────────────────────────┘ │ │ │
│ │ └──────────────────────────────────────────────────────────┘ │ │
│ └──────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

Each parsing step in this project peels back one layer:
- `PcapReader` handles the file wrapper
- `PacketParser` handles Ethernet, IP, and TCP/UDP
- `SNIExtractor` handles the Application layer payload

### The Five-Tuple

Every network connection is uniquely identified by five values:

| Field | Example | Purpose |
|---|---|---|
| Source IP | `192.168.1.100` | Who is sending |
| Destination IP | `142.250.183.78` | Where it's going |
| Source Port | `54321` | Sender's ephemeral port |
| Destination Port | `443` | The service (443 = HTTPS) |
| Protocol | `TCP (6)` | Reliable vs. unreliable delivery |

All packets sharing the same five-tuple belong to the same connection (flow). This is how traffic is grouped for analysis.

---

## 🔄 How a Packet Flows Through the System

### Step 1 — PcapReader reads the binary file

A `.pcap` file has a specific binary format:

```
┌────────────────────────────┐
│ Global Header (24 bytes)   │  ← Read once on open. Magic number: 0xa1b2c3d4
├────────────────────────────┤
│ Packet Header (16 bytes)   │  ← Timestamp (sec + microsec), captured length
│ Packet Data (variable)     │  ← The raw bytes of one network packet
├────────────────────────────┤
│ Packet Header (16 bytes)   │
│ Packet Data (variable)     │
└────────────────────────────┘
```

`PcapReader` reads the global header once, verifies the magic number (confirms it's a valid pcap file), then loops — reading one 16-byte packet header followed by N bytes of packet data per iteration. Each iteration returns a `RawPacket`.

---

### Step 2 — PacketParser decodes the headers

`PacketParser` takes the raw byte array and decodes each protocol layer by reading fixed byte offsets.

**Ethernet Header (bytes 0–13):**
```
Bytes 0–5:   Destination MAC address
Bytes 6–11:  Source MAC address
Bytes 12–13: EtherType (0x0800 = IPv4, 0x86DD = IPv6)
```

**IP Header (bytes 14–33):**
```
Byte 14:      Version (4 bits) + IHL (4 bits) — IHL × 4 = header length
Byte 23:      Protocol (6 = TCP, 17 = UDP)
Bytes 26–29:  Source IP (4 bytes)
Bytes 30–33:  Destination IP (4 bytes)
```

**TCP Header (bytes 34+):**
```
Bytes 34–35:  Source Port
Bytes 36–37:  Destination Port
Bytes 38–41:  Sequence Number
Byte 46:      Data Offset (upper 4 bits) — × 4 = TCP header length
Byte 47:      Flags (bit 1=SYN, bit 4=ACK, bit 0=FIN)
Bytes 48+:    Payload starts here (offset = 14 + IP_header_len + TCP_header_len)
```

> **📌 Network Byte Order:** Network protocols transmit multi-byte integers in **big-endian** order (most significant byte first). Java's `ByteBuffer` defaults to `BIG_ENDIAN`, which matches network byte order natively. This is one advantage of the Java port — C++ requires explicit `ntohs()` and `ntohl()` calls for every field.

---

### Step 3 — SNIExtractor inspects the payload

For TCP traffic on port 443, the payload is inspected for a **TLS Client Hello** message — the first message a browser sends when starting an HTTPS connection.

**TLS record structure:**
```
Byte 0:       Content Type = 0x16 (Handshake record)
Bytes 1–2:    TLS Version (0x0301 = TLS 1.0, 0x0303 = TLS 1.3)
Bytes 3–4:    Record Length

Byte 5:       Handshake Type = 0x01 (Client Hello)
Bytes 6–8:    Handshake Message Length
```

**Navigating to the SNI extension:**
```
Skip: Client Version (2 bytes)
Skip: Random (32 bytes)
Skip: Session ID (1-byte length + N bytes)
Skip: Cipher Suites (2-byte length + N bytes)
Skip: Compression Methods (1-byte length + N bytes)
Read: Extensions Length (2 bytes)

For each extension:
    Extension Type (2 bytes)
    Extension Length (2 bytes)
    Extension Data (N bytes)

    If Extension Type == 0x0000 (SNI):
        SNI List Length (2 bytes)
        SNI Type (1 byte) = 0x00 (hostname)
        SNI Length (2 bytes)
        SNI Value → "www.youtube.com"  ← EXTRACTED
```

**Why is the domain name unencrypted?**

TLS uses the domain name to decide which server certificate to send — this happens before encryption is negotiated. The SNI field exists in the very first message of the handshake, before any keys are exchanged. This is a fundamental property of TLS 1.2 and earlier. TLS 1.3 introduced Encrypted Client Hello (ECH) to address this privacy gap, but it isn't universally deployed yet.

---

### Step 4 — PacketInfo is assembled

All extracted data is collected into a `PacketInfo` object:

```java
public class PacketInfo {
    String sourceIp;
    String destinationIp;
    int    sourcePort;
    int    destinationPort;
    String protocol;      // "TCP" or "UDP"
    String application;   // "YouTube", "Facebook", "HTTPS", "DNS", etc.
    String domain;        // "www.youtube.com" from SNI, or null
}
```

Application classification maps detected domains to known services:

| SNI Contains | Classified As |
|---|---|
| `youtube` | YouTube |
| `facebook`, `fb.com` | Facebook |
| `google` | Google |
| `github` | GitHub |
| Port 53 (UDP) | DNS |
| Port 443, no SNI yet | HTTPS |
| Port 80 | HTTP |

---

## 🔬 SNI Extraction — The Core of DPI

This is the most technically interesting part of the project. Here is a full walkthrough of what happens when a browser connects to `https://www.youtube.com`:

```
Browser                                    YouTube Server
   │                                             │
   │─── TLS Client Hello ───────────────────────►│
   │    [Content: 0x16]                          │
   │    [Handshake: 0x01 Client Hello]           │
   │    [SNI Extension: www.youtube.com] ← HERE  │
   │                                             │
   │◄── TLS Server Hello ───────────────────────-│
   │    [Certificate for www.youtube.com]        │
   │                                             │
   │─── Key Exchange ────────────────────────────►│
   │                                             │
   │◄══► Encrypted Application Data ◄══►        │
   │     (everything from here is encrypted)     │
```

The SNI is only visible in the **Client Hello** — the very first packet of the HTTPS connection. After that, everything is encrypted. The DPI engine catches this first packet and extracts the hostname before it disappears into the encrypted stream.

---

## 🛠️ Tech Stack

| Component | Technology | Why |
|---|---|---|
| Language | Java 21 | Strong byte manipulation, cross-platform, JVM memory safety |
| Packet library | Pcap4J 1.8.2 | Java wrapper for libpcap / Npcap — industry standard |
| Build tool | Maven | Dependency management, familiar to Java ecosystem |
| Native dependency | libpcap (Linux/macOS) / Npcap (Windows) | Required by Pcap4J for reading pcap files |

---

## ⚙️ Setup & Installation

### 1. Install the native pcap library

Pcap4J wraps the native `libpcap` library. Install it for your OS first.

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install libpcap-dev
```

**macOS:**
```bash
brew install libpcap
```

**Windows:**
Download and install [Npcap](https://npcap.com/#download). During installation, check **"Install Npcap in WinPcap API-compatible Mode"**.

---

### 2. Add Maven dependencies

In `pom.xml`:

```xml
<dependencies>
    <!-- Pcap4J core library -->
    <dependency>
        <groupId>org.pcap4j</groupId>
        <artifactId>pcap4j-core</artifactId>
        <version>1.8.2</version>
    </dependency>
    <!-- Static packet factory (required for pcap4j-core) -->
    <dependency>
        <groupId>org.pcap4j</groupId>
        <artifactId>pcap4j-packetfactory-static</artifactId>
        <version>1.8.2</version>
    </dependency>
</dependencies>
```

---

### 3. Build the project

```bash
cd packet-analyzer

# Using Maven Wrapper (no global Maven needed)
./mvnw clean package        # Linux / macOS
mvnw.cmd clean package      # Windows

# Or if Maven is installed globally
mvn clean package
```

---

## ▶️ Running the Project

1. Place a `.pcap` file in the project directory, or use `resources/sample.pcap`
2. Update the file path in `Main.java`:
   ```java
   String pcapFile = "resources/sample.pcap";
   ```
3. Run the application:
   ```bash
   java -jar target/packet-analyzer-1.0.jar
   ```
   Or run directly from IntelliJ by opening `Main.java` and clicking ▶️ Run.

**Generating a test `.pcap` file:**
If you don't have a capture file, you can generate one using the included Python script:
```bash
python3 generate_test_pcap.py
```
This produces a `test_dpi.pcap` with a variety of simulated traffic including HTTPS, DNS, and HTTP.

---

## 📊 Sample Output

```
Packet #1
  From:         192.168.1.2 → 142.250.183.78
  Protocol:     TCP
  Ports:        51532 → 443
  Application:  HTTPS
  Domain (SNI): google.com

Packet #2
  From:         192.168.1.2 → 31.13.93.35
  Protocol:     TCP
  Ports:        52341 → 443
  Application:  Facebook
  Domain (SNI): www.facebook.com

Packet #3
  From:         192.168.1.5 → 8.8.8.8
  Protocol:     UDP
  Ports:        59812 → 53
  Application:  DNS
  Domain (SNI): —

──────────────────────────────────────────────
 SUMMARY
──────────────────────────────────────────────
 Total Packets Processed:   77
 TCP Packets:               73
 UDP Packets:                4

 APPLICATION BREAKDOWN
  HTTPS        →  39 packets  (50.6%)
  Unknown      →  16 packets  (20.8%)
  YouTube      →   4 packets   (5.2%)
  DNS          →   4 packets   (5.2%)
  Facebook     →   3 packets   (3.9%)
  Google       →   8 packets  (10.4%)
  GitHub       →   3 packets   (3.9%)

 DETECTED DOMAINS
  www.youtube.com      → YouTube
  www.facebook.com     → Facebook
  www.google.com       → Google
  github.com           → GitHub
──────────────────────────────────────────────
```

---

## 🧠 Key Learnings

Building this project teaches concepts foundational to networking, security, and systems programming:

| Concept | What It Demonstrates |
|---|---|
| **Network Protocol Parsing** | How headers are structured as fixed-byte fields and how to decode them at the binary level |
| **Deep Packet Inspection** | How to extract meaningful application-layer data from traffic that appears opaque at the IP level |
| **TLS Handshake Structure** | How HTTPS connections are established and specifically *why* the domain name is visible before encryption begins |
| **Network Byte Order** | Why multi-byte fields must be read in big-endian order and how Java's `ByteBuffer` handles this correctly |
| **Five-Tuple Flow Tracking** | How connections are uniquely identified and how packets belonging to the same session are grouped |
| **Java Byte Manipulation** | Working with raw `byte[]` arrays, bit masking with `& 0xFF`, and `ByteBuffer` for low-level protocol parsing |
| **Pcap4J / libpcap** | The packet capture library used by real-world tools including Wireshark and tcpdump |

---

## 🚀 Future Enhancements

| Feature | Description |
|---|---|
| 🔒 Packet filtering / blocking | Write a filtered output `.pcap` — drop packets matching blocked domains or applications |
| 📡 Live capture | Capture from a live network interface instead of a static `.pcap` file using Pcap4J's live capture API |
| 🧵 Multi-threaded pipeline | Reader → Load Balancer → Fast Path worker threads for processing large captures efficiently |
| 🌐 HTTP Host extraction | Extract domain from the HTTP `Host:` header for plaintext HTTP traffic (port 80) |
| 📈 Statistics dashboard | Real-time protocol and application breakdown updated as packets are processed |
| 🔁 QUIC / HTTP3 support | Parse UDP port 443 traffic using the QUIC protocol (used by modern YouTube, Google services) |
| 💾 JSON / CSV export | Structured output file for downstream analysis or visualisation |
| 🛡️ Malware signature detection | Match payload patterns against known signatures to flag suspicious traffic |
