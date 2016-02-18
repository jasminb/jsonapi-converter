#### jsonapi-converter
JSONAPI-Converter is a simple library that provides consumption of JSON API spec APIs.

For information on JSON API spec please see: http://jsonapi.org/format/

Besides providing support for request/response parsing, library provides a simple retrofit plugin.

Library is using Jackson library (https://github.com/FasterXML/jackson-databind) for actual data parsing.

##### Example usage

Define simple POJO, please pay attention to defined annotations:

```
@Type(name = "user")
public class User {
  
  @Id
  private String id;
  private String name;
  
  @Relationship(name = "somerelationship")
  private SomeOtherResource resource;
  
  # getters and setters
}
```

Create a converter instance:

```
ResourceConverter converter = new ResourceConverter(User.class, SomeOtherResoruce.class);

// To convert raw data into POJO
byte [] rawResponse = ...get data from wire
User user = converter.readObject(rawResponse, User.class);

// To convert user back to raw byte
byte [] rawData = converter.writeObject(user);
```

##### Example usage with retrofit

As as first step, define your model classes and annotate them using annotations described above.

After defining models, define your service interfaces as you would usually do with 'standard' JSON/XML APIs.

To create retrofit instance:

```
Retrofit retrofit = new Retrofit.Builder()
		.baseUrl("https://yourapi")
		.addConverterFactory(new JSONAPIConverterFactory(User.class, SomeOtherResoruce.class))
		.build();
		
// Create services using service stubs and use it as usual.
```


