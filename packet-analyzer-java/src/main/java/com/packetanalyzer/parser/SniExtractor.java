package com.packetanalyzer.parser;

/**
 * SniExtractor
 *
 * Extracts the Server Name Indication (SNI) from a TLS ClientHello payload.
 *
 * TLS ClientHello structure (relevant bytes):
 *   [0]      = Content Type (0x16 = Handshake)
 *   [1-2]    = TLS Version
 *   [3-4]    = Record Length
 *   [5]      = Handshake Type (0x01 = ClientHello)
 *   [6-8]    = Handshake Length
 *   [9-10]   = Client Version
 *   [11-42]  = Random (32 bytes)
 *   [43]     = Session ID Length (variable)
 *   ...      = Cipher Suites, Compression Methods
 *   ...      = Extensions (where SNI lives)
 */
public class SniExtractor {

    /**
     * Attempts to extract the SNI hostname from raw TCP payload bytes.
     *
     * @param payload Raw bytes of the TCP segment payload
     * @return The SNI hostname string, or null if not found
     */
    public static String extract(byte[] payload) {
        if (payload == null || payload.length < 5) return null;

        // Must be TLS Handshake (0x16)
        if ((payload[0] & 0xFF) != 0x16) return null;

        // Must be ClientHello (0x01) at byte 5
        if ((payload[5] & 0xFF) != 0x01) return null;

        try {
            int pos = 43; // Start of Session ID Length

            // Skip Session ID
            int sessionIdLen = payload[pos] & 0xFF;
            pos += 1 + sessionIdLen;

            // Skip Cipher Suites
            int cipherSuitesLen = ((payload[pos] & 0xFF) << 8) | (payload[pos + 1] & 0xFF);
            pos += 2 + cipherSuitesLen;

            // Skip Compression Methods
            int compressionLen = payload[pos] & 0xFF;
            pos += 1 + compressionLen;

            // Extensions Length
            int extensionsLen = ((payload[pos] & 0xFF) << 8) | (payload[pos + 1] & 0xFF);
            pos += 2;

            int extensionsEnd = pos + extensionsLen;

            // Walk through extensions looking for SNI (type 0x0000)
            while (pos + 4 <= extensionsEnd) {
                int extType = ((payload[pos] & 0xFF) << 8) | (payload[pos + 1] & 0xFF);
                int extLen  = ((payload[pos + 2] & 0xFF) << 8) | (payload[pos + 3] & 0xFF);
                pos += 4;

                if (extType == 0x0000) { // SNI extension
                    // SNI list length (2 bytes), then entry type (1 byte), then name length (2 bytes)
                    int nameLen = ((payload[pos + 3] & 0xFF) << 8) | (payload[pos + 4] & 0xFF);
                    return new String(payload, pos + 5, nameLen);
                }

                pos += extLen;
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            // Packet truncated or malformed — not a valid ClientHello
        }

        return null;
    }
}