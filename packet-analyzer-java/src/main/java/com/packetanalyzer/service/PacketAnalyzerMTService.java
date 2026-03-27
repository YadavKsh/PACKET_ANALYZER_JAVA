package com.packetanalyzer.service;

import com.packetanalyzer.model.PacketInfo;
import com.packetanalyzer.parser.PacketParser;
import com.packetanalyzer.reader.PcapReader;
import org.pcap4j.packet.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Multi-threaded Packet Analyzer Service
 */
public class PacketAnalyzerMTService {

    private final PcapReader reader = new PcapReader();
    private final PacketParser parser = new PacketParser();

    public List<PacketInfo> analyze(String filePath) {

        List<Packet> packets = reader.readPackets(filePath);

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        System.out.println("Number of threads: " + threads);

        List<Future<PacketInfo>> futures = new ArrayList<>();

        for (Packet packet : packets) {
            futures.add(executor.submit(() -> parser.parse(packet)));
        }

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

        executor.shutdown();

        return results;
    }
}