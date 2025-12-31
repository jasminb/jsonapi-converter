# Migration Guide: JSON Library Abstraction

## Overview

Starting with version 1.x, JSON API Converter supports multiple JSON processing libraries through an abstraction layer. This guide helps you migrate from direct Jackson usage to the new abstraction layer, or switch between JSON libraries.

## Table of Contents

- [Quick Start](#quick-start)
- [Zero-Configuration Migration](#zero-configuration-migration)
- [Migrating Custom ObjectMapper Configurations](#migrating-custom-objectmapper-configurations)
- [Switching JSON Libraries](#switching-json-libraries)
- [Advanced Migration Scenarios](#advanced-migration-scenarios)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

### ✅ No Changes Required for Most Users

If you're using `ResourceConverter` with default settings, **no code changes are needed**. The library automatically detects Jackson on your classpath and continues working exactly as before.

```java
// This code continues to work unchanged
ResourceConverter converter = new ResourceConverter(Article.class, Person.class);
JSONAPIDocument<Article> doc = converter.readDocument(jsonBytes, Article.class);
```

**100% backward compatibility guaranteed.**

---

## Zero-Configuration Migration

### Scenario 1: Using Default Jackson

**Before (v0.x):**
```java
ResourceConverter converter = new ResourceConverter(Article.class);
```

**After (v1.x):**
```java
// Same code - Jackson auto-detected
ResourceConverter converter = new ResourceConverter(Article.class);
```

**What happens:** Service discovery automatically detects Jackson on your classpath and uses it.

### Scenario 2: Switching to Gson

**Step 1:** Update your `pom.xml`:
```xml
<!-- Remove or keep Jackson as 'provided' -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <scope>provided</scope>
</dependency>

<!-- Add Gson -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

**Step 2:** That's it! No code changes needed.
```java
// Same code - Gson auto-detected
ResourceConverter converter = new ResourceConverter(Article.class);
```

### Scenario 3: Switching to JSON-B

**Step 1:** Update your `pom.xml`:
```xml
<!-- Add JSON-B API and implementation -->
<dependency>
    <groupId>jakarta.json.bind</groupId>
    <artifactId>jakarta.json.bind-api</artifactId>
    <version>3.0.0</version>
</dependency>
<dependency>
    <groupId>org.eclipse</groupId>
    <artifactId>yasson</artifactId>
    <version>3.0.3</version>
    <runtime>runtime</runtime>
</dependency>
```

**Step 2:** That's it! No code changes needed.
```java
// Same code - JSON-B auto-detected
ResourceConverter converter = new ResourceConverter(Article.class);
```

---

## Migrating Custom ObjectMapper Configurations

### Custom ObjectMapper (Pre-Abstraction)

**Before (v0.x):**
```java
ObjectMapper mapper = new ObjectMapper();
mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

ResourceConverter converter = new ResourceConverter(mapper, Article.class);
```

**After (v1.x) - Option 1: Continue Using Jackson ObjectMapper**
```java
// Jackson-specific configuration still supported
ObjectMapper mapper = new ObjectMapper();
mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

JacksonJsonProcessor processor = new JacksonJsonProcessor(mapper);
ResourceConverter converter = new ResourceConverter(processor, Article.class);
```

**After (v1.x) - Option 2: Use Abstracted Configuration**
```java
// Library-agnostic approach (works with any JSON library)
JsonProcessorConfig config = JsonProcessorConfig.builder()
    .fieldNamingStrategy(FieldNamingStrategy.SNAKE_CASE)
    .serializeNulls(false)
    .build();

JsonProcessor processor = JsonProcessorFactory.createWithConfig(config);
ResourceConverter converter = new ResourceConverter(processor, Article.class);
```

### Common Configuration Mappings

| Jackson Configuration | Abstracted Configuration |
|----------------------|--------------------------|
| `JsonInclude.Include.NON_NULL` | `config.serializeNulls(false)` |
| `PropertyNamingStrategies.SNAKE_CASE` | `FieldNamingStrategy.SNAKE_CASE` |
| `PropertyNamingStrategies.KEBAB_CASE` | `FieldNamingStrategy.KEBAB_CASE` |
| `FAIL_ON_UNKNOWN_PROPERTIES` | Handled automatically by processors |

### Advanced ObjectMapper Features

If you use advanced Jackson features (custom serializers, modules, mixins), you have two options:

**Option 1: Stay with Jackson** (Recommended for complex configurations)
```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());
mapper.addMixIn(MyClass.class, MyMixin.class);
mapper.registerModule(new CustomModule());

JacksonJsonProcessor processor = new JacksonJsonProcessor(mapper);
ResourceConverter converter = new ResourceConverter(processor, Article.class);
```

**Option 2: Implement Custom JsonProcessor**
```java
public class CustomJsonProcessor implements JsonProcessor {
    // Implement custom behavior for your specific needs
}

ResourceConverter converter = new ResourceConverter(new CustomJsonProcessor(), Article.class);
```

---

## Switching JSON Libraries

### From Jackson to Gson

**Step 1:** Update Maven dependencies (see Zero-Configuration Migration above)

**Step 2:** Migrate configuration if you had custom ObjectMapper:

```java
// Jackson approach
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
JacksonJsonProcessor processor = new JacksonJsonProcessor(mapper);

// Gson approach
Gson gson = new GsonBuilder()
    .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create();
GsonJsonProcessor processor = new GsonJsonProcessor(gson);

ResourceConverter converter = new ResourceConverter(processor, Article.class);
```

**Step 3:** Update annotations if needed:

```java
// Jackson annotations
@JsonProperty("custom_name")
private String customName;

// Gson annotations
@SerializedName("custom_name")
private String customName;
```

### From Jackson to JSON-B

```java
// Jackson approach
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

// JSON-B approach
Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
    .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES));

JsonBJsonProcessor processor = new JsonBJsonProcessor(jsonb);
ResourceConverter converter = new ResourceConverter(processor, Article.class);
```

### From Gson to JSON-B

```java
// Gson approach
Gson gson = new GsonBuilder()
    .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create();

// JSON-B approach (similar configuration)
Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
    .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES));

