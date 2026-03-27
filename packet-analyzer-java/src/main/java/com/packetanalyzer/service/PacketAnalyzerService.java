package com.packetanalyzer.service;

import com.packetanalyzer.model.PacketInfo;
import com.packetanalyzer.parser.PacketParser;
import com.packetanalyzer.reader.PcapReader;
import org.pcap4j.packet.Packet;

import java.util.ArrayList;
import java.util.List;

/**
 * PacketAnalyzerService
 *
 * This class acts as a service layer.
 * It connects reader + parser and returns structured results.
 */
/**
 * Single-threaded implementation
 */
public class PacketAnalyzerService {

    private final PcapReader reader = new PcapReader();
    private final PacketParser parser = new PacketParser();

    /**
     * Analyze a PCAP file and return parsed packet info
     *
     * @param filePath path to pcap file
     * @return list of PacketInfo
     */
    public List<PacketInfo> analyze(String filePath) {

        // Step 1: Read packets
        List<Packet> packets = reader.readPackets(filePath);

        // Step 2: Parse packets
        List<PacketInfo> results = new ArrayList<>();

        for (Packet packet : packets) {
            PacketInfo info = parser.parse(packet);

            if (info != null) {
                results.add(info);
            }
        }

        return results;
    }
}