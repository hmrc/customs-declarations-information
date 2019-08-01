
# Customs Declarations Information

This API permits requesting the status of a declaration.

This endpoint is available to CSP users only. The CSP user must supply a movement reference number (MRN) as a parameter in the request URL. The endpoint returns a set of latest status details for the MRN.


## GET Status Request 
 ### `GET /mrn/{valid mrn}/status`

 ### curl command
 ```
  curl -v -X GET "http://localhost:9000/mrn/{valid mrn}/status" \
   -H 'Accept: application/vnd.hmrc.1.0+xml' \
   -H 'Authorization: Bearer {ADD VALID TOKEN}' \
   -H 'X-Badge-Identifier: {Badge Id}' \
   -H 'X-Client-ID: {Valid Client Id}' \
   -H 'cache-control: no-cache' 
 
 ```
---


### Lookup of `fieldsId` UUID from `api-subscription-fields` service

The `X-Client-ID` header, together with the application context and version are used
 to call the `api-subscription-fields` service to get the unique `fieldsId` UUID to pass on to the backend request (where it is known as `clientID`).

So there is now a direct dependency on the `api-subscription-fields` service. Note the service to get the `fieldsId` is not currently stubbed. 

### Seeding Data in `api-subscription-fields` for local end to end testing

Make sure the [`api-subscription-fields`](https://github.com/hmrc/api-subscription-fields) service is running on port `9650`. Then run the below curl command.

Please note that version `1.0` is used as an example in the commands given and you should insert the customs declarations api version number which you will call subsequently.

Please note that value `d65f2252-9fcf-4f04-9445-5971021226bb` is used as an example in the commands given and you should insert the UUID value which suits your needs.

    curl -v -X PUT "http://localhost:9650/field/application/d65f2252-9fcf-4f04-9445-5971021226bb/context/customs%2Fdeclarations-information/version/1.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "" : "" } }'

We then have to manually reset the `fieldId` field to match the id expected by the downstream services. In a MongoDB command
window paste the following, one after the other.

    use api-subscription-fields

    db.subscriptionFields.update(
        { "clientId" : "d65f2252-9fcf-4f04-9445-5971021226bb", "apiContext" : "customs/declarations-information", "apiVersion" : "1.0" },
        { $set:
            {"fieldsId" : "d65f2252-9fcf-4f04-9445-5971021226bb"}
        }
    )
    
When you then send a request to `customs-declarations-information` make sure you have the HTTP header `X-Client-ID` with the value `d65f2252-9fcf-4f04-9445-5971021226bb`


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
            context = /declarations/retrieveinformation/v1

            stub {
              host = localhost
              port = 9479
              bearer-token = "some_stub_token"
              context = /declarations/retrieveinformation/v1
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
