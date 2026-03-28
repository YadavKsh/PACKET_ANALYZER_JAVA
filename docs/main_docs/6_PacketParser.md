# 🔬 PacketParser — Code Documentation

## 🗂️ Overview

`PacketParser` is the **analysis layer** of the Packet Analyzer application. It takes a decoded `EthernetPacket` (as produced by `PcapReader`) and extracts meaningful network information from it, returning a structured `PacketInfo` object containing IP addresses, transport protocol, ports, and application-level protocol identification.

> 🎯 **Single Responsibility:** This class only interprets and structures packet data. Reading packets from disk is handled by `PcapReader`; storing results is handled by `PacketInfo`.

---

## 🏗️ Class-Level Details

```java
public class PacketParser {
```

- A **plain Java class** with no Spring annotations — instantiated directly by `PacketAnalyzerService` and `PacketAnalyzerMTService`.
- Designed to be **stateless** — `parse()` takes a packet, returns a result, and holds no internal state between calls. This is what makes it safe to share across threads in `PacketAnalyzerMTService`.
- Extracts four categories of information: IP addresses, transport protocol (TCP/UDP), port numbers, and application protocol (HTTP/HTTPS/DNS).

---

## 🌐 Method — `parse(Packet packet)`

```java
public PacketInfo parse(Packet packet)
```

| Part | Meaning |
|---|---|
| `Packet packet` | A decoded `EthernetPacket` from `PcapReader` |
| `PacketInfo` | Returns a structured object with extracted fields, or `null` if the packet is not IP-based |

---

## 🔄 Method Flow — Step by Step

### Step 1 — 🧭 Extract the IP Layer

```java
IpPacket ipPacket = packet.get(IpPacket.class);

if (ipPacket == null) {
    return null;
}
```

- `packet.get(IpPacket.class)` — Traverses the packet's layer chain (Ethernet → IP → TCP/UDP) and returns the **IP layer** if present, or `null` if not found.
- This is pcap4j's way of safely navigating nested protocol layers without manual byte-offset arithmetic.
- `if (ipPacket == null) return null` — Filters out non-IP traffic such as **ARP** (Address Resolution Protocol) or other Layer 2 frames that don't carry an IP payload. The caller (`PacketAnalyzerService`) already handles `null` returns by skipping them.

---

### Step 2 — 🏷️ Extract Source and Destination IP Addresses

```java
PacketInfo info = new PacketInfo();
info.setSrcIp(ipPacket.getHeader().getSrcAddr().getHostAddress());
info.setDstIp(ipPacket.getHeader().getDstAddr().getHostAddress());
```

- `new PacketInfo()` — Creates the result object that will be populated and returned.
- `ipPacket.getHeader()` — Accesses the **IP header**, which contains addressing and routing metadata.
- `.getSrcAddr().getHostAddress()` — Extracts the **source IP address** as a plain string (e.g., `"192.168.1.2"`).
- `.getDstAddr().getHostAddress()` — Extracts the **destination IP address** as a plain string (e.g., `"93.184.216.34"`).
- Both are set on `info` immediately since every IP packet — whether TCP or UDP — carries these fields.

---

### Step 3 — 🔎 Detect Transport Protocol (TCP or UDP)

```java
TcpPacket tcpPacket = ipPacket.get(TcpPacket.class);
UdpPacket udpPacket = ipPacket.get(UdpPacket.class);
```

- Same `.get()` traversal technique, but now applied to the **IP layer** to find the transport layer inside it.
- `ipPacket.get(TcpPacket.class)` — Returns the TCP segment if the IP packet carries TCP, or `null` otherwise.
- `ipPacket.get(UdpPacket.class)` — Returns the UDP datagram if the IP packet carries UDP, or `null` otherwise.
- Only one of the two will be non-null for any given packet (a packet is either TCP or UDP, not both).

---

### Step 4 — 🔵 Handle TCP Packet

```java
if (tcpPacket != null) {
    info.setProtocol("TCP");
    int srcPort = tcpPacket.getHeader().getSrcPort().valueAsInt();
    int dstPort = tcpPacket.getHeader().getDstPort().valueAsInt();
    info.setSrcPort(srcPort);
    info.setDstPort(dstPort);
```

- `info.setProtocol("TCP")` — Tags the result as a TCP packet.
- `tcpPacket.getHeader().getSrcPort().valueAsInt()` — Reads the **source port** from the TCP header and converts it from pcap4j's `TcpPort` type to a plain `int`.
- `tcpPacket.getHeader().getDstPort().valueAsInt()` — Same for the **destination port**.
- Both ports are stored on `info` for display and further filtering.

---

#### 4a — 🌐 Detect Application Protocol by Port

```java
if      (dstPort == 80  || srcPort == 80)  info.setApplicationProtocol("HTTP");
else if (dstPort == 443 || srcPort == 443) info.setApplicationProtocol("HTTPS");
```

- Performs **port-based application protocol detection** — the standard heuristic used by network analyzers like Wireshark.
- Checks **both** source and destination ports because:
    - `dstPort == 80` → outbound HTTP request (client → server)
    - `srcPort == 80` → inbound HTTP response (server → client)
