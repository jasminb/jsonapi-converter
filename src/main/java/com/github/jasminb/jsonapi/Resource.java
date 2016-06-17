package com.github.jasminb.jsonapi;

import com.github.jasminb.jsonapi.annotations.Id;

/**
 * Created by mushtu on 6/17/16.
 */
public class Resource {
    @Id
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