JsonBJsonProcessor processor = new JsonBJsonProcessor(jsonb);
ResourceConverter converter = new ResourceConverter(processor, Article.class);
```

---

## Advanced Migration Scenarios

### Scenario: Multiple ResourceConverter Instances with Different JSON Libraries

```java
// Use Jackson for one converter
JacksonJsonProcessor jacksonProcessor = new JacksonJsonProcessor();
ResourceConverter jacksonConverter = new ResourceConverter(jacksonProcessor, Article.class);

// Use Gson for another converter
GsonJsonProcessor gsonProcessor = new GsonJsonProcessor();
ResourceConverter gsonConverter = new ResourceConverter(gsonProcessor, Person.class);

// Both can coexist in the same application
```

### Scenario: Retrofit Integration

**Before (v0.x):**
```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addConverterFactory(new JSONAPIConverterFactory(Article.class, Person.class))
    .build();
```

**After (v1.x) - Auto-detection:**
```java
// Same code - JSON library auto-detected
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addConverterFactory(new JSONAPIConverterFactory(Article.class, Person.class))
    .build();
```

**After (v1.x) - Explicit JSON library:**
```java
// Explicitly use Gson
ResourceConverter converter = new ResourceConverter(
    new GsonJsonProcessor(),
    Article.class, Person.class
);

Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addConverterFactory(new JSONAPIConverterFactory(converter))
    .build();
```

### Scenario: Thread-Safe Shared Converters

```java
// Create a shared, thread-safe JSON processor
JsonProcessor processor = JsonProcessorFactory.create(); // Auto-detects library

// Share across multiple converters (thread-safe)
ResourceConverter articleConverter = new ResourceConverter(processor, Article.class);
ResourceConverter personConverter = new ResourceConverter(processor, Person.class);

// All converters can be safely used across threads
```

---

## Troubleshooting

### Issue: "No JSON processor found on classpath"

**Cause:** No supported JSON library is available.

**Solution:** Add one of the supported libraries:
```xml
<!-- Option 1: Jackson -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.0</version>
</dependency>

