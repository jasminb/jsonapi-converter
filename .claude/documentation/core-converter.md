# Core Converter Module

## Overview

The core converter module contains the main conversion engine responsible for transforming JSON API documents to Java POJOs and vice versa. This module contains 20 classes organized around three main components:

1. **ResourceConverter** - The primary conversion engine
2. **ConverterConfiguration** - Type registration and metadata management
3. **JSONAPIDocument** - Document wrapper for complete JSON API responses

---

## ResourceConverter (`ResourceConverter.java:44`)

The heart of the JSON API Converter library. Handles bidirectional conversion between JSON API documents and Java objects.

### Key Responsibilities
- **Deserialization**: JSON API documents → Java POJOs
- **Serialization**: Java POJOs → JSON API documents
- **Relationship Resolution**: Handle complex object relationships
- **Resource Caching**: Prevent infinite loops in circular references
- **Validation**: Ensure JSON API specification compliance

### Constructor Options

```java
// Basic constructor with registered types
ResourceConverter(Class<?>... classes)

// With base URL for link generation
ResourceConverter(String baseURL, Class<?>... classes)

// With custom Jackson ObjectMapper
ResourceConverter(ObjectMapper mapper, Class<?>... classes)

// Full constructor
ResourceConverter(ObjectMapper mapper, String baseURL, Class<?>... classes)
```

### Core Methods

#### Deserialization

```java
// Read single resource document
<T> JSONAPIDocument<T> readDocument(byte[] data, Class<T> clazz)
<T> JSONAPIDocument<T> readDocument(InputStream dataStream, Class<T> clazz)

// Read collection document
<T> JSONAPIDocument<List<T>> readDocumentCollection(byte[] data, Class<T> clazz)
<T> JSONAPIDocument<List<T>> readDocumentCollection(InputStream dataStream, Class<T> clazz)

// Legacy methods (deprecated)
<T> T readObject(byte[] data, Class<T> clazz)
<T> List<T> readObjectCollection(byte[] data, Class<T> clazz)
```

#### Serialization

```java
// Write single resource document
byte[] writeDocument(JSONAPIDocument<?> document)
byte[] writeDocument(JSONAPIDocument<?> document, SerializationSettings settings)

// Write collection document
byte[] writeDocumentCollection(JSONAPIDocument<? extends Iterable<?>> documentCollection)
byte[] writeDocumentCollection(JSONAPIDocument<? extends Iterable<?>> documentCollection, SerializationSettings settings)

// Legacy methods (deprecated)
byte[] writeObject(Object object)
<T> byte[] writeObjectCollection(Iterable<T> objects)
```

### Relationship Resolution

The converter supports automatic relationship resolution via HTTP calls:

```java
// Set global resolver for all relationship types
converter.setGlobalResolver(RelationshipResolver resolver)

// Set type-specific resolver
converter.setTypeResolver(RelationshipResolver resolver, Class<?> type)
```

**RelationshipResolver Interface:**
```java
public interface RelationshipResolver {
    byte[] resolve(String relationshipURL);
}
```

### Configuration Methods

```java
// Type registration
boolean registerType(Class<?> type)
boolean isRegisteredType(Class<?> type)

// Feature configuration
void enableDeserializationOption(DeserializationFeature option)
void disableDeserializationOption(DeserializationFeature option)
void enableSerializationOption(SerializationFeature option)
void disableSerializationOption(SerializationFeature option)
```

### Internal Processing Flow

#### Deserialization Process (`readDocument` flow):

1. **Parsing**: Parse JSON using Jackson ObjectMapper
2. **Validation**: Validate JSON API document structure
3. **Data Processing**: Extract and convert `data` section to POJO
4. **Included Processing**: Parse all `included` resources
5. **Relationship Linking**: Connect relationships between resources
6. **Meta/Links**: Extract top-level meta and links
7. **Document Creation**: Wrap everything in JSONAPIDocument

