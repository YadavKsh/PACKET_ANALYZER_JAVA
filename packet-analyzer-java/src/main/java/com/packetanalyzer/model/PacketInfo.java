package com.packetanalyzer.model;

/**
 * PacketInfo
 *
 * This class represents a structured form of a network packet
 * after parsing. Instead of dealing with raw packet data,
 * we store only useful extracted information here.
 *
 * This acts as a Data Transfer Object (DTO).
 */
public class PacketInfo {

    // Source IP address (e.g., 192.168.1.2)
    private String srcIp;

    // Destination IP address (e.g., 142.250.183.78)
    private String dstIp;

    // Protocol type (TCP / UDP)
    private String protocol;

    // Source port number
    private int srcPort;

    // Destination port number
    private int dstPort;

    // Application protocol (HTTP, HTTPS, DNS, etc.)
    private String applicationProtocol;

    // Extracted domain (from SNI, if available)
    private String domain;

    /**
     * Default constructor
     */
    public PacketInfo() {
    }

    /**
     * Parameterized constructor
     */
    public PacketInfo(String srcIp, String dstIp, String protocol,
                      int srcPort, int dstPort,
                      String applicationProtocol, String domain) {
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.protocol = protocol;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.applicationProtocol = applicationProtocol;
        this.domain = domain;
    }

    // Getters and Setters

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public String getDstIp() {
        return dstIp;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }

    public String getApplicationProtocol() {
        return applicationProtocol;
    }

    public void setApplicationProtocol(String applicationProtocol) {
        this.applicationProtocol = applicationProtocol;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * toString()
     *
     * Converts PacketInfo into readable format.
     * Useful for printing output.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("PacketInfo {\n");
        sb.append("  ").append(srcIp).append(" → ").append(dstIp).append("\n");
        sb.append("  ").append(protocol).append(" ").append(srcPort).append(" → ").append(dstPort).append("\n");

        if (applicationProtocol != null) {
            sb.append("  App: ").append(applicationProtocol).append("\n");
        }

        if (domain != null) {
            sb.append("  Domain: ").append(domain).append("\n");
        }

        sb.append("}");

        return sb.toString();
    }
}
