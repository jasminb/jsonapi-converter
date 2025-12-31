# Retrofit Integration Module

## Overview

The Retrofit integration module provides seamless integration between the JSON API Converter and the [Retrofit HTTP client library](https://square.github.io/retrofit/). This module contains 5 classes that handle automatic conversion of JSON API requests and responses within Retrofit's converter factory system.

**Location**: `src/main/java/com/github/jasminb/jsonapi/retrofit/`

---

## Module Architecture

The Retrofit integration follows the factory pattern to create appropriate converters based on type information:

```
JSONAPIConverterFactory (Factory)
├── JSONAPIResponseBodyConverter (Single resource responses)
├── JSONAPIDocumentResponseBodyConverter (Document responses)
├── JSONAPIRequestBodyConverter (Request serialization)
└── RetrofitType (Type analysis utility)
```

---

## JSONAPIConverterFactory (`JSONAPIConverterFactory.java:24`)

The main entry point for Retrofit integration. Creates appropriate converters based on method signatures and type information.

### Class Structure

```java
public final class JSONAPIConverterFactory extends Converter.Factory {
    private final ResourceConverter resourceConverter;
    private final Converter.Factory alternativeConverterFactory;

    // Factory methods and converter creation
}
```

### Factory Creation

```java
// Basic factory with resource converter
public static JSONAPIConverterFactory create(ResourceConverter resourceConverter)

// Factory with fallback for non-JSON-API endpoints
public static JSONAPIConverterFactory create(ResourceConverter resourceConverter,
                                           Converter.Factory alternativeConverterFactory)
```

### Usage Examples

#### Basic Setup
```java
// Create resource converter with your domain classes
ResourceConverter converter = new ResourceConverter(
    Article.class, Person.class, Comment.class
);

// Create Retrofit instance
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .addConverterFactory(JSONAPIConverterFactory.create(converter))
    .build();

// Create API service
ArticleService service = retrofit.create(ArticleService.class);
```

#### Mixed API Support (JSON API + regular JSON)
```java
ResourceConverter converter = new ResourceConverter(Article.class);

// Fallback to Jackson for non-JSON-API endpoints
Converter.Factory jacksonFactory = JacksonConverterFactory.create();

Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .addConverterFactory(JSONAPIConverterFactory.create(converter, jacksonFactory))
    .build();
```

#### Complete Configuration
```java
@Configuration
public class RetrofitConfig {

    @Bean
    public ResourceConverter resourceConverter() {
        ResourceConverter converter = new ResourceConverter(
            "https://api.example.com",  // Base URL for link generation
            Article.class, Person.class, Comment.class, Tag.class
        );

        // Configure features
        converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);
        converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);

        return converter;
    }

    @Bean
    public Retrofit retrofit(ResourceConverter converter) {
        return new Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(JSONAPIConverterFactory.create(converter))
            .client(okHttpClient()) // Custom OkHttp client
            .build();
    }

    @Bean
    public ArticleService articleService(Retrofit retrofit) {
        return retrofit.create(ArticleService.class);
    }
}
```

### Converter Selection Logic

The factory analyzes method signatures to determine which converter to use:

1. **Response Body Converters**:
   - `JSONAPIDocument<T>` → `JSONAPIDocumentResponseBodyConverter`
   - `T` (registered type) → `JSONAPIResponseBodyConverter`
   - Other types → Alternative factory (if configured)

2. **Request Body Converters**:
   - `JSONAPIDocument<?>` → `JSONAPIRequestBodyConverter`
   - Registered types → `JSONAPIRequestBodyConverter`
   - Other types → Alternative factory (if configured)

---

## Response Body Converters

### JSONAPIResponseBodyConverter (`JSONAPIResponseBodyConverter.java:19`)

Converts JSON API responses to unwrapped resource objects (single resources or collections).

#### Supported Return Types

```java
public interface ArticleService {
    // Single resource - returns Article directly
    @GET("articles/{id}")
    Call<Article> getArticle(@Path("id") String id);

    // Collection - returns List<Article> directly
    @GET("articles")
    Call<List<Article>> getArticles();

    // RxJava support
    @GET("articles/{id}")
    Single<Article> getArticleRx(@Path("id") String id);

    // Async support
    @GET("articles")
    Call<List<Article>> getArticlesAsync();
}
```

#### Usage Examples

```java
@Service
public class ArticleService {
    private final ArticleApi articleApi;

    public Article findById(String id) {
        try {
            Response<Article> response = articleApi.getArticle(id).execute();
            if (response.isSuccessful()) {
                return response.body(); // Direct Article object
            } else {
                handleErrorResponse(response);
                return null;
            }
        } catch (IOException e) {
            throw new ServiceException("Network error", e);
        }
    }

    public List<Article> findAll() {
        try {
            Response<List<Article>> response = articleApi.getArticles().execute();
            return response.body(); // Direct List<Article>
        } catch (IOException e) {
            throw new ServiceException("Network error", e);
        }
    }
}
```

#### Async Usage

```java
public void loadArticleAsync(String id, Callback<Article> callback) {
    articleApi.getArticle(id).enqueue(new Callback<Article>() {
        @Override
        public void onResponse(Call<Article> call, Response<Article> response) {
            if (response.isSuccessful()) {
                Article article = response.body();
                callback.onSuccess(article);
            } else {
                handleError(response);
            }
        }

        @Override
        public void onFailure(Call<Article> call, Throwable t) {
            callback.onError(t);
        }
    });
}
```

#### RxJava Integration

```java
public class RxArticleService {
    private final ArticleApi api;

    public Single<Article> getArticle(String id) {
        return api.getArticleRx(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<List<Article>> getAllArticles() {
        return api.getArticlesRx()
            .flatMapObservable(Observable::fromIterable)
            .toList()
            .toObservable();
    }
}
```

### JSONAPIDocumentResponseBodyConverter (`JSONAPIDocumentResponseBodyConverter.java:18`)

Converts JSON API responses to complete `JSONAPIDocument<T>` objects, preserving meta, links, and included data.

#### Supported Return Types

```java
public interface ArticleService {
    // Single resource document - preserves meta/links/included
    @GET("articles/{id}")
    Call<JSONAPIDocument<Article>> getArticleDocument(@Path("id") String id);

    // Collection document - preserves pagination info
    @GET("articles")
    Call<JSONAPIDocument<List<Article>>> getArticlesDocument(@QueryMap Map<String, String> params);

    // Error handling - can return error documents
    @POST("articles")
    Call<JSONAPIDocument<Article>> createArticle(@Body JSONAPIDocument<Article> article);
}
```

#### Usage Examples

##### Accessing Meta Information
```java
public PaginatedResult<Article> getArticlesWithPagination(int page, int size) {
    Map<String, String> params = new HashMap<>();
    params.put("page[number]", String.valueOf(page));
    params.put("page[size]", String.valueOf(size));

    try {
        Response<JSONAPIDocument<List<Article>>> response =
            articleApi.getArticlesDocument(params).execute();

        if (response.isSuccessful()) {
            JSONAPIDocument<List<Article>> document = response.body();

            // Extract data
            List<Article> articles = document.get();

            // Extract pagination meta
            Map<String, ?> meta = document.getMeta();
            Integer totalCount = (Integer) meta.get("total");
            Integer totalPages = (Integer) meta.get("pages");

            // Extract pagination links
            Links links = document.getLinks();
            String nextUrl = links.getNext() != null ? links.getNext().getHref() : null;
            String prevUrl = links.getPrev() != null ? links.getPrev().getHref() : null;

            return new PaginatedResult<>(articles, totalCount, totalPages, nextUrl, prevUrl);
        }
    } catch (IOException e) {
        throw new ServiceException("Failed to load articles", e);
    }

    return null;
}
```

##### Accessing Included Resources
```java
public ArticleWithIncludes getArticleWithAuthor(String id) {
    try {
        Response<JSONAPIDocument<Article>> response =
            articleApi.getArticleDocument(id).execute();

        if (response.isSuccessful()) {
            JSONAPIDocument<Article> document = response.body();
            Article article = document.get();

            // Access included resources through relationships
            // (Automatically resolved by the converter)
            Person author = article.getAuthor();
            List<Comment> comments = article.getComments();

            return new ArticleWithIncludes(article, author, comments);
        }
    } catch (IOException e) {
        throw new ServiceException("Failed to load article", e);
    }

    return null;
}
```

##### Error Handling with Documents
```java
public Article createArticle(Article article) {
    JSONAPIDocument<Article> requestDoc = new JSONAPIDocument<>(article);

    try {
        Response<JSONAPIDocument<Article>> response =
            articleApi.createArticle(requestDoc).execute();

        if (response.isSuccessful()) {
            return response.body().get();
        } else {
            // Handle error response (might contain JSON API errors)
            handleErrorResponse(response);
            return null;
        }
    } catch (IOException e) {
        throw new ServiceException("Failed to create article", e);
    }
}

private void handleErrorResponse(Response<?> response) {
    try {
        // Try to parse as JSON API error document
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            Errors errors = ErrorUtils.parseError(errorBody);
            throw new ApiErrorException(errors);
        }
    } catch (IOException e) {
        // Fallback to generic error
        throw new ServiceException("API request failed: " + response.code());
    }
}
```

---

## Request Body Converter

### JSONAPIRequestBodyConverter (`JSONAPIRequestBodyConverter.java:18`)

Converts Java objects to JSON API request bodies with proper content type.

#### Supported Request Types

```java
public interface ArticleService {
    // Direct object serialization
    @POST("articles")
    Call<JSONAPIDocument<Article>> createArticle(@Body Article article);

    // Document wrapper serialization
    @POST("articles")
    Call<JSONAPIDocument<Article>> createArticleDocument(@Body JSONAPIDocument<Article> document);

    // Updates
    @PATCH("articles/{id}")
    Call<JSONAPIDocument<Article>> updateArticle(@Path("id") String id, @Body Article article);

    // Bulk operations
    @POST("articles/bulk")
    Call<JSONAPIDocument<List<Article>>> createArticles(@Body List<Article> articles);
}
```

#### Usage Examples

##### Simple Resource Creation
```java
public Article createArticle(String title, String content, String authorId) {
    Article article = new Article();
    article.setTitle(title);
    article.setContent(content);

    // Set author relationship
    Person author = new Person();
    author.setId(authorId);
    article.setAuthor(author);

    try {
        Response<JSONAPIDocument<Article>> response =
            articleApi.createArticle(article).execute(); // Direct object

        if (response.isSuccessful()) {
            return response.body().get();
        } else {
            handleErrorResponse(response);
            return null;
        }
    } catch (IOException e) {
        throw new ServiceException("Failed to create article", e);
    }
}
```

##### Document with Meta
```java
public Article createArticleWithMeta(Article article, Map<String, Object> meta) {
    JSONAPIDocument<Article> document = new JSONAPIDocument<>(article);
    document.setMeta(meta);

    try {
        Response<JSONAPIDocument<Article>> response =
            articleApi.createArticleDocument(document).execute(); // Document wrapper

        return response.body().get();
    } catch (IOException e) {
        throw new ServiceException("Failed to create article", e);
    }
}
```

##### Bulk Operations
```java
public List<Article> createArticlesBatch(List<Article> articles) {
    try {
        Response<JSONAPIDocument<List<Article>>> response =
            articleApi.createArticles(articles).execute();

        if (response.isSuccessful()) {
            return response.body().get();
        } else {
            handleBulkErrorResponse(response);
            return Collections.emptyList();
        }
    } catch (IOException e) {
        throw new ServiceException("Failed to create articles", e);
    }
}
```

#### Content Type Handling

The request converter automatically sets the correct content type:
- **Content-Type**: `application/vnd.api+json`
- Compliant with JSON API specification requirements

---

## Type Analysis Utility

### RetrofitType (`RetrofitType.java:13`)

Utility class for analyzing generic types in Retrofit method signatures.

#### Purpose
- Extract generic type information from method parameters and return types
- Determine whether types are JSONAPIDocument wrappers or direct resource types
- Support for complex generic scenarios (e.g., `JSONAPIDocument<List<Article>>`)

#### Usage (Internal)
This class is used internally by the factory to determine converter types:

```java
// Internal usage in JSONAPIConverterFactory
Type responseType = method.getGenericReturnType();
if (RetrofitType.isJSONAPIDocument(responseType)) {
    Class<?> resourceType = RetrofitType.extractResourceType(responseType);
    return createDocumentConverter(resourceType);
} else if (converter.isRegisteredType(responseType)) {
    return createResourceConverter(responseType);
}
```

---

## Complete Service Examples

### Basic CRUD Service

```java
public interface ArticleApi {
    @GET("articles")
    Call<JSONAPIDocument<List<Article>>> getArticles(
        @Query("page[number]") Integer pageNumber,
        @Query("page[size]") Integer pageSize,
        @Query("filter[title]") String titleFilter
    );

    @GET("articles/{id}")
    Call<Article> getArticle(@Path("id") String id);

    @POST("articles")
    Call<JSONAPIDocument<Article>> createArticle(@Body Article article);

    @PATCH("articles/{id}")
    Call<Article> updateArticle(@Path("id") String id, @Body Article article);

    @DELETE("articles/{id}")
    Call<Void> deleteArticle(@Path("id") String id);
}
```

### Service Implementation

```java
@Service
public class ArticleService {
    private final ArticleApi api;

    public ArticleService(ArticleApi api) {
        this.api = api;
    }

    public PagedResult<Article> findArticles(int page, int size, String titleFilter) {
        try {
            Response<JSONAPIDocument<List<Article>>> response = api
                .getArticles(page, size, titleFilter)
                .execute();

            if (response.isSuccessful()) {
                JSONAPIDocument<List<Article>> document = response.body();
                List<Article> articles = document.get();

                // Extract pagination info from meta
                Map<String, ?> meta = document.getMeta();
                int totalCount = ((Number) meta.get("total")).intValue();

                return new PagedResult<>(articles, page, size, totalCount);
            } else {
                throw handleApiError(response);
            }
        } catch (IOException e) {
            throw new ServiceException("Network error while fetching articles", e);
        }
    }

    public Article findById(String id) {
        try {
            Response<Article> response = api.getArticle(id).execute();

            if (response.isSuccessful()) {
                return response.body();
            } else if (response.code() == 404) {
                return null; // Not found
            } else {
                throw handleApiError(response);
            }
        } catch (IOException e) {
            throw new ServiceException("Network error while fetching article", e);
        }
    }

    public Article create(Article article) {
        try {
            Response<JSONAPIDocument<Article>> response = api
                .createArticle(article)
                .execute();

            if (response.isSuccessful()) {
                return response.body().get();
            } else {
                throw handleApiError(response);
            }
        } catch (IOException e) {
            throw new ServiceException("Network error while creating article", e);
        }
    }

    public Article update(String id, Article article) {
        try {
            Response<Article> response = api
                .updateArticle(id, article)
                .execute();

            if (response.isSuccessful()) {
                return response.body();
            } else {
                throw handleApiError(response);
            }
        } catch (IOException e) {
            throw new ServiceException("Network error while updating article", e);
        }
    }

    public void delete(String id) {
        try {
            Response<Void> response = api.deleteArticle(id).execute();

            if (!response.isSuccessful()) {
                throw handleApiError(response);
            }
        } catch (IOException e) {
            throw new ServiceException("Network error while deleting article", e);
        }
    }

    private RuntimeException handleApiError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                Errors errors = ErrorUtils.parseError(response.errorBody());
                return new ApiException(errors, response.code());
            }
        } catch (IOException e) {
            // Fall through to generic error
        }

        return new ServiceException("API request failed with code: " + response.code());
    }
}
```

### Async Service with Callbacks

```java
@Service
public class AsyncArticleService {
    private final ArticleApi api;

    public void getArticles(int page, int size, ServiceCallback<PagedResult<Article>> callback) {
        api.getArticles(page, size, null).enqueue(new Callback<JSONAPIDocument<List<Article>>>() {
            @Override
            public void onResponse(Call<JSONAPIDocument<List<Article>>> call,
                                 Response<JSONAPIDocument<List<Article>>> response) {
                if (response.isSuccessful()) {
                    JSONAPIDocument<List<Article>> document = response.body();
                    List<Article> articles = document.get();
                    Map<String, ?> meta = document.getMeta();
                    int total = ((Number) meta.get("total")).intValue();

                    callback.onSuccess(new PagedResult<>(articles, page, size, total));
                } else {
                    callback.onError(new ApiException("Request failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<JSONAPIDocument<List<Article>>> call, Throwable t) {
                callback.onError(new ServiceException("Network error", t));
            }
        });
    }

    public void getArticle(String id, ServiceCallback<Article> callback) {
        api.getArticle(id).enqueue(new Callback<Article>() {
            @Override
            public void onResponse(Call<Article> call, Response<Article> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else if (response.code() == 404) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(new ApiException("Request failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Article> call, Throwable t) {
                callback.onError(new ServiceException("Network error", t));
            }
        });
    }
}

public interface ServiceCallback<T> {
    void onSuccess(T result);
    void onError(Throwable error);
}
```

---

## Advanced Configuration

### Custom Headers and Interceptors

```java
@Bean
public OkHttpClient okHttpClient() {
    return new OkHttpClient.Builder()
        .addInterceptor(new AuthenticationInterceptor())
        .addInterceptor(new JsonApiHeaderInterceptor())
        .addInterceptor(new LoggingInterceptor())
        .build();
}

public class JsonApiHeaderInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        // Add JSON API headers
        Request.Builder requestBuilder = original.newBuilder()
            .header("Accept", "application/vnd.api+json")
            .header("Content-Type", "application/vnd.api+json");

        return chain.proceed(requestBuilder.build());
    }
}
```

### Error Handling Interceptor

```java
public class ApiErrorInterceptor implements Interceptor {
    private final ResourceConverter converter;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        if (!response.isSuccessful() && response.body() != null) {
            // Check if response is JSON API error format
            MediaType mediaType = response.body().contentType();
            if (mediaType != null && "application/vnd.api+json".equals(mediaType.toString())) {
                try {
                    Errors errors = ErrorUtils.parseError(response.body());
                    throw new ResourceParseException(errors);
                } catch (Exception e) {
                    // Not a JSON API error, proceed normally
                }
            }
        }

        return response;
    }
}
```

### Multiple Base URLs

```java
@Configuration
public class MultiApiConfig {

    @Bean
    @Qualifier("articlesApi")
    public Retrofit articlesRetrofit(ResourceConverter converter) {
        return new Retrofit.Builder()
            .baseUrl("https://content-api.example.com/")
            .addConverterFactory(JSONAPIConverterFactory.create(converter))
            .build();
    }

    @Bean
    @Qualifier("usersApi")
    public Retrofit usersRetrofit(ResourceConverter converter) {
        return new Retrofit.Builder()
            .baseUrl("https://users-api.example.com/")
            .addConverterFactory(JSONAPIConverterFactory.create(converter))
            .build();
    }
}
```

---

## Testing

### Unit Testing Services

```java
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {
    @Mock private ArticleApi api;
    private ArticleService service;

    @BeforeEach
    void setUp() {
        service = new ArticleService(api);
    }

    @Test
    void shouldReturnArticleWhenFound() throws IOException {
        // Given
        Article article = new Article();
        article.setId("123");
        article.setTitle("Test Article");

        Call<Article> call = mock(Call.class);
        Response<Article> response = Response.success(article);
        when(call.execute()).thenReturn(response);
        when(api.getArticle("123")).thenReturn(call);

        // When
        Article result = service.findById("123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("123");
        assertThat(result.getTitle()).isEqualTo("Test Article");
    }

    @Test
    void shouldReturnNullWhenNotFound() throws IOException {
        // Given
        Call<Article> call = mock(Call.class);
        Response<Article> response = Response.error(404, ResponseBody.create(null, ""));
        when(call.execute()).thenReturn(response);
        when(api.getArticle("999")).thenReturn(call);

        // When
        Article result = service.findById("999");

        // Then
        assertThat(result).isNull();
    }
}
```

### Integration Testing

```java
@Test
public class RetrofitIntegrationTest {
    private MockWebServer server;
    private ArticleApi api;

    @BeforeEach
    void setUp() {
        server = new MockWebServer();

        ResourceConverter converter = new ResourceConverter(Article.class, Person.class);

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(JSONAPIConverterFactory.create(converter))
            .build();

        api = retrofit.create(ArticleApi.class);
    }

    @Test
    void shouldDeserializeJsonApiResponse() throws Exception {
        // Given
        String jsonResponse = """
            {
              "data": {
                "type": "articles",
                "id": "1",
                "attributes": {
                  "title": "Hello World",
                  "content": "This is a test article"
                },
                "relationships": {
                  "author": {
                    "data": {"type": "people", "id": "42"}
                  }
                }
              },
              "included": [
                {
                  "type": "people",
                  "id": "42",
                  "attributes": {
                    "firstName": "John",
                    "lastName": "Doe"
                  }
                }
              ]
            }
            """;

        server.enqueue(new MockResponse()
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/vnd.api+json"));

        // When
        Response<Article> response = api.getArticle("1").execute();

        // Then
        assertThat(response.isSuccessful()).isTrue();

        Article article = response.body();
        assertThat(article.getId()).isEqualTo("1");
        assertThat(article.getTitle()).isEqualTo("Hello World");
        assertThat(article.getAuthor()).isNotNull();
        assertThat(article.getAuthor().getFirstName()).isEqualTo("John");
    }
}
```

---

## Best Practices

### 1. Use Document Types for Complex Responses
```java
// Good - preserves pagination/meta information
@GET("articles")
Call<JSONAPIDocument<List<Article>>> getArticles(@QueryMap Map<String, String> params);

// Less ideal - loses meta/pagination info
@GET("articles")
Call<List<Article>> getArticlesSimple();
```

### 2. Handle Errors Gracefully
```java
public Article getArticle(String id) {
    try {
        Response<Article> response = api.getArticle(id).execute();

        if (response.isSuccessful()) {
            return response.body();
        } else {
            // Try to parse JSON API error response
            Errors errors = ErrorUtils.parseError(response.errorBody());
            throw new ApiException(errors);
        }
    } catch (ResourceParseException e) {
        // JSON API error response
        throw new ApiException(e.getErrors());
    } catch (IOException e) {
        throw new ServiceException("Network error", e);
    }
}
```

### 3. Use Async Operations for UI
```java
// Android example
public void loadArticles(String query) {
    api.searchArticles(query).enqueue(new Callback<List<Article>>() {
        @Override
        public void onResponse(Call<List<Article>> call, Response<List<Article>> response) {
            if (response.isSuccessful()) {
                updateUI(response.body());
            }
        }

        @Override
        public void onFailure(Call<List<Article>> call, Throwable t) {
            showError(t);
        }
    });
}
```

### 4. Configure Timeouts
```java
@Bean
public OkHttpClient okHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build();
}
```

---

*Source locations: All Retrofit integration classes are in `src/main/java/com/github/jasminb/jsonapi/retrofit/`*