#### Serialization Process (`writeDocument` flow):

1. **Resource Processing**: Convert POJO to JSON API resource object
2. **Relationship Extraction**: Process @Relationship annotated fields
3. **Included Generation**: Build included section for related resources
4. **Meta/Links**: Add resource and relationship meta/links
5. **Document Assembly**: Combine into final JSON API document

### Resource Caching

The converter uses `ResourceCache` (`ResourceCache.java:49`) for:
- **Circular Reference Prevention**: Avoid infinite loops in bidirectional relationships
- **Performance Optimization**: Reuse already-parsed objects
- **Thread Safety**: ThreadLocal storage per conversion operation

```java
// Cache lifecycle per conversion
resourceCache.init()        // Initialize for operation
resourceCache.cache(id, obj) // Store object by identifier
resourceCache.contains(id)  // Check if object exists
resourceCache.get(id)       // Retrieve cached object
resourceCache.clear()       // Cleanup after operation
```

---

## ConverterConfiguration (`ConverterConfiguration.java:25`)

Manages type registration, annotation processing, and metadata for all registered classes.

### Key Responsibilities
- **Type Registration**: Map JSON API type names to Java classes
- **Field Mapping**: Track annotated fields for each registered type
- **Handler Management**: Manage ID handlers for different ID types
- **Reflection Cache**: Cache field lookups for performance

### Core Data Structures

```java
private final Map<String, Class<?>> typeToClassMapping        // "articles" -> Article.class
private final Map<Class<?>, Type> typeAnnotations            // Article.class -> @Type annotation
private final Map<Class<?>, Field> idMap                     // Article.class -> @Id field
private final Map<Class<?>, Field> localIdMap                // Article.class -> @LocalId field
private final Map<Class<?>, ResourceIdHandler> idHandlerMap  // Article.class -> StringIdHandler
private final Map<Class<?>, List<Field>> relationshipMap     // Article.class -> [@Relationship fields]
```

### Key Methods

#### Type Management
```java
boolean registerType(Class<?> type)                    // Register new type
boolean isRegisteredType(Class<?> type)               // Check registration
Class<?> getTypeClass(String typeName)                // Get class by JSON API type name
String getTypeName(Class<?> clazz)                    // Get JSON API type name for class
Type getType(Class<?> clazz)                          // Get @Type annotation
static boolean isEligibleType(Class<?> type)          // Check if class can be registered
```

#### Field Lookups
```java
Field getIdField(Class<?> clazz)                      // Get @Id field
Field getLocalIdField(Class<?> clazz)                 // Get @LocalId field
Field getMetaField(Class<?> clazz)                    // Get @Meta field
Field getLinksField(Class<?> clazz)                   // Get @Links field
List<Field> getRelationshipFields(Class<?> clazz)     // Get all @Relationship fields
Field getRelationshipField(Class<?> clazz, String fieldName)       // Get specific relationship field
Field getRelationshipMetaField(Class<?> clazz, String relationshipName)   // Get relationship @Meta field
Field getRelationshipLinksField(Class<?> clazz, String relationshipName)  // Get relationship @Links field
```

#### Handler Management
```java
ResourceIdHandler getIdHandler(Class<?> clazz)        // Get ID handler for type
ResourceIdHandler getLocalIdHandler(Class<?> clazz)   // Get local ID handler for type
```

#### Type Resolution
```java
Class<?> getRelationshipType(Class<?> clazz, String fieldName)           // Get relationship target type
Class<?> getRelationshipMetaType(Class<?> clazz, String relationshipName) // Get relationship meta type
Relationship getFieldRelationship(Field field)                           // Get @Relationship annotation
```

### Registration Process

When a class is registered via `registerType()`:

1. **Annotation Validation**: Verify `@Type` and `@Id` annotations exist
2. **Type Mapping**: Store type name → class mapping
3. **Field Processing**: Find and cache all annotated fields
4. **Handler Instantiation**: Create ID handlers based on annotations
5. **Relationship Analysis**: Process relationship fields and their types
6. **Recursive Registration**: Auto-register relationship target types

