# Exception Handling Module

## Overview

The exception handling module provides custom exceptions for various error conditions that can occur during JSON API document processing. This module contains 4 specialized exception classes that help developers handle different failure scenarios gracefully.

**Location**: `src/main/java/com/github/jasminb/jsonapi/exceptions/`

---

## Exception Hierarchy

All custom exceptions extend from `RuntimeException`, making them unchecked exceptions that don't require explicit handling but can be caught when needed.

```
RuntimeException
├── DocumentSerializationException    # Serialization failures
├── InvalidJsonApiResourceException   # Invalid JSON API format
├── ResourceParseException           # Error responses from server
└── UnregisteredTypeException        # Unknown resource types
```

---

## DocumentSerializationException (`DocumentSerializationException.java:10`)

**Purpose**: Thrown when document serialization to JSON API format fails.

**When thrown**: During `writeDocument()` or `writeDocumentCollection()` operations

**Common causes**:
- Jackson serialization errors
- Invalid object state (null required fields)
- Circular references not properly handled
- Custom ID handler failures

```java
public class DocumentSerializationException extends RuntimeException {
    public DocumentSerializationException(String message) {
        super(message);
    }

    public DocumentSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentSerializationException(Throwable cause) {
        super(cause);
    }
}
```

### Usage Examples

```java
try {
    JSONAPIDocument<Article> document = new JSONAPIDocument<>(article);
    byte[] json = converter.writeDocument(document);
} catch (DocumentSerializationException e) {
    logger.error("Failed to serialize article: {}", e.getMessage(), e);

    // Check for common causes
    if (e.getCause() instanceof JsonProcessingException) {
        // Jackson serialization issue
        handleJacksonError((JsonProcessingException) e.getCause());
    } else if (e.getCause() instanceof IllegalAccessException) {
        // Field access issue
        handleFieldAccessError((IllegalAccessException) e.getCause());
    }
}
```

### Prevention Strategies

```java
// 1. Validate objects before serialization
public void validateArticle(Article article) {
    if (article.getId() == null) {
        throw new IllegalStateException("Article ID cannot be null");
    }
    if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
        throw new IllegalStateException("Article title cannot be empty");
    }
}

// 2. Use proper Jackson annotations
@Type("articles")
public class Article {
    @Id
    private String id;

    @JsonProperty("title")  // Explicit mapping
    @JsonInclude(JsonInclude.Include.NON_NULL)  // Handle nulls
    private String title;
}

// 3. Handle circular references properly
@Type("articles")
public class Article {
    @Relationship(value = "author", serialiseData = false)  // Links only
    private Person author;
}
```

---

## InvalidJsonApiResourceException (`InvalidJsonApiResourceException.java:9`)

**Purpose**: Thrown when the JSON API document structure doesn't conform to the specification.

**When thrown**: During parsing/validation of incoming JSON API documents

**Common causes**:
- Missing required fields (`type`, `data`)
- Invalid resource object structure
- Malformed relationship objects
- Non-compliant JSON API format

```java
public class InvalidJsonApiResourceException extends RuntimeException {
    public InvalidJsonApiResourceException(String message) {
        super(message);
    }
}
```

### Usage Examples

```java
try {
    JSONAPIDocument<Article> document = converter.readDocument(jsonBytes, Article.class);
    Article article = document.get();
} catch (InvalidJsonApiResourceException e) {
    logger.error("Invalid JSON API format: {}", e.getMessage());

    // Return appropriate HTTP response
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(createErrorResponse("Invalid JSON API format", e.getMessage()));
}
```

### Validation Rules Enforced

The library validates against JSON API specification requirements:

#### Document Structure
```json
// Valid document - must have top-level 'data' or 'errors'
{
  "data": { ... },     // Required if no errors
  "included": [...],   // Optional
  "meta": { ... },     // Optional
  "links": { ... },    // Optional
  "jsonapi": { ... }   // Optional
}

// Or error document
{
  "errors": [...]      // Required if no data
}
```

#### Resource Object Structure
```json
// Valid resource object
{
  "type": "articles",     // Required
  "id": "1",             // Required (or lid)
  "attributes": { ... }, // Optional
  "relationships": { ... }, // Optional
  "links": { ... },      // Optional
  "meta": { ... }        // Optional
}
```

