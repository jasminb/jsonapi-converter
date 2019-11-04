package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.jasminb.jsonapi.exceptions.InvalidJsonApiResourceException;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

/**
 * Covering cases for validation utility class.
 *
 * @author jbegic
 */
public class ValidationUtilsTest {
	private ObjectMapper mapper;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup() {
		mapper = new ObjectMapper();
	}

	//ensureValidDocument

	@Test(expected = InvalidJsonApiResourceException.class)
	public void testExpectData() throws IOException {
		JsonNode node = mapper.readTree("{}");
		ValidationUtils.ensureValidDocument(mapper, node);
	}

	@Test(expected = ResourceParseException.class)
	public void testNodeIsError() throws IOException {
		JsonNode node = mapper.readTree(IOUtils.getResourceAsString("errors.json"));
		ValidationUtils.ensureValidDocument(mapper, node);
	}

	@Test
	public void nullObjectNode() throws IOException {
		ValidationUtils.ensureValidDocument(mapper, mapper.readTree("{\"data\" : null}"));
	}

	@Test
	public void metaResource() throws IOException {
		ValidationUtils.ensureValidDocument(mapper, mapper.readTree("{\"meta\": {}}"));
	}

	//isResourceIdentifierObject

	@Test
	public void testResourceIdentifierValidationPositive() throws IOException {
		Assert.assertTrue(ValidationUtils.isResourceIdentifierObject(mapper.readTree("{\"type\" : \"type\", \"id\" : " +
				"\"id\"}")));
	}

	@Test
	public void testResourceIdentifierValidationNoId() throws IOException {
		Assert.assertFalse(ValidationUtils.isResourceIdentifierObject(mapper.readTree("{\"type\" : \"type\"}")));
	}

	@Test
	public void testResourceIdentifierValidationIdNotValue() throws IOException {
		Assert.assertFalse(ValidationUtils.isResourceIdentifierObject(mapper.readTree("{\"type\" : \"type\", \"id\" : {}}")));
	}

	@Test
	public void testResourceIdentifierValidationNoType() throws IOException {
		Assert.assertFalse(ValidationUtils.isResourceIdentifierObject(mapper.readTree("{ \"id\" : \"id\"}")));
	}

	@Test
	public void testResourceIdentifierValidationTypeNotValue() throws IOException {
		Assert.assertFalse(ValidationUtils.isResourceIdentifierObject(mapper.readTree("{\"type\" : {}, \"id\" : \"id\"}")));
	}

	@Test
	public void testResourceIdentifierValidationMetaNotContainer() throws IOException {
		Assert.assertFalse(ValidationUtils.isResourceIdentifierObject(mapper.readTree("{\"type\" : \"type\", \"meta\" : \"meta\", \"id\" : " +
																							  "\"id\"}")));
	}

	@Test
	public void testResourceIdentifierValidationMetaContainer() throws IOException {
		Assert.assertTrue(ValidationUtils.isResourceIdentifierObject(mapper.readTree("{\"type\" : \"type\",\"meta\" : {}, \"id\" : " +
																							 "\"id\"}")));
	}

	@Test
	public void testResourceIdentifierValidationEmpty() throws IOException {
		Assert.assertFalse(ValidationUtils.isResourceIdentifierObject(mapper.readTree("{}")));
	}

	@Test
	public void testResourceIdentifierValidationNull() {
		Assert.assertFalse(ValidationUtils.isResourceIdentifierObject(null));
	}

	@Test
	public void testResourceIdentifierValidationNullNode() {
		Assert.assertFalse(ValidationUtils.isResourceIdentifierObject(NullNode.getInstance()));
	}

    //isResourceObject

