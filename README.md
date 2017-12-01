### jsonapi-converter
This is a fork from https://github.com/jasminb/jsonapi-converter
To meet DPIM/Concerto api's needs for consistency

* US40429 Link as Map<String, String> instead of Map<String, Object> in order to be consistent
* DE24253 Concerto costim links in resource's property (List<Links>) is shadowed by base class, JSONAPI-converter's Links
* remove attribute {} when there is any property in the resource