### Validation Rules

- **Required**: `@Type` annotation with non-empty value
- **Required**: Single `@Id` annotated field per class
- **Optional**: Single `@LocalId` annotated field per class
- **Optional**: Single `@Meta` annotated field per class
- **Optional**: Single `@Links` annotated field per class
- **Multiple**: Multiple `@Relationship` annotated fields allowed
- **Constraints**: ID handler must have no-arg constructor

---

## JSONAPIDocument (`JSONAPIDocument.java:19`)

Wrapper class representing a complete JSON API document with data, meta, links, errors, and JSON API objects.

### Generic Type Parameter
```java
JSONAPIDocument<T>                    // T = single resource type
JSONAPIDocument<List<T>>             // Collection of resources
JSONAPIDocument<?>                   // Flexible typing (e.g., for errors)
```

### Core Fields

```java
private T data                                          // Main resource data
private Iterable<? extends Error> errors               // Error objects
private Links links                                     // Top-level links
private Map<String, Object> meta                       // Top-level meta
private JsonApi jsonApi                                 // JSON API version object
private JsonNode responseJSONNode                       // Raw JSON for advanced use cases
private ObjectMapper deserializer                      // For meta type conversion
```

### Constructor Options

```java
// Data-focused constructors
JSONAPIDocument(T data)
JSONAPIDocument(T data, ObjectMapper deserializer)
JSONAPIDocument(T data, JsonNode jsonNode, ObjectMapper deserializer)
JSONAPIDocument(T data, Links links, Map<String, Object> meta)
JSONAPIDocument(T data, Links links, Map<String, Object> meta, ObjectMapper deserializer)

// Error-focused constructors
JSONAPIDocument(Iterable<? extends Error> errors)
JSONAPIDocument(Error error)

// Factory methods
static JSONAPIDocument<?> createErrorDocument(Iterable<? extends Error> errors)

// Default constructor
JSONAPIDocument()
```

### Core Methods

#### Data Access
```java
@Nullable T get()                                       // Get main resource data
@Nullable Iterable<? extends Error> getErrors()        // Get error objects
JsonNode getResponseJSONNode()                          // Get raw JSON response
```

#### Meta Management
```java
@Nullable Map<String, ?> getMeta()                     // Get meta as Map
<M> M getMeta(Class<?> metaType)                       // Get typed meta object
void setMeta(Map<String, ?> meta)                      // Set meta data
void addMeta(String key, Object value)                 // Add single meta entry
```

#### Links Management
```java
@Nullable Links getLinks()                             // Get links object
void setLinks(Links links)                             // Set links
void addLink(String linkName, Link link)               // Add single named link
```

#### JSON API Object
```java
JsonApi getJsonApi()                                   // Get JSON API object
void setJsonApi(JsonApi jsonApi)                       // Set JSON API object
```

### Usage Patterns

#### Successful Response
```java
// Create document with data
Article article = new Article();
JSONAPIDocument<Article> document = new JSONAPIDocument<>(article);

// Add meta information
document.addMeta("total", 150);
document.addMeta("page", 1);

// Add links
document.addLink("self", new Link("https://api.example.com/articles/1"));
document.addLink("related", new Link("https://api.example.com/articles/1/comments"));
```

#### Error Response
```java
// Create error document
Error error = new Error();
error.setStatus("404");
error.setTitle("Resource Not Found");
JSONAPIDocument<?> errorDoc = JSONAPIDocument.createErrorDocument(Arrays.asList(error));
```

#### Collection Response
```java
// Create collection document
List<Article> articles = Arrays.asList(article1, article2);
JSONAPIDocument<List<Article>> collectionDoc = new JSONAPIDocument<>(articles);
```

---

## Supporting Classes

### ID Handlers

The library provides a strategy pattern for handling different ID types:

