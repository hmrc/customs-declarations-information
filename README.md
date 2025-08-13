
# Customs Declarations Information

The Customs Declarations Information API can be used to request the status and versions of a declaration.


## Development Setup
- Run locally: `sbt run` which runs on port `9834` by default
- Run with test endpoints: `sbt 'run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes'`

##  Service Manager Profiles
The Customs Declarations Information service can be run locally from Service Manager, using the following profiles:

| Profile Details                       | Command                                                           | Description                                                    |
|---------------------------------------|:------------------------------------------------------------------|----------------------------------------------------------------|
| CUSTOMS_DECLARATION_ALL               | sm2 --start CUSTOMS_DECLARATION_ALL                               | To run all CDS applications.                                   |
| CUSTOMS_INVENTORY_LINKING_EXPORTS_ALL | sm2 --start CUSTOMS_INVENTORY_LINKING_EXPORTS_ALL                 | To run all CDS Inventory Linking Exports related applications. |
| CUSTOMS_INVENTORY_LINKING_IMPORTS_ALL | sm2 --start CUSTOMS_INVENTORY_LINKING_IMPORTS_ALL                 | To run all CDS Inventory Linking Imports related applications. |

## Run Tests
- Run Unit Tests: `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report: `./run_all_tests.sh`<br/> which runs `clean coverage test it:test coverageReport dependencyUpdates"`

### Acceptance Tests
This repository does not have any associated Acceptance Tests.

### Performance Tests
To run performance tests, see [here](https://github.com/hmrc/customs-notification-gateway-performance-test).


## API documentation
For Customs Declarations API documentation, see [here](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/customs-declarations).

### Customs Declarations Information specific routes
| Path - internal routes prefixed by `/customs/declarations-information`           | Supported Methods |     | Description                                                                        |
|----------------------------------------------------------------------------------|:-----------------:|:----|------------------------------------------------------------------------------------|
| `/mrn/{mrn}/status`                                                              |        GET        |     | Retrieves the latest set of status details by MRN.                                 |
| `/ducr/{ducr}/status`                                                            |        GET        |     | Retrieves the latest set of status details by DUCR.                                |
| `/ucr/{ucr}/status`                                                              |        GET        |     | Retrieves the latest set of status details by UCR.                                 |
| `/inventory-reference/{inventoryReference}/status`                               |        GET        |     | Retrieves the latest set of status details by inventory reference.                 |
| `/mrn/{mrn}/version`                                                             |        GET        |     | Retrieves the latest versions of a declaration by MRN, with the most recent first. |
| `/mrn/{mrn}/full`                                                                |        GET        |     | Retrieves the full declaration by MRN.                                             |
| `/search?partyRole=submitter&declarationCategory=EX&goodsLocationCode=BELBELOB4` |        GET        |     | Retrieves matching declarations and returned in summary form.                      |


### Test-only specific routes
This service does not have any specific test-only endpoints.


### Lookup of `authenticatedEori` from `api-subscription-fields` service for CSPs only

The `X-Client-ID` header, together with the application context and version are used
 to call the `api-subscription-fields` service to get the `authenticatedEori` to pass on to the backend request (where it is known as `authenticatedPartyID`).

So there is a direct dependency on the `api-subscription-fields` service. Note the service to get the `authenticatedEori` is not currently stubbed. 

### Seeding Data in `api-subscription-fields` for local end to end testing

Make sure the [`api-subscription-fields`](https://github.com/hmrc/api-subscription-fields) service is running on port `9650`. Then run the below curl command.

Please note that version `1.0` is used as an example in the commands given and you should insert the customs declarations information api version number which you will call subsequently.

Please note that value `d65f2252-9fcf-4f04-9445-5971021226bb` is used as an example in the commands given and you should insert the UUID value which suits your needs.

    curl -v -X PUT "http://localhost:9650/field/application/d65f2252-9fcf-4f04-9445-5971021226bb/context/customs%2Fdeclarations-information/version/1.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "authenticatedEori" : "GB123456789000" } }'

When you then send a request to `customs-declarations-information` make sure you have the HTTP header `X-Client-ID` with the value `d65f2252-9fcf-4f04-9445-5971021226bb`

There is no lookup of `api-subscription-fields` for non-CSP requests.

## Switching service endpoints

Dynamic switching of service endpoints has been implemented for the declaration status connector. To configure dynamic
switching of the endpoint there must be a corresponding section in the application config file
(see example below). This should contain the endpoint config details.


### Example
The service `customs-declarations-information` has a `default` configuration and a `stub` configuration. Note
that `default` configuration is declared directly inside the `customs-declarations-information` section.

    Prod {
        ...
        services {
          ...

          declaration-status {
            host = some.host
            port = 80
            bearer-token = "some_token"
            context = /declarations/querystatusinformation/v1

            stub {
              host = localhost
              port = 9479
              bearer-token = "some_stub_token"
              context = /declarations/querystatusinformation/v1
            }
          }
        }
    }
    
### Switch service configuration to stub for an endpoint

#### REQUEST
    default version (application/vnd.hmrc.1.0+xml):
    curl -X "POST" http://customs-declarations-information-host/test-only/service/declaration-status/configuration -H 'content-type: application/json' -d '{ "environment": "stub" }'
    
#### RESPONSE

    The service customs-declarations-information is now configured to use the stub environment

### Switch service configuration to default for an endpoint

#### REQUEST

    curl -X "POST" http://customs-declarations-information-host/test-only/service/declaration-status/configuration -H 'content-type: application/json' -d '{ "environment": "default" }'

#### RESPONSE


    The service customs-declarations-information is now configured to use the default environment

### Get the current configuration for a service

#### REQUEST

    curl -X "GET" http://customs-declarations-information-host/test-only/service/declaration-status/configuration

#### RESPONSE

    {
      "service": "declaration-status",
      "environment": "stub",
      "url": "http://currenturl/customs-declarations-information"
      "bearerToken": "current token"
    }


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
