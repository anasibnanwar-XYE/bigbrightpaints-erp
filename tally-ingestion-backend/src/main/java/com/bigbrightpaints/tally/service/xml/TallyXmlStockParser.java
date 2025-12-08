package com.bigbrightpaints.tally.service.xml;

import com.bigbrightpaints.tally.domain.IngestionRun;
import com.bigbrightpaints.tally.domain.staging.StagingStockItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Tally Prime XML exports (Stock Summary format)
 * Handles the unique format with spaces between characters
 */
@Slf4j
@Service
public class TallyXmlStockParser {

    // Pattern to parse quantity with unit (e.g., "6212 PCS", "123.45 KG")
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("([\\d,.-]+)\\s*([A-Za-z]+)?");

    /**
     * Parse Tally XML stock summary and return staging items
     */
    public List<StagingStockItem> parseStockSummary(InputStream xmlStream, IngestionRun run) throws Exception {
        List<StagingStockItem> items = new ArrayList<>();

        // Read and preprocess the XML content
        byte[] rawBytes = xmlStream.readAllBytes();
        String xmlContent = preprocessTallyXml(rawBytes);

        log.info("Preprocessed XML length: {} characters", xmlContent.length());
        log.debug("First 500 chars: {}", xmlContent.substring(0, Math.min(500, xmlContent.length())));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        ByteArrayInputStream processedStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        Document doc = builder.parse(processedStream);
        doc.getDocumentElement().normalize();

        log.info("Parsing Tally XML Stock Summary...");
        log.info("Root element: {}", doc.getDocumentElement().getNodeName());

        // Find all DSPACCNAME elements
        NodeList accountNodes = doc.getElementsByTagName("DSPACCNAME");
        log.info("Found {} DSPACCNAME elements", accountNodes.getLength());

        int rowNumber = 0;
        for (int i = 0; i < accountNodes.getLength(); i++) {
            try {
                Element accountElement = (Element) accountNodes.item(i);
                StagingStockItem item = parseStockItem(accountElement, run, ++rowNumber);
                if (item != null) {
                    items.add(item);
                    log.debug("Parsed item {}: {}", rowNumber, item.getItemName());
                }
            } catch (Exception e) {
                log.error("Error parsing stock item at index {}: {}", i, e.getMessage());
            }
        }

        log.info("Successfully parsed {} stock items", items.size());
        return items;
    }

    /**
     * Preprocess Tally XML to remove spaces between characters
     * Tally exports XML with spaces between each character (UTF-16 artifact)
     */
    private String preprocessTallyXml(byte[] rawBytes) {
        String content;

        // Try UTF-16LE first (most common for Tally)
        content = new String(rawBytes, StandardCharsets.UTF_16LE);

        // Remove BOM and null characters
        content = content.replace("\uFEFF", "")  // UTF-16 BOM
                         .replace("\uFFFE", "")  // Reverse BOM
                         .replace("\u0000", ""); // Null chars

        log.info("Raw content first 100 chars: {}",
                content.substring(0, Math.min(100, content.length())).replaceAll("[\\x00-\\x1F]", "?"));

        // Check if content has spaced characters (e.g., "< E N V E L O P E >")
        if (content.contains("< E N V E L O P E >") || content.contains("< D S P") ||
            content.contains("< E N V") || content.contains("E N V E L O P E")) {
            log.info("Detected spaced XML format, removing spaces between characters...");

            // Simple approach: remove all single spaces that are between non-space characters
            StringBuilder result = new StringBuilder();
            char[] chars = content.toCharArray();

            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];

                if (c == ' ') {
                    // Check if this space is between two non-space printable characters
                    // If so, it's likely an artifact and should be removed
                    boolean prevIsContent = i > 0 && chars[i-1] != ' ' && chars[i-1] != '\n' && chars[i-1] != '\r';
                    boolean nextIsContent = i < chars.length - 1 && chars[i+1] != ' ' && chars[i+1] != '\n' && chars[i+1] != '\r';

                    if (prevIsContent && nextIsContent) {
                        // Skip this space - it's between content characters
                        continue;
                    }
                }

                // Skip carriage returns and newlines for cleaner XML
                if (c == '\r' || c == '\n') {
                    continue;
                }

                result.append(c);
            }

