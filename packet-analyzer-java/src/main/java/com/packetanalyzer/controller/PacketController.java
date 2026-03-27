package com.packetanalyzer.controller;

import com.packetanalyzer.model.PacketInfo;
import com.packetanalyzer.service.PacketAnalyzerMTService;
import com.packetanalyzer.service.PacketAnalyzerService;
import com.packetanalyzer.service.PcapGeneratorService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

/**
 * PacketController
 *
 * This class exposes REST API endpoints.
 */
@RestController
@CrossOrigin // allows frontend calls
public class PacketController {

    private final PacketAnalyzerMTService service = new PacketAnalyzerMTService();

    /**
     * API to upload and analyze PCAP file
     *
     * @param file uploaded file
     * @return parsed packet info list
     */
    @PostMapping("/analyze")
    public List<PacketInfo> analyze(@RequestParam("file") MultipartFile file) {

        try {
            // Step 1: Save file temporarily
            File tempFile = File.createTempFile("upload", ".pcap");

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            // Step 2: Call service
            List<PacketInfo> result = service.analyze(tempFile.getAbsolutePath());

            // Step 3: Delete temp file
            tempFile.delete();

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of(); // return empty list on error
        }
    }

    @GetMapping("/generate")
    public ResponseEntity<Resource> generatePcap() throws Exception {

        PcapGeneratorService generator = new PcapGeneratorService();
        File file = generator.generateSamplePcap();

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample.pcap")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}