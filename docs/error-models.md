# Error Models Module

## Overview

The error models module provides Java classes that represent JSON API error objects according to the [JSON API Error Object specification](https://jsonapi.org/format/#error-objects). This module contains 4 classes that model the complete JSON API error structure.

**Location**: `src/main/java/com/github/jasminb/jsonapi/models/errors/`

---

## Error Object Structure

JSON API defines a standardized error object structure that these classes implement:

```json
{
  "errors": [
    {
      "id": "unique-error-identifier",
      "links": {
        "about": "https://example.com/docs/errors/validation"
      },
      "status": "422",
      "code": "VALIDATION_ERROR",
      "title": "Validation Failed",
      "detail": "The title field cannot be empty",
      "source": {
        "pointer": "/data/attributes/title",
        "parameter": "filter[title]"
      },
      "meta": {
        "timestamp": "2023-01-15T10:30:00Z"
      }
    }
  ],
  "jsonapi": {
    "version": "1.0"
  }
}
```

---

## Error (`Error.java:15`)

Represents a single JSON API error object with all possible fields.

### Class Structure

```java
public class Error {
    private String id;
    private Links links;
    private String status;
    private String code;
    private String title;
    private String detail;
    private Source source;
    private Object meta;

    // Constructors, getters, setters, equals, hashCode, toString
}
```

### Fields Description

| Field | Type | Purpose | Example |
|-------|------|---------|---------|
| `id` | String | Unique identifier for this error occurrence | `"error-uuid-123"` |
| `links` | Links | Links object with error-related links | `{"about": "https://docs.example.com/errors/422"}` |
| `status` | String | HTTP status code as string | `"422"`, `"404"`, `"500"` |
| `code` | String | Application-specific error code | `"VALIDATION_ERROR"`, `"NOT_FOUND"` |
| `title` | String | Human-readable summary of the error | `"Validation Failed"` |
| `detail` | String | Human-readable explanation specific to this error | `"Title cannot be empty"` |
| `source` | Source | Object containing references to the source of the error | pointer to `/data/attributes/title` |
| `meta` | Object | Meta information about the error | `{"timestamp": "2023-01-15T10:30:00Z"}` |

### Usage Examples

#### Basic Error Creation
```java
Error error = new Error();
error.setStatus("422");
error.setTitle("Validation Error");
error.setDetail("The title field is required and cannot be empty");
```

#### Validation Error with Source
```java
Error validationError = new Error();
validationError.setId(UUID.randomUUID().toString());
validationError.setStatus("422");
validationError.setCode("VALIDATION_FAILED");
validationError.setTitle("Validation Error");
validationError.setDetail("Title must be between 1 and 255 characters");

Source source = new Source();
source.setPointer("/data/attributes/title");
validationError.setSource(source);

Map<String, Object> meta = new HashMap<>();
meta.put("field", "title");
meta.put("constraint", "length");
meta.put("min", 1);
meta.put("max", 255);
validationError.setMeta(meta);
```

#### Error with Documentation Links
```java
Error serverError = new Error();
serverError.setId("internal-error-001");
serverError.setStatus("500");
serverError.setTitle("Internal Server Error");
serverError.setDetail("An unexpected error occurred while processing the request");

Links errorLinks = new Links();
errorLinks.addLink("about", new Link("https://docs.api.example.com/errors/500"));
serverError.setLinks(errorLinks);
```

#### Authentication/Authorization Errors
```java
// Authentication error
Error authError = new Error();
authError.setStatus("401");
authError.setCode("AUTH_TOKEN_EXPIRED");
authError.setTitle("Authentication Failed");
authError.setDetail("The provided authentication token has expired");

// Authorization error
Error authzError = new Error();
authzError.setStatus("403");
authzError.setCode("INSUFFICIENT_PERMISSIONS");
authzError.setTitle("Access Denied");
authzError.setDetail("You do not have permission to modify this resource");
```

### Spring Boot Integration

```java
@RestController
public class ArticleController {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<JSONAPIDocument<?>> handleValidation(ValidationException e) {
        List<Error> errors = e.getFieldErrors().stream()
            .map(this::createValidationError)
            .collect(Collectors.toList());

        JSONAPIDocument<?> errorDocument = JSONAPIDocument.createErrorDocument(errors);
        return ResponseEntity.status(422).body(errorDocument);
    }

    private Error createValidationError(FieldError fieldError) {
        Error error = new Error();
        error.setId(UUID.randomUUID().toString());
        error.setStatus("422");
        error.setCode("VALIDATION_ERROR");
        error.setTitle("Validation Failed");
        error.setDetail(fieldError.getDefaultMessage());

        Source source = new Source();
        source.setPointer("/data/attributes/" + fieldError.getField());
        error.setSource(source);

        return error;
    }
}
```

---

## Errors (`Errors.java:13`)

Container for multiple error objects, representing the top-level errors array in JSON API error responses.

### Class Structure

```java
public class Errors {
    private List<Error> errors;
    private JsonApi jsonapi;

    // Constructors, getters, setters, toString
}
```

### Usage Examples

#### Creating Error Collections
```java
// Multiple validation errors
List<Error> validationErrors = new ArrayList<>();

Error titleError = new Error();
titleError.setStatus("422");
titleError.setDetail("Title cannot be empty");
validationErrors.add(titleError);

Error contentError = new Error();
contentError.setStatus("422");
contentError.setDetail("Content must be at least 10 characters");
validationErrors.add(contentError);

Errors errors = new Errors();
errors.setErrors(validationErrors);

// Set JSON API version
JsonApi jsonApi = new JsonApi();
jsonApi.setVersion("1.0");
errors.setJsonapi(jsonApi);
```

#### Processing Errors from API Response
```java
try {
    JSONAPIDocument<Article> document = converter.readDocument(response, Article.class);
    return document.get();
} catch (ResourceParseException e) {
    Errors apiErrors = e.getErrors();

    for (Error error : apiErrors.getErrors()) {
        logger.error("API Error - Status: {}, Title: {}, Detail: {}",
            error.getStatus(), error.getTitle(), error.getDetail());

        // Handle specific error types
        switch (error.getStatus()) {
            case "404":
                throw new EntityNotFoundException(error.getDetail());
            case "422":
                handleValidationError(error);
                break;
            case "403":
                throw new AccessDeniedException(error.getDetail());
            default:
                logger.warn("Unhandled error status: {}", error.getStatus());
        }
    }
}
```

#### Bulk Error Processing
```java
public class ErrorProcessor {

    public ErrorSummary processErrors(Errors errors) {
        ErrorSummary summary = new ErrorSummary();

        for (Error error : errors.getErrors()) {
            switch (error.getStatus()) {
                case "400":
                    summary.addClientError(error);
                    break;
                case "422":
                    summary.addValidationError(error);
                    break;
                case "404":
                    summary.addNotFoundError(error);
                    break;
                case "500":
                    summary.addServerError(error);
                    break;
            }
        }

        return summary;
    }
}
```

---

## Source (`Source.java:10`)

Represents the source of an error, indicating which part of the request caused the error.

### Class Structure

```java
public class Source {
    private String pointer;
    private String parameter;

    // Constructors, getters, setters, equals, hashCode, toString
}
```

### Fields Description

| Field | Type | Purpose | Example |
|-------|------|---------|---------|
| `pointer` | String | JSON Pointer to the field in the request document | `/data/attributes/title` |
| `parameter` | String | Name of query parameter that caused the error | `filter[title]` |

### JSON Pointer Usage

JSON Pointer ([RFC 6901](https://tools.ietf.org/html/rfc6901)) is used to reference specific locations in the JSON document:

#### Common Pointer Examples
```java
// Root level data error
Source dataError = new Source("/data", null);

// Attribute error
Source titleError = new Source("/data/attributes/title", null);

// Relationship error
Source authorError = new Source("/data/relationships/author/data", null);

// Array element error
Source tagError = new Source("/data/attributes/tags/0", null);

// Included resource error
Source includedError = new Source("/included/0/attributes/name", null);
```

#### Query Parameter Errors
```java
// Filter parameter error
Source filterError = new Source(null, "filter[title]");

// Pagination parameter error
Source pageError = new Source(null, "page[size]");

// Sort parameter error
Source sortError = new Source(null, "sort");
```

### Usage Examples

#### Validation Error with Field Reference
```java
@Service
public class ValidationService {

    public void validateArticle(Article article) throws ValidationException {
        List<Error> errors = new ArrayList<>();

        if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
            Error error = new Error();
            error.setStatus("422");
            error.setCode("REQUIRED_FIELD");
            error.setTitle("Required Field Missing");
            error.setDetail("Title is required and cannot be empty");

            Source source = new Source();
            source.setPointer("/data/attributes/title");
            error.setSource(source);

            errors.add(error);
        }

        if (article.getTags() != null) {
            for (int i = 0; i < article.getTags().size(); i++) {
                String tag = article.getTags().get(i);
                if (tag == null || tag.trim().isEmpty()) {
                    Error error = new Error();
                    error.setStatus("422");
                    error.setDetail("Tag cannot be empty");

                    Source source = new Source();
                    source.setPointer("/data/attributes/tags/" + i);
                    error.setSource(source);

                    errors.add(error);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
```

#### Query Parameter Validation
```java
@RestController
public class ArticleController {

    @GetMapping("/articles")
    public ResponseEntity<?> getArticles(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer pageSize) {

        List<Error> errors = new ArrayList<>();

        if (pageSize != null && (pageSize < 1 || pageSize > 100)) {
            Error error = new Error();
            error.setStatus("400");
            error.setCode("INVALID_PARAMETER");
            error.setTitle("Invalid Parameter");
            error.setDetail("Page size must be between 1 and 100");

            Source source = new Source();
            source.setParameter("page[size]");
            error.setSource(source);

            errors.add(error);
        }

        if (!errors.isEmpty()) {
            JSONAPIDocument<?> errorDoc = JSONAPIDocument.createErrorDocument(errors);
            return ResponseEntity.badRequest().body(errorDoc);
        }

        // Process request...
        return ResponseEntity.ok(articles);
    }
}
```

---

## Links (`Links.java:11`)

Error-specific links object that typically contains an "about" link pointing to documentation about the error.

### Class Structure

```java
public class Links {
    private Link about;

    // Constructors, getters, setters, equals, hashCode, toString
}
```

### Usage Examples

#### Documentation Links
```java
Error error = new Error();
error.setStatus("422");
error.setCode("VALIDATION_FAILED");
error.setTitle("Validation Error");

Links errorLinks = new Links();
errorLinks.setAbout(new Link("https://docs.api.example.com/errors/validation"));
error.setLinks(errorLinks);
```

#### Dynamic Error Documentation
```java
public class ErrorDocumentationService {
    private static final String DOCS_BASE_URL = "https://docs.api.example.com/errors";

    public Links createErrorLinks(String errorCode) {
        Links links = new Links();

        String aboutUrl = switch (errorCode) {
            case "VALIDATION_ERROR" -> DOCS_BASE_URL + "/validation";
            case "AUTH_FAILED" -> DOCS_BASE_URL + "/authentication";
            case "RATE_LIMIT" -> DOCS_BASE_URL + "/rate-limiting";
            default -> DOCS_BASE_URL + "/general";
        };

        links.setAbout(new Link(aboutUrl));
        return links;
    }
}
```

---

## Common Error Patterns

### 1. Validation Errors (422)

```java
public class ValidationErrorBuilder {

    public static Error createRequiredFieldError(String field) {
        Error error = new Error();
        error.setId(UUID.randomUUID().toString());
        error.setStatus("422");
        error.setCode("REQUIRED_FIELD");
        error.setTitle("Required Field Missing");
        error.setDetail(String.format("The field '%s' is required", field));

        Source source = new Source();
        source.setPointer("/data/attributes/" + field);
        error.setSource(source);

        return error;
    }

    public static Error createInvalidFormatError(String field, String expectedFormat) {
        Error error = new Error();
        error.setId(UUID.randomUUID().toString());
        error.setStatus("422");
        error.setCode("INVALID_FORMAT");
        error.setTitle("Invalid Field Format");
        error.setDetail(String.format("The field '%s' must be in format: %s", field, expectedFormat));

        Source source = new Source();
        source.setPointer("/data/attributes/" + field);
        error.setSource(source);

        return error;
    }

    public static Error createOutOfRangeError(String field, Object min, Object max) {
        Error error = new Error();
        error.setId(UUID.randomUUID().toString());
        error.setStatus("422");
        error.setCode("OUT_OF_RANGE");
        error.setTitle("Value Out of Range");
        error.setDetail(String.format("The field '%s' must be between %s and %s", field, min, max));

        Source source = new Source();
        source.setPointer("/data/attributes/" + field);
        error.setSource(source);

        Map<String, Object> meta = new HashMap<>();
        meta.put("min", min);
        meta.put("max", max);
        error.setMeta(meta);

        return error;
    }
}
```

### 2. Authentication/Authorization Errors (401/403)

```java
public class SecurityErrorBuilder {

    public static Error createAuthenticationError() {
        Error error = new Error();
        error.setId("auth-failed-" + System.currentTimeMillis());
        error.setStatus("401");
        error.setCode("AUTH_REQUIRED");
        error.setTitle("Authentication Required");
        error.setDetail("Valid authentication credentials are required to access this resource");

        Links links = new Links();
        links.setAbout(new Link("https://docs.api.example.com/authentication"));
        error.setLinks(links);

        return error;
    }

    public static Error createAuthorizationError(String resource, String action) {
        Error error = new Error();
        error.setId("authz-failed-" + System.currentTimeMillis());
        error.setStatus("403");
        error.setCode("INSUFFICIENT_PERMISSIONS");
        error.setTitle("Access Denied");
        error.setDetail(String.format("You don't have permission to %s %s", action, resource));

        Map<String, Object> meta = new HashMap<>();
        meta.put("resource", resource);
        meta.put("action", action);
        meta.put("timestamp", Instant.now());
        error.setMeta(meta);

        return error;
    }
}
```

### 3. Resource Errors (404/409)

```java
public class ResourceErrorBuilder {

    public static Error createNotFoundError(String resourceType, String resourceId) {
        Error error = new Error();
        error.setId(UUID.randomUUID().toString());
        error.setStatus("404");
        error.setCode("RESOURCE_NOT_FOUND");
        error.setTitle("Resource Not Found");
        error.setDetail(String.format("%s with id '%s' was not found", resourceType, resourceId));

        Map<String, Object> meta = new HashMap<>();
        meta.put("resourceType", resourceType);
        meta.put("resourceId", resourceId);
        error.setMeta(meta);

        return error;
    }

    public static Error createConflictError(String resourceType, String field, String value) {
        Error error = new Error();
        error.setId(UUID.randomUUID().toString());
        error.setStatus("409");
        error.setCode("RESOURCE_CONFLICT");
        error.setTitle("Resource Conflict");
        error.setDetail(String.format("A %s with %s '%s' already exists", resourceType, field, value));

        Source source = new Source();
        source.setPointer("/data/attributes/" + field);
        error.setSource(source);

        return error;
    }
}
```

### 4. Server Errors (500)

```java
public class ServerErrorBuilder {

    public static Error createInternalServerError() {
        Error error = new Error();
        error.setId("server-error-" + UUID.randomUUID());
        error.setStatus("500");
        error.setCode("INTERNAL_ERROR");
        error.setTitle("Internal Server Error");
        error.setDetail("An unexpected error occurred while processing your request");

        Links links = new Links();
        links.setAbout(new Link("https://docs.api.example.com/errors/server-errors"));
        error.setLinks(links);

        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", Instant.now());
        meta.put("support", "Please contact support with this error ID");
        error.setMeta(meta);

        return error;
    }

    public static Error createServiceUnavailableError(String service) {
        Error error = new Error();
        error.setId(UUID.randomUUID().toString());
        error.setStatus("503");
        error.setCode("SERVICE_UNAVAILABLE");
        error.setTitle("Service Unavailable");
        error.setDetail(String.format("The %s service is temporarily unavailable", service));

        Map<String, Object> meta = new HashMap<>();
        meta.put("service", service);
        meta.put("retryAfter", "300"); // 5 minutes
        error.setMeta(meta);

        return error;
    }
}
```

---

## Testing Error Models

### Unit Tests

```java
@Test
public class ErrorModelsTest {

    @Test
    public void shouldSerializeErrorCorrectly() throws Exception {
        Error error = new Error();
        error.setId("test-error-123");
        error.setStatus("422");
        error.setCode("VALIDATION_ERROR");
        error.setTitle("Validation Failed");
        error.setDetail("Title cannot be empty");

        Source source = new Source();
        source.setPointer("/data/attributes/title");
        error.setSource(source);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(error);

        JsonNode jsonNode = mapper.readTree(json);
        assertThat(jsonNode.get("id").asText()).isEqualTo("test-error-123");
        assertThat(jsonNode.get("status").asText()).isEqualTo("422");
        assertThat(jsonNode.get("source").get("pointer").asText()).isEqualTo("/data/attributes/title");
    }

    @Test
    public void shouldDeserializeErrorsCorrectly() throws Exception {
        String json = """
            {
              "errors": [
                {
                  "id": "error-1",
                  "status": "422",
                  "title": "Validation Error",
                  "source": {"pointer": "/data/attributes/title"}
                }
              ]
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        Errors errors = mapper.readValue(json, Errors.class);

        assertThat(errors.getErrors()).hasSize(1);
        Error error = errors.getErrors().get(0);
        assertThat(error.getId()).isEqualTo("error-1");
        assertThat(error.getStatus()).isEqualTo("422");
        assertThat(error.getSource().getPointer()).isEqualTo("/data/attributes/title");
    }
}
```

### Integration Tests

```java
@Test
public void shouldHandleApiErrorResponse() {
    String errorResponseJson = """
        {
          "errors": [
            {
              "status": "404",
              "code": "ARTICLE_NOT_FOUND",
              "title": "Article Not Found",
              "detail": "Article with id '123' was not found"
            }
          ]
        }
        """;

    ResourceParseException exception = assertThrows(
        ResourceParseException.class,
        () -> converter.readDocument(errorResponseJson.getBytes(), Article.class)
    );

    Errors errors = exception.getErrors();
    assertThat(errors.getErrors()).hasSize(1);

    Error error = errors.getErrors().get(0);
    assertThat(error.getStatus()).isEqualTo("404");
    assertThat(error.getCode()).isEqualTo("ARTICLE_NOT_FOUND");
    assertThat(error.getDetail()).contains("Article with id '123' was not found");
}
```

---

## Best Practices

### 1. Consistent Error Codes
```java
public class ErrorCodes {
    // Validation errors
    public static final String REQUIRED_FIELD = "REQUIRED_FIELD";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";
    public static final String OUT_OF_RANGE = "OUT_OF_RANGE";

    // Resource errors
    public static final String NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String CONFLICT = "RESOURCE_CONFLICT";
    public static final String GONE = "RESOURCE_GONE";

    // Security errors
    public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";
    public static final String RATE_LIMITED = "RATE_LIMITED";
}
```

### 2. Error ID Generation
```java
public class ErrorIdGenerator {
    private static final String PREFIX = "err";

    public static String generate() {
        return PREFIX + "-" + System.currentTimeMillis() + "-" +
               ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    public static String generateForType(String errorType) {
        return errorType.toLowerCase() + "-" +
               System.currentTimeMillis() + "-" +
               ThreadLocalRandom.current().nextInt(100, 999);
    }
}
```

### 3. Error Documentation
```java
// Always provide meaningful error documentation
Links errorLinks = new Links();
errorLinks.setAbout(new Link("https://docs.api.example.com/errors/" + error.getCode().toLowerCase()));
error.setLinks(errorLinks);
```

### 4. Structured Error Meta
```java
Map<String, Object> meta = new HashMap<>();
meta.put("timestamp", Instant.now());
meta.put("requestId", requestId);
meta.put("userAgent", userAgent);
meta.put("endpoint", request.getRequestURI());
error.setMeta(meta);
```

---

*Source locations: All error model classes are in `src/main/java/com/github/jasminb/jsonapi/models/errors/`*