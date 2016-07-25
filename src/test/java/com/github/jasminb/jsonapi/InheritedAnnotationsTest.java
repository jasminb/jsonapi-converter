package com.github.jasminb.jsonapi;

import com.github.jasminb.jsonapi.models.inheritance.City;
import com.github.jasminb.jsonapi.models.inheritance.Engineer;
import com.github.jasminb.jsonapi.models.inheritance.EngineeringField;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Covering testing of resource classes with inherited annotations.
 *
 * @author jbegic
 */
public class InheritedAnnotationsTest {
	private ResourceConverter resourceConverter;

	@Before
	public void setup() {
		resourceConverter = new ResourceConverter(Engineer.class, EngineeringField.class, City.class);
	}

	@Test
	public void testConversion() throws IOException {
		InputStream data = IOUtils.getResource("engineer.json");

		JSONAPIDocument<Engineer> engineerDocument = resourceConverter.readDocument(data, Engineer.class);

		Engineer engineer = engineerDocument.get();

		Assert.assertNotNull(engineer);
		Assert.assertNotNull(engineer.getField());
		Assert.assertNotNull(engineer.getCity());

		Assert.assertEquals("John", engineer.getFirstName());
		Assert.assertEquals("Doe", engineer.getLastName());

		Assert.assertEquals("Software Engineering", engineer.getField().getName());
		Assert.assertEquals("Sarajevo", engineer.getCity().getName());

		Assert.assertEquals("Note", engineer.getMeta().getNote());
	}
}
