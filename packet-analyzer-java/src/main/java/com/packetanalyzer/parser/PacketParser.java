package com.packetanalyzer.parser;

import com.packetanalyzer.model.PacketInfo;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.EthernetPacket;

/**
 * PacketParser
 *
 * This class is responsible for converting raw Packet objects
 * into structured PacketInfo objects.
 *
 * It extracts:
 * - IP addresses
 * - Protocol (TCP/UDP)
 * - Ports
 * - Application protocol (basic detection)
 */
public class PacketParser {

    /**
     * Parses a raw packet and converts it into PacketInfo.
     *
     * @param packet Raw packet from PCAP
     * @return PacketInfo object (or null if not IP packet)
     */
    public PacketInfo parse(Packet packet) {
        // packet is now a decoded EthernetPacket
        // .get() traverses the layer chain: Ethernet -> IP -> TCP/UDP
        IpPacket ipPacket = packet.get(IpPacket.class);

        if (ipPacket == null) {
            return null; // ARP or other non-IP
        }

        PacketInfo info = new PacketInfo();
        info.setSrcIp(ipPacket.getHeader().getSrcAddr().getHostAddress());
        info.setDstIp(ipPacket.getHeader().getDstAddr().getHostAddress());

        TcpPacket tcpPacket = ipPacket.get(TcpPacket.class);
        UdpPacket udpPacket = ipPacket.get(UdpPacket.class);

        if (tcpPacket != null) {
            info.setProtocol("TCP");
            int srcPort = tcpPacket.getHeader().getSrcPort().valueAsInt();
            int dstPort = tcpPacket.getHeader().getDstPort().valueAsInt();
            info.setSrcPort(srcPort);
            info.setDstPort(dstPort);

            if      (dstPort == 80  || srcPort == 80)  info.setApplicationProtocol("HTTP");
            else if (dstPort == 443 || srcPort == 443) info.setApplicationProtocol("HTTPS");

            // Extract SNI domain from TLS ClientHello (only present on the FIRST packet of a handshake)
            if (dstPort == 443 && tcpPacket.getPayload() != null) {
                byte[] payload = tcpPacket.getPayload().getRawData();
                String sni = SniExtractor.extract(payload);
                if (sni != null) {
                    info.setDomain(sni);
                }
            }
        } else if (udpPacket != null) {
            info.setProtocol("UDP");
            int srcPort = udpPacket.getHeader().getSrcPort().valueAsInt();
            int dstPort = udpPacket.getHeader().getDstPort().valueAsInt();
            info.setSrcPort(srcPort);
            info.setDstPort(dstPort);

            if (dstPort == 53 || srcPort == 53) info.setApplicationProtocol("DNS");
        }

        return info;
    }
}