<!-- Option 2: Gson -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- Option 3: JSON-B -->
<dependency>
    <groupId>jakarta.json.bind</groupId>
    <artifactId>jakarta.json.bind-api</artifactId>
    <version>3.0.0</version>
</dependency>
<dependency>
    <groupId>org.eclipse</groupId>
    <artifactId>yasson</artifactId>
    <version>3.0.3</version>
</dependency>
```

### Issue: Wrong JSON library being selected

**Cause:** Multiple libraries on classpath with unexpected priority.

**Solution:** Explicitly specify the processor:
```java
// Force Jackson
JsonProcessor processor = new JacksonJsonProcessor();
ResourceConverter converter = new ResourceConverter(processor, Article.class);

// Force Gson
JsonProcessor processor = new GsonJsonProcessor();
ResourceConverter converter = new ResourceConverter(processor, Article.class);

// Force JSON-B
JsonProcessor processor = new JsonBJsonProcessor();
ResourceConverter converter = new ResourceConverter(processor, Article.class);
```

### Issue: "ObjectMapper is no longer accessible"

**Cause:** Trying to access internal ObjectMapper after migrating to abstraction.

**Solution:** Use `JacksonJsonProcessor` for Jackson-specific features:
```java
// Get access to underlying ObjectMapper
JacksonJsonProcessor jacksonProcessor = new JacksonJsonProcessor();
ObjectMapper mapper = jacksonProcessor.getObjectMapper();
// Customize mapper as needed
mapper.registerModule(new JavaTimeModule());

ResourceConverter converter = new ResourceConverter(jacksonProcessor, Article.class);
```

### Issue: Performance degradation after migration

**Cause:** Possible misconfiguration or unexpected library selection.

**Solution:**
1. Check which library is being used:
```java
JsonProcessorDiagnostics diagnostics = new JsonProcessorDiagnostics();
System.out.println("Available processors: " + diagnostics.getAvailableProcessors());
System.out.println("Selected processor: " + diagnostics.getSelectedProcessor());
```

2. For Jackson users, ensure you're using `JacksonJsonProcessor` directly for optimal performance:
```java
JacksonJsonProcessor processor = new JacksonJsonProcessor();
ResourceConverter converter = new ResourceConverter(processor, Article.class);
```

### Issue: Annotation incompatibility after switching libraries

**Cause:** Different JSON libraries use different annotation frameworks.

**Solution:** Update annotations for the target library:

| Jackson | Gson | JSON-B |
|---------|------|--------|
| `@JsonProperty("name")` | `@SerializedName("name")` | `@JsonbProperty("name")` |
| `@JsonIgnore` | `@Expose(serialize = false, deserialize = false)` | `@JsonbTransient` |
| `@JsonFormat` | Use `@JsonAdapter` | `@JsonbDateFormat` |

---

## Validation Checklist

After migration, verify:

- [ ] All tests passing
- [ ] JSON serialization/deserialization working correctly
- [ ] Custom configurations applied properly
- [ ] Performance within acceptable range (<5% variance)
- [ ] No runtime errors or warnings
- [ ] Annotations compatible with chosen JSON library
- [ ] Retrofit integration working (if applicable)

---

## Getting Help

If you encounter issues not covered in this guide:

1. Check the [Troubleshooting Guide](TROUBLESHOOTING.md)
2. Review [JSON Library Comparison](JSON_LIBRARY_COMPARISON.md)
3. See [Configuration Examples](CONFIGURATION_EXAMPLES.md)
4. Report issues at: https://github.com/jasminb/jsonapi-converter/issues

---

**Next Steps:**
- [JSON Library Comparison Guide](JSON_LIBRARY_COMPARISON.md) - Choose the right library for your needs
- [Service Discovery Documentation](SERVICE_DISCOVERY.md) - Understand auto-detection behavior
- [Configuration Examples](CONFIGURATION_EXAMPLES.md) - See practical examples
