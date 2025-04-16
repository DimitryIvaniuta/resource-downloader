package com.files.downloader.controller;

import com.files.downloader.dto.DownloadResponseDTO;
import com.files.downloader.dto.PageRequestDTO;
import com.files.downloader.repository.DownloadedFileRepository;
import com.files.downloader.service.FileDownloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private final FileDownloadService fileDownloadService;
    private final DownloadedFileRepository downloadedFileRepository;

    public DownloadController(FileDownloadService fileDownloadService,
                              DownloadedFileRepository downloadedFileRepository) {
        this.fileDownloadService = fileDownloadService;
        this.downloadedFileRepository = downloadedFileRepository;
    }

    /**
     * Triggers the download process. The server logs in to the portal with the configured credentials,
     * scrapes file links, downloads new files, and stores the records.
     */
    @PostMapping("/download-files")
    public ResponseEntity<String> downloadFiles(@RequestBody(required = false) PageRequestDTO pageRequest) {
        fileDownloadService.fetchAndDownloadFiles(pageRequest.getPageName());
        return ResponseEntity.ok("Download process initiated");
    }

    /**
     * Returns a list of downloaded file records.
     */
    @GetMapping("/downloaded-files")
    public ResponseEntity<List<DownloadResponseDTO>> getDownloadedFiles() {
        List<DownloadResponseDTO> files = downloadedFileRepository.findAll().stream()
                .map(file -> new DownloadResponseDTO(file.getFileUrl(), file.getLocalPath(), file.getDownloadedAt()))
                .toList();
        return ResponseEntity.ok(files);
    }
}
