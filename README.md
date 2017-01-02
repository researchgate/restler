Restler  
======================

[![Build Status](https://travis-ci.org/researchgate/restler.svg?branch=master)](https://travis-ci.org/researchgate/restler)

Restler is a project aiming on providing a unified way to easily build REST-services based on document-oriented databases, like MongoDB. It automatically exposes CRUD operations for domain entities without sacrificing flexibility, should you need some extra functionality or business-logic. 

The project consists of two parts: 

* **restler-core** - implementation of the query language for REST resources (that's what you should include into your service)
* **restler-service** - example Dropwizard-based (http://www.dropwizard.io/) web service that uses restler-core and demonstrates its features. For you own service you can simply take this service as a base.



Default CRUD Resource
--------------

**restler** provides default CRUD operations for out of the box. The big focus is made on the _uniform_ data retrieval (that,is a `GET` method), when there is just 1 retrieval endpoint where you can specify **what** (via URL matrix parameters) and **how** (via query parameters) you want to retrieve data.

For the sake of example, let's assume that you have an `Account` entity that you store in MongoDB, that looks as follows:

```java
// Morphia annotations for MongoDB collection name and indexes and 
@Entity("accounts")
         @Index(fields = @Field("rating")),
         @Index(fields = {@Field("stats.scoreBreakdown"), @Field(value = "rating", type = IndexType.DESC)}),
         @Index(fields = @Field("nickname"))
public class Account {

	// MongoDB primary key (_id)
    @Id
    @JsonSerialize(using = ObjectIdSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;

    private List<Long> publicationUids;

    private Boolean deleted;

    private List<Publication> publications;

    @JsonSerialize(using = ObjectIdSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId mentorAccountId;

    private Long rating;

    private AccountState state;

	// different field name stored in MongoDB
    @Property("creaetd")
    private Date createdAt;

	// different field name stored in MongoDB
    @Property("modified")
    private Date modifiedAt;

    private Date longDate;

    private AccountStats stats;

    private List<AccountStats> additionalStats;

    private String nickname;

    ...
    
}
```

Restler will take also care of:
* Transforming ids and criteria from URL to the right types in the code
* Validating whether your query is `safe` to execute, e.g. it uses MongoDB indexes so that full table scan is not performed. 

### GET ####

URL: `/accounts/$segment?$query_params`

`$segment=$id1;$id2;...;key1=value1;key2=value2;...;`

Typical `$query_params`:

* `limit (Integer)` - maximum number of records to fetch
* `offset (Integer)` - how many records to skip
* `order (String)` - order by a certain field
* `fields (String)` - comma-separated list of fields to return 
* `groupBy (String)` - group by a certain field
* `indexValidation (boolean)` - whether enable index validation in your DAO (`true` by default)

Examples:

* `accounts/5718ba18f53e2e6b86f155d1,5718ba19f53e2e6b86f155d7`
	* get two accounts by their ids of type Mongo's `ObjectId`
* `accounts/-;stats.scoreBreakdown=3?fields=rating,stats.followerCnt`
	* get accounts where array `stats.scoreBreakdown` contains `3`. Returning fields `rating` 	and a nested field `stats.followerCnt`
* `accounts/-;rating__ne=0?groupBy=mentorAccountId&limit=2&order=rating`
	* Getting all accounts whose rating is not 0,  grouped by mentorAccountId and returned top 2 accounts with highest rating per each mentor id.	
* `accounts/-;rating >=3;nickname:$null?limit=10&offset=5` or `accounts/-;rating__gt=3;nickname:$null?limit=10&offset=5`
	* get all accounts whose rating is more than `3` and `nickname` is not present in DB (with limit and offset)

By default the framework reads index information about the collection and forbids queries that don't use an index. For debugging purposes this validation can be disabled by the `indexValidation=false` query parameter

IMPORTANT: If values in the criteria contain reserved or illegal symbols, like space, '=', ';', etc., the URL must be URL-encoded. For example, the '=' sign is used as a key-value separator. In order have to express <= or >=, you have to duplicate the equals sign and URL encode it, e.g. `rating%20>%3D=20` means rating is more or equal than 20 (space got encoded as well). Alternatively, one can use analogous operations that don't require encoding.

##### Alternative syntax for comparison operations

In order to avoid URL encoding when testing e.g. from a browser (otherwise, you must always encode URL), there is an alternative syntax, e.g. `rating__gte=20` -- returns documents where rating field is greater than 20. Supported operations and their meanings:

* gt : >
* gte : >=
* lt : <
* lte : <=
* ne : <> 

#### Advanced Operations

##### Group by

Add a `groupBy=$fieldName` as a query parameter: returned results will be grouped by this field. Provided limit will be applied for each group.

##### Querying for documents that match criteria in the same element in the array 
This is analogue of Mongo's `$elementMatch` operator. For this provide a `syncMatch=$field1,$field$` query parameter.

Assume that `Account` has an array of stats objects that contain `folllowerCnt` and `publicationCnt`. E.g. one account contains `stats=[(1,1), (2,2)]`. Then query: 

`/accounts/;stats.publicationCnt=1;stats.followerCnt=2?`

will return this object because a `stats` array contains elements where `followerCnt==1` and `publicationCnt==2`, whereas a query: 

`/accounts/;stats.publicationCnt=1;stats.followerCnt=2?syncMatch=stats`

will return 0 elements, because the criteria is checked for each element individually. 


#### Other details

##### Reserved keywords

* `$null` - represents null value
* `$any` - mostly used for overwriting default query parameters that exist for resource. E.g. `-;deleted=false` could be a default parameter, but in some cases you want to retrieve everything
* `$exists` - checks whether value exists.

##### Query info

Since typially resources have a default list of fields, some limit and maybe default criteria, it's important to know which query will be ultimately made. For this just append `info` to the normal get query:
URL: `/accounts/$segment/info?$query_params`

It will returned the final query fields, it's URL form, so that it can be pasted into the browser URL bar, and also whether query is safe to use, i.e. it uses indexes.  

##### Counting objects without returning results

Just provide `limit=0` query parameter and read the `totalItems` field from the response.
Note: this behaviour is different from Morphia's where 0 limit is considered to be a query _without_ a limit. 


### DELETE ####

URL: `/accounts/$segment?$query_params`

Deletion can be done not only by id but also by criteria. Deletion without specifying ids or criteria is forbidden for security reasons.

### POST ####

URL: `/accounts/`

### PUT ####

URL: `/accounts/$id`


# For developers

## Project setup
You should include restler functionality by including:

```gradle
compile group: 'net.researchgate', name: 'restler', version: '$restler-version'
```

## Exceptions and their mapping

In case of restler-specific errors a `RestDslException` will be thrown. It's unchecked exception. This exception has a type attribute:

* `PARAMS_ERROR` - thrown when a REST request contains some errors in its syntax. 
* `QUERY_ERROR` - thrown when ServiceQuery (most often manually constructed) has some errors. 
* `ENTITY_ERROR` - thrown when entity to be persisted/modified is invalid or violates some constrains. 
* `DUPLICATE_KEY` - thrown when entity to be persisted/modified is a duplicate of some sort, e.g. violates unique index in Mongo. 
* `GENERAL_ERROR`- unknown error when something unpredictable went wrong, e.g. implementation error or MongoDB is not reachable. 

In order to map those exceptions correctly (i.e. with semantically correct HTTP response code), you can refer to `ServiceExceptionMapper` from the `restler-service` project. Mappings from an exception type to HTTP response code:

* `PARAMS_ERROR` - BAD REQUEST 400
* `QUERY_ERROR` - BAD REQUEST 400
* `ENTITY_ERROR` - BAD REQUEST 400
* `DUPLICATE_KEY` - CONFLICT 409
* `GENERAL_ERROR` -  INTERNAL SERVER ERROR 500

## Usage in code

Main classes:

* ServiceQuery - representation of a query to a storage.
* MongoServiceDao - DAO for MongoDB
* ServiceModel - a basic model that implements typical CRUD operations
* ServiceResource - a basic resource that exposes CRUD operations

Just extend a corresponding class (dao, model or resource) with your types for primary key and entity. If you need just CRUD, it's likely that you won't have to do anything more. 

You can always look at restler-service module, to see how these classes are supposed to be used. 

## Query Shapes

Since the GET endpoint is pretty flexible it's becomes more important to understand how it is used and if we have performance problems what access patterns cause them. For this a query shapes functionality exists in restler. By providing an implementation of the `StatsReporter` interface, the rest you will get for free.

One of the possibilities is to log query shapes to graphite. E.g. under `$servicePath/queries/shapes`. The all grouped by a Mongo collection name (`accounts` in the example below).

The format of a query shape can be described by the following regex:

`("IDS")?(-"CRITERIA"-(fieldName_)*fieldName)?(-"ORDER"-fieldName)?(-"GROUPBY"-fieldName)?(-"LIMIT")`
  
where `fieldName` is a field name from your entity. 

* `IDS` means that a primary key (_id) were provided into the query
* `CRITERIA` tells that filtering was made on those additional fields
* `ORDER` – sorting was done on a particular field 
* `GROUPBY` returned results will be grouped together by the field provided& 
* `LIMIT` - a query contained a limit. Typically when querying by a criteria a limit should be provided.

#### Examples

* `-CRITERIA-accountId_rating-ORDER–createdAt`
	* A query was filtering on "accountId" and "rating" fields
	* Sorting was done on "createdAt" field descending
* `IDS-CRITERIA-nickname_state`
	* `IDS` means that primary keys were provided 
	* Additionally those entities were filtered on "nickname" and "state" fields
* `-CRITERIA-nickname_state_rating-ORDER--createdAt-GROUPBY-mentorAccountId-LIMIT`
	* Filtering on three fields (nickname, state, rating)
	* Ordering by "createdAt" descending (note the minus) 
	* Grouping the returned results by the "mentorAccountId" field
	* Limiting every group result to some amount of entries


