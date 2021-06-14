# Changelog

### 3.1.0
 * Backwards incompatible change: The BaseServiceResource now uses a BaseServiceModel (was: ServiceModel), with a smaller api. If you need the api of the ServiceModel, you can keep a reference to the passed in ServiceModel to the constructor. 
 * Feature: Split out BaseServiceModel and BaseServiceDao that expose just the read restDsl 
 * Feature: It is now possible to prevent client from performing groupBy queries. 