package com.github.jasminb.jsonapi;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing functionality of JSON API Document methods.
 *
 * @author Marcel Overdijk
 */
public class JSONAPIDocumentTest {

    @Test
    public void testAddMeta() {
        JSONAPIDocument document = new JSONAPIDocument();
        Assert.assertNull(document.getMeta());
        document.addMeta("foo", "bar");
        Assert.assertEquals(1, document.getMeta().size());
        Assert.assertEquals("bar", document.getMeta().get("foo"));
    }

    @Test
    public void testAddLink() {
        JSONAPIDocument document = new JSONAPIDocument();
        Assert.assertNull(document.getLinks());
        document.addLink("foo", new Link("bar"));
        Assert.assertEquals(1, document.getLinks().getLinks().size());
        Assert.assertEquals("bar", document.getLinks().getLink("foo").getHref());
    }
}
