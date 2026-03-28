# 🚀 Main — Code Documentation

## 🗂️ Overview

`Main` is the **application entry point** of the Packet Analyzer. It is the class that launches the entire Spring Boot application — starting the embedded web server, bootstrapping the Spring context, and making all REST endpoints available for incoming requests.

> 🎯 **Role in the Architecture:** This is the first class executed when the application starts. Everything else — `PacketController`, services, parsers, and readers — is initialized and wired together by Spring after this class kicks off the startup sequence.

---

## 🏗️ Class-Level Details

```java
@SpringBootApplication
public class Main {
```

### `@SpringBootApplication`

This single annotation is actually a **shorthand for three annotations combined**:

| Annotation | What it does |
|---|---|
| `@SpringBootConfiguration` | Marks this class as a source of Spring bean definitions |
| `@EnableAutoConfiguration` | Tells Spring Boot to automatically configure the application based on dependencies found on the classpath (e.g., sets up an embedded Tomcat server because `spring-boot-starter-web` is present) |
| `@ComponentScan` | Scans the package `com.packetanalyzer` and all sub-packages for Spring-managed components (`@RestController`, `@Service`, `@Component`, etc.) |

---

## 🌐 Method — `main(String[] args)`

```java
public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
}
```

| Part | Meaning |
|---|---|
| `public static void main` | Standard Java application entry point — the JVM calls this method first |
| `String[] args` | Command-line arguments passed to the application at startup (e.g., `--server.port=8081`) |
| `SpringApplication.run(Main.class, args)` | Bootstraps and launches the entire Spring Boot application |

### What `SpringApplication.run()` does internally:

1. **Creates the Spring Application Context** — the container that manages all beans and their dependencies.
2. **Triggers auto-configuration** — detects classpath dependencies and configures components automatically (e.g., embedded Tomcat, Jackson JSON serializer).
3. **Runs component scanning** — finds and registers `PacketController` and any other annotated classes under `com.packetanalyzer`.
4. **Starts the embedded web server** — launches Tomcat (default port `8080`) and registers the `/analyze` endpoint.
5. **Prints the startup banner** — logs Spring Boot version info and startup time to the console.

---

## 🔁 Application Startup Flow

```
JVM starts
      |
      ▼
Main.main(args)
      |
      ▼
SpringApplication.run(Main.class, args)
      |
      ├── Create Spring Application Context
      ├── Run auto-configuration
      |       ├── Detect spring-boot-starter-web → start embedded Tomcat
      |       └── Detect Jackson → enable JSON serialization
      |
      ├── Component scan: com.packetanalyzer.*
      |       └── Register PacketController (@RestController)
      |
      ├── Start embedded Tomcat on port 8080
      |       └── Map POST /analyze → PacketController.analyze()
      |
      ▼
Application ready — accepting HTTP requests

Client  →  POST /analyze  →  PacketController
                                    |
                              PacketAnalyzerMTService
                                    |
                          PcapReader + PacketParser
                                    |
                            List<PacketInfo> (JSON)
                                    |
                              Client ←
```

---

## 🗂️ Main's Place in the Full Class Hierarchy

```
com.packetanalyzer
│
├── Main.java                         ← Entry point (this file)
│
├── controller/
│   └── PacketController.java         ← REST API endpoint (/analyze)
│
├── service/
│   ├── PacketAnalyzerService.java    ← Single-threaded analyzer
│   ├── PacketAnalyzerMTService.java  ← Multi-threaded analyzer (used by controller)
│   └── PcapGeneratorService.java     ← Synthetic PCAP file generator
│
├── reader/
│   └── PcapReader.java               ← Reads packets from .pcap file
│
├── parser/
│   ├── PacketParser.java             ← Converts raw packets to PacketInfo
│   └── SniExtractor.java             ← Extracts TLS SNI hostname
│
└── model/
    └── PacketInfo.java               ← DTO for structured packet data
```

---

## 📋 Summary

| What | Detail |
|---|---|
| **File** | `Main.java` |
| **Package** | `com.packetanalyzer` |
| **Role** | Application entry point — bootstraps and launches Spring Boot |
| **Annotation** | `@SpringBootApplication` (combines configuration + auto-config + component scan) |
| **Entry Method** | `public static void main(String[] args)` |
| **Launch Call** | `SpringApplication.run(Main.class, args)` |
| **Web Server** | Embedded Tomcat, started automatically on port `8080` |
| **Scan Scope** | All classes under `com.packetanalyzer.*` |