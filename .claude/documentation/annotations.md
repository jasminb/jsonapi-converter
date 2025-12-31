# Annotations Module

## Overview

The annotations module provides the public API for marking up domain model classes to work with the JSON API Converter. This module contains 8 annotations that define how Java classes and fields map to JSON API specification elements.

**Location**: `src/main/java/com/github/jasminb/jsonapi/annotations/`

---

## Resource Definition Annotations

### @Type (`Type.java:15`)

**Purpose**: Marks a Java class as a JSON API resource type.

**Target**: Class level (`ElementType.TYPE`)

**Attributes**:
- `value()` (required) - The JSON API resource type name
- `path()` (optional) - URL path template for generating self links

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Type {
    String value();                    // Resource type name
    String path() default "";          // Resource path for link generation
}
```

#### Usage Examples

```java
// Basic resource type
@Type("articles")
public class Article {
    // ...
}

// With path for automatic link generation
@Type(value = "articles", path = "articles/{id}")
public class Article {
    @Id
    private String id;
    // Generates self link: https://api.example.com/articles/123
}

// Complex path with nested resources
@Type(value = "comments", path = "articles/{articleId}/comments/{id}")
public class Comment {
    @Id
    private String id;
    private String articleId;
}
```

#### Rules and Constraints
- **Required on all resource classes**: Every JSON API resource must have @Type
- **Unique type names**: Each type value should be unique across your domain
- **Path placeholders**: Use `{id}` placeholder in path for ID substitution
- **URL encoding**: Paths will be properly URL encoded

---

### @Id (`Id.java:16`)

**Purpose**: Marks a field as the resource identifier.

**Target**: Field level (`ElementType.FIELD`)

**Attributes**:
- `value()` (optional) - ID handler class, defaults to `StringIdHandler`

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    Class<? extends ResourceIdHandler> value() default StringIdHandler.class;
}
```

#### Usage Examples

```java
// String ID (default)
@Type("articles")
public class Article {
    @Id
    private String id;
}

// Integer ID
@Type("articles")
public class Article {
    @Id(IntegerIdHandler.class)
    private Integer id;
}

// Long ID
@Type("articles")
public class Article {
    @Id(LongIdHandler.class)
    private Long id;
}

// Custom ID handler
public class UuidIdHandler implements ResourceIdHandler {
    public String asString(Object idValue) {
        return idValue != null ? idValue.toString() : null;
    }

    public Object fromString(String stringValue) {
        return stringValue != null ? UUID.fromString(stringValue) : null;
    }
}

@Type("articles")
public class Article {
    @Id(UuidIdHandler.class)
    private UUID id;
}
```

#### Rules and Constraints
- **Required**: Every resource class must have exactly one @Id field
- **Unique per class**: Only one @Id field allowed per class
- **Handler requirement**: ID handler must have a no-argument constructor
- **Null handling**: ID handlers should handle null values gracefully

#### Built-in ID Handlers
- `StringIdHandler` - Default, handles String IDs
- `IntegerIdHandler` - Handles Integer IDs
- `LongIdHandler` - Handles Long IDs

---

### @LocalId (`LocalId.java:15`)

**Purpose**: Marks a field as the local identifier (lid) for client-generated identifiers.

**Target**: Field level (`ElementType.FIELD`)

**Attributes**:
- `value()` (optional) - ID handler class, defaults to `StringIdHandler`

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalId {
    Class<? extends ResourceIdHandler> value() default StringIdHandler.class;
}
```

#### Usage Examples

```java
// Client-generated string ID
@Type("articles")
public class Article {
    @Id
    private String id;           // Server-generated

    @LocalId
    private String clientId;     // Client-generated for temporary use
}

// During creation workflow
@Type("articles")
public class Article {
    @Id
    private String id;           // Will be null during creation

    @LocalId
    private String tempId;       // Used by client to track during creation
}
```

#### Rules and Constraints
- **Optional**: Not required, unlike @Id
- **Unique per class**: Only one @LocalId field allowed per class
- **Mutual exclusion**: A resource cannot have both 'id' and 'lid' in JSON
- **Temporary usage**: Typically used during resource creation workflows

---

## Field Annotations

### @Meta (`Meta.java:15`)

**Purpose**: Marks a field to hold resource-level meta information.

**Target**: Field level (`ElementType.FIELD`)

**No attributes**: Simple marker annotation

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Meta {
}
```

#### Usage Examples

