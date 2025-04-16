package com.files.downloader.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.files.downloader.model.DownloadedFile;

import java.util.Optional;

public interface DownloadedFileRepository extends JpaRepository<DownloadedFile, Long> {
    Optional<DownloadedFile> findByFileUrl(String fileUrl);
}