            content = result.toString();
        }

        // Clean up any remaining issues
        content = content.trim();

        // Remove any leading garbage before the XML
        int xmlStart = content.indexOf("<ENVELOPE>");
        if (xmlStart == -1) {
            xmlStart = content.indexOf("<envelope>");
        }
        if (xmlStart > 0) {
            content = content.substring(xmlStart);
        }

        // Ensure we have proper XML declaration
        if (!content.startsWith("<?xml")) {
            content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + content;
        }

        log.info("Processed XML first 200 chars: {}",
                content.substring(0, Math.min(200, content.length())));

        return content;
    }

    /**
     * Parse individual stock item from DSPACCNAME element
     */
    private StagingStockItem parseStockItem(Element accountElement, IngestionRun run, int rowNumber) {
        try {
            // Get product name from DSPDISPNAME
            String itemName = getTextContent(accountElement, "DSPDISPNAME");
            if (itemName == null || itemName.trim().isEmpty()) {
                log.warn("Skipping item at row {} - no item name", rowNumber);
                return null;
            }
            itemName = itemName.trim();

            // Get the next sibling DSPSTKINFO element
            Element stockInfoElement = getNextSiblingElement(accountElement.getParentNode(), accountElement, "DSPSTKINFO");

            BigDecimal quantity = null;
            String unit = null;
            BigDecimal rate = null;
            BigDecimal amount = null;

            if (stockInfoElement != null) {
                // Try DSPSTKC first, then DSPSTKCL
                Element stockCElement = getChildElement(stockInfoElement, "DSPSTKC");
                if (stockCElement == null) {
                    stockCElement = getChildElement(stockInfoElement, "DSPSTKCL");
                }

                if (stockCElement != null) {
                    // Parse quantity with unit - try both DSPCLQTY
                    String quantityStr = getTextContent(stockCElement, "DSPCLQTY");
                    if (quantityStr != null && !quantityStr.trim().isEmpty()) {
                        Matcher matcher = QUANTITY_PATTERN.matcher(quantityStr.trim());
                        if (matcher.find()) {
                            quantity = parseBigDecimal(matcher.group(1));
                            if (matcher.group(2) != null) {
                                unit = matcher.group(2).toUpperCase();
                            }
                        }
                    }

                    // Parse rate
                    rate = parseBigDecimal(getTextContent(stockCElement, "DSPCLRATE"));

                    // Parse amount - try both DSPCLAMT and DSPCLAMT A (DSPCLAMT A is actually DSPCLAM TA -> DSPCLAM + TA)
                    amount = parseBigDecimal(getTextContent(stockCElement, "DSPCLAMT"));
                    if (amount == null) {
                        amount = parseBigDecimal(getTextContent(stockCElement, "DSPCLAMTA"));
                    }
                }
            }

            // Auto-classify based on name patterns
            StagingStockItem.ItemType itemType = classifyItem(itemName);

            // Store raw data
            Map<String, Object> rawData = new HashMap<>();
            rawData.put("itemName", itemName);
            rawData.put("quantity", quantity);
            rawData.put("unit", unit);
            rawData.put("rate", rate);
            rawData.put("amount", amount);

            // Generate source hash
            String sourceHash = generateHash(itemName + quantity + rate + amount);

            // Parse name components (brand, size, color)
            Map<String, String> nameComponents = parseNameComponents(itemName);

            return StagingStockItem.builder()
                    .run(run)
                    .rowNumber(rowNumber)
                    .itemName(itemName)
                    .closingQuantity(quantity)
                    .unitOfMeasure(unit)
                    .closingRate(rate)
                    .closingAmount(amount != null ? amount.abs() : null)
                    .itemType(itemType)
                    .brand(nameComponents.get("brand"))
                    .sizeLabel(nameComponents.get("size"))
                    .color(nameComponents.get("color"))
                    .category(nameComponents.get("category"))
                    .baseProductName(nameComponents.get("baseName"))
                    .rawData(rawData)
                    .sourceHash(sourceHash)
                    .validationStatus(StagingStockItem.ValidationStatus.PENDING)
                    .processed(false)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing stock item at row {}: {}", rowNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Auto-classify item based on name patterns
     */
    private StagingStockItem.ItemType classifyItem(String itemName) {
        String nameLower = itemName.toLowerCase();

        // Packaging materials
        if (nameLower.contains("bucket") || nameLower.contains("tin") || nameLower.contains("cartoon") ||
            nameLower.contains("drum") || nameLower.contains("box") || nameLower.contains("bkt") ||
            nameLower.contains("container") || nameLower.contains("packing")) {
            return StagingStockItem.ItemType.PACKAGING;
        }

        // Expense/Office items
        if (nameLower.contains("t-shirt") || nameLower.contains("tshirt") || nameLower.contains("pen") ||
            nameLower.contains("stationery") || nameLower.contains("notebook") || nameLower.contains("diary") ||
            nameLower.contains("card") || nameLower.contains("book") || nameLower.contains("gift") ||
            nameLower.contains("flex") || nameLower.contains("letterpad") || nameLower.contains("mug") ||
            nameLower.contains("pad") || nameLower.contains("envelope") || nameLower.contains("cartridge") ||
            nameLower.contains("banner") || nameLower.contains("airtel")) {
            return StagingStockItem.ItemType.EXPENSE;
        }

        // Raw materials (chemicals, pigments, resins)
        if (nameLower.contains("pigment") || nameLower.contains("resin") || nameLower.contains("chemical") ||
            nameLower.contains("oxide") || nameLower.contains("powder") || nameLower.contains("filler") ||
            nameLower.contains("titanium") || nameLower.contains("calcium") || nameLower.contains("binder") ||
            nameLower.contains("additive") || nameLower.contains("solvent")) {
            return StagingStockItem.ItemType.RAW_MATERIAL;
        }

        // Finished products (paints with brand names and sizes)
        if (nameLower.contains("emulsion") || nameLower.contains("distemper") || nameLower.contains("primer") ||
            nameLower.contains("enamel") || nameLower.contains("paint") || nameLower.contains("putty") ||
            nameLower.contains("thinner") || nameLower.contains("ltr") || nameLower.contains("litre") ||
            nameLower.contains("inox") || nameLower.contains("safari") || nameLower.contains("sapphire")) {
            return StagingStockItem.ItemType.FINISHED_PRODUCT;
        }

        return StagingStockItem.ItemType.UNKNOWN;
    }

    /**
     * Parse name components (brand, size, color, category)
     */
    private Map<String, String> parseNameComponents(String itemName) {
        Map<String, String> components = new HashMap<>();
        String nameLower = itemName.toLowerCase();

        // Extract brand
        if (nameLower.contains("inox")) {
            components.put("brand", "INOX");
        } else if (nameLower.contains("safari")) {
            components.put("brand", "SAFARI");
        } else if (nameLower.contains("sapphire")) {
            components.put("brand", "SAPPHIRE");
        }

        // Extract size
        Pattern sizePattern = Pattern.compile("(\\d+(?:\\.\\d+)?\\s*)(ltr|kg|ml|gm|litre|gram|l)\\b", Pattern.CASE_INSENSITIVE);
        Matcher sizeMatcher = sizePattern.matcher(itemName);
        if (sizeMatcher.find()) {
            components.put("size", sizeMatcher.group().trim());
        }

        // Extract category
        if (nameLower.contains("emulsion")) {
            components.put("category", "Emulsion");
        } else if (nameLower.contains("distemper")) {
            components.put("category", "Distemper");
        } else if (nameLower.contains("primer")) {
            components.put("category", "Primer");
        } else if (nameLower.contains("enamel")) {
            components.put("category", "Enamel");
        } else if (nameLower.contains("putty")) {
            components.put("category", "Putty");
        } else if (nameLower.contains("thinner")) {
            components.put("category", "Thinner");
        }

        components.put("baseName", itemName);
        return components;
    }

    /**
     * Get text content from child element
     */
    private String getTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            String text = nodeList.item(0).getTextContent();
            return text != null ? text.trim() : null;
        }
        return null;
    }

    /**
     * Get child element by tag name
     */
    private Element getChildElement(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        }
        return null;
    }

    /**
     * Get next sibling element with specific tag name from parent's children
     */
    private Element getNextSiblingElement(Node parent, Element current, String tagName) {
        if (parent == null) return null;

        NodeList children = parent.getChildNodes();
        boolean foundCurrent = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child == current) {
                foundCurrent = true;
                continue;
            }
            if (foundCurrent && child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                if (element.getTagName().equals(tagName)) {
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * Parse BigDecimal from string, handling commas and negatives
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String cleaned = value.trim().replace(",", "").replace(" ", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Could not parse number: '{}'", value);
            return null;
        }
    }

    /**
     * Generate SHA-256 hash for source data
     */
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