#### Common Validation Failures
```json
// Missing type field
{
  "id": "1",
  "attributes": { "title": "Hello" }
  // ERROR: Missing required 'type' field
}

// Both id and lid present
{
  "type": "articles",
  "id": "1",
  "lid": "temp-123"
  // ERROR: Cannot have both 'id' and 'lid'
}

// Invalid relationship structure
{
  "type": "articles",
  "id": "1",
  "relationships": {
    "author": "john-doe"    // ERROR: Should be object with 'data'/'links'/'meta'
  }
}
```

---

## ResourceParseException (`ResourceParseException.java:11`)

**Purpose**: Thrown when the server response contains JSON API error objects instead of data.

**When thrown**: During deserialization when the response has an `errors` section

**Special feature**: Wraps the actual `Errors` object from the response for programmatic access

```java
public class ResourceParseException extends RuntimeException {
    private final Errors errors;

    public ResourceParseException(Errors errors) {
        super(errors.toString());
        this.errors = errors;
    }

    /**
     * Returns Errors or null
     * @return {@link Errors}
     */
    public Errors getErrors() {
        return errors;
    }
}
```

### Usage Examples

#### Basic Error Handling
```java
try {
    JSONAPIDocument<Article> document = converter.readDocument(responseBytes, Article.class);
    Article article = document.get();
    return article;
} catch (ResourceParseException e) {
    Errors errors = e.getErrors();
    logger.warn("Server returned errors: {}", errors);

    // Handle specific error types
    for (Error error : errors.getErrors()) {
        handleApiError(error);
    }

    throw new ServiceException("Failed to load article", e);
}
```

#### Detailed Error Processing
```java
public class ApiErrorHandler {

    public void handleResourceParseException(ResourceParseException e) {
        Errors errors = e.getErrors();

        for (Error error : errors.getErrors()) {
            switch (error.getStatus()) {
                case "404":
                    throw new EntityNotFoundException(error.getDetail());
                case "403":
                    throw new AccessDeniedException(error.getDetail());
                case "422":
                    handleValidationError(error);
                    break;
                default:
                    logger.error("Unexpected API error: {}", error);
            }
        }
    }

    private void handleValidationError(Error error) {
        if (error.getSource() != null) {
            String field = error.getSource().getPointer();
            String message = error.getDetail();
            throw new ValidationException(field, message);
        }
    }
}
```

#### Server-Side Error Response Creation
```java
@RestController
public class ArticleController {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<byte[]> handleValidation(ValidationException e) {
        Error error = new Error();
        error.setStatus("422");
        error.setTitle("Validation Error");
        error.setDetail(e.getMessage());
        error.setSource(new Source(e.getField(), null));

        JSONAPIDocument<?> errorDoc = JSONAPIDocument.createErrorDocument(Arrays.asList(error));

        try {
            byte[] json = converter.writeDocument(errorDoc);
            return ResponseEntity.status(422)
                .contentType(MediaType.valueOf("application/vnd.api+json"))
                .body(json);
        } catch (DocumentSerializationException ex) {
            return ResponseEntity.status(500).build();
        }
    }
}
```

### Error Response Format

JSON API error responses that trigger this exception:

```json
{
  "errors": [
    {
      "id": "error-uuid",
      "status": "422",
      "code": "VALIDATION_ERROR",
      "title": "Validation Failed",
      "detail": "Title cannot be empty",
      "source": {
        "pointer": "/data/attributes/title"
      },
      "meta": {
        "timestamp": "2023-01-15T10:30:00Z"
      }
    }
  ],
  "meta": {
    "request-id": "abc-123"
  }
}
```

---

## UnregisteredTypeException (`UnregisteredTypeException.java:9`)

**Purpose**: Thrown when the converter encounters a resource type that hasn't been registered.

**When thrown**: During deserialization when a `type` field value has no corresponding registered class

**Common causes**:
- Forgot to register a class with the converter
- Server returns new resource types not known to client
- Typos in `@Type` annotation values
- Version mismatches between client and server

```java
public class UnregisteredTypeException extends RuntimeException {
    public UnregisteredTypeException(String message) {
        super(message);
    }
}
```

### Usage Examples

