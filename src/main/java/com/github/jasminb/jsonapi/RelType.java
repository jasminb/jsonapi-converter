package com.github.jasminb.jsonapi;

/**
 * Encapsulates supported <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">relationship
 * types</a>.
 */
public enum RelType {

    SELF (JSONAPISpecConstants.SELF),

    RELATED (JSONAPISpecConstants.RELATED);

    private String relName;

    RelType(String relName) {
        this.relName = relName;
    }

    /**
     * Obtains the name of the relationship, suitable for use in serialized JSON.
     *
     * @return the relationship name
     */
    public String getRelName() {
        return relName;
    }
}
