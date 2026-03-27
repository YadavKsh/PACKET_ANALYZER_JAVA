package com.packetanalyzer.service;

import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.MacAddress;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Random;

/**
 * PcapGeneratorService
 *
 * Generates a realistic sample PCAP file with mixed traffic:
 * - HTTP (TCP 80)
 * - HTTPS (TCP 443)
 * - DNS (UDP 53)
 */
public class PcapGeneratorService {

    public File generateSamplePcap() throws PcapNativeException, NotOpenException, IOException {

        Random random = new Random();

        // Source IP pool
        String[] ips = {
                "192.168.1.2",
                "192.168.1.3",
                "10.0.0.5",
                "172.16.0.8"
        };

        // Destination IP pool
        String[] dstIps = {
                "93.184.216.34",   // example.com
                "142.250.183.78",  // google
                "151.101.1.69",    // github
                "104.244.42.1"     // twitter
        };

        // Create temp file
        File file = File.createTempFile("sample", ".pcap");

        // Open dumper
        PcapHandle handle = Pcaps.openDead(DataLinkType.EN10MB, 65536);
        PcapDumper dumper = handle.dumpOpen(file.getAbsolutePath());

        // Generate multiple packets
        for (int i = 0; i < 25; i++) {

            // Random IP selection
            Inet4Address srcIp = (Inet4Address) InetAddress.getByName(
                    ips[random.nextInt(ips.length)]
            );

            Inet4Address dstIp = (Inet4Address) InetAddress.getByName(
                    dstIps[random.nextInt(dstIps.length)]
            );

            int protocolType = random.nextInt(3); // 0=HTTP, 1=HTTPS, 2=DNS

            Packet packet;

            // ================= TCP (HTTP / HTTPS) =================
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
                        .srcAddr(srcIp)     // REQUIRED for checksum
                        .dstAddr(dstIp)     // REQUIRED for checksum
                        .correctChecksumAtBuild(true)
                        .correctLengthAtBuild(true);

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

                packet = new EthernetPacket.Builder()
                        .srcAddr(MacAddress.getByName("00:11:22:33:44:55"))
                        .dstAddr(MacAddress.getByName("66:77:88:99:AA:BB"))
                        .type(EtherType.IPV4)
                        .payloadBuilder(new SimpleBuilder(ipPacket))
                        .paddingAtBuild(true)
                        .build();
            }

            // ================= UDP (DNS) =================
            else {

                UdpPacket.Builder udpBuilder = new UdpPacket.Builder()
                        .srcPort(UdpPort.getInstance((short) (10000 + random.nextInt(50000))))
                        .dstPort(UdpPort.DOMAIN)
                        .srcAddr(srcIp)
                        .dstAddr(dstIp)
                        .correctChecksumAtBuild(true)
                        .correctLengthAtBuild(true);

                IpV4Packet ipPacket = new IpV4Packet.Builder()
                        .version(IpVersion.IPV4)
                        .tos(IpV4Rfc791Tos.newInstance((byte) 0))
                        .identification((short) random.nextInt(1000))
                        .ttl((byte) 64)
                        .protocol(IpNumber.UDP)
                        .srcAddr(srcIp)
                        .dstAddr(dstIp)
                        .payloadBuilder(udpBuilder)
                        .correctChecksumAtBuild(true)
                        .correctLengthAtBuild(true)
                        .build();

                packet = new EthernetPacket.Builder()
                        .srcAddr(MacAddress.getByName("00:11:22:33:44:55"))
                        .dstAddr(MacAddress.getByName("66:77:88:99:AA:BB"))
                        .type(EtherType.IPV4)
                        .payloadBuilder(new SimpleBuilder(ipPacket))
                        .paddingAtBuild(true)
                        .build();
            }

            // Dump packet
            dumper.dump(packet);
        }

        dumper.close();
        handle.close();

        return file;
    }
}