#### Basic Handling
```java
try {
    JSONAPIDocument<Article> document = converter.readDocument(responseBytes, Article.class);
} catch (UnregisteredTypeException e) {
    logger.error("Unknown resource type encountered: {}", e.getMessage());

    // Option 1: Ignore unknown types and continue
    converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);

    // Option 2: Register the missing type dynamically
    if (e.getMessage().contains("author-profiles")) {
        converter.registerType(AuthorProfile.class);
        // Retry the operation
    }
}
```

#### Dynamic Type Registration
```java
public class DynamicTypeRegistry {
    private final ResourceConverter converter;
    private final Map<String, Class<?>> availableTypes;

    public void handleUnregisteredType(String typeName) {
        Class<?> typeClass = availableTypes.get(typeName);
        if (typeClass != null) {
            boolean registered = converter.registerType(typeClass);
            if (registered) {
                logger.info("Dynamically registered type: {} -> {}", typeName, typeClass.getName());
            }
        } else {
            logger.warn("No class mapping found for type: {}", typeName);
        }
    }
}
```

#### Prevention Strategies
```java
// 1. Register all known types at startup
@Configuration
public class JsonApiConfig {

    @Bean
    public ResourceConverter resourceConverter() {
        return new ResourceConverter(
            Article.class,
            Person.class,
            Comment.class,
            Tag.class,
            Category.class
            // Add all domain classes
        );
    }
}

// 2. Use feature flags for flexible handling
converter.enableDeserializationOption(
    DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS
);

// 3. Implement fallback for unknown types in relationships
converter.enableDeserializationOption(
    DeserializationFeature.ALLOW_UNKNOWN_TYPE_IN_RELATIONSHIP
);
```

#### Debugging Type Registration Issues
```java
public void debugTypeRegistration(ResourceConverter converter, String problematicType) {
    // Check if type is registered
    boolean isRegistered = converter.isRegisteredType(SomeClass.class);
    logger.debug("Type {} registered: {}", SomeClass.class.getName(), isRegistered);

    // Check annotation
    Type typeAnnotation = SomeClass.class.getAnnotation(Type.class);
    if (typeAnnotation != null) {
        logger.debug("Class {} has @Type value: {}", SomeClass.class.getName(), typeAnnotation.value());

        if (!typeAnnotation.value().equals(problematicType)) {
            logger.error("Type mismatch! Expected: {}, Found: {}", problematicType, typeAnnotation.value());
        }
    } else {
        logger.error("Class {} missing @Type annotation", SomeClass.class.getName());
    }
}
```

---

## Exception Handling Best Practices

### 1. Layered Error Handling

```java
@Service
public class ArticleService {
    private final ResourceConverter converter;

    public Article findById(String id) {
        try {
            byte[] response = apiClient.getArticle(id);
            JSONAPIDocument<Article> document = converter.readDocument(response, Article.class);
            return document.get();

        } catch (ResourceParseException e) {
            throw mapApiErrors(e.getErrors());
        } catch (UnregisteredTypeException e) {
            logger.error("Configuration error - unregistered type: {}", e.getMessage());
            throw new ServiceConfigurationException("Missing type registration", e);
        } catch (InvalidJsonApiResourceException e) {
            logger.error("Invalid API response format: {}", e.getMessage());
            throw new ApiIntegrationException("Invalid response format", e);
        } catch (DocumentSerializationException e) {
            logger.error("Failed to process API response: {}", e.getMessage());
            throw new ServiceException("Response processing failed", e);
        }
    }

    private RuntimeException mapApiErrors(Errors errors) {
        // Map JSON API errors to domain exceptions
        for (Error error : errors.getErrors()) {
            if ("404".equals(error.getStatus())) {
                return new ArticleNotFoundException(error.getDetail());
            }
            if ("403".equals(error.getStatus())) {
                return new AccessDeniedException(error.getDetail());
            }
        }
        return new ServiceException("API request failed: " + errors);
    }
}
```

### 2. Global Exception Handler

