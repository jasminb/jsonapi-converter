package com.github.jasminb.jsonapi.resolutionstrategy;

import com.github.jasminb.jsonapi.ResolutionStrategy;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

/**
 * Contains a @Relationship field that is resolved to a reference, but it assigned to a non-String field.  Relationships
 * that are resolved to a reference require a {@code String} type to store the reference.
 */
@Type("foo")
public class Foo {

    @Id
    private String id;

    private String name;

    @Relationship(value = "bar", resolve = true, strategy = ResolutionStrategy.REF)
    private Integer bar;

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

    public Integer getBar() {
        return bar;
    }

    public void setBar(Integer bar) {
        this.bar = bar;
    }
}
