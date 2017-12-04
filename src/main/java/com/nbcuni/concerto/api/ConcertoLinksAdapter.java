package com.nbcuni.concerto.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.JSONAPISpecConstants;
import com.github.jasminb.jsonapi.Link;

public class ConcertoLinksAdapter implements Serializable {

    private static final long serialVersionUID = 1305238708279094084L;

    /**
     * A map of link objects keyed by link name.
     */
    private Map<String, String> links;

    /**
     * Create new Links.
     * @param linkMap {@link Map} link data
     */
    public ConcertoLinksAdapter(Map<String, Link> linkMap) {
        Map<String, String> selfLinks = new HashMap<>();
        linkMap.entrySet()
                .stream()
                .forEach(entry -> selfLinks.put(entry.getKey(), entry.getValue().getHref()));

        links = selfLinks;
    }

    /**
     * Convenience method for returning named link.
     * @param linkName name of the link to return
     * @return the link object, or {@code null} if the named link does not exist
     */
    public String getLink(String linkName) {
        return links.get(linkName);
    }

    /**
     * Convenience method for returning the {@code prev} link.
     *
     * @return the link, or {@code null} if the named link does not exist
     */
    @JsonIgnore
    public String getPrevious() {
        return getLink(JSONAPISpecConstants.PREV);
    }

    /**
     * Convenience method for returning the {@code first} link.
     *
     * @return the link, or {@code null} if the named link does not exist
     */
    @JsonIgnore
    public String getFirst() {
        return getLink(JSONAPISpecConstants.FIRST);
    }

    /**
     * Convenience method for returning the {@code next} link.
     *
     * @return the link, or {@code null} if the named link does not exist
     */
    @JsonIgnore
    public String getNext() {
        return getLink(JSONAPISpecConstants.NEXT);
    }

    /**
     * Convenience method for returning the {@code last} link.
     *
     * @return the link, or {@code null} if the named link does not exist
     */
    @JsonIgnore
    public String getLast() {
        return getLink(JSONAPISpecConstants.LAST);
    }

    /**
     * Convenience method for returning the {@code self} link.
     *
     * @return the link, or {@code null} if the named link does not exist
     */
    @JsonIgnore
    public String getSelf() {
        return getLink(JSONAPISpecConstants.SELF);
    }

    /**
     * Convenience method for returning the {@code related} link.
     *
     * @return the link, or {@code null} if the named link does not exist
     */
    @JsonIgnore
    public String getRelated() {
        return getLink(JSONAPISpecConstants.RELATED);
    }

    /**
     * Gets all registered links.
     * @return {@link Map} link data
     */
    public Map<String, String> getLinks() {
        return new HashMap<>(links);
    }
}
