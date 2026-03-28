# 🔐 SniExtractor — Code Documentation

## 🗂️ Overview

`SniExtractor` is a **low-level TLS byte parser** that extracts the **Server Name Indication (SNI)** hostname from the raw bytes of a TCP payload. SNI is a field inside the TLS `ClientHello` handshake message that reveals the domain name a client is connecting to — even though the rest of the HTTPS session is fully encrypted.

> 🔍 **Why this matters:** HTTPS traffic is encrypted, so the URL and content are hidden. However, the SNI field is sent in plaintext during the TLS handshake. Extracting it allows the analyzer to identify destinations like `github.com` or `google.com` from HTTPS packets without decryption.

---

## 🏗️ Class-Level Details

```java
public class SniExtractor {
```

- A **plain utility class** with a single static method — no instances needed.
- All logic is contained in one method: `extract(byte[] payload)`.
- Operates entirely on **raw bytes** — no pcap4j types, no model objects.
- Called exclusively by `PacketParser` when a TCP packet with `dstPort == 443` is encountered.

---

## 🧱 TLS ClientHello Packet Structure

Before reading the code, it helps to understand the binary structure being parsed. Every TLS `ClientHello` follows this layout:

```
Byte(s)    Field                         Value / Notes
─────────────────────────────────────────────────────────────────
[0]        Content Type                  0x16 = Handshake
[1–2]      TLS Version                   e.g. 0x03 0x01 = TLS 1.0
[3–4]      Record Length                 Total length of the record
[5]        Handshake Type                0x01 = ClientHello
[6–8]      Handshake Length              Length of the handshake body
[9–10]     Client Version                e.g. 0x03 0x03 = TLS 1.2
[11–42]    Random                        32 fixed bytes (timestamp + random)
[43]       Session ID Length             Variable — 0 if no session
[...]      Session ID                    (sessionIdLen bytes)
[...]      Cipher Suites Length          2 bytes
[...]      Cipher Suites                 (cipherSuitesLen bytes)
[...]      Compression Methods Length    1 byte
[...]      Compression Methods           (compressionLen bytes)
[...]      Extensions Length             2 bytes
[...]      Extensions                    (extensionsLen bytes) ← SNI is here
```

The SNI extension itself has this internal structure:

```
Extension Type      2 bytes   0x00 0x00 = SNI
Extension Length    2 bytes
SNI List Length     2 bytes
Entry Type          1 byte    0x00 = host_name
Name Length         2 bytes
Hostname            (nameLen bytes, UTF-8 plaintext)
```

---

## 🌐 Method — `extract(byte[] payload)`

```java
public static String extract(byte[] payload)
```

| Part | Meaning |
|---|---|
| `byte[] payload` | Raw bytes of the TCP segment's payload (the TLS record) |
| `String` | Returns the SNI hostname (e.g., `"github.com"`), or `null` if not found |

---

## 🔄 Method Flow — Step by Step

### Step 1 — 🛡️ Minimum Length Guard

```java
if (payload == null || payload.length < 5) return null;
```

- Rejects `null` payloads and any byte array shorter than 5 bytes — the minimum needed to read the TLS record header (Content Type + Version + Length).
- Prevents `ArrayIndexOutOfBoundsException` on the very first byte access.

---

### Step 2 — ✅ Verify TLS Handshake Content Type

```java
if ((payload[0] & 0xFF) != 0x16) return null;
```

- `payload[0]` — The first byte of a TLS record is the **Content Type**.
- `0x16` (decimal 22) is the TLS code for a **Handshake** record. Other values indicate application data (`0x17`), alerts (`0x15`), or change cipher spec (`0x14`) — none of which contain SNI.
- `& 0xFF` — Java `byte` is signed (-128 to 127), so this mask converts it to an unsigned integer (0–255) for correct comparison.
- Returns `null` immediately if this is not a handshake record — no SNI to extract.

---

### Step 3 — ✅ Verify ClientHello Handshake Type

```java
if ((payload[5] & 0xFF) != 0x01) return null;
```

- `payload[5]` — After the 5-byte TLS record header, byte 5 is the **Handshake Type**.
- `0x01` means `ClientHello`. Other handshake types (`0x02` = ServerHello, `0x0B` = Certificate, etc.) do not carry SNI.
- SNI only appears in the `ClientHello`, sent by the **client** at the very start of the TLS handshake — so only the first packet of a new HTTPS connection will contain it.
- Returns `null` for any other handshake type.

---

### Step 4 — ⏩ Skip to the Extensions Section

The code now navigates past several variable-length fields to reach the Extensions block where SNI lives. A running position cursor `pos` is used throughout.

#### 4a — Start Position

```java
int pos = 43;
```

- Jumps directly to byte 43 — skipping the fixed-size fields: TLS record header (5 bytes), Handshake header (4 bytes), Client Version (2 bytes), and Random (32 bytes). These total 43 bytes and are always the same size.

---

#### 4b — Skip Session ID

```java
int sessionIdLen = payload[pos] & 0xFF;
pos += 1 + sessionIdLen;
```

- `payload[pos]` — Reads the **Session ID Length** byte (0 if no prior session exists).
- `pos += 1 + sessionIdLen` — Advances past the length byte itself (`+1`) plus however many session ID bytes follow.

---

#### 4c — Skip Cipher Suites

```java
int cipherSuitesLen = ((payload[pos] & 0xFF) << 8) | (payload[pos + 1] & 0xFF);
pos += 2 + cipherSuitesLen;
```