```java
// Generic meta as Map
@Type("articles")
public class Article {
    @Id private String id;
    private String title;

    @Meta
    private Map<String, Object> meta;
}

// Typed meta object
public class ArticleMeta {
    private Integer viewCount;
    private LocalDateTime lastModified;
    private List<String> tags;
    // getters/setters...
}

@Type("articles")
public class Article {
    @Id private String id;
    private String title;

    @Meta
    private ArticleMeta metadata;
}

// JSON API document example:
{
  "data": {
    "type": "articles",
    "id": "1",
    "attributes": { "title": "Hello World" },
    "meta": {
      "viewCount": 42,
      "lastModified": "2023-01-15T10:30:00Z",
      "tags": ["tutorial", "beginner"]
    }
  }
}
```

#### Rules and Constraints
- **Optional**: Resources don't need meta fields
- **Single per class**: Only one @Meta field allowed per class
- **Flexible typing**: Can be `Map<String, Object>` or custom POJO
- **Jackson serialization**: Uses configured ObjectMapper for conversion

---

### @Links (`Links.java:15`)

**Purpose**: Marks a field to hold resource-level link information.

**Target**: Field level (`ElementType.FIELD`)

**No attributes**: Simple marker annotation

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Links {
}
```

#### Usage Examples

```java
@Type("articles")
public class Article {
    @Id private String id;
    private String title;

    @Links
    private com.github.jasminb.jsonapi.Links links;
}

// Programmatic link management
Article article = new Article();
article.setId("123");

Links links = new Links();
links.setSelf(new Link("https://api.example.com/articles/123"));
links.setRelated(new Link("https://api.example.com/articles/123/comments"));
article.setLinks(links);

// JSON API output:
{
  "data": {
    "type": "articles",
    "id": "123",
    "attributes": { "title": "Hello" },
    "links": {
      "self": "https://api.example.com/articles/123",
      "related": "https://api.example.com/articles/123/comments"
    }
  }
}
```

#### Rules and Constraints
- **Optional**: Resources don't need link fields
- **Single per class**: Only one @Links field allowed per class
- **Type requirement**: Must be `com.github.jasminb.jsonapi.Links` or subclass
- **Link objects**: Can be string URLs or Link objects with href + meta

---

## Relationship Annotations

### @Relationship (`Relationship.java:18`)

**Purpose**: Marks a field as a relationship with extensive configuration options.

**Target**: Field level (`ElementType.FIELD`)

**Attributes**:
- `value()` (required) - The relationship name in JSON
- `resolve()` (default: false) - Enable automatic resolution via HTTP
- `serialise()` (default: true) - Include relationship in serialization
- `serialiseData()` (default: true) - Include relationship data section
- `relType()` (default: `RelType.SELF`) - Link type for resolution
- `path()` (default: "") - Path template for self link
- `relatedPath()` (default: "") - Path template for related link

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Relationship {
    String value();                             // Relationship name
    boolean resolve() default false;            // Auto HTTP resolution
    boolean serialise() default true;           // Include in output
    boolean serialiseData() default true;       // Include data section
    RelType relType() default RelType.SELF;     // Resolution link type
    String path() default "";                   // Self link path
    String relatedPath() default "";            // Related link path
}
```

#### Usage Examples

##### Basic Relationship
```java
@Type("articles")
public class Article {
    @Id private String id;

    @Relationship("author")
    private Person author;

    @Relationship("comments")
    private List<Comment> comments;
}
```

##### Relationship with Link Generation
```java
@Type("articles")
public class Article {
    @Id private String id;

    @Relationship(value = "author", path = "author", relatedPath = "author")
    private Person author;

    @Relationship(value = "comments", path = "relationships/comments", relatedPath = "comments")
    private List<Comment> comments;
}

// Generates links like:
// "self": "https://api.example.com/articles/123/relationships/comments"
// "related": "https://api.example.com/articles/123/comments"
```

##### Lazy Loading with Resolution
```java
@Type("articles")
public class Article {
    @Id private String id;

    // Automatically resolve author via HTTP call when accessed
    @Relationship(value = "author", resolve = true, relType = RelType.RELATED)
    private Person author;
}

// Requires RelationshipResolver to be configured:
converter.setGlobalResolver(url -> {
    // Make HTTP call and return JSON bytes
    return restTemplate.getForObject(url, byte[].class);
});
```

##### Read-Only Relationships
```java
@Type("articles")
public class Article {
    @Id private String id;

    // Include in deserialization but not serialization
    @Relationship(value = "internal", serialise = false)
    private InternalData internal;

    // Include relationship but not the data (links/meta only)
    @Relationship(value = "stats", serialiseData = false)
    private ArticleStats stats;
}
```

