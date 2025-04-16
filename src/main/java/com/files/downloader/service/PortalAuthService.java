package com.files.downloader.service;

import com.files.downloader.config.CustomProperties;
import com.files.downloader.config.PortalProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class PortalAuthService {

    private final PortalProperties portalProperties;
    private final CustomProperties customProperties;
    private final RestTemplate restTemplate;

    public PortalAuthService(PortalProperties portalProperties, CustomProperties customProperties) {
        this.portalProperties = portalProperties;
        this.customProperties = customProperties;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Logs in to the portal by POSTing login credentials and returns the session cookie string.
     * Adjust the parameter names ("username", "password") as required by your portal.
     */
    public String loginAndGetSessionCookie() {
        String loginUrl = portalProperties.getLoginUrl();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", portalProperties.getUsername());
        formData.add("password", portalProperties.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, requestEntity, String.class);
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null && !cookies.isEmpty()) {
            // Join the cookies by semicolon for reuse in subsequent requests.
            return String.join("; ", cookies);
        }
        return null;
    }
}
