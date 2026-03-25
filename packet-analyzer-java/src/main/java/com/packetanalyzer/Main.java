package com.packetanalyzer;

import com.packetanalyzer.reader.PcapReader;
import org.pcap4j.packet.Packet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.packetanalyzer.parser.PacketParser;
import com.packetanalyzer.model.PacketInfo;

/**
 * Main entry point of the Packet Analyzer application.
 */
public class Main {

    public static void main(String[] args) {

        Map<String, Integer> domainCount = new HashMap<>();

        // Path to your .pcap file (update this!)
        String filePath = "packet-analyzer-java/test_dpi.pcap";

        // Create reader object
        PcapReader reader = new PcapReader();

        // Read packets
        List<Packet> packets = reader.readPackets(filePath);

        // Print number of packets
        System.out.println("Total packets read: " + packets.size());

        PacketParser parser = new PacketParser();

        for (Packet packet : packets) {

            PacketInfo info = parser.parse(packet);

            // Skip non-IP packets
            if (info != null) {
                System.out.println(info);
            }
            if (info.getDomain() != null) {
                domainCount.put(info.getDomain(),
                        domainCount.getOrDefault(info.getDomain(), 0) + 1);
            }
        }

        System.out.println("\nTop Domains:");
        domainCount.forEach((k, v) -> System.out.println(k + " → " + v));
    }
}