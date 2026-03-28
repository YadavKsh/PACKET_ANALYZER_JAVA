# 🚀 PacketAnalyzerMTService — Code Documentation

## 🗂️ Overview

`PacketAnalyzerMTService` is the **multi-threaded service layer** of the Packet Analyzer application. It performs the same job as `PacketAnalyzerService` — reading a PCAP file and returning structured packet data — but does so by dispatching packet parsing across **multiple CPU threads in parallel**, making it significantly faster for large capture files.

> ⚡ **This is the production implementation** used by `PacketController`.

---

## 🏗️ Class-Level Details

```java
public class PacketAnalyzerMTService {
```

- No Spring annotations — this is a **plain Java class**, instantiated manually by `PacketController`.
- Designed for **concurrent processing**; each packet is parsed in its own thread task.

---

## 🔧 Fields

```java
private final PcapReader reader = new PcapReader();
private final PacketParser parser = new PacketParser();
```

| Field | Type | Role |
|---|---|---|
| `reader` | `PcapReader` | Reads all raw packets from the `.pcap` file into memory |
| `parser` | `PacketParser` | Converts a single raw `Packet` into a structured `PacketInfo` object |

- Both are declared `final` — initialized once and never reassigned.
- Both are shared across all threads; `PacketParser.parse()` should be **thread-safe** for this to work correctly.

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

- Calls `PcapReader.readPackets()` to load all packets from the `.pcap` file into a `List<Packet>`.
- This step is **single-threaded** — the file is read sequentially before parallel processing begins.

---

### Step 2 — 🧮 Detect Available CPU Threads

```java
int threads = Runtime.getRuntime().availableProcessors();
ExecutorService executor = Executors.newFixedThreadPool(threads);
System.out.println("Number of threads: " + threads);
```

- `Runtime.getRuntime().availableProcessors()` — Queries the JVM for the number of **logical CPU cores** available on the host machine (e.g., returns `8` on an 8-core machine).
- `Executors.newFixedThreadPool(threads)` — Creates a thread pool with exactly that many worker threads. No more, no fewer — this avoids over-subscribing the CPU.
- The `println` logs the thread count to the console for observability/debugging.

---

### Step 3 — 📬 Submit Parsing Tasks to the Thread Pool

```java
List<Future<PacketInfo>> futures = new ArrayList<>();

for (Packet packet : packets) {
    futures.add(executor.submit(() -> parser.parse(packet)));
}
```

- `new ArrayList<>()` — A list to hold `Future` handles, one per packet.
- The `for` loop iterates over all packets and **submits a parsing task** for each one to the executor.
- `executor.submit(() -> parser.parse(packet))` — Wraps `parser.parse(packet)` in a lambda (a `Callable`) and hands it off to a thread pool worker. This call is **non-blocking** — it returns a `Future<PacketInfo>` immediately without waiting for parsing to finish.
- `Future<PacketInfo>` — A handle to a result that will be available at some point in the future once the worker thread completes.
- All packets are submitted in rapid succession; the thread pool processes them **concurrently** across available CPU cores.

---

### Step 4 — 🧲 Collect Results from Futures

```java
List<PacketInfo> results = new ArrayList<>();

for (Future<PacketInfo> future : futures) {
    try {
        PacketInfo info = future.get();
        if (info != null) {
            results.add(info);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

- Iterates over each `Future` in order (preserving original packet sequence).
- `future.get()` — **Blocks** until that specific task is complete, then retrieves the `PacketInfo` result.
- `if (info != null)` — Skips packets the parser could not handle (unknown or malformed protocols).
- `results.add(info)` — Adds valid results to the final list.
- `catch (Exception e)` — Handles two possible exceptions:
    - `InterruptedException` — The waiting thread was interrupted.
    - `ExecutionException` — The parsing task itself threw an exception.
    - In both cases, the bad packet is skipped and the stack trace is printed for debugging.

---

### Step 5 — 🛑 Shut Down the Thread Pool

```java
executor.shutdown();
```

- Signals the executor to **stop accepting new tasks** and gracefully wind down once all submitted tasks are complete.
- Critical for preventing **thread leaks** — without this, the JVM may not exit cleanly.

---

### ✅ Return Result

```java
return results;
```

- Returns the fully populated `List<PacketInfo>` to `PacketController`, which serializes it to JSON.

---

## 🔁 Complete Flow Diagram

```
filePath (String)
      |
      ▼
PcapReader.readPackets()          ← single-threaded file read
      |
      ▼
List<Packet>  (all raw packets)
      |
      ▼
Detect CPU cores → newFixedThreadPool(N)
      |
      ▼
 for each Packet
      └── executor.submit(() -> parser.parse(packet))
                    |
          ┌─────────────────┐
          │  Thread Pool    │
          │  [T1][T2]...[TN]│  ← N threads parsing in parallel
          └─────────────────┘
                    |
      ▼
List<Future<PacketInfo>>
      |
      ▼
 for each Future
      ├── future.get()  (blocks until done)
      |         |
      |         ├── null or exception? → ❌ skip
      |         └── valid PacketInfo?  → ✅ add to results
      |
      ▼
executor.shutdown()               ← clean up threads
      |
      ▼
List<PacketInfo>  (structured results)
      |
      ▼
   PacketController
```

---

## ⚖️ Multi-Threaded vs Single-Threaded Comparison

| Aspect | `PacketAnalyzerMTService` (this file) | `PacketAnalyzerService` |
|---|---|---|
| Processing | Parallel across N CPU cores | Sequential, one at a time |
| Performance | Fast on large files | Slower on large files |
| Thread pool | `newFixedThreadPool(availableProcessors)` | None |
| Result collection | Via `Future.get()` | Direct return from `parse()` |
| Error handling | Per-task `try/catch` on `future.get()` | Single outer `try/catch` |
| Used by | `PacketController` (production) | Standalone / testing |

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `PacketAnalyzerMTService.java` |
| **Package** | `com.packetanalyzer.service` |
| **Role** | Multi-threaded service layer |
| **Input** | File path to a `.pcap` file |
| **Output** | `List<PacketInfo>` (structured packet data) |
| **Dependencies** | `PcapReader`, `PacketParser`, `ExecutorService` |
| **Thread Count** | Matches number of available CPU logical cores |
| **Null Handling** | Skips packets that parse to `null` |
| **Error Handling** | Per-future `try/catch`; bad packets are skipped |
| **Cleanup** | `executor.shutdown()` called after all results collected |