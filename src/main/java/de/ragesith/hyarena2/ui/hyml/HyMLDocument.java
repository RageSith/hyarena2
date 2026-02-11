package de.ragesith.hyarena2.ui.hyml;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parsed HyML document data model.
 * Represents a complete page parsed from a .hyml file.
 */
public class HyMLDocument {

    private final String pageTitle;
    private final int width;
    private final int height;
    private final boolean sectioned;
    private final List<HyMLSection> sections;
    private final List<HyMLElement> elements;

    public HyMLDocument(String pageTitle, int width, int height,
                        List<HyMLSection> sections, List<HyMLElement> elements) {
        this.pageTitle = pageTitle;
        this.width = width;
        this.height = height;
        this.sections = sections != null ? sections : Collections.emptyList();
        this.elements = elements != null ? elements : Collections.emptyList();
        this.sectioned = !this.sections.isEmpty();
    }

    public String getPageTitle() { return pageTitle; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isSectioned() { return sectioned; }
    public List<HyMLSection> getSections() { return sections; }
    public List<HyMLElement> getElements() { return elements; }

    /**
     * A single HyML element (title, heading, text, bullet, stat, divider, gap).
     */
    public static class HyMLElement {
        private final String tag;
        private final String textContent;
        private final Map<String, String> attributes;

        public HyMLElement(String tag, String textContent, Map<String, String> attributes) {
            this.tag = tag;
            this.textContent = textContent;
            this.attributes = attributes != null ? attributes : Collections.emptyMap();
        }

        public String getTag() { return tag; }
        public String getTextContent() { return textContent; }
        public Map<String, String> getAttributes() { return attributes; }

        public String getAttribute(String name) {
            return attributes.get(name);
        }

        public boolean hasAttribute(String name) {
            return attributes.containsKey(name);
        }
    }

    /**
     * A named section containing a list of elements.
     * Used for the two-column sectioned layout.
     */
    public static class HyMLSection {
        private final String name;
        private final List<HyMLElement> elements;

        public HyMLSection(String name, List<HyMLElement> elements) {
            this.name = name;
            this.elements = elements != null ? elements : Collections.emptyList();
        }

        public String getName() { return name; }
        public List<HyMLElement> getElements() { return elements; }
    }
}
