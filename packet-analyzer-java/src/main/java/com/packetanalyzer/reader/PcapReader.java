package com.packetanalyzer.reader;

import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.EthernetPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * PcapReader
 *
 * This class is responsible for reading packets from a .pcap file.
 * It acts as the data source layer of the application.
 *
 * Instead of processing packets here, we only read and return them.
 */
public class PcapReader {

    /**
     * Reads all packets from a given .pcap file.
     *
     * @param filePath Path to the .pcap file
     * @return List of packets
     */
    public List<Packet> readPackets(String filePath) {

        // List to store all packets read from file
        List<Packet> packets = new ArrayList<>();

        try {
            /**
             * Open the .pcap file in offline mode.
             * This does NOT capture live traffic, only reads from file.
             */
            PcapHandle handle = Pcaps.openOffline(filePath);

            /**
             * Loop through all packets until no packets are left
             */

            Packet packet;

            while ((packet = handle.getNextPacket()) != null) {
                try {
                    // Must manually decode — getNextPacket() returns UnknownPacket
                    // because pcap4j-core alone doesn't auto-register Ethernet factories
                    Packet decoded = EthernetPacket.newPacket(
                            packet.getRawData(), 0, packet.length()
                    );
                    packets.add(decoded);
                } catch (Exception e) {
                    // Skip malformed packets, continue reading
                    System.err.println("Skipping malformed packet: " + e.getMessage());
                }
            }

            // Close handle to free system resources
            handle.close();

        } catch (Exception e) {
            /**
             * Handles:
             * - File not found
             * - Permission issues
             * - Parsing errors
             */
            e.printStackTrace();
        }

        // Return all packets read from file
        return packets;
    }
}