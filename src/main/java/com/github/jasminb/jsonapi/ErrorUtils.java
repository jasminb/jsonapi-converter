package com.github.jasminb.jsonapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.ErrorResponse;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * Utility class providing methods needed for parsing JSON API Spec errors.
 *
 * @author jbegic
 */
public class ErrorUtils {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	static {
		MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	private ErrorUtils() {
		// Private constructor
	}

	/**
	 * Parses provided ResponseBody and returns it as ErrorResponse.
	 * @param errorResponse error response body
	 * @return ErrorResponse collection
	 * @throws IOException
	 */
	public static ErrorResponse parseErrorResponse(ResponseBody errorResponse) throws IOException {
		return MAPPER.readValue(errorResponse.bytes(), ErrorResponse.class);
	}

	/**
	 * Parses provided JsonNode and returns it as ErrorResponse.
	 * @param errorResponse error response body
	 * @return ErrorResponse collection
	 * @throws JsonProcessingException thrown in case JsonNode cannot be parsed
	 */
	public static ErrorResponse parseError(JsonNode errorResponse) throws JsonProcessingException {
		return MAPPER.treeToValue(errorResponse, ErrorResponse.class);
	}

	/**
	 * Returns {@code true} if the candidate node contains an 'errors' member.  Nodes that contain an 'errors' member
	 * may be processed by the {@code ErrorUtils} class.
	 *
	 * @param candidate a {@code JsonNode} that may or may not contain an 'errors' member.
	 * @return true if the candidate node contains an 'errors' member, false otherwise.
     */
	public static boolean hasErrors(JsonNode candidate) {
		return candidate != null && candidate.has(JSONAPISpecConstants.ERRORS);
	}

	/**
	 * Populates the supplied {@code StringBuilder} with the contents of the {@code errorResponse}.
	 *
	 * @param errorResponse the error
	 * @param errMessage the {@code StringBuilder} to fill
	 * @return the same {@code StringBuilder} instance, filled with the error message
     */
	public static StringBuilder fill(ErrorResponse errorResponse, StringBuilder errMessage) {
		for (Error e : errorResponse.getErrors()) {
			if (e.getTitle() != null) {
				errMessage.append(e.getTitle()).append(": ");
			}
			if (e.getCode() != null) {
				errMessage.append("Error code: ").append(e.getCode()).append(" ");
			}
			if (e.getDetail() != null) {
				errMessage.append("Detail: ").append(e.getDetail());
			}
		}

		return errMessage;
	}

}