- Same bidirectional logic applies for HTTPS on port `443`.
- If neither port matches, `applicationProtocol` is left unset (no protocol label assigned).

---

#### 4b — 🔐 Extract SNI Domain from TLS ClientHello

```java
if (dstPort == 443 && tcpPacket.getPayload() != null) {
    byte[] payload = tcpPacket.getPayload().getRawData();
    String sni = SniExtractor.extract(payload);
    if (sni != null) {
        info.setDomain(sni);
    }
}
```

- Only attempted for **outbound HTTPS** (`dstPort == 443`) — the direction where a TLS `ClientHello` handshake message would appear.
- `tcpPacket.getPayload().getRawData()` — Gets the raw byte array of the TCP payload (the TLS record bytes).
- `SniExtractor.extract(payload)` — Parses the TLS record to find the **Server Name Indication (SNI)** extension, which contains the plaintext hostname the client is connecting to (e.g., `"github.com"`). SNI is only present in the **first packet of a TLS handshake**, which is why this check is conditional.
- `if (sni != null)` — SNI is not guaranteed to be present (e.g., if the payload is not a `ClientHello`, or the extension is absent), so the domain is only set when successfully extracted.

> 🔍 **Why SNI matters:** Even though HTTPS traffic is encrypted, the SNI field in the TLS handshake is sent in plaintext — meaning the destination domain name can be recovered without decrypting the traffic.

---

### Step 5 — 🟡 Handle UDP Packet

```java
} else if (udpPacket != null) {
    info.setProtocol("UDP");
    int srcPort = udpPacket.getHeader().getSrcPort().valueAsInt();
    int dstPort = udpPacket.getHeader().getDstPort().valueAsInt();
    info.setSrcPort(srcPort);
    info.setDstPort(dstPort);

    if (dstPort == 53 || srcPort == 53) info.setApplicationProtocol("DNS");
}
```

- Mirrors the TCP branch but for **UDP packets**.
- `info.setProtocol("UDP")` — Tags the result as a UDP packet.
- Port extraction follows the same pattern as TCP using `valueAsInt()`.
- `dstPort == 53 || srcPort == 53` — Detects **DNS traffic** bidirectionally: port 53 outbound is a DNS query, port 53 inbound is a DNS response.
- No SNI extraction for UDP — DNS traffic is plaintext and handled differently; TLS over UDP (DTLS) is out of scope here.

---

### ✅ Return Result

```java
return info;
```

- Returns the fully populated `PacketInfo` object to the caller.
- At minimum it will always contain `srcIp` and `dstIp`.
- Optionally contains `protocol`, `srcPort`, `dstPort`, `applicationProtocol`, and `domain` depending on what the packet carried.

---

## 🔁 Complete Flow Diagram

```
Packet (EthernetPacket from PcapReader)
      |
      ▼
packet.get(IpPacket.class)
      |
      ├── null? (ARP / non-IP) → return null
      |
      ▼
Extract srcIp, dstIp  from IP header
      |
      ▼
ipPacket.get(TcpPacket.class)
ipPacket.get(UdpPacket.class)
      |
      ├── TCP != null
      |       ├── setProtocol("TCP")
      |       ├── extract srcPort, dstPort
      |       ├── port 80?  → setApplicationProtocol("HTTP")
      |       ├── port 443? → setApplicationProtocol("HTTPS")
      |       └── dstPort 443 + payload?
      |               └── SniExtractor.extract() → setDomain(sni)
      |
      └── UDP != null
              ├── setProtocol("UDP")
              ├── extract srcPort, dstPort
              └── port 53? → setApplicationProtocol("DNS")
      |
      ▼
PacketInfo (populated result object)
      |
      ▼
   Caller (PacketAnalyzerService / PacketAnalyzerMTService)
```

---

## 🗂️ PacketInfo Fields Populated by This Class

| Field | Source | Always Set? |
|---|---|---|
| `srcIp` | IP header | ✅ Yes (for all IP packets) |
| `dstIp` | IP header | ✅ Yes (for all IP packets) |
| `protocol` | Transport layer detection | ⚠️ Only if TCP or UDP |
| `srcPort` | TCP / UDP header | ⚠️ Only if TCP or UDP |
| `dstPort` | TCP / UDP header | ⚠️ Only if TCP or UDP |
| `applicationProtocol` | Port-based heuristic | ⚠️ Only if port matches known protocol |
| `domain` | SNI extraction from TLS | ⚠️ Only for TLS ClientHello packets |

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `PacketParser.java` |
| **Package** | `com.packetanalyzer.parser` |
| **Role** | Analysis layer — converts raw packets into structured `PacketInfo` |
| **Input** | A decoded `EthernetPacket` from `PcapReader` |
| **Output** | `PacketInfo` object, or `null` for non-IP packets |
| **IP Extraction** | From IP header (src + dst address) |
| **Protocol Detection** | Layer traversal via `packet.get()` |
| **App Protocol Detection** | Port-based heuristic (80=HTTP, 443=HTTPS, 53=DNS) |
| **SNI Extraction** | From TLS ClientHello payload on port 443 |
| **Thread Safety** | ✅ Stateless — safe for concurrent use across threads |
| **Returns null when** | Packet has no IP layer (e.g., ARP) |