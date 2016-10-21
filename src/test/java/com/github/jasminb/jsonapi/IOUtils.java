package com.github.jasminb.jsonapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Provides utility methods for IO related actions.
 *
 * @author jbegic
 */
public class IOUtils {

	public static String getResourceAsString(String name) throws IOException {
		InputStream input = IOUtils.class.getClassLoader().getResourceAsStream(name);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
			String line;
			StringBuilder resultBuilder = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				resultBuilder.append(line);
			}

			return resultBuilder.toString();
		}
	}
	public static InputStream getResource(String name) throws IOException {
		InputStream input = IOUtils.class.getClassLoader().getResourceAsStream(name);
		return input;
	}
}
