### jsonapi-converter

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://paypal.me/jbegic)

JSONAPI-Converter is a library that provides means for integrating with services using JSON API specification.

For information on JSON API specification please see: http://jsonapi.org/format/

Besides providing support for request/response parsing, library provides a retrofit plugin.

Library is using Jackson (https://github.com/FasterXML/jackson-databind) for JSON data parsing.

#### Including the library in your project

Maven:

```xml
<dependency>
  <groupId>com.github.jasminb</groupId>
  <artifactId>jsonapi-converter</artifactId>
  <version>0.11</version>
</dependency>
```

Gradle:
```groovy
implementation 'com.github.jasminb:jsonapi-converter:0.10'
```

SBT:

```groovy
libraryDependencies += "com.github.jasminb" % "jsonapi-converter" % "0.10"
```

In case you want to use current `SNAPSHOT` version of the project, make sure to add sonatype repository to your pom:

```xml
<repositories>
    <repository>
        <id>oss-sonatype</id>
        <name>oss-sonatype</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

Than to add dependency:

```xml
<dependency>
  <groupId>com.github.jasminb</groupId>
  <artifactId>jsonapi-converter</artifactId>
  <version>0.12-SNAPSHOT</version>
</dependency>
```

#### Writing your model classes

When writing models that will be used to represent requests and responses, one needs to pay attention to following:

 - Each model class must be annotated with `com.github.jasminb.jsonapi.annotations.Type` annotation
 - Each class must contain an `String` attribute annotated with `com.github.jasminb.jsonapi.annotations.Id` annotation
 - All relationships must be annotated with `com.github.jasminb.jsonapi.annotations.Relationship` annotation
 - Attributes in the API that are _not_ well-formed Java identifiers, must use `JsonProperty` annotation

#### Type annotation

Type annotation is used to instruct the serialisation/deserialisation library on how to process the given model class.
Annotation has single property `value` which is required and it should be set to whatever is the designated JSON API SPEC name for that type.

Example:

```java
@Type("book")
public class Book {
 ...
}
```

Note that `@Type` annotation is not inherited from supperclasses.

#### Id annotation

Id annotation is used to flag an attribute of a class as an `id` attribute. Each resource class must have an id field.

In case field annotated by the `@Id` annotation is not a `String` field, `@Id` annotation needs to be configured with proper `ResourceIdHandler`. Lirary provides handlers for `Long` and `Integer` types, in case types other than those mentioned are used, user must implement and provide proper id handler.

Id is a special attribute that is, together with type, used to uniquely identify an resource.

Id annotation is inheritable, one can define a base model class that contains a field with `@Id` annotation and then extend it to create a new type.

Example:

```java
@Type("book")
public class Book {
  
  @Id
  private String isbn;
  ...
}
```

Example with inheritance:

```java
public class BaseModel {
  @Id
  private String id;
}

@Type("book")
public class Book extends BaseModel {
  # Your custom member variables
}
```

Example using `Long` as id

```java
@Type("book")
public class Book {
  
  @Id(LongIdHandler.class)
  private Long id;
  ...
}


```

#### Relationship annotation

Relationship annotation is used to designate other resource types as a relationships.

Imagine modeling a simple library application, you would end up having a `Book` resource and another logical resource would be `Author`.

You can model this as two different classes where `Book` resource would have a relationship to an `Author`:

```java
@Type("book")
public class Book {
  @Id
  private String isbn;
  private String title;
  
  @Relationship("author")
  private Author author;
}
```

Relationship annotation has following attributes:

 - value
 - resolve
 - serialise
 - relType
 
Value attribute is required and each relationship must have it set (value attribute represents the 'name' of the relationship).

Resolve attribute is used to instruct the library on how to handle server responses where resource relationships are not provided in `included` section but are rather returned as `type` and `id` combination.

Library has a support for registering global and typed relationship resolves which are used to resolve unresolved relationships.
Resolving a relationship means using provided `links` attribute to perform additional `HTTP` request and get the related object using the link provided.

Relationship resolver interface has a single method:

```java
byte [] resolve(String relationshipURL);
````

After implementing relationship resolver, in order to use it, one must register it with the instance of the `ResourceConverter`.

Example:

```java
ResourceConverter converter = new ResourceConverter(Book.class, Author.class);
converter.setGlobalResolver(new CustomRelationshipResolverInstance());
```

Besides support for global resolvers, there is an option to have different resolvers for different resource types:

```java
ResourceConverter converter = new ResourceConverter(Book.class, Author.class);
converter.setTypeResolver(new CustomBooksResolver(), Book.class);
converter.setTypeResolver(new CustomAuthorResolver(), Author.class);

```

Serialise attribute is used to instruct the serialisar whether to include or exclude given relationship when serialising resources.
I is enabled by default, if disabled relationship will not be serialised.

Relationship type (`relType`) is used to instruct the library on how to resolve link data from raw server responses in order to
resolve given relationship.

There two different relationship types:

 - `SELF` (`self` link will be followed to resolve relationship
 - `RELATED` (`related` link will be followed)
 
Have in mind that relationship (same as id) is inheritable and can be defined in a base class.

#### Polymorphic Relationships

In order to support polymorphic relationships, an `interface` needs to be created and than implemented by all possible types relationship supports. Created interface is used as a relationship's type (see example below).


Example response containing multiple types in a relationship:

```json
{
  "data": {
    "type": "dealerships",
    "id": "1",
    "attributes": {
      "name": "Dealership name"
    },
    "links": {
      "self": "http://example.com/dealerships/1"
    },
    "relationships": {
      "inventory": {
        "links": {
          "self": "http://example.com/dealerships/1/relationships/inventory",
          "related": "http://example.com/dealerships/1/inventory"
        },
        "data": [
          { "type": "cars", "id": "2" },
          { "type": "trucks", "id": "1" }
        ]
      }
    }
  },
  "included": [{
    "type": "cars",
    "id": "2",
    "attributes": {
      "make": "BMW",
      "model": "i8 Roadster"
    },
    "links": {
      "self": "http://example.com/cars/2"
    }
  }, {
    "type": "trucks",
    "id": "1",
    "attributes": {
      "make": "Ford",
      "model": "Semi"
    },
    "links": {
      "self": "http://example.com/trucks/1"
    }
  }]
}
```

Needed classes, and example usage:

```java
public interface Driveable {}

@Type("cars")
public class Car implements Driveable {
	@Id
	private String id;
	private String model;
	private String make;
	// Getters and setters...
}

@Type("trucks")
public class Truck implements Driveable {

	@Id
	private String id;
	private String make;
	private String model;
        // Getters and setters...
}

@Type("dealerships")
public class Dealership {
	@Id
	private String id;
	private String name;
	private String city;

        // Interface is used as relalationship type (instead of concrete resource type)
	@Relationship("inventory")
	private Collection<Driveable> automobiles;
}

// Putting everything together
ResourceConverter converter = new ResourceConverter("https://api.example.com", Car.class, Dealership.class, Truck.class);
JSONAPIDocument<Dealership> document = converter.readDocument(apiResponse, Dealership.class);


```

#### Relationship meta and links

jsonapi-spec allows for having relationship-level metadata and links.

In order to gain access to returned relationship meta and links or ability to serialize it, use following annotations:
 - `RelationshipMeta`
 - `RelationshipLinks`
 
 Here is a version of the `Book` class with relationship meta/links added:
 
 ```java
@Type("book")
public class Book {
  @Id
  private String isbn;
  private String title;
  
  @Relationship("author")
  private Author author;
  
  @RelationshipMeta("author")
  private Meta authorMeta
  
  @RelationshipLinks("author")
  private Links authorLinks
}
```

Make sure not to confuse relationship meta and links with regular meta-data and link data explained below.

#### Meta annotation

By JSON API specification, each resource can hold `meta` attribute. Meta can be arbitrary object that is defined by the API implementation.

In order to map and make meta available trough resource conversion, one must create a model that coresponds to the meta object returned by the API, create a member variable in the resource class using created model and annotate it using the `@Meta` annotation.

Meta example:

```java
# Meta model class

public class MyCustomMetaClass {
    private String myAttribute;
    
    public String getMyAttribute() {
    	return myAttribute;
    }
    
    public void setMyAttribute(String value) {
    	this.myAttribute = value;
    }
}

# Resource class with meta attribute

@Type("book")
public class Book {
  @Id
  private String isbn;
  private String title;
  
  @Relationship("author")
  private Author author;
  
  @Meta
  private MyCustomMetaClass meta;
}

```

Meta annotation/attriubutes are inheritable.

#### Links annotation

JSON API specification allows for `links` to be part of resources. Links usually cary information about the resource itself (eg. its URI on the server).

Liks are not arbitray objects, JSON API spec provides links structure therefore it is not required to create a new model to make links object available.

Library provides a `com.github.jasminb.jsonapi.Links` class that must be used in order to make links data available in resources.

Example:

```java
@Type("book")
public class Book {
  @Id
  private String isbn;
  private String title;
  
  @Relationship("author")
  private Author author;
  
  @Meta
  private MyCustomMetaClass meta;
  
  @Links
  private com.github.jasminb.jsonapi.Links links;
}
```

Links are inheritable.

#### Attribute annotations

If your JSON API endpoint returns attributes that do not map well to Java identifiers, you'll get a fatal error on deserialization. The log message will tell you
about an unrecognized field with that name. To fix it, you can use `com.fasterxml.jackson.annotation.JsonProperty`.

Example: your JSON API endpoint returns something like this

```json
{
    "data": [
        {
            "id": "1",
            "type": "gears",
            "attributes": {
                "tooth-count": 13,
                "tooth-depth": 21
            }
        }
    ]
}
```

then your model must be:

```java

import com.fasterxml.jackson.annotation.JsonProperty;

@Type("gears")
public class Gear {
  @Id
  private String id;

  @JsonProperty("tooth-count")
  private Long toothCount;
  @JsonProperty("tooth-depth")
  private Long toothDepth;
}
```

This also lets you use different names for your fields than your API.

#### Full example

Define simple POJO, please pay attention to the added annotations:

```java
# Meta is optional, one does not have to define or use it
public class Meta {
    private String myAttribute;
    
    public String getMyAttribute() {
    	return myAttribute;
    }
    
    public void setMyAttribute(String value) {
    	this.myAttribute = value;
    }
}

# Creating base class is optional but allows for writing more compact model classes
public class BaseResource {
    @Id
    private String id;
    
    @Meta
    private Meta meta;
    
    @Links
    private Links links;
}

@Type("book")
public class Book extends BaseResource {
  private String title;
  
  @Relationship("author")
  private Author author;
  
  @RelationshipMeta("author")
  private Meta authorMeta
  
  @RelationshipLinks("author")
  private Links authorLinks
  
  # getters and setters
}

@Type("author")
public class Author extends BaseResource {
  private String name;
  
  @Relationship("books")
  private List<Book> books;
  
  # getters and setters
}
```

Create a converter instance:

```java
ResourceConverter converter = new ResourceConverter(Book.class, Author.class);

// Get response data
byte [] rawResponse = ...get data from the wire

// To convert raw data into single POJO
JSONAPIDocument<Book> bookDocument = converter.readDocument(rawResponse, Book.class);
Book book = bookDocument.get();

// To convert raw data into collection
JSONAPIDocument<List<Book>> bookDocumentCollection = converter.readDocumentCollection(rawResponse, Book.class);
List<Book> bookCollection = bookDocumentCollection.get();

```

Note that calling `readDocument(...)` or `readDocumentCollection(...)` using content that contains `errors` (`{"errors" : [{...}]}`) attribute will produce `ResourceParseException`.

Thrown exception has a method (`getErrorResponse()`) that returns parsed `errors` content. Errors content is expected to comply to JSON API Spec.

#### Top level links and meta

Besides having links and meta information on resource level, by JSON API spec it is also possible to have meta, links or both as top level objects in server responses.

To gain access to top level meta/links, this library provides convenience methods available in `JSONAPIDocument`, namely:
 
 - `getMeta()`
 - `getLinks()`
 
#### Resource serialization

Besides providing options to deserialize json-api spec complaint resource representation, library also includes support for serializing resources.

Following are available serialization options that can be enabled/disabled on `ResourceConverter` instance:

 - `INCLUDE_META` enabled by default, if enabled, meta data will be serialized
 - `INCLUDE_LINKS` enabled by default, if enabled links will be serialized
 - `INCLUDE_RELATIONSHIP_ATTRIBUTES` disabled by default, if enabled, relationship objects will be serialized fully, this means that besides generating `relationship` objects for each relationship, `included` section will be created that contains actual relationship attributes

To enable or disable serialization options:

```java
ResourceConverter converter = ...
# Enable generating included section
converter.enableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);

# Disable generating included section
converter.disableSerializationOption(SerializationFeature.INCLUDE_RELATIONSHIP_ATTRIBUTES);

```


Example with `INCLUDE_RELATIONSHIP_ATTRIBUTES` disabled:

```json
{
  "data": {
    "type": "articles",
    "id": "id",
    "attributes": {
      "title": "title"
    },
    "relationships": {
      "author": {
        "data": {
          "type": "people",
          "id": "id"
        }
      }
    }
  }
}
```

Example with `INCLUDE_RELATIONSHIP_ATTRIBUTES` enabled:

```json
{
  "data": {
    "type": "articles",
    "id": "id",
    "attributes": {
      "title": "title"
    },
    "relationships": {
      "author": {
        "data": {
          "type": "people",
          "id": "id"
        }
      }
    }
  },
  "included": [
    {
      "type": "people",
      "id": "id",
      "attributes": {
        "firstName": "John"
      }
    }
  ]
}
```

#### Example usage with retrofit

As a first step, define your model classes and annotate them using annotations described above.

After defining models, define your service interfaces as you would usually do with 'standard' JSON/XML APIs.

To create retrofit instance:

```java
// Create object mapper
ObjectMapper objectMapper = new ObjectMapper();

// Set serialisation/deserialisation options if needed (property naming strategy, etc...)

Retrofit retrofit = new Retrofit.Builder()
		.baseUrl("https://yourapi")
		.addConverterFactory(new JSONAPIConverterFactory(objectMapper, Book.class, Author.class))
		.build();
		
// Create service using service interface

MyBooksService booksService = retrofit.create(MyBooksService.class);

```

###### Synchronous usage

```java
Response<JSONAPIDocument<Book>> bookResponse = booksService.find("123").execute();

if (bookResponse.isSuccess()) {
    // Consume response
} else {
    ErrorResponse errorResponse = ErrorUtils.parseErrorResponse(bookResponse.errorBody());
    // Handle error
}
```

###### Asynchronous usage

```java
Call<JSONAPIDocument<Book>> bookServiceCall = service.getExampleResource();

bookServiceCall.enqueue(new Callback<Book>() {
  @Override
  public void onResponse(Response<JSONAPIDocument<Book>> bookResponse, Retrofit retrofit) {
    if (bookResponse.isSuccess()) {
        // Consume response
    } else {
        ErrorResponse errorResponse = ErrorUtils.parseErrorResponse(bookResponse.errorBody());
        // Handle error
    }
  }
  
  @Override
  public void onFailure(Throwable throwable) {
    // Handle network errors/unexpected errors
  }
});
```

Notice that expected return types in `MyBookService` calls are all wrapped with `JSONAPIDocument`, this is intended way to use the library since it allows for gaining access to response level `meta` and `links` data.

Example service interface:

```java
public interface MyBooksService {
    @GET("books")
    Call<JSONAPIDocument<List<Book>> allBooks();
}

```

#### Tips

If you need a `String` as an output when serializing objects, you can do the following:


```java
byte [] serializedObject = resourceConverter.writeObject(...);
String serializedAsString = new String(serializedObject);
```

#### Note for kotlin users

Have in mind that using `open` classes as type parameters in relationship collections will not work, for instance:

```java
@Type("base")
open class MyClass {

    @Relationship("my-relationship")
    var bases: List<MyClass>? = null
}
```

Removing the `open` modifier will solve the issue.

#### Note for Proguard

This library use reflection at runtime for checking if all registred types have an ID-field.  
You have to add this proguard rule:
```
# Keep jsonapi-converter relative fields
-keepclassmembers class * {
    @com.github.jasminb.jsonapi.annotations.Id <fields>;
}
```

Proguard should remove all fields and methods that you are not accessing. If you would like to keep all deserialized fields, you can add a rule for unconditionally keep all your POJO classes.
```
# Keep all POJO classes
-keep class com.example.json.** {*;}
```
Replace `com.example.json` by the correct package.

```
# Keep custom id handlers
-keep class * implements com.github.jasminb.jsonapi.ResourceIdHandler
```