#### ResourceIdHandler Interface (`ResourceIdHandler.java:10`)
```java
public interface ResourceIdHandler {
    String asString(Object idValue);        // Convert ID to string
    Object fromString(String stringValue);   // Parse string to ID type
}
```

#### Built-in Implementations
- **StringIdHandler** (`StringIdHandler.java:8`) - Default for String IDs
- **IntegerIdHandler** (`IntegerIdHandler.java:8`) - For Integer IDs
- **LongIdHandler** (`LongIdHandler.java:8`) - For Long IDs

### Configuration Classes

#### SerializationSettings (`SerializationSettings.java:12`)
Builder pattern for per-request serialization configuration:

```java
SerializationSettings settings = SerializationSettings.builder()
    .includeRelationship("author", "comments")    // Include specific relationships
    .excludeRelationship("internal")              // Exclude relationships
    .serializeLinks(true)                         // Control link serialization
    .serializeMeta(false)                         // Control meta serialization
    .build();

byte[] json = converter.writeDocument(document, settings);
```

#### Features Enums
- **DeserializationFeature** (`DeserializationFeature.java:9`) - Control deserialization behavior
- **SerializationFeature** (`SerializationFeature.java:9`) - Control serialization behavior

### Utility Classes

#### ValidationUtils (`ValidationUtils.java:17`)
Validates JSON API document structure against specification:
```java
static void ensureValidDocument(ObjectMapper mapper, JsonNode rootNode)
static void ensurePrimaryDataValidObjectOrNull(JsonNode dataNode)
static void ensurePrimaryDataValidArray(JsonNode dataNode)
static void ensureValidResourceObjectArray(JsonNode included)
static boolean isResourceIdentifierObject(JsonNode node)
```

#### ReflectionUtils (`ReflectionUtils.java:15`)
Helper methods for annotation processing and reflection:
```java
static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation, boolean inherited)
static String getTypeName(Class<?> clazz)           // Get @Type value
static Class<?> getFieldType(Field relationshipField) // Get relationship target type
```

#### ErrorUtils (`ErrorUtils.java:15`)
Utility for parsing error responses:
```java
static Errors parseError(ResponseBody errorBody)    // Parse ResponseBody to Errors
static Errors parseError(JsonNode errorNode)        // Parse JsonNode to Errors
```

---

## Configuration and Features

### Global Configuration

```java
ResourceConverter converter = new ResourceConverter(Article.class, Person.class);

// Configure deserialization
converter.enableDeserializationOption(DeserializationFeature.REQUIRE_RESOURCE_ID);
converter.disableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);

// Configure serialization
converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);
converter.enableSerializationOption(SerializationFeature.INCLUDE_META);
```

### Custom ObjectMapper Integration

```java
ObjectMapper customMapper = new ObjectMapper();
customMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
customMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

ResourceConverter converter = new ResourceConverter(customMapper, Article.class);
```

### Base URL Configuration

```java
ResourceConverter converter = new ResourceConverter("https://api.example.com", Article.class);

// This enables automatic link generation for resources with @Type(path="...")
```

---

## Thread Safety and Performance

### Thread Safety
- **ResourceConverter**: Thread-safe, can be shared
- **ConverterConfiguration**: Immutable after initialization
- **ResourceCache**: ThreadLocal, isolated per thread
- **JSONAPIDocument**: Not thread-safe, use per-request

### Performance Tips
1. **Reuse ResourceConverter instances** - Expensive to create
2. **Pre-register all types** - Avoid runtime registration
3. **Use custom ObjectMapper** - Configure for your performance needs
4. **Leverage resource caching** - Automatic optimization for circular refs

### Memory Management
- ResourceCache automatically clears after each operation
- Consider using SerializationSettings to control included relationships
- Jackson streaming can be used for very large responses

---

*Source locations: Core classes are located in `src/main/java/com/github/jasminb/jsonapi/`*