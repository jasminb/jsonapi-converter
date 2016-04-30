package com.github.jasminb.jsonapi.models.recursion;

import com.github.jasminb.jsonapi.RelType;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

/**
 * A test class that has a parent.
 */
@Type("node")
public class RecursingNode {

    @Id
    private String id;

    private String name;

    private String url;

    @Relationship(value = "parent", resolve = true, relType = RelType.RELATED)
    private RecursingNode parent;

    public RecursingNode getParent() {
        return parent;
    }

    public void setParent(RecursingNode parent) {
        this.parent = parent;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
