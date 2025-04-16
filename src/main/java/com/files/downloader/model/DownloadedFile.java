package com.files.downloader.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "downloaded_files")
@Getter
@Setter
@NoArgsConstructor
public class DownloadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RD_UNIQUE_ID")
    @SequenceGenerator(name = "RD_UNIQUE_ID", sequenceName = "RD_UNIQUE_ID", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String fileUrl;

    @Column(nullable = false)
    private String localPath;

    @Column(nullable = false)
    private LocalDateTime downloadedAt;

    public DownloadedFile(String fileUrl, String localPath, LocalDateTime downloadedAt) {
        this.fileUrl = fileUrl;
        this.localPath = localPath;
        this.downloadedAt = downloadedAt;
    }

}
