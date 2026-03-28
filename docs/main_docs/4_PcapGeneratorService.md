# 🛠️ PcapGeneratorService — Code Documentation

## 🗂️ Overview

`PcapGeneratorService` is a **utility service** that programmatically generates a realistic sample `.pcap` file populated with **25 synthetic network packets**. It is useful for testing and development — providing a ready-made capture file without needing a real network interface or live traffic capture.

The generated file contains a realistic mix of three common traffic types:

| Traffic Type | Protocol | Port |
|---|---|---|
| 🌐 HTTP | TCP | 80 |
| 🔒 HTTPS | TCP | 443 |
| 🔍 DNS | UDP | 53 |

---

## 🏗️ Class-Level Details

```java
public class PcapGeneratorService {
```

- A **plain Java class** with no Spring annotations — instantiated manually wherever needed.
- Uses the **pcap4j** library to construct and write real binary-format `.pcap` files.
- Builds packets layer by layer from the ground up: Ethernet → IP → TCP/UDP.

---

## 🌐 Method — `generateSamplePcap()`

```java
public File generateSamplePcap() throws PcapNativeException, NotOpenException, IOException
```

| Part | Meaning |
|---|---|
| `File` | Returns a reference to the generated temporary `.pcap` file on disk |
| `throws PcapNativeException` | Thrown if the native pcap library encounters an error |
| `throws NotOpenException` | Thrown if writing to a closed pcap handle is attempted |
| `throws IOException` | Thrown if the temp file cannot be created on disk |

---

## 🔄 Method Flow — Step by Step

### Step 1 — 🎲 Initialize Randomizer and IP Pools

```java
Random random = new Random();

String[] ips = { "192.168.1.2", "192.168.1.3", "10.0.0.5", "172.16.0.8" };

String[] dstIps = {
    "93.184.216.34",   // example.com
    "142.250.183.78",  // google
    "151.101.1.69",    // github
    "104.244.42.1"     // twitter
};
```

- `Random random` — Used throughout to randomize IPs, ports, sequence numbers, and protocol types, making the generated traffic feel realistic rather than uniform.
- `ips[]` — A pool of **private/internal source IPs** simulating different machines on a local network.
- `dstIps[]` — A pool of **real public destination IPs** (example.com, Google, GitHub, Twitter) to simulate outbound internet traffic.

---

### Step 2 — 💾 Create Temp File and Open Pcap Writer

```java
File file = File.createTempFile("sample", ".pcap");
PcapHandle handle = Pcaps.openDead(DataLinkType.EN10MB, 65536);
PcapDumper dumper = handle.dumpOpen(file.getAbsolutePath());
```

- `File.createTempFile("sample", ".pcap")` — Creates a blank temp file on disk (e.g., `sample12345.pcap`) in the system temp directory.
- `Pcaps.openDead(DataLinkType.EN10MB, 65536)` — Opens a **"dead" pcap handle** (not tied to a live network interface), configured for Ethernet (`EN10MB`) frames with a max packet size of 65536 bytes. This is the standard way to write `.pcap` files offline.
- `handle.dumpOpen(...)` — Opens a `PcapDumper` that writes packets to the temp file in standard `.pcap` binary format.

---

### Step 3 — 🔁 Generate 25 Packets (Loop)

```java
for (int i = 0; i < 25; i++) {
```

- Iterates 25 times, generating one complete network packet per iteration.
- Each iteration independently randomizes all packet attributes (IPs, ports, protocol type).

---

#### 3a — 🎯 Pick Random Source and Destination IPs

```java
Inet4Address srcIp = (Inet4Address) InetAddress.getByName(
        ips[random.nextInt(ips.length)]
);
Inet4Address dstIp = (Inet4Address) InetAddress.getByName(
        dstIps[random.nextInt(dstIps.length)]
);
```

- `random.nextInt(ips.length)` — Picks a random index into the IP pool.
- `InetAddress.getByName(...)` — Resolves the IP string into a Java `Inet4Address` object, required by pcap4j builders.
- Cast to `(Inet4Address)` — narrows from `InetAddress` to the IPv4-specific type.

---

#### 3b — 🎰 Pick Protocol Type

```java
int protocolType = random.nextInt(3); // 0=HTTP, 1=HTTPS, 2=DNS
```

- Randomly selects one of three protocol types:
    - `0` → HTTP (TCP port 80)
    - `1` → HTTPS (TCP port 443)
    - `2` → DNS (UDP port 53)

---

### Step 4 — 🔵 Build TCP Packet (HTTP or HTTPS)

```java
if (protocolType == 0 || protocolType == 1) {

    int dstPort = (protocolType == 0) ? 80 : 443;

    TcpPacket.Builder tcpBuilder = new TcpPacket.Builder()
            .srcPort(TcpPort.getInstance((short) (10000 + random.nextInt(50000))))
            .dstPort(TcpPort.getInstance((short) dstPort))
            .sequenceNumber(random.nextInt(10000))
            .acknowledgmentNumber(random.nextInt(10000))
            .dataOffset((byte) 5)
            .syn(true)
            .ack(true)
            .window((short) 65535)
            .srcAddr(srcIp)
            .dstAddr(dstIp)
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true);
```

- `dstPort` — Set to `80` for HTTP or `443` for HTTPS based on the protocol type.
- `srcPort` — A random **ephemeral port** between 10000–60000, simulating a real client connection.
- `sequenceNumber` / `acknowledgmentNumber` — Random values simulating a mid-connection TCP state.
- `dataOffset((byte) 5)` — Standard TCP header size (5 × 32-bit words = 20 bytes, no options).
- `syn(true).ack(true)` — Simulates a **SYN-ACK** handshake packet.
- `window((short) 65535)` — Sets TCP receive window to maximum, typical for modern systems.
- `correctChecksumAtBuild(true)` — Instructs pcap4j to **auto-calculate** the TCP checksum when the packet is built.
- `correctLengthAtBuild(true)` — Instructs pcap4j to **auto-calculate** the TCP length field.

```java
    IpV4Packet ipPacket = new IpV4Packet.Builder()
            .version(IpVersion.IPV4)
            .tos(IpV4Rfc791Tos.newInstance((byte) 0))
            .identification((short) random.nextInt(1000))
            .ttl((byte) 64)
            .protocol(IpNumber.TCP)
            .srcAddr(srcIp)
            .dstAddr(dstIp)
            .payloadBuilder(tcpBuilder)
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true)
            .build();
```

- Wraps the TCP segment inside an **IPv4 packet**.
- `tos((byte) 0)` — Default Type of Service (best effort, no QoS).
- `identification` — Random value used for IP fragmentation tracking.
- `ttl((byte) 64)` — Standard Time-To-Live (64 hops), typical for Linux/macOS systems.
- `protocol(IpNumber.TCP)` — Tells the IP layer the payload is TCP.
- `payloadBuilder(tcpBuilder)` — Nests the TCP builder inside the IP builder; pcap4j links them at build time.

```java
    packet = new EthernetPacket.Builder()
            .srcAddr(MacAddress.getByName("00:11:22:33:44:55"))
            .dstAddr(MacAddress.getByName("66:77:88:99:AA:BB"))
            .type(EtherType.IPV4)
            .payloadBuilder(new SimpleBuilder(ipPacket))
            .paddingAtBuild(true)
            .build();
```

- Wraps the IP packet inside an **Ethernet frame** — the outermost layer.
- `srcAddr` / `dstAddr` — Fixed fake MAC addresses (hardware identifiers for source and destination NICs).
- `type(EtherType.IPV4)` — Tells the Ethernet frame that its payload is an IPv4 packet.
- `paddingAtBuild(true)` — Auto-pads the frame to the minimum Ethernet size (64 bytes) if needed.

---

### Step 5 — 🟡 Build UDP Packet (DNS)

```java
} else {

    UdpPacket.Builder udpBuilder = new UdpPacket.Builder()
            .srcPort(UdpPort.getInstance((short) (10000 + random.nextInt(50000))))
            .dstPort(UdpPort.DOMAIN)
            .srcAddr(srcIp)
            .dstAddr(dstIp)
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true);
```

- Same structure as TCP but uses `UdpPacket.Builder` instead.
- `dstPort(UdpPort.DOMAIN)` — pcap4j's named constant for **port 53** (DNS).
- No `syn`/`ack` flags — UDP is **connectionless** and has no handshake.
- The UDP segment is then wrapped in the same IPv4 → Ethernet layer structure as the TCP packets above, with `protocol(IpNumber.UDP)` set on the IP layer.

---

### Step 6 — 📤 Write Packet to File

```java
dumper.dump(packet);
```

- Writes the fully assembled Ethernet frame (containing IP → TCP/UDP) to the `.pcap` file in binary format.
- Called once per loop iteration — 25 total packets are written.

---

### Step 7 — 🔒 Close Handles and Return File

```java
dumper.close();
handle.close();

return file;
```

- `dumper.close()` — Flushes and closes the pcap file writer. **Must be called** to ensure all data is written and the file is properly finalized.
- `handle.close()` — Releases the native pcap handle and frees system resources.
- `return file` — Returns the completed `.pcap` file reference to the caller.

---

## 🔁 Complete Flow Diagram

```
generateSamplePcap()
      |
      ├── Create temp file (sample.pcap)
      ├── Open PcapHandle (dead, Ethernet, 65536 MTU)
      ├── Open PcapDumper → writes to temp file
      |
      ▼
 Loop 25 times
      |
      ├── Pick random srcIp, dstIp
      ├── Pick random protocolType (0, 1, 2)
      |
      ├── protocolType 0 or 1 (HTTP / HTTPS)
      |       └── Build TcpPacket
      |               └── Wrap in IpV4Packet (protocol=TCP)
      |                       └── Wrap in EthernetPacket
      |
      └── protocolType 2 (DNS)
              └── Build UdpPacket
                      └── Wrap in IpV4Packet (protocol=UDP)
                              └── Wrap in EthernetPacket
      |
      ├── dumper.dump(packet)  → write to file
      |
      ▼
 dumper.close() + handle.close()
      |
      ▼
 Return File (sample.pcap)
```

---

## 🧱 Packet Layer Structure

Every generated packet follows the same 3-layer encapsulation model:

```
┌─────────────────────────────────┐
│         Ethernet Frame          │  MAC src/dst, EtherType=IPv4
│  ┌───────────────────────────┐  │
│  │       IPv4 Packet         │  │  src/dst IP, TTL=64, protocol
│  │  ┌─────────────────────┐  │  │
│  │  │   TCP  or  UDP      │  │  │  src/dst port, flags/checksum
│  │  └─────────────────────┘  │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `PcapGeneratorService.java` |
| **Package** | `com.packetanalyzer.service` |
| **Role** | Generates a synthetic sample `.pcap` file for testing |
| **Output** | A temporary `.pcap` file with 25 mixed packets |
| **Traffic Types** | HTTP (TCP/80), HTTPS (TCP/443), DNS (UDP/53) |
| **IP Randomization** | 4 source IPs × 4 destination IPs, chosen randomly |
| **Library Used** | pcap4j (packet construction + file writing) |
| **Packet Structure** | Ethernet → IPv4 → TCP or UDP |
| **Checksums** | Auto-calculated by pcap4j at build time |
| **Cleanup** | Caller is responsible for deleting the temp file after use |