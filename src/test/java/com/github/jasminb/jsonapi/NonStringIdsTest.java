package com.github.jasminb.jsonapi;

import com.github.jasminb.jsonapi.exceptions.DocumentSerializationException;
import com.github.jasminb.jsonapi.models.IntegerIdResource;
import com.github.jasminb.jsonapi.models.LongIdResource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Covers functionality of using types other than {@link String} as resource identifier.
 *
 * @author jbegic
 */
public class NonStringIdsTest {
	
	@Test
	public void test() throws DocumentSerializationException {
		LongIdResource resource = new LongIdResource();
		resource.setId(Long.MAX_VALUE);
		resource.setValue("long-resource-value");
		
		IntegerIdResource integerIdResource = new IntegerIdResource();
		integerIdResource.setId(Integer.MAX_VALUE);
		integerIdResource.setValue("integer-resource-value");
		
		resource.setIntegerIdResource(integerIdResource);
		
		ResourceConverter converter = new ResourceConverter(LongIdResource.class, IntegerIdResource.class);
		converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);
		
		byte[] bytes = converter.writeDocument(new JSONAPIDocument<>(resource));
		
		JSONAPIDocument<LongIdResource> resourceDeserialised = converter.readDocument(bytes, LongIdResource.class);
		
		Assert.assertEquals(resource.getId(), resourceDeserialised.get().getId());
		Assert.assertEquals(integerIdResource.getId(), resourceDeserialised.get().getIntegerIdResource().getId());
	}
}
