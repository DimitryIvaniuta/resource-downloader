package com.files.downloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.files.downloader.config.CustomProperties;
import com.files.downloader.dto.UrlDownloadNode;
import com.files.downloader.dto.UrlNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HtmlContentParsingService {

    private final CustomProperties customProperties;

    // Reuse a single ObjectMapper instance for performance.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HtmlContentParsingService(CustomProperties customProperties) {
        this.customProperties = customProperties;
    }

    /**
     * Parses the given HTML content, finds the first <div> that has a "data-content" attribute,
     * and converts the attribute's value (assumed to be a JSON string) into a JsonNode.
     *
     * @param htmlContent the HTML content to parse
     * @return the parsed JSON as a JsonNode or null if not found or parsing failed.
     */
    public JsonNode extractDataContentJson(String htmlContent) {
        // Parse the HTML using Jsoup
        Document doc = Jsoup.parse(htmlContent);

        // Select the first <div> element that contains the "data-content" attribute.
        Element divElement = doc.selectFirst("div[data-content]");
        if (divElement != null) {
            // Get the value of the data-content attribute.
            String jsonString = divElement.attr("data-content");
            try {
                // Convert the JSON string into a JsonNode.
                return objectMapper.readTree(jsonString);
            } catch (Exception e) {
                log.error("Failed to parse JSON", e);
            }
        } else {
            log.info("No <div> element with a data-content attribute found.");
        }
        return null;
    }

    /**
     * Extracts the JSON string stored in the data-content attribute of the first
     * <div> element that has such an attribute from the given HTML content.
     *
     * @param htmlContent the HTML string to parse.
     * @return the parsed JsonNode, or null if none found or parsing fails.
     */
    public JsonNode extractDataContentJson2(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        Element divElement = doc.selectFirst("div[data-content]");
        if (divElement != null) {
            String jsonString = divElement.attr("data-content");
            try {
                return objectMapper.readTree(jsonString);
            } catch (IOException e) {
                log.error("Error parsing JSON from data-content: ", e);
            }
        } else {
            log.info("No <div> element with data-content attribute found.");
        }
        return null;
    }

    /**
     * Processes the JsonNode (which could be an array or an object).
     *
     * @param root the JsonNode obtained from the HTML's data-content attribute.
     */
    public List<UrlNode> filterAndDisplayUrl(JsonNode root) {
        List<UrlNode> urlNodes = new ArrayList<>();
        if (root == null) {
            log.info("No JSON to process.");
            return urlNodes;
        }

        // Helper method to process a single JSON object node.
        addUrlNode(processNode(root), urlNodes);

        // If the root node is an array, process each element.
        if (root.isArray()) {
            for (JsonNode node : root) {
                addUrlNode(processNode(node), urlNodes);
            }
        }
        urlNodes.addAll(traverseUrlNodes(root, ""));
        return urlNodes;
    }

    public UrlDownloadNode filterUrlDownloadNode(JsonNode root) {
        List<UrlDownloadNode> urlDownloadNodes = new ArrayList<>();
        if (root == null) {
            log.info("No JSON to process.");
            return null;
        }

        // Helper method to process a single JSON object node.
        addUrlDownloadNode(processDownloadNode(root), urlDownloadNodes);

        // If the root node is an array, process each element.
        if (root.isArray()) {
            for (JsonNode node : root) {
                addUrlDownloadNode(processDownloadNode(node), urlDownloadNodes);
            }
        }
        urlDownloadNodes.addAll(traverseUrlDownloadNodes(root, ""));
        return urlDownloadNodes.isEmpty() ? null : urlDownloadNodes.getFirst();
    }

    /**
     * Recursively traverses a JSON tree.
     *
     * @param node The current JsonNode.
     * @param path A string indicating the current path in the tree for debugging purposes.
     */
    public List<UrlNode> traverseUrlNodes(JsonNode node, String path) {
        List<UrlNode> urlNodes = new ArrayList<>();
        if (node.isObject()) {
            UrlNode urlNode = new UrlNode();
            if (node.has(customProperties.getUrl())) {
                urlNode.setUrl(node.get(customProperties.getUrl()).asText());
            }
            if (node.has(customProperties.getRate())) {
                JsonNode ratesNode = node.get(customProperties.getRate());
                // Verify that the value is numeric
                if (ratesNode.isNumber()) {
                    urlNode.setVote(ratesNode.asInt());
                }
            }

            if (node.has("date")) {
                JsonNode dateNode = node.get("date");
                String dateString = dateNode.asText();
                long epochSeconds = Long.parseLong(dateString);
                Instant instant = Instant.ofEpochSecond(epochSeconds);
                LocalDate localDate = LocalDate.ofInstant(instant, ZoneId.systemDefault());

                // 4. Format the LocalDateTime as a readable string.
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String formattedDate = localDate.format(formatter);
                urlNode.setFormattedDate(formattedDate);
            }
            if (node.has("id")) {
                JsonNode idNode = node.get("id");
                String idString = idNode.asText();
                urlNode.setId(idString);
            }
            addUrlNode(urlNode, urlNodes);
            // Iterate over all fields and recursively search them.
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                urlNodes.addAll(traverseUrlNodes(entry.getValue(), newPath));
            }
        } else if (node.isArray()) {
            int index = 0;
            // If the node is an array, process each element.
            for (JsonNode element : node) {
                String newPath = path + "[" + index + "]";
                urlNodes.addAll(traverseUrlNodes(element, newPath));
                index++;
            }
        }
        return urlNodes;
    }

    public List<UrlDownloadNode> traverseUrlDownloadNodes(JsonNode node, String path) {
        List<UrlDownloadNode> urlDownloadNodes = new ArrayList<>();
        if (node.isObject()) {
            UrlDownloadNode urlDownloadNode = new UrlDownloadNode();
            if (node.has(customProperties.getId())) {
                JsonNode binaryIdNode = node.get(customProperties.getId());
                urlDownloadNode.setId(binaryIdNode.asText());
            }
            addUrlDownloadNode(urlDownloadNode, urlDownloadNodes);
            // Iterate over all fields and recursively search them.
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                urlDownloadNodes.addAll(traverseUrlDownloadNodes(entry.getValue(), newPath));
            }
        } else if (node.isArray()) {
            int index = 0;
            // If the node is an array, process each element.
            for (JsonNode element : node) {
                String newPath = path + "[" + index + "]";
                urlDownloadNodes.addAll(traverseUrlDownloadNodes(element, newPath));
                index++;
            }
        }
        return urlDownloadNodes;
    }

    private void addUrlNode(UrlNode urlNode, List<UrlNode> urlNodes) {
        if (StringUtils.isNotBlank(urlNode.getUrl())
                && urlNode.getUrl().contains(customProperties.getExt())
                && StringUtils.isNotBlank(urlNode.getId())) {
            urlNodes.add(urlNode);
        }
    }

    private void addUrlDownloadNode(UrlDownloadNode urlDownloadNode, List<UrlDownloadNode> urlDownloadNodes) {
        if (StringUtils.isNotBlank(urlDownloadNode.getId())) {
            urlDownloadNodes.add(urlDownloadNode);
        }
    }

    private UrlNode processNode(JsonNode node) {
        UrlNode urlNode = new UrlNode();
        JsonNode linkUrlNode = node.findValue(customProperties.getUrl());
        if (linkUrlNode != null && linkUrlNode.isTextual()) {
            String linkUrl = linkUrlNode.asText();
            urlNode.setUrl(linkUrl);
            if (linkUrl.contains(customProperties.getExt())) {
                JsonNode ratesNode = node.get(customProperties.getRate());
                int rates = ratesNode != null && ratesNode.isInt() ? ratesNode.asInt() : -1;
                urlNode.setVote(rates);
            }
        }
        return urlNode;
    }

    private UrlDownloadNode processDownloadNode(JsonNode node) {
        UrlDownloadNode urlDownloadNode = new UrlDownloadNode();
        JsonNode linkUrlDownloadNode = node.findValue(customProperties.getId());
        if (linkUrlDownloadNode != null && linkUrlDownloadNode.isTextual()) {
            String tabDownloadKey = linkUrlDownloadNode.asText();
            urlDownloadNode.setId(tabDownloadKey);
        }
        return urlDownloadNode;
    }

}