#### Relationship JSON Structure

```json
{
  "data": {
    "type": "articles",
    "id": "1",
    "relationships": {
      "author": {
        "data": { "type": "people", "id": "9" },
        "links": {
          "self": "https://api.example.com/articles/1/relationships/author",
          "related": "https://api.example.com/articles/1/author"
        }
      },
      "comments": {
        "data": [
          { "type": "comments", "id": "5" },
          { "type": "comments", "id": "12" }
        ],
        "links": {
          "self": "https://api.example.com/articles/1/relationships/comments",
          "related": "https://api.example.com/articles/1/comments"
        }
      }
    }
  },
  "included": [
    {
      "type": "people",
      "id": "9",
      "attributes": { "firstName": "John", "lastName": "Doe" }
    }
  ]
}
```

#### Rules and Constraints
- **Type registration**: Relationship target types must be registered with converter
- **Resolution requirements**: If `resolve = true`, must have `relType` and configured resolver
- **Collection support**: Supports both single objects and Collections/Lists
- **Circular references**: Automatic cycle detection prevents infinite loops
- **Polymorphism**: Relationship fields can be interfaces with multiple implementations

---

### @RelationshipMeta (`RelationshipMeta.java:15`)

**Purpose**: Marks a field to hold meta data for a specific relationship.

**Target**: Field level (`ElementType.FIELD`)

**Attributes**:
- `value()` (required) - The name of the relationship this meta belongs to

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RelationshipMeta {
    String value();    // Relationship name
}
```

#### Usage Examples

```java
public class CommentMeta {
    private boolean verified;
    private String moderationStatus;
    // getters/setters...
}

@Type("articles")
public class Article {
    @Id private String id;

    @Relationship("comments")
    private List<Comment> comments;

    @RelationshipMeta("comments")
    private CommentMeta commentsMeta;
}

// JSON output:
{
  "data": {
    "type": "articles",
    "id": "1",
    "relationships": {
      "comments": {
        "data": [{"type": "comments", "id": "5"}],
        "meta": {
          "verified": true,
          "moderationStatus": "approved"
        }
      }
    }
  }
}
```

#### Rules and Constraints
- **Relationship coupling**: Must reference an existing @Relationship field
- **Name matching**: The `value()` must match a @Relationship's `value()`
- **Flexible typing**: Can be Map<String, Object> or custom POJO
- **Optional**: Relationships don't require meta information

---

### @RelationshipLinks (`RelationshipLinks.java:15`)

**Purpose**: Marks a field to hold links for a specific relationship.

**Target**: Field level (`ElementType.FIELD`)

**Attributes**:
- `value()` (required) - The name of the relationship these links belong to

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RelationshipLinks {
    String value();    // Relationship name
}
```

#### Usage Examples

```java
@Type("articles")
public class Article {
    @Id private String id;

    @Relationship("comments")
    private List<Comment> comments;

    @RelationshipLinks("comments")
    private Links commentsLinks;
}

// Programmatic setup:
Article article = new Article();

Links commentsLinks = new Links();
commentsLinks.setSelf(new Link("https://api.example.com/articles/1/relationships/comments"));
commentsLinks.setRelated(new Link("https://api.example.com/articles/1/comments"));
commentsLinks.addLink("first", new Link("https://api.example.com/articles/1/comments?page=1"));
commentsLinks.addLink("next", new Link("https://api.example.com/articles/1/comments?page=2"));

article.setCommentsLinks(commentsLinks);

// JSON output:
{
  "data": {
    "type": "articles",
    "id": "1",
    "relationships": {
      "comments": {
        "data": [{"type": "comments", "id": "5"}],
        "links": {
          "self": "https://api.example.com/articles/1/relationships/comments",
          "related": "https://api.example.com/articles/1/comments",
          "first": "https://api.example.com/articles/1/comments?page=1",
          "next": "https://api.example.com/articles/1/comments?page=2"
        }
      }
    }
  }
}
```

#### Rules and Constraints
- **Relationship coupling**: Must reference an existing @Relationship field
- **Name matching**: The `value()` must match a @Relationship's `value()`
- **Type requirement**: Must be `com.github.jasminb.jsonapi.Links` type
- **Link combination**: Combines with auto-generated links from @Relationship path attributes

---

## Annotation Processing Rules

### General Rules
1. **Retention**: All annotations use `RetentionPolicy.RUNTIME` for runtime processing
2. **Inheritance**: Annotation scanning includes inherited fields (`inherited = true`)
3. **Accessibility**: Annotated fields are automatically made accessible via reflection
4. **Validation**: Invalid annotation combinations throw `IllegalArgumentException`