    @Test
    public void testResourceValidationPositive() throws IOException {
        Assert.assertTrue(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\", \"attributes\" : {}}")));
    }

    @Test
    public void testResourceValidationNoAttributes() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\"}")));
    }

    @Test
    public void testResourceValidationAttributesNotContainer() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\", \"attributes\" : \"attributes\"}")));
    }

    @Test
    public void testResourceValidationNoType() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{ \"attributes\" : {}}")));
    }

    @Test
    public void testResourceValidationTypeNotValue() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : {}, \"attributes\" : {}}")));
    }

    @Test
    public void testResourceValidationMetaNotContainer() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\", \"meta\" : \"meta\", \"attributes\" : " +
                                                                                    "{}}")));
    }

    @Test
    public void testResourceValidationMetaContainer() throws IOException {
        Assert.assertTrue(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\",\"meta\" : {}, \"attributes\" : " +
                                                                                   "{}}")));
    }

    @Test
    public void testResourceValidationRelationshipsNotContainer() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\", \"relationships\" : \"Relationships\", "
                                                                                    + "\"attributes\" : " +
                                                                                    "{}}")));
    }

    @Test
    public void testResourceValidationRelationshipsContainer() throws IOException {
        Assert.assertTrue(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\",\"relationships\" : {}, \"attributes\" : " +
                                                                                   "{}}")));
    }

    @Test
    public void testResourceValidationLinksNotContainer() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\", \"links\" : \"Links\", \"attributes\" : " +
                                                                                    "{}}")));
    }

    @Test
    public void testResourceValidationLinksContainer() throws IOException {
        Assert.assertTrue(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\",\"links\" : {}, \"attributes\" : " +
                                                                                   "{}}")));
    }

    @Test
    public void testResourceValidationIdValue() throws IOException {
        Assert.assertTrue(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\", \"id\" : \"Id\", \"attributes\" : " +
                                                                                   "{}}")));
    }

    @Test
    public void testResourceValidationIdNotValue() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{\"type\" : \"type\",\"id\" : {}, \"attributes\" : {}}")));
    }

    @Test
    public void testResourceValidationEmpty() throws IOException {
        Assert.assertFalse(ValidationUtils.isResourceObject(mapper.readTree("{}")));
    }

    @Test
    public void testResourceValidationNull() {
        Assert.assertFalse(ValidationUtils.isResourceObject(null));
    }

    @Test
    public void testResourceValidationNullNode() {
        Assert.assertFalse(ValidationUtils.isResourceObject(NullNode.getInstance()));
    }

    //isArrayOfResourceObjects

    @Test
    public void testResourceArrayValidationPositive() throws IOException {
        JsonNode dataNode = mapper.readTree("[{\"type\" : \"type\", \"attributes\" : {}}]");

        Assert.assertTrue(ValidationUtils.isArrayOfResourceObjects(dataNode));
    }

    @Test
    public void testResourceArrayValidationInvalidNode() throws IOException {
        JsonNode dataNode = mapper.readTree("[{\"type\" : \"type\", \"attributes\" : \"attributes\"}]");

        Assert.assertFalse(ValidationUtils.isArrayOfResourceObjects(dataNode));
    }

    @Test
    public void testResourceArrayValidationInvalidNodeWithValidNode() throws IOException {
        JsonNode dataNode = mapper.readTree(
                "[{\"type\" : \"type\", \"attributes\" : {}} , "
                        + "{\"type\" : \"type\", \"attributes\" : \"attributes\"}, "
                        + "{\"type\" : \"type\", \"attributes\" : {}} ]");

        Assert.assertFalse(ValidationUtils.isArrayOfResourceObjects(dataNode));
    }

    @Test
    public void testResourceArrayValidationEmpty() throws IOException {
        JsonNode dataNode = mapper.readTree("[]");

        Assert.assertTrue(ValidationUtils.isArrayOfResourceObjects(dataNode));
    }

    @Test
    public void testResourceArrayValidationNull() {
        Assert.assertFalse(ValidationUtils.isArrayOfResourceObjects(null));
    }

    @Test
    public void testResourceArrayValidationNullNode() {
        Assert.assertFalse(ValidationUtils.isArrayOfResourceObjects(NullNode.getInstance()));
    }

    //isArrayOfResourceIdentifierObjects

    @Test
    public void testResourceIdentifierArrayValidationPositive() throws IOException {
        JsonNode dataNode = mapper.readTree("[{\"type\" : \"type\", \"id\" : \"id\"}]");

        Assert.assertTrue(ValidationUtils.isArrayOfResourceIdentifierObjects(dataNode));
    }

    @Test
    public void testResourceIdentifierArrayValidationInvalidNode() throws IOException {
        JsonNode dataNode = mapper.readTree("[{\"type\" : \"type\", \"id\" : {}}]");

        Assert.assertFalse(ValidationUtils.isArrayOfResourceIdentifierObjects(dataNode));
    }

    @Test
    public void testResourceIdentifierArrayValidationInvalidNodeWithValidNode() throws IOException {
        JsonNode dataNode = mapper.readTree(
                "[{\"type\" : \"type\", \"id\" : \"id\"} , "
                        + "{\"type\" : \"type\", \"id\" : {}}, "
                        + "{\"type\" : \"type\", \"id\" : \"id\"} ]");

        Assert.assertFalse(ValidationUtils.isArrayOfResourceIdentifierObjects(dataNode));
    }

    @Test
    public void testResourceIdentifierArrayValidationEmpty() throws IOException {
        JsonNode dataNode = mapper.readTree("[]");

        Assert.assertTrue(ValidationUtils.isArrayOfResourceIdentifierObjects(dataNode));
    }

    @Test
    public void testResourceIdentifierArrayValidationNull() {
        Assert.assertFalse(ValidationUtils.isArrayOfResourceIdentifierObjects(null));
    }

    @Test
    public void testResourceIdentifierArrayValidationNullNode() {
        Assert.assertFalse(ValidationUtils.isArrayOfResourceIdentifierObjects(NullNode.getInstance()));
    }
}