- The **Cipher Suites Length** is a **2-byte big-endian integer** — two bytes are combined into one value:
    - `(payload[pos] & 0xFF) << 8` — Takes the high byte and shifts it 8 bits left.
    - `| (payload[pos + 1] & 0xFF)` — ORs in the low byte.
- `pos += 2 + cipherSuitesLen` — Advances past the 2-byte length field plus all the cipher suite bytes.

---

#### 4d — Skip Compression Methods

```java
int compressionLen = payload[pos] & 0xFF;
pos += 1 + compressionLen;
```

- Same single-byte length pattern as Session ID.
- Modern TLS effectively always uses no compression (value = `0x01`, one method: `null`), but the skip is still necessary for correctness.

---

#### 4e — Read Extensions Block

```java
int extensionsLen = ((payload[pos] & 0xFF) << 8) | (payload[pos + 1] & 0xFF);
pos += 2;

int extensionsEnd = pos + extensionsLen;
```

- Same 2-byte big-endian read to get the total **Extensions Length**.
- `pos += 2` — Advances past the length field; `pos` now points to the **first extension**.
- `extensionsEnd` — Pre-calculates the byte position where extensions end, used as the `while` loop boundary.

---

### Step 5 — 🔍 Walk Extensions to Find SNI (type `0x0000`)

```java
while (pos + 4 <= extensionsEnd) {
    int extType = ((payload[pos] & 0xFF) << 8) | (payload[pos + 1] & 0xFF);
    int extLen  = ((payload[pos + 2] & 0xFF) << 8) | (payload[pos + 3] & 0xFF);
    pos += 4;

    if (extType == 0x0000) {
        int nameLen = ((payload[pos + 3] & 0xFF) << 8) | (payload[pos + 4] & 0xFF);
        return new String(payload, pos + 5, nameLen);
    }

    pos += extLen;
}
```

- `pos + 4 <= extensionsEnd` — Loop guard: ensures at least 4 bytes remain to read an extension header (type + length). Prevents overrun.
- `extType` — 2-byte big-endian extension type identifier.
- `extLen` — 2-byte big-endian extension data length.
- `pos += 4` — Advances past the extension header (type + length fields).
- `extType == 0x0000` — **SNI extension type** is `0x0000`. When found:
    - `pos + 3` and `pos + 4` — Skip the SNI List Length (2 bytes) and Entry Type (1 byte) to reach the **Name Length** field.
    - `pos + 5` — The first byte of the actual **hostname string**.
    - `new String(payload, pos + 5, nameLen)` — Decodes the hostname bytes as a UTF-8 string (e.g., `"github.com"`).
    - Returns immediately — there is only ever one SNI entry per `ClientHello`.
- `pos += extLen` — If the current extension is **not** SNI, skip its entire data block and move to the next extension.

---

### Step 6 — 🛡️ Malformed Packet Handling

```java
} catch (ArrayIndexOutOfBoundsException e) {
    // Packet truncated or malformed — not a valid ClientHello
}

return null;
```

- The entire navigation logic is wrapped in a `try/catch`.
- If any byte access goes out of bounds (truncated capture, corrupted packet, non-TLS data misidentified as TLS), the exception is **silently swallowed**.
- `return null` — Falls through to indicate no SNI was found, which `PacketParser` handles gracefully.

---

## 🔁 Complete Flow Diagram

```
byte[] payload (raw TCP segment bytes)
      |
      ├── null or length < 5?          → return null
      ├── payload[0] != 0x16?          → return null (not TLS Handshake)
      ├── payload[5] != 0x01?          → return null (not ClientHello)
      |
      ▼
pos = 43  (start of variable-length fields)
      |
      ├── Skip Session ID     (1 byte len + N bytes data)
      ├── Skip Cipher Suites  (2 byte len + N bytes data)
      ├── Skip Compression    (1 byte len + N bytes data)
      ├── Read Extensions Len (2 bytes)
      |
      ▼
while extensions remain:
      |
      ├── Read extType (2 bytes) + extLen (2 bytes)
      |
      ├── extType == 0x0000? (SNI)
      |       └── ✅ Read nameLen → decode hostname string → return "github.com"
      |
      └── Not SNI? → skip extLen bytes → next extension
      |
      ▼ (loop ends, SNI not found)
ArrayIndexOutOfBoundsException? → silently caught
      |
      ▼
return null
```

---

## 🗂️ TLS Extension Type Reference

| Extension Type | Hex Code | Meaning |
|---|---|---|
| **SNI** | `0x0000` | Server Name Indication ← extracted here |
| Max Fragment Length | `0x0001` | |
| Supported Groups | `0x000A` | Elliptic curves |
| Signature Algorithms | `0x000D` | |
| ALPN | `0x0010` | Application-Layer Protocol Negotiation |
| Session Ticket | `0x0023` | |
| Supported Versions | `0x002B` | TLS 1.3 negotiation |

All non-SNI extensions are skipped over by the `pos += extLen` line.

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `SniExtractor.java` |
| **Package** | `com.packetanalyzer.parser` |
| **Role** | Extracts the plaintext SNI hostname from a TLS ClientHello payload |
| **Input** | Raw `byte[]` of a TCP segment's payload |
| **Output** | Hostname string (e.g., `"github.com"`), or `null` if not found |
| **Called By** | `PacketParser` (only for `dstPort == 443` packets with a payload) |
| **Method Type** | `static` — no instantiation needed |
| **Parsing Strategy** | Manual byte-level cursor navigation through TLS binary format |
| **Safety** | Guards against null, short payloads, and truncated/malformed bytes |
| **Returns null when** | Not a TLS record, not a ClientHello, SNI extension absent, or malformed |