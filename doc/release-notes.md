# Changelog

### 7.0.0

Major release upgrading the runtime platform to Java 17.

#### Breaking changes

* **Java 17 required** — minimum JVM raised from Java 11 to Java 17.
* **Guice 4.2.2 → 6.0.0** — Guice 6 removed several deprecated APIs and tightened circular-dependency detection. Review any direct Guice usage for removed APIs.
* **`commons-collections` removed** — the transitive `commons-collections:3.2.1` dependency is gone. If your code relied on it implicitly, add an explicit dependency or migrate to standard Java (`Collection.isEmpty()`, `new ArrayList<>()`, `new HashSet<>()`).

#### Dependency upgrades (`restler-core`)

| Dependency | Old | New        |
|---|---|------------|
| Java | 11 | 17         |
| Jackson databind | 2.14.2 | 2.21.4     |
| Guice | 4.2.2 | 6.0.0      |
| mongodb-driver-sync | 4.10.2 | 4.11.5     |
| Guava | 18.0 | 32.1.3-jre |
| commons-lang3 | 3.4 | 3.14.0     |
| swagger-annotations | 2.0.1 | 2.2.25     |
| javax.ws.rs-api | 2.0.1 | 2.1.1      |

#### Removed transitive dependencies

* `commons-collections:3.2.1`

---

### 6.1.5

* Bump Morphia from 2.4.15 to 2.4.20 to incorporate performance improvements on point queries

### 6.1.4
* Fix an issue with delete endpoint -> bulk deletes used to delete only one document.
* Bumps test-container version to 1.21.4 for compatibility with newer docker runtimes.

### 6.1.3

* Updated query sorting logic to support multiple fields.

### 6.1.2

* Bump morphia version from 2.4.4 to 2.4.15 to include a bug fix for embedded entity mapping exceptions.

### 6.1.1

* Fixed an issue with PUT endpoint and id param not present in the body of the request

### 6.1.0

* Expose metrics more structured
* Deprecations:
  * net.researchgate.restdsl.metrics.StatsReporter; to be replaced by net.researchgate.restdsl.metrics.MetricSink

### 6.0.2

* Fixed an issue with `totalItems` on `groupBy` queries returning incorrect results

### 6.0.1

* Restored error handling for duplicate key exceptions

### 6.0.0

* Backwards incompatible changes:
  * Upgraded to mongodb-driver-sync 4.10.2 and morphia 2.4.4
  * Upgraded from Java 8 to Java 11
  * Interface changes:
    * `preUpdate` now uses List<UpdateOperator> instead of UpdateOperations
    * `MongoBaseServiceDao::createUpdateOperations` has been removed
  * Morphia required changes
    * BasicDAO `morphiaDao` is no longer exposed. Use Datastore `datastore` instead. Examples:
      * instead of `morphiaDao.save(entity);`, use `datastore.save(entity);`
      * instead of `morphiaDao.createQuery().filter("test", 123).asList();` use `datastore.find(entityClazz).filter(Filters.eq("test", 123)).iterator().toList();`
    * Annotation changes
      * `@Embedded` is no longer supported. Use `@Entity` for embedded documents
      * Change `@Entity(value = "docs", noClassnameStored = true)` to `@Entity(value = "docs", useDiscriminator = false)`
  * For ServiceQuery projections, limit, offset, and order to be applied, use `MongoBaseServiceDao::get` or `getOne`
  * If you issue the query not via Restler, you can use `MongoBaseServiceDao::toFindOptions` to prepare the FindOptions from the ServiceQuery
  * Note if upgrading to MongoDB 4.2+:
    * If you use `MongoServiceDao::findAndModify` or a replace document operation that includes upsert: true and is on a sharded collection,
    * the filter must include an equality match on the full shard key.

### 5.0.0

* Backwards incompatible change: Upgraded mongoDB client to 4.1.1

### 4.0.0

* Backwards incompatible change: Upgraded morphia 1.3.2 to 1.5.8. This changes the morphia namespace from 'org.mongodb.morphia' to 'dev.morphia'

### 3.1.0

* Backwards incompatible change: The BaseServiceResource now uses a BaseServiceModel (was: ServiceModel), with a smaller api. If you need the api of the ServiceModel, you can keep a reference to the passed in ServiceModel to the constructor.
* Feature: Split out BaseServiceModel and BaseServiceDao that expose just the read restDsl
* Feature: It is now possible to prevent client from performing groupBy queries.

* Backwards incompatible change: Artifacts are no longer published to JCenter, but will use [GitHub packages](https://github.com/features/packages) instead at [restler packages](https://github.com/orgs/researchgate/packages?repo_name=restler)
