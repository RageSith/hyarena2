package de.ragesith.hyarena2.ui.hyml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses .hyml files into HyMLDocument objects.
 * Uses Java's built-in XML DOM parser with placeholder substitution.
 */
public class HyMLParser {

    private static final Set<String> VALID_TAGS = Set.of(
        "title", "heading", "text", "bullet", "stat", "divider", "gap"
    );

    /**
     * Parses a .hyml file from the filesystem and substitutes placeholders.
     *
     * @param path the path to the .hyml file
     * @param vars placeholder variables ({key} → value)
     * @return the parsed document, or null on failure
     */
    public static HyMLDocument parse(Path path, Map<String, String> vars) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parseContent(content, vars, path.toString());
        } catch (Exception e) {
            System.err.println("[HyMLParser] Failed to parse " + path + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses a .hyml file from the classpath (inside the JAR) and substitutes placeholders.
     *
     * @param resourcePath classpath resource path (e.g. "hyml/rules.hyml")
     * @param vars         placeholder variables ({key} → value)
     * @return the parsed document, or null on failure
     */
    public static HyMLDocument parseResource(String resourcePath, Map<String, String> vars) {
        try (InputStream is = HyMLParser.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[HyMLParser] Resource not found: " + resourcePath);
                return null;
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return parseContent(content, vars, "resource:" + resourcePath);
        } catch (Exception e) {
            System.err.println("[HyMLParser] Failed to parse resource " + resourcePath + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses raw HyML content string into a document.
     */
    private static HyMLDocument parseContent(String content, Map<String, String> vars, String sourceName) {
        try {
            // Substitute {key} → value from vars map
            if (vars != null) {
                for (Map.Entry<String, String> entry : vars.entrySet()) {
                    content = content.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }

            // Parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

            // Extract <page> root element
            Element root = doc.getDocumentElement();
            if (!"page".equals(root.getTagName())) {
                System.err.println("[HyMLParser] Root element must be <page>, got: " + root.getTagName());
                return null;
            }

            String pageTitle = root.getAttribute("title");
            if (pageTitle.isEmpty()) pageTitle = "Page";

            int width = parseIntAttr(root, "width", 600);
            int height = parseIntAttr(root, "height", 450);

            // Check if any child elements are <section>
            List<HyMLDocument.HyMLSection> sections = new ArrayList<>();
            List<HyMLDocument.HyMLElement> elements = new ArrayList<>();

            NodeList children = root.getChildNodes();
            boolean hasSections = false;

            // First pass: detect sections
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE && "section".equals(child.getNodeName())) {
                    hasSections = true;
                    break;
                }
            }

            // Second pass: parse elements or sections
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() != Node.ELEMENT_NODE) continue;

                Element elem = (Element) child;

                if (hasSections) {
                    if ("section".equals(elem.getTagName())) {
                        String sectionName = elem.getAttribute("name");
                        if (sectionName.isEmpty()) sectionName = "Section " + (sections.size() + 1);
                        List<HyMLDocument.HyMLElement> sectionElements = parseElements(elem);
                        sections.add(new HyMLDocument.HyMLSection(sectionName, sectionElements));
                    } else {
                        System.out.println("[HyMLParser] Ignoring non-section element in sectioned page: " + elem.getTagName());
                    }
                } else {
                    HyMLDocument.HyMLElement hymlElement = parseElement(elem);
                    if (hymlElement != null) {
                        elements.add(hymlElement);
                    }
                }
            }

            return new HyMLDocument(pageTitle, width, height, sections, elements);

        } catch (Exception e) {
            System.err.println("[HyMLParser] Failed to parse " + sourceName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses all child elements of a parent node.
     */
    private static List<HyMLDocument.HyMLElement> parseElements(Element parent) {
        List<HyMLDocument.HyMLElement> elements = new ArrayList<>();
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;

            HyMLDocument.HyMLElement element = parseElement((Element) child);
            if (element != null) {
                elements.add(element);
            }
        }

        return elements;
    }

    /**
     * Parses a single element node into a HyMLElement.
     */
    private static HyMLDocument.HyMLElement parseElement(Element elem) {
        String tag = elem.getTagName();

        if (!VALID_TAGS.contains(tag)) {
            System.out.println("[HyMLParser] Unknown tag '" + tag + "', skipping");
            return null;
        }

        // Get text content (for non-void elements)
        String textContent = null;
        if (!"divider".equals(tag) && !"gap".equals(tag)) {
            textContent = elem.getTextContent();
            if (textContent != null) {
                textContent = textContent.trim();
            }
        }

        // Collect attributes
        Map<String, String> attributes = new HashMap<>();
        NamedNodeMap attrs = elem.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            attributes.put(attr.getNodeName(), attr.getNodeValue());
        }

        return new HyMLDocument.HyMLElement(tag, textContent, attributes);
    }

    private static int parseIntAttr(Element elem, String name, int defaultValue) {
        String val = elem.getAttribute(name);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
