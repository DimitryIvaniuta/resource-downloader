package com.files.downloader.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DownloadResponseDTO {

    private String fileUrl;

    private String localPath;

    private LocalDateTime downloadedAt;

    public DownloadResponseDTO(String fileUrl, String localPath, LocalDateTime downloadedAt) {
        this.fileUrl = fileUrl;
        this.localPath = localPath;
        this.downloadedAt = downloadedAt;
    }

}
