package com.github.jasminb.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.PolymorphRelationship;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;

@Type("polymorph-parent")
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepeatedRelationships {
    @Id
    private String id;

    /*
     * Notice: two @Relationship (not polymorph) of same name
     */
    @Relationship("arbitraryRelationship")
    public User user;

    @Relationship("arbitraryRelationship")
    public Author author;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
