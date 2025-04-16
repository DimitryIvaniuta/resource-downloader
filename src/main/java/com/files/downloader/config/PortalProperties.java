package com.files.downloader.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.portal")
@Slf4j
@Data
public class PortalProperties {

    private String loginUrl;

    private String resourceUrl;

    private String username;

    private String password;

}