### Class-Level Validation
- Exactly one `@Type` annotation required per resource class
- Exactly one `@Id` annotated field required per resource class
- At most one `@LocalId` annotated field per resource class
- At most one `@Meta` annotated field per resource class
- At most one `@Links` annotated field per resource class

### Field-Level Validation
- `@RelationshipMeta` and `@RelationshipLinks` must reference valid relationship names
- ID handler classes must have no-argument constructors
- Links fields must be of type `Links` or subclass
- Relationship fields with `resolve=true` must specify `relType`

### Processing Order
1. **Type Registration**: @Type annotation processed first
2. **Field Discovery**: All annotated fields found via reflection
3. **Handler Instantiation**: ID handlers created for @Id/@LocalId fields
4. **Relationship Analysis**: Target types extracted and auto-registered
5. **Validation**: All constraints verified
6. **Caching**: Field mappings cached for performance

---

## Complete Example

Here's a comprehensive example showing all annotations in use:

```java
// Author resource
@Type(value = "people", path = "people/{id}")
public class Person {
    @Id
    private String id;

    private String firstName;
    private String lastName;

    @Meta
    private Map<String, Object> meta;

    @Links
    private Links links;
}

// Comment resource
@Type("comments")
public class Comment {
    @Id(LongIdHandler.class)
    private Long id;

    private String body;
    private LocalDateTime createdAt;

    @Relationship("author")
    private Person author;
}

// Main article resource with comprehensive annotations
@Type(value = "articles", path = "articles/{id}")
public class Article {
    @Id
    private String id;

    @LocalId  // For client-generated IDs during creation
    private String clientId;

    private String title;
    private String content;

    @Meta
    private ArticleMeta metadata;

    @Links
    private Links links;

    // Simple relationship
    @Relationship("author")
    private Person author;

    // Collection relationship with auto-resolution
    @Relationship(
        value = "comments",
        resolve = true,
        relType = RelType.RELATED,
        path = "relationships/comments",
        relatedPath = "comments"
    )
    private List<Comment> comments;

    // Relationship meta
    @RelationshipMeta("comments")
    private CommentCollectionMeta commentsMeta;

    // Relationship links
    @RelationshipLinks("comments")
    private Links commentsLinks;

    // Read-only relationship (not serialized)
    @Relationship(value = "internal-stats", serialise = false)
    private ArticleStats internalStats;
}

// Supporting classes
public class ArticleMeta {
    private Integer viewCount;
    private List<String> tags;
    private LocalDateTime publishedAt;
}

public class CommentCollectionMeta {
    private Integer totalCount;
    private String moderationStatus;
}
```

This example demonstrates:
- Multiple ID types and handlers
- Resource-level meta and links
- Various relationship configurations
- Relationship-specific meta and links
- Mix of serialization strategies

---

## Migration and Best Practices

### Migration from Legacy Annotations
If migrating from other JSON API libraries:

1. **Replace type annotations**: Map existing type declarations to @Type
2. **Update ID annotations**: Replace ID annotations with @Id + appropriate handler
3. **Relationship mapping**: Map relationship annotations to @Relationship with proper configuration
4. **Meta/Links consolidation**: Combine scattered meta/link annotations

### Best Practices

#### Naming Conventions
```java
// Use kebab-case for JSON API type names
@Type("blog-posts")          // Good
@Type("BlogPosts")           // Avoid

// Use camelCase for relationship names
@Relationship("authorProfile")   // Good
@Relationship("author_profile")  // Avoid
```

#### Performance Optimization
```java
// Pre-register all types to avoid runtime registration
ResourceConverter converter = new ResourceConverter(
    Article.class, Person.class, Comment.class, Tag.class
);

// Use typed meta objects instead of Maps when possible
@Meta
private ArticleMeta metadata;    // Good - type safe, efficient

@Meta
private Map<String, Object> meta; // Ok - flexible but less efficient
```

#### Relationship Design
```java
// Prefer lazy loading for expensive relationships
@Relationship(value = "analytics", resolve = true)
private ArticleAnalytics analytics;

// Control serialization granularly
@Relationship(value = "draft", serialise = false)  // Internal only
private ArticleDraft draft;

@Relationship(value = "refs", serialiseData = false) // Links only
private List<Reference> references;
```

---

*Source locations: All annotation classes are in `src/main/java/com/github/jasminb/jsonapi/annotations/`*