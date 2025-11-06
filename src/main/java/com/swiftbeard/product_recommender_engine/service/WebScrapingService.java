package com.swiftbeard.product_recommender_engine.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Service for scraping and extracting content from web pages
 */
@Service
@Slf4j
public class WebScrapingService {

    private static final int TIMEOUT_MS = 10000; // 10 seconds
    private static final int MAX_CONTENT_LENGTH = 50000; // 50KB max content

    /**
     * Scrape a shopping website and extract relevant product information
     */
    public String scrapeWebsite(String urlString) throws IOException {
        log.info("Scraping website: {}", urlString);

        // Validate URL
        validateUrl(urlString);

        // Fetch and parse the HTML
        Document doc = Jsoup.connect(urlString)
                .timeout(TIMEOUT_MS)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get();

        // Extract relevant content
        String content = extractContent(doc);

        log.info("Successfully scraped website. Content length: {} characters", content.length());

        return content;
    }

    /**
     * Validate the URL
     */
    private void validateUrl(String urlString) throws MalformedURLException {
        try {
            URL url = new URL(urlString);
            if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                throw new IllegalArgumentException("Only HTTP and HTTPS protocols are supported");
            }
        } catch (MalformedURLException e) {
            log.error("Invalid URL: {}", urlString);
            throw e;
        }
    }

    /**
     * Extract relevant content from the HTML document
     */
    private String extractContent(Document doc) {
        StringBuilder contentBuilder = new StringBuilder();

        // Remove unwanted elements (scripts, styles, etc.)
        doc.select("script, style, nav, header, footer, iframe, noscript").remove();

        // Extract page title
        String title = doc.title();
        if (!title.isEmpty()) {
            contentBuilder.append("Page Title: ").append(title).append("\n\n");
        }

        // Extract meta description
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            String description = metaDesc.attr("content");
            if (!description.isEmpty()) {
                contentBuilder.append("Description: ").append(description).append("\n\n");
            }
        }

        // Extract product information from common e-commerce patterns
        extractProductInfo(doc, contentBuilder);

        // Extract main content
        extractMainContent(doc, contentBuilder);

        String content = contentBuilder.toString();

        // Truncate if too long
        if (content.length() > MAX_CONTENT_LENGTH) {
            log.warn("Content truncated from {} to {} characters", content.length(), MAX_CONTENT_LENGTH);
            content = content.substring(0, MAX_CONTENT_LENGTH) + "\n... (content truncated)";
        }

        return content;
    }

    /**
     * Extract product information using common e-commerce HTML patterns
     */
    private void extractProductInfo(Document doc, StringBuilder contentBuilder) {
        // Look for common product container classes/ids
        String[] productSelectors = {
                ".product", ".item", "[class*=product]", "[class*=item]",
                "[id*=product]", "[itemtype*=Product]"
        };

        for (String selector : productSelectors) {
            Elements products = doc.select(selector);

            if (!products.isEmpty()) {
                contentBuilder.append("=== Products Found ===\n\n");

                int count = 0;
                for (Element product : products) {
                    if (count >= 50) break; // Limit to 50 products

                    String productText = extractProductDetails(product);
                    if (!productText.isEmpty() && productText.length() > 20) { // Filter out tiny fragments
                        contentBuilder.append("Product ").append(count + 1).append(":\n");
                        contentBuilder.append(productText).append("\n\n");
                        count++;
                    }
                }

                if (count > 0) {
                    break; // Found products, no need to try other selectors
                }
            }
        }
    }

    /**
     * Extract details from a product element
     */
    private String extractProductDetails(Element product) {
        StringBuilder details = new StringBuilder();

        // Extract product name/title
        Elements titles = product.select("h1, h2, h3, h4, [class*=title], [class*=name], [class*=product-name]");
        if (!titles.isEmpty()) {
            details.append("Name: ").append(titles.first().text()).append("\n");
        }

        // Extract price
        Elements prices = product.select("[class*=price], .price, [itemprop=price]");
        for (Element price : prices) {
            String priceText = price.text();
            if (priceText.matches(".*\\$?\\d+(\\.\\d{2})?.*")) {
                details.append("Price: ").append(priceText).append("\n");
                break;
            }
        }

        // Extract description
        Elements descriptions = product.select("[class*=description], .description, p");
        if (!descriptions.isEmpty()) {
            String desc = descriptions.first().text();
            if (desc.length() > 20) {
                details.append("Description: ").append(desc).append("\n");
            }
        }

        // Extract sizes if available
        Elements sizes = product.select("[class*=size], [id*=size]");
        if (!sizes.isEmpty()) {
            details.append("Sizes: ").append(sizes.text()).append("\n");
        }

        // Extract any other relevant text
        String otherText = product.text();
        if (details.length() == 0 && otherText.length() > 20) {
            // If we didn't find structured data, include the raw text
            details.append(otherText.substring(0, Math.min(500, otherText.length())));
        }

        return details.toString();
    }

    /**
     * Extract main content from the page
     */
    private void extractMainContent(Document doc, StringBuilder contentBuilder) {
        // Try to find main content area
        Element mainContent = doc.selectFirst("main, article, [role=main], #content, .content");

        if (mainContent != null) {
            contentBuilder.append("=== Main Content ===\n\n");
            contentBuilder.append(mainContent.text()).append("\n");
        } else {
            // Fall back to body text
            String bodyText = doc.body().text();
            if (!bodyText.isEmpty() && contentBuilder.length() < 1000) {
                // Only add body text if we haven't extracted much content yet
                contentBuilder.append("=== Page Content ===\n\n");
                contentBuilder.append(bodyText.substring(0, Math.min(5000, bodyText.length()))).append("\n");
            }
        }
    }

    /**
     * Get a preview of the scraped content (first 500 characters)
     */
    public String getContentPreview(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.substring(0, Math.min(500, content.length())) +
               (content.length() > 500 ? "..." : "");
    }
}
