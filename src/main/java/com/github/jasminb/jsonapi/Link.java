package com.github.jasminb.jsonapi;

import java.util.Collections;
import java.util.Map;

/**
 * Models a JSON API Link object.
 */
public class Link {

    private String href;

    private Map<String, ?> meta = Collections.emptyMap();

    public Link() {

    }

    public Link(String href) {
        this.href = href;
    }

    public Link(String href, Map<String, ?> meta) {
        this.href = href;
        this.meta = meta;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public Map<String, ?> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, ?> meta) {
        this.meta = meta;
    }

}
