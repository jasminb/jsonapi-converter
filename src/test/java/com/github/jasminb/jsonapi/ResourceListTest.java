package com.github.jasminb.jsonapi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

/**
 * Insures that the {@code ResourceList} behavior is in accordance with its Javadoc.
 */
public class ResourceListTest {

    private final ResourceList EMPTY_RESOURCE_LIST = new ResourceList<>(Collections.emptyList());

    private final Link LINK_NO_META = new Link("http://example.com/link/rel");

    @Before
    public void setUp() throws Exception {
        Assert.assertTrue(EMPTY_RESOURCE_LIST.size() == 0);
        Assert.assertTrue(EMPTY_RESOURCE_LIST.getLinks().isEmpty());
        Assert.assertTrue(EMPTY_RESOURCE_LIST.getMeta().isEmpty());

        Assert.assertTrue(LINK_NO_META.getMeta().size() == 0);
        Assert.assertEquals("http://example.com/link/rel", LINK_NO_META.getHref());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullityLinksSetter() throws Exception {
        EMPTY_RESOURCE_LIST.setLinks(null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testNullityMetaSetter() throws Exception {
        EMPTY_RESOURCE_LIST.setMeta(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullityConstructor() throws Exception {
        new ResourceList<>(null);
    }

    @Test
    public void testGetLink() throws Exception {
        Assert.assertNull(EMPTY_RESOURCE_LIST.getLink("foo"));

        EMPTY_RESOURCE_LIST.setLinks(new HashMap<String, Link>() {
            {
                put("foo", LINK_NO_META);
            }
        });

        Assert.assertNotNull(EMPTY_RESOURCE_LIST.getLink("foo"));
    }

    @Test
    public void testGetPrev() throws Exception {
        Assert.assertNull(EMPTY_RESOURCE_LIST.getPrevious());

        EMPTY_RESOURCE_LIST.setLinks(new HashMap<String, Link>() {
            {
                put(JSONAPISpecConstants.PREV, LINK_NO_META);
            }
        });

        Assert.assertNotNull(EMPTY_RESOURCE_LIST.getPrevious());
    }

    @Test
    public void testGetNext() throws Exception {
        Assert.assertNull(EMPTY_RESOURCE_LIST.getNext());

        EMPTY_RESOURCE_LIST.setLinks(new HashMap<String, Link>() {
            {
                put(JSONAPISpecConstants.NEXT, LINK_NO_META);
            }
        });

        Assert.assertNotNull(EMPTY_RESOURCE_LIST.getNext());
    }

    @Test
    public void testGetFirst() throws Exception {
        Assert.assertNull(EMPTY_RESOURCE_LIST.getFirst());

        EMPTY_RESOURCE_LIST.setLinks(new HashMap<String, Link>() {
            {
                put(JSONAPISpecConstants.FIRST, LINK_NO_META);
            }
        });

        Assert.assertNotNull(EMPTY_RESOURCE_LIST.getFirst());
    }

    @Test
    public void testGetLast() throws Exception {
        Assert.assertNull(EMPTY_RESOURCE_LIST.getLast());

        EMPTY_RESOURCE_LIST.setLinks(new HashMap<String, Link>() {
            {
                put(JSONAPISpecConstants.LAST, LINK_NO_META);
            }
        });

        Assert.assertNotNull(EMPTY_RESOURCE_LIST.getLast());
    }

    @Test
    public void testGetSelf() throws Exception {
        Assert.assertNull(EMPTY_RESOURCE_LIST.getSelf());

        EMPTY_RESOURCE_LIST.setLinks(new HashMap<String, Link>() {
            {
                put(JSONAPISpecConstants.SELF, LINK_NO_META);
            }
        });

        Assert.assertNotNull(EMPTY_RESOURCE_LIST.getSelf());
    }

    @Test
    public void testGetRelated() throws Exception {
        Assert.assertNull(EMPTY_RESOURCE_LIST.getRelated());

        EMPTY_RESOURCE_LIST.setLinks(new HashMap<String, Link>() {
            {
                put(JSONAPISpecConstants.RELATED, LINK_NO_META);
            }
        });

        Assert.assertNotNull(EMPTY_RESOURCE_LIST.getRelated());
    }

    @Test
    public void testGetMeta() throws Exception {
        Assert.assertEquals(0, EMPTY_RESOURCE_LIST.getMeta().size());

        EMPTY_RESOURCE_LIST.setMeta(new HashMap<String, Object>() {
                                        {
                                            put("total_results", 10);
                                        }
                                    });

        Assert.assertEquals(1, EMPTY_RESOURCE_LIST.getMeta().size());
    }
}