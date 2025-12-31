# JSON API Converter - Complete Documentation

## Overview

The JSON API Converter is a Java library that provides comprehensive conversion between JSON API specification documents and Java POJOs. This library implements the [JSON API v1.0+ specification](https://jsonapi.org/) and provides features for both serialization and deserialization of JSON API documents.

## Architecture Overview

The library is organized into 5 main modules:

### 1. Core Converter Module (`com.github.jasminb.jsonapi`)
- **20 classes** - Main conversion engine and document handling
- Entry point: `ResourceConverter.java:44`
- Key classes: `ResourceConverter`, `ConverterConfiguration`, `JSONAPIDocument`

### 2. Annotations Module (`com.github.jasminb.jsonapi.annotations`)
- **8 annotations** - Public API for annotating domain model classes
- Main annotations: `@Type`, `@Id`, `@Relationship`, `@Meta`, `@Links`

### 3. Exception Handling Module (`com.github.jasminb.jsonapi.exceptions`)
- **4 exceptions** - Custom exceptions for various error conditions
- Key exception: `ResourceParseException` (wraps JSON API error responses)

### 4. Error Models Module (`com.github.jasminb.jsonapi.models.errors`)
- **4 classes** - Models representing JSON API error specification
- Models: `Error`, `Errors`, `Source`, `Links`

### 5. Retrofit Integration Module (`com.github.jasminb.jsonapi.retrofit`)
- **5 classes** - Seamless Retrofit framework integration
- Factory: `JSONAPIConverterFactory` for creating converters

## Key Features

### ✅ Complete JSON API Spec Compliance
- Resource objects with `type`, `id`/`lid`, `attributes`
- Relationships with data, links, and meta
- Top-level `data`, `included`, `meta`, `links`, `errors`, `jsonapi` objects
- Resource linkage and compound documents

### ✅ Flexible ID Handling
- String, Integer, Long ID types via `ResourceIdHandler` strategy pattern
- Local ID (`lid`) support for client-generated identifiers
- Configurable ID handlers per resource type

### ✅ Relationship Management
- Lazy loading via `RelationshipResolver` interface
- Bidirectional relationships with circular reference prevention
- Configurable serialization/deserialization per relationship
- Support for `self` and `related` link types

### ✅ Advanced Configuration
- Jackson ObjectMapper integration with custom naming strategies
- Per-request serialization settings override global configuration
- Feature flags for deserialization/serialization behavior
- Thread-safe resource caching

### ✅ Framework Integration
- First-class Retrofit support with factory pattern
- Jackson annotation compatibility
- Spring Boot friendly

## Quick Start

```java
// 1. Define resource classes with annotations
@Type("articles")
public class Article {
    @Id
    private String id;

    private String title;

    @Relationship("author")
    private Person author;
}

// 2. Create converter with registered types
ResourceConverter converter = new ResourceConverter(Article.class, Person.class);

// 3. Deserialize JSON API document
JSONAPIDocument<Article> document = converter.readDocument(jsonBytes, Article.class);
Article article = document.get();

// 4. Serialize back to JSON API
byte[] json = converter.writeDocument(new JSONAPIDocument<>(article));
```

## Documentation Structure

| Module | File | Description |
|--------|------|-------------|
| Core | [core-converter.md](core-converter.md) | Main conversion engine, configuration, document wrapper |
| Annotations | [annotations.md](annotations.md) | All annotations for marking up domain models |
| Exceptions | [exceptions.md](exceptions.md) | Error handling and custom exceptions |
| Error Models | [error-models.md](error-models.md) | JSON API error object models |
| Retrofit | [retrofit-integration.md](retrofit-integration.md) | Retrofit framework integration |

## Source Code Organization

```
src/main/java/com/github/jasminb/jsonapi/
├── ResourceConverter.java              # Main converter class
├── ConverterConfiguration.java         # Type registration & field mapping
├── JSONAPIDocument.java               # Document wrapper
├── annotations/
│   ├── Type.java                      # @Type - resource type declaration
│   ├── Id.java                        # @Id - resource identifier
│   ├── LocalId.java                   # @LocalId - client-generated ID
│   ├── Relationship.java              # @Relationship - relationship configuration
│   ├── Meta.java                      # @Meta - meta data fields
│   ├── Links.java                     # @Links - link fields
│   ├── RelationshipMeta.java          # @RelationshipMeta - relationship meta
│   └── RelationshipLinks.java         # @RelationshipLinks - relationship links
├── exceptions/
│   ├── DocumentSerializationException.java
│   ├── InvalidJsonApiResourceException.java
│   ├── ResourceParseException.java    # Wraps JSON API errors
│   └── UnregisteredTypeException.java
├── models/errors/
│   ├── Error.java                     # Single JSON API error
│   ├── Errors.java                    # Error collection wrapper
│   ├── Source.java                    # Error source pointer
│   └── Links.java                     # Error-specific links
└── retrofit/
    ├── JSONAPIConverterFactory.java   # Retrofit converter factory
    ├── JSONAPIResponseBodyConverter.java
    ├── JSONAPIDocumentResponseBodyConverter.java
    ├── JSONAPIRequestBodyConverter.java
    └── RetrofitType.java
```

## Configuration Options

### Deserialization Features
- `REQUIRE_RESOURCE_ID` - Enforce non-empty resource IDs
- `REQUIRE_LOCAL_RESOURCE_ID` - Enforce non-empty local IDs
- `ALLOW_UNKNOWN_INCLUSIONS` - Handle unknown types in included section
- `ALLOW_UNKNOWN_TYPE_IN_RELATIONSHIP` - Handle unknown relationship types

### Serialization Features
- `INCLUDE_RELATIONSHIP_ATTRIBUTES` - Include relationship objects in `included`
- `INCLUDE_META` - Include meta information
- `INCLUDE_LINKS` - Include link objects
- `INCLUDE_ID` - Include resource IDs
- `INCLUDE_LOCAL_ID` - Include local IDs
- `INCLUDE_JSONAPI_OBJECT` - Include top-level JSON API version object

## Thread Safety

The library is designed to be thread-safe:
- `ResourceConverter` can be shared across threads
- `ResourceCache` uses `ThreadLocal` storage
- `ConverterConfiguration` is immutable after initialization

## Performance Considerations

- Use shared `ResourceConverter` instances when possible
- Resource caching prevents infinite loops in circular relationships
- Jackson `ObjectMapper` can be customized for performance
- Lazy relationship loading reduces memory usage

---

*Generated documentation for JSON API Converter v1.x*