#### jsonapi-converter
JSONAPI-Converter is a library that provides means for integrating with services using JSON API specification.

For information on JSON API specification please see: http://jsonapi.org/format/

Besides providing support for request/response parsing, library provides a retrofit plugin.

Library is using Jackson (https://github.com/FasterXML/jackson-databind) for JSON data parsing.

##### Writing your model classes

When writing models that will be used to represent requests and responses, one needs to pay attention to following:

 - Each model class must be annotated with `com.github.jsonapi.annotations.Type` annotation
 - Each class must contain an `String` attribute annotated with `com.github.jsonapi.annotations.Id` annotation
 - All relationships must be annotated with `com.github.jsonapi.annotations.Relationship` annotation

###### Type annotation

Type annotation is used to instruct the serialisation/deserialisation library on how to process the given model class.
Annotation has single property `value` which is required and it should be set to to whatever is the designated JSON API SPEC name for that type.

Example:

```
@Type(name = "book")
public class Book {
 ...
}
```

###### Id annotation

Id annotation is used to flag an attribute of a class as an `id` attribute. Each resource class must have an id field and it must be of type `String` (defined by the JSON API specification).

Id is a special attribute that is, together with type, used to uniquely identify an resource.

Id annotation has no attributes.

Example:

```
@Type(name = "book")
public class Book {
  
  @Id
  private String isbn;
  ...
}
```

###### Relationship annotation

Relationship annotation is used to designate other resource types as a relationships.

Imagine modeling a simple library application, you would end up having a `Book` resource and another logical resource would be `Author`.

You can model this as two different classes where `Book` resource would have an relationship to an `Author`:

```
@Type
public class Book {
  @Id
  private String isbn;
  private String title;
  
  @Relationship(name = "author")
  private Author author;
}
```

Relationship annotation has following attributes:

 - name
 - resolve
 - serialise

Name attribute is required and each relationship must have it set.

Resolve attribute is used to instruct the library on how to handle server responses where resource relationships are not provided in `included` section but are rather returned as `type` and `id` combination.

Library has a support for registering global and typed relationship resloves which are used to resolve unresolved relationships.
Resolving a relationship means using provided `links` attribute to perform additional `HTTP` request and get the related object using the link provided.

Relationship resolver interface has a single method:

```
byte [] resolve(String relationshipURL);
````

After implementing relationship resolver, in order to use it, one must register it with the instance of the `ResourceConverter`.

Example:

```
ResourceConverter converter = new ResourceConverter(Book.class, Author.class);
converter.setGlobalResolver(new CustomRelationshipResolverInstance());
```

Besides support for global resolvers, there is an option to have different resolvers for different resource types:

```
ResourceConverter converter = new ResourceConverter(Book.class, Author.class);
converter.setTypeResolver(new CustomBooksResolver(), Book.class);
converter.setTypeResolver(new CustomAuthorResolver(), Author.class);

```

Serialise attribute is used to instruct the serialisar whether to include or exclude given relationship when serialising resources.
I is enabled by default, if disabled relationship will not be serialised.

##### Full example

Define simple POJO, please pay attention to added annotations:

```
@Type(name = "book")
public class Book {
  
  @Id
  private String isbn;
  private String title;
  
  @Relationship(name = "author")
  private Author author;
  
  # getters and setters
}

@Type(name = "author")
public class Author {
  
  @Id
  private String id;
  private String name;
  
  @Relationship(name = "books")
  private List<Book> books;
  
  # getters and setters
}
```

Create a converter instance:

```
ResourceConverter converter = new ResourceConverter(Book.class, Author.class);

// To convert raw data into POJO
byte [] rawResponse = ...get data from wire
Book book = converter.readObject(rawResponse, Book.class);

// To convert book object back to bytes
byte [] rawData = converter.writeObject(book);
```

##### Example usage with retrofit

As as first step, define your model classes and annotate them using annotations described above.

After defining models, define your service interfaces as you would usually do with 'standard' JSON/XML APIs.

To create retrofit instance:

```
// Create object mapper
ObjectMapper objectMapper = new ObjectMapper();

// Set serialisation/deserialisation options if needed (property naming strategy, etc...)

Retrofit retrofit = new Retrofit.Builder()
		.baseUrl("https://yourapi")
		.addConverterFactory(new JSONAPIConverterFactory(objectMapper, Book.class, Author.class))
		.build();
		
// Create services using service stubs and use it as usual.
```


