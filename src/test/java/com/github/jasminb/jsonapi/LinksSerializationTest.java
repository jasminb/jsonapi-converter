package com.github.jasminb.jsonapi;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Covers {@link Links} serializable feature.
 *
 * @author jbegic
 */
public class LinksSerializationTest {
	
	@Test
	public void testSerializeDeserialize() throws IOException, ClassNotFoundException {
		Map<String, Link> linkMap = new HashMap<>();
		linkMap.put("self", new Link("test"));
		
		Links links = new Links(linkMap);
		
		// Serialize
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(outputStream);
		oos.writeObject(links);
		oos.flush();
		
		// Deserialize
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
		Links newLinks = (Links) ois.readObject();
		
		// Assert attributes
		Assert.assertEquals("test", newLinks.getSelf().getHref());
	}
}
