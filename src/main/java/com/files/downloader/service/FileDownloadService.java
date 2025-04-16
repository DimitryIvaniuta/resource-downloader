package com.files.downloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.files.downloader.config.CustomProperties;
import com.files.downloader.config.DownloadProperties;
import com.files.downloader.config.PortalProperties;
import com.files.downloader.dto.UrlDownloadNode;
import com.files.downloader.dto.UrlNode;
import com.files.downloader.model.DownloadedFile;
import com.files.downloader.repository.DownloadedFileRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FileDownloadService {

    private final PortalProperties portalProperties;

    private final DownloadProperties downloadProperties;

    private final DownloadedFileRepository downloadedFileRepository;

    private final PortalAuthService portalAuthService;

    private final RestTemplate restTemplate;

    private final CustomProperties customProperties;

    private final HtmlContentParsingService htmlContentParsingService;

    public FileDownloadService(PortalProperties portalProperties,
                               DownloadProperties downloadProperties,
                               DownloadedFileRepository downloadedFileRepository,
                               PortalAuthService portalAuthService,
                               CustomProperties customProperties,
                               HtmlContentParsingService htmlContentParsingService
    ) {
        this.portalProperties = portalProperties;
        this.downloadProperties = downloadProperties;
        this.downloadedFileRepository = downloadedFileRepository;
        this.portalAuthService = portalAuthService;
        this.customProperties = customProperties;
        this.htmlContentParsingService = htmlContentParsingService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Logs in to the portal using the provided credentials,
     * retrieves the protected resource page, extracts downloadable file links,
     * downloads new files, and records each download.
     */
    @Transactional
    public void fetchAndDownloadFiles(String pageName) {
        // Log in to the portal and get the session cookie.
        String sessionCookie = portalAuthService.loginAndGetSessionCookie();
        if (sessionCookie == null) {
            log.error("Failed to log in to the portal.");
            return;
        }

        String resourceName = portalProperties.getResourceUrl();
        if (pageName != null && !pageName.isBlank()) {
            resourceName = resourceName + pageName;
        }
        // Use the session cookie in headers.
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, sessionCookie);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Retrieve the protected resource page.
            ResponseEntity<String> response = restTemplate.exchange(
                    resourceName,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            String htmlContent = response.getBody();
            if (htmlContent == null) {
                log.info("No content returned from the portal.");
                return;
            }

            // Parse the HTML to extract file links.
            List<UrlNode> urlNodes = processHtmlContent(htmlContent);
            if (!urlNodes.isEmpty()) {
                UrlNode urlNode = urlNodes.getFirst();
                ResponseEntity<String> responseDownload = restTemplate.exchange(
                        urlNode.getUrl(),
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                String htmlDownloadContent = responseDownload.getBody();
                UrlDownloadNode downloadNode = processHtmlDownloadContent(htmlDownloadContent);
                Optional.ofNullable(downloadNode.getId())
                        .filter(StringUtils::isNotBlank)
                        .map(id -> {
                            String downloadFileLink = String.format(customProperties.getDownload(), id);
                            Optional<DownloadedFile> existingFile = downloadedFileRepository.findByFileUrl(urlNode.getId());
                            if (existingFile.isEmpty()) {
                                log.info("File already downloaded: {}", urlNode.getId());
                            }
                            // Download the file using the same session cookie.
                            downloadFile(downloadFileLink, headers, urlNode);
                            return id;
                        });
                log.info("");
            }
        } catch (Exception ex) {
            log.error("Error fetching or parsing the portal page: {}", ex.getMessage());
        }
    }

    public List<UrlNode> processHtmlContent(String htmlContent) {
        // Extract JSON from the <div> with a data-content attribute.
        JsonNode dataContentJson = htmlContentParsingService.extractDataContentJson2(htmlContent);
        // Filter and display the url.
        return htmlContentParsingService.filterAndDisplayUrl(dataContentJson);
    }

    public UrlDownloadNode processHtmlDownloadContent(String htmlDownloadContent) {
        JsonNode dataDownloadContentJson = htmlContentParsingService.extractDataContentJson2(htmlDownloadContent);
        return htmlContentParsingService.filterUrlDownloadNode(dataDownloadContentJson);
    }

    private void downloadFile(String fileUrl, HttpHeaders headers, UrlNode urlNode) {
        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> fileResponse = restTemplate.exchange(
                    fileUrl,
                    HttpMethod.GET,
                    requestEntity,
                    byte[].class
            );

            if (fileResponse.getStatusCode().is2xxSuccessful() && fileResponse.getBody() != null) {
                File dir = new File(downloadProperties.getDir());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String fileName = getFileNameFromLink(urlNode);
                File localFile = new File(dir, fileName);
                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    fos.write(fileResponse.getBody());
                }
                DownloadedFile downloadedFile = new DownloadedFile(
                        fileUrl,
                        localFile.getAbsolutePath(),
                        LocalDateTime.now()
                );
                downloadedFileRepository.save(downloadedFile);
                log.info("Downloaded and saved file: " + localFile.getAbsolutePath());
            } else {
                log.info("Failed to download file: {} (HTTP {})",
                        fileUrl, fileResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error downloading file: " + fileUrl + " => " + e.getMessage());
        }
    }

    /**
     * Converts a hyphen-separated string into a title case string.
     * Example: "that-name" => "That Name"
     *
     * @param segment the original url segment
     * @return the string in title case
     */
    public static String toTitleCase(String segment) {
        if (segment == null || StringUtils.isBlank(segment)) {
            return "no-name";
        }
        String[] parts = segment.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    private String getFileNameFromLink(UrlNode urlNode) {
        try {
            // Parse the URL to extract the path.
            URI uri = new URI(urlNode.getUrl());
            String path = uri.getPath();

            // Split the path into segments.
            String[] segments = path.split("/");

            if (segments.length >= 4) {
                String firstSegment = segments[2];
                String secondSegment = segments[3];

                // Process the first segment into title case.
                String titleFirstSegment = toTitleCase(firstSegment);
                String processedSecond = secondSegment.replaceAll(customProperties.getExt()+"\\d+$", "");
                String titleSecondSegment = toTitleCase(processedSecond);
                return titleFirstSegment + " - " + titleSecondSegment + " - " + urlNode.getFormattedDate() + " - " + urlNode.getVote();
            } else {
                log.error("URL does not have the expected structure.");
            }
        } catch (Exception e) {
            log.error("Error creating file name", e);
        }
        return "no name";
    }

    private boolean isDownloadable(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".pdf") ||
                lowerUrl.endsWith(".zip") ||
                lowerUrl.endsWith(".docx") ||
                lowerUrl.endsWith(".xlsx") ||
                lowerUrl.endsWith(".txt");
    }

    private String extractFileNameFromUrl(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        return fileName.isEmpty() ? "file_" + System.currentTimeMillis() : fileName;
    }

}
