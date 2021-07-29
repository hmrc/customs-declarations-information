
# Customs Declarations Information

This API permits requesting the status and versions of a declaration.


## GET Status Request 

The user must supply an MRN, DUCR, UCR or inventory reference as a parameter in the request URL. The endpoint returns the latest set of status details.

 ### `GET /mrn/{mrn}/status`
 ### `GET /ducr/{ducr}/status`
 ### `GET /ucr/{ucr}/status`
 ### `GET /inventory-reference/{inventoryReference}/status`

 ### curl command (using mrn as an example)
 ```
  curl -v -X GET "http://localhost:9000/mrn/{valid mrn}/status" \
   -H 'Accept: application/vnd.hmrc.1.0+xml' \
   -H 'Authorization: Bearer {ADD VALID TOKEN}' \
   -H 'X-Badge-Identifier: {Badge Id}' \
   -H 'X-Submitter-Identifier: {Submitter Id}' \
   -H 'X-Client-ID: {Valid Client Id}' \
   -H 'cache-control: no-cache' 

 ```
---
## GET Version Request

The user must supply an MRN as a parameter in the request URL. The endpoint returns the latest versions of a declaration with the most recent first.

 ### `GET /mrn/{mrn}/version`

### curl command
 ```
  curl -v -X GET "http://localhost:9000/mrn/{valid mrn}/version" \
   -H 'Accept: application/vnd.hmrc.1.0+xml' \
   -H 'Authorization: Bearer {ADD VALID TOKEN}' \
   -H 'X-Badge-Identifier: {Badge Id}' \
   -H 'X-Submitter-Identifier: {Submitter Id}' \
   -H 'X-Client-ID: {Valid Client Id}' \
   -H 'cache-control: no-cache' 

 ```
---
## GET Full Request

The user must supply an MRN as a parameter in the request URL. An optional declarationVersion query parameter can also be supplied. The full declaration is returned.

### `GET /mrn/{mrn}/full`

### curl command
 ```
  curl -v -X GET "http://localhost:9000/mrn/{valid mrn}/full" \
   -H 'Accept: application/vnd.hmrc.1.0+xml' \
   -H 'Authorization: Bearer {ADD VALID TOKEN}' \
   -H 'X-Badge-Identifier: {Badge Id}' \
   -H 'X-Submitter-Identifier: {Submitter Id}' \
   -H 'X-Client-ID: {Valid Client Id}' \
   -H 'cache-control: no-cache' 

 ```
---
## GET Search Request

The user must supply a partyRole and a declarationCategory. All other parameters are optional. All matching declarations that the consumer is permissioned to view are returned in summary form.

### `GET /search?partyRole=submitter&declarationCategory=EX&goodsLocationCode=BELBELOB4`

### curl command
 ```
  curl -v -X GET "http://localhost:9000/search?partyRole=submitter&declarationCategory=EX&goodsLocationCode=BELBELOB4" \
   -H 'Accept: application/vnd.hmrc.1.0+xml' \
   -H 'Authorization: Bearer {ADD VALID TOKEN}' \
   -H 'X-Badge-Identifier: {Badge Id}' \
   -H 'X-Submitter-Identifier: {Submitter Id}' \
   -H 'X-Client-ID: {Valid Client Id}' \
   -H 'cache-control: no-cache' 

 ```
---

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
