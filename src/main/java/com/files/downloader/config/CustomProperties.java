package com.files.downloader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="custom")
@Data
public class CustomProperties {

    private String ext;

    private String rate;

    private String url;

    private String id;

    private String download;

}