```java
@ControllerAdvice
public class JsonApiExceptionHandler {

    @ExceptionHandler(ResourceParseException.class)
    public ResponseEntity<?> handleApiErrors(ResourceParseException e) {
        // Forward the original API errors to client
        Errors apiErrors = e.getErrors();
        return ResponseEntity.status(determineHttpStatus(apiErrors))
            .contentType(MediaType.valueOf("application/vnd.api+json"))
            .body(converter.writeDocument(JSONAPIDocument.createErrorDocument(apiErrors.getErrors())));
    }

    @ExceptionHandler(UnregisteredTypeException.class)
    public ResponseEntity<?> handleUnregisteredType(UnregisteredTypeException e) {
        Error error = new Error();
        error.setStatus("500");
        error.setTitle("Configuration Error");
        error.setDetail("Server configuration issue: " + e.getMessage());

        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }

    @ExceptionHandler(InvalidJsonApiResourceException.class)
    public ResponseEntity<?> handleInvalidFormat(InvalidJsonApiResourceException e) {
        Error error = new Error();
        error.setStatus("400");
        error.setTitle("Invalid Request Format");
        error.setDetail("Request does not conform to JSON API specification: " + e.getMessage());

        return createErrorResponse(HttpStatus.BAD_REQUEST, error);
    }
}
```

### 3. Client-Side Retry Logic

```java
@Component
public class ResilientApiClient {
    private final ResourceConverter converter;

    @Retryable(value = {UnregisteredTypeException.class}, maxAttempts = 2)
    public <T> T fetchResource(String url, Class<T> type) {
        try {
            byte[] response = httpClient.get(url);
            JSONAPIDocument<T> document = converter.readDocument(response, type);
            return document.get();

        } catch (UnregisteredTypeException e) {
            // Auto-register missing types and retry
            autoRegisterMissingType(e);
            throw e; // Trigger retry
        }
    }

    @Recover
    public <T> T recoverFromUnregisteredType(UnregisteredTypeException e, String url, Class<T> type) {
        logger.error("Failed to auto-register type after retry: {}", e.getMessage());
        throw new ServiceException("Unable to process response due to missing type registration", e);
    }
}
```

### 4. Testing Exception Scenarios

```java
@Test
public class ExceptionHandlingTest {

    @Test
    public void shouldHandleApiErrorResponse() {
        String errorJson = """
            {
              "errors": [{
                "status": "404",
                "title": "Not Found",
                "detail": "Article with id '123' not found"
              }]
            }
            """;

        ResourceParseException exception = assertThrows(
            ResourceParseException.class,
            () -> converter.readDocument(errorJson.getBytes(), Article.class)
        );

        Errors errors = exception.getErrors();
        assertThat(errors.getErrors()).hasSize(1);
        assertThat(errors.getErrors().get(0).getStatus()).isEqualTo("404");
    }

    @Test
    public void shouldHandleUnregisteredType() {
        String jsonWithUnknownType = """
            {
              "data": {
                "type": "unknown-resource",
                "id": "1"
              }
            }
            """;

        UnregisteredTypeException exception = assertThrows(
            UnregisteredTypeException.class,
            () -> converter.readDocument(jsonWithUnknownType.getBytes(), Article.class)
        );

        assertThat(exception.getMessage()).contains("unknown-resource");
    }
}
```

---

## Configuration for Error Handling

### Deserialization Features for Error Tolerance

```java
// Allow unknown types in included section
converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);

// Allow unknown types in relationships (more permissive)
converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_TYPE_IN_RELATIONSHIP);

// Require resource IDs (stricter validation)
converter.enableDeserializationOption(DeserializationFeature.REQUIRE_RESOURCE_ID);
```

### Custom Error Handling with Features

```java
try {
    document = converter.readDocument(response, Article.class);
} catch (IllegalArgumentException e) {
    if (e.getMessage().contains("unknown resource type")) {
        // Handle as UnregisteredTypeException would be thrown
        handleUnknownType(e);
    } else if (e.getMessage().contains("must have a non null and non-empty 'id'")) {
        // Handle ID validation failure
        handleMissingId(e);
    }
}
```

---

## Summary

The exception handling module provides comprehensive error handling for JSON API processing:

| Exception | Purpose | Recovery Strategy |
|-----------|---------|-------------------|
| `DocumentSerializationException` | Serialization failures | Validate object state, check Jackson config |
| `InvalidJsonApiResourceException` | Invalid JSON API format | Validate input, check API compliance |
| `ResourceParseException` | Server error responses | Extract and handle API errors appropriately |
| `UnregisteredTypeException` | Unknown resource types | Register missing types, use tolerance features |

All exceptions provide meaningful error messages and, where applicable, access to underlying error details for programmatic handling.

---

*Source locations: All exception classes are in `src/main/java/com/github/jasminb/jsonapi/exceptions/`*