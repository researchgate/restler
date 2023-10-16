# Changelog

### 6.0.0
* Backwards incompatible changes: 
  * Upgraded to mongodb-driver-sync 4.10.2 and morphia 2.4.4
  * Upgraded from Java 8 to Java 11
  * Interface changes: 
    * preUpdate now uses List<UpdateOperator> instead of UpdateOperations
    * MongoBaseServiceDao::createUpdateOperations has been removed
  * Morphia required changes
    * BasicDAO `morphiaDao` is no longer exposed. Use Datastore `datastore` instead. Examples:
      * instead of `morphiaDao.save(entity);`, use `datastore.save(entity);`
      * instead of `morphiaDao.createQuery().filter("test", 123).asList();` use `datastore.find(entityClazz).filter(Filters.eq("test", 123)).iterator().toList();`
    * Annotation changes
      * `@Embedded` is no longer supported. Use `@Entity` for embedded documents
      * Change `@Entity(value = "docs", noClassnameStored = true)` to `@Entity(value = "docs", useDiscriminator = false)`
  * For ServiceQuery projections, limit, offset, and order to be applied, use MongoBaseServiceDao::get or getOne
  * If you issue the query not via Restler, you can use MongoBaseServiceDao::toFindOptions to prepare the FindOptions from the ServiceQuery

### 5.0.0
* Backwards incompatible change: Upgraded mongoDB client to 4.1.1

### 4.0.0
* Backwards incompatible change: Upgraded morphia 1.3.2 to 1.5.8. This changes the morphia namespace from 'org.mongodb.morphia' to 'dev.morphia'

### 3.1.0
 * Backwards incompatible change: The BaseServiceResource now uses a BaseServiceModel (was: ServiceModel), with a smaller api. If you need the api of the ServiceModel, you can keep a reference to the passed in ServiceModel to the constructor. 
 * Feature: Split out BaseServiceModel and BaseServiceDao that expose just the read restDsl 
 * Feature: It is now possible to prevent client from performing groupBy queries.

 * Backwards incompatible change: Artifacts are no longer published to JCenter, but will use [GitHub packages](https://github.com/features/packages) instead at [restler packages](https://github.com/orgs/researchgate/packages?repo_name=restler) 