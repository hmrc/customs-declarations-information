# customs-declarations-information

## About
This microservice is part of the Customs Declaration Service (CDS) APIs. It is consumed by third-party software through the API Platform.

The Customs Declarations Information API can be used to request the status and versions of a declaration.

| Key | Value |
|---|---|
| Digital service | CDS |
| Local port | `9834` |
| Base path | `/customs/declarations-information` |
| API versions | `1.0` and `2.0` (see [Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/customs-declarations-information)) |
| Front-end | Not applicable — this is a public API consumed through the API Platform |
| Acceptance tests | [customs-automation-test](https://github.com/hmrc/customs-automation-test) |
| Performance tests | [customs-declaration-performance-test](https://github.com/hmrc/customs-declaration-performance-test) |
| Stubs used locally | `CUSTOMS_API_HODS_STUBS` ([customs-api-hods-stubs](https://github.com/hmrc/customs-api-hods-stubs), stubs the CDS backend) |

## How to Run This Service

### Prerequisites
Use [Service Manager](#service-manager-profiles) to start all the services that this service depends on:

```bash
sm2 --start CUSTOMS_DECLARATION_ALL
```

### Running the Service Locally
To run this service from source (for example, to test your own changes), stop the instance started by Service Manager and run it with sbt:

```bash
sm2 --stop CUSTOMS_DECLARATIONS_INFORMATION
sbt run
```

The service runs on port `9834` by default. You can also specify the port explicitly:

```bash
sbt "run 9834"
```

To call it as a CSP you must first seed `api-subscription-fields`, see [Seeding Data in api-subscription-fields](#seeding-data-in-api-subscription-fields-for-local-end-to-end-testing).

## How to Test This Service

### Local

#### Unit Tests
```bash
sbt test
```

To run tests for a single file:

```bash
sbt "test/testOnly *TEST_FILE_NAME*"
```

#### Integration Tests
```bash
sbt it/test
```

#### Pre-Push Check
There is a script called `run_all_tests.sh` that runs all unit and integration tests, examines their coverage and checks for dependency updates.
It is good practice to run it just before pushing to GitHub.

```bash
./run_all_tests.sh
```

#### Test-Only Endpoints
To enable these endpoints you must specify at startup the test-only conf file:

```bash
sbt 'run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes'
```

The only test-only endpoints are the dynamic service configuration endpoints under `/test-only/service`, see [Switching service endpoints](#switching-service-endpoints).

#### Acceptance Tests (smoke and regression)
The acceptance tests for this service are in [customs-automation-test](https://github.com/hmrc/customs-automation-test). For information about how to run them, and which environments they run against, see [README](https://github.com/hmrc/customs-automation-test#running-the-tests).

#### Performance tests
The performance tests for this service are in [customs-declaration-performance-test](https://github.com/hmrc/customs-declaration-performance-test). For information about how to run them, and which environments they run against, see [README](https://github.com/hmrc/customs-declaration-performance-test#running-the-tests).

### Manual Testing in QA and Staging
After the successful [deployment pipeline](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSAPIs/job/customs-declarations-information-pipeline/), manual testing is performed in the QA, Staging and/or External Test environments, see the [customs-automation-test README](https://github.com/hmrc/customs-automation-test#running-the-tests).

## Service Catalogue
- [customs-declarations-information in the MDTP Catalogue](https://catalogue.tax.service.gov.uk/repositories/customs-declarations-information)

## Jenkins Pipeline
- [Build pipeline](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSAPIs/job/customs-declarations-information/)
- [Deployment pipeline](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSAPIs/job/customs-declarations-information-pipeline/)

- The acceptance test Jenkins jobs are [cds-api-acceptance-tests-local](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSAPIs/job/cds-api-acceptance-tests-local/) (against Service Manager) and [cds-api-acceptance-tests-dev](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSAPIs/job/cds-api-acceptance-tests-dev/) (against Development).
- The performance test Jenkins job is listed in the [customs-declaration-performance-test README](https://github.com/hmrc/customs-declaration-performance-test#jenkins).

## Service Manager Profiles
These profiles are defined in [service-manager-config](https://github.com/hmrc/service-manager-config).

| Profile | Use | Services started |
|---|---|---|
| `CUSTOMS_DECLARATION_ALL` | Running and developing this service locally | `CUSTOMS_DECLARATIONS`, `CUSTOMS_DECLARATIONS_INFORMATION`, `CUSTOMS_DECLARATIONS_METRICS`, `CUSTOMS_NOTIFICATION`, `CUSTOMS_NOTIFICATION_GATEWAY`, `CUSTOMS_NOTIFICATIONS_RECEIVER_STUB`, `CUSTOMS_API_HODS_STUBS`, `API_SUBSCRIPTION_FIELDS`, `API_NOTIFICATION_QUEUE`, `API_NOTIFICATION_PULL`, `AUTH`, `AUTH_LOGIN_API`, `AUTH_LOGIN_STUB`, `CENTRALISED_AUTHORISATION_SERVER`, `IDENTITY_VERIFICATION`, `OAUTH_FRONTEND`, `THIRD_PARTY_APPLICATION`, `USER_DETAILS`, `DATASTREAM`, `API_PLATFORM_TEST_USER` |
| `CDS_API_ATS` | Running the [acceptance tests](https://github.com/hmrc/customs-automation-test) | See [service-manager-config](https://github.com/hmrc/service-manager-config/blob/main/profiles.json) |
| `CDS_API_ALL` | All CDS API services (declarations, inventory linking, notifications and this service) | See [service-manager-config](https://github.com/hmrc/service-manager-config/blob/main/profiles.json) |

| Service | Port | Repository |
|---|---|---|
| `CUSTOMS_DECLARATIONS_INFORMATION` | 9834 | This service |
| `CUSTOMS_API_HODS_STUBS` | 9479 | [customs-api-hods-stubs](https://github.com/hmrc/customs-api-hods-stubs) |
| `API_SUBSCRIPTION_FIELDS` | 9650 | [api-subscription-fields](https://github.com/hmrc/api-subscription-fields) |

## Endpoints

All paths are relative to the service root (`http://localhost:9834/customs/declarations-information` when running locally) and need an [authenticated caller](#authentication-and-enrolment).

Every request must carry an `Accept` header of `application/vnd.hmrc.1.0+xml` or `application/vnd.hmrc.2.0+xml`. CSP requests must also carry at least one of `X-Badge-Identifier` or `X-Submitter-Identifier`.

| Method | Path | Purpose | Sample request | Response |
|---|---|---|---|---|
| GET | `/mrn/:mrn/status` | Retrieves the latest set of status details by MRN. | `/mrn/20GB52P19SNIHX8PA6/status` | `200` with `DeclarationStatusResponse` XML |
| GET | `/ducr/:ducr/status` | Retrieves the latest set of status details by DUCR. | `/ducr/8GB796458895018-G8536C/status` | `200` with `DeclarationStatusResponse` XML |
| GET | `/ucr/:ucr/status` | Retrieves the latest set of status details by UCR. | `/ucr/9GB010969918000-5A8BD3F/status` | `200` with `DeclarationStatusResponse` XML |
| GET | `/inventory-reference/:inventoryReference/status` | Retrieves the latest set of status details by inventory reference. | – | `200` with `DeclarationStatusResponse` XML |
| GET | `/mrn/:mrn/version` | Retrieves the latest versions of a declaration by MRN, with the most recent first. | `/mrn/20GB52P19SNIHX8PA6/version` | `200` with `DeclarationVersionResponse` XML |
| GET | `/mrn/:mrn/full` | Retrieves the full declaration by MRN. | `/mrn/20GB52P19SNIHX8PA6/full`<br>Optional: `declarationVersion` | `200` with `DeclarationFullResponse` XML |
| GET | `/search` | Retrieves matching declarations and returned in summary form. | `/search?partyRole=submitter&declarationCategory=EX&goodsLocationCode=BELBELOB4`<br>Required: `partyRole`, `declarationCategory`<br>Optional: `goodsLocationCode`, `declarationStatus`, `dateFrom`, `dateTo`, `pageNumber` | `200` with `DeclarationSearchResponse` XML |

For error responses and codes, see the [Developer Hub documentation](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/customs-declarations-information).

### Example payloads

Full request and response examples for every endpoint and version are in `public/api/conf/<version>/examples` and on the Developer Hub.

#### Status by DUCR

`GET /ducr/8GB796458895018-G8536C/status`

```text
Accept: application/vnd.hmrc.2.0+xml
Authorization: Bearer <token>
X-Client-ID: d65f2252-9fcf-4f04-9445-5971021226bb
```

Example response, taken from `public/api/conf/2.0/examples/declarationInformationRetrievalDucrStatusResponseExample.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<p:DeclarationStatusResponse
        xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
        xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
        xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2"
        xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
        xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/status/v2">
    <p:DeclarationStatusDetails>
        <p:Declaration>
            <p:AcceptanceDateTime>
                <p1:DateTimeString formatCode="304">20200510113028Z</p1:DateTimeString>
            </p:AcceptanceDateTime>
            <p:ID>20GB56FWPJ8JPFGVA1</p:ID>
            <p:VersionID>1</p:VersionID>
            <p:ReceivedDateTime>
                <p:DateTimeString formatCode="304">20200510113028Z</p:DateTimeString>
            </p:ReceivedDateTime>
            <p:ROE>H</p:ROE>
            <p:ICS>14</p:ICS>
        </p:Declaration>
        <p2:Declaration>
            <p2:FunctionCode>9</p2:FunctionCode>
            <p2:TypeCode>EXD</p2:TypeCode>
            <p2:GoodsItemQuantity>1</p2:GoodsItemQuantity>
            <p2:TotalPackageQuantity>1.0</p2:TotalPackageQuantity>
            <p2:Submitter>
                <p2:ID>GB025115166433</p2:ID>
            </p2:Submitter>
            <p2:GoodsShipment>
                <p2:PreviousDocument>
                    <p2:ID>8GB796458895018-G8536C</p2:ID>
                    <p2:TypeCode>DCR</p2:TypeCode>
                </p2:PreviousDocument>
            </p2:GoodsShipment>
        </p2:Declaration>
    </p:DeclarationStatusDetails>
</p:DeclarationStatusResponse>
```

#### Error response

```xml
<errorResponse>
  <code>BAD_REQUEST</code>
  <message>Missing MRN parameter</message>
</errorResponse>
```

### Authentication and enrolment
Requests arrive through the API Platform with an OAuth bearer token in the `Authorization` header. They are authorised in one of two ways, tried in this order:

| Caller | Auth provider | Requirement |
|---|---|---|
| CSP | Privileged application | `write:customs-declarations-information` scope |
| Trader (non-CSP) | Government Gateway | Enrolment below |

Redirect URL: Not applicable — this service has no frontend.

| Enrolment Key | Identifier Name | Identifier Value (example) |
|---|---|---|
| `HMRC-CUS-ORG` | `EORINumber` | `GB123456789000` |

## Developer Notes
### Lookup of `authenticatedEori` from `api-subscription-fields` service for CSPs only

The `X-Client-ID` header, together with the application context and version are used
 to call the `api-subscription-fields` service to get the `authenticatedEori` to pass on to the backend request (where it is known as `authenticatedPartyID`).

So there is a direct dependency on the `api-subscription-fields` service. Note the service to get the `authenticatedEori` is not currently stubbed. 

### Seeding Data in `api-subscription-fields` for local end to end testing

Make sure the [`api-subscription-fields`](https://github.com/hmrc/api-subscription-fields) service is running on port `9650`. Then run the below curl command.

Please note that version `1.0` is used as an example in the commands given and you should insert the customs declarations information api version number which you will call subsequently.

Please note that value `d65f2252-9fcf-4f04-9445-5971021226bb` is used as an example in the commands given and you should insert the UUID value which suits your needs.

```bash
curl -v -X PUT "http://localhost:9650/field/application/d65f2252-9fcf-4f04-9445-5971021226bb/context/customs%2Fdeclarations-information/version/1.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "authenticatedEori" : "GB123456789000" } }'
```

When you then send a request to `customs-declarations-information` make sure you have the HTTP header `X-Client-ID` with the value `d65f2252-9fcf-4f04-9445-5971021226bb`

There is no lookup of `api-subscription-fields` for non-CSP requests.

### Switching service endpoints

Dynamic switching of service endpoints has been implemented for the declaration status, declaration version, declaration full and declaration search connectors. To configure dynamic
switching of the endpoint there must be a corresponding section in the application config file
(see example below). This should contain the endpoint config details.


#### Example
The service `customs-declarations-information` has a `default` configuration and a `stub` configuration. Note
that `default` configuration is declared directly inside the connector's section (for example `declaration-status`).

```
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
```

#### Switch service configuration to stub for an endpoint

##### REQUEST
```
default version (application/vnd.hmrc.1.0+xml):
curl -X "POST" http://customs-declarations-information-host/test-only/service/declaration-status/configuration -H 'content-type: application/json' -d '{ "environment": "stub" }'
```

##### RESPONSE

```
The service customs-declarations-information is now configured to use the stub environment
```

#### Switch service configuration to default for an endpoint

##### REQUEST

```
curl -X "POST" http://customs-declarations-information-host/test-only/service/declaration-status/configuration -H 'content-type: application/json' -d '{ "environment": "default" }'
```

##### RESPONSE


```
The service customs-declarations-information is now configured to use the default environment
```

#### Get the current configuration for a service

##### REQUEST

```
curl -X "GET" http://customs-declarations-information-host/test-only/service/declaration-status/configuration
```

##### RESPONSE

```
{
  "service": "declaration-status",
  "environment": "stub",
  "url": "http://currenturl/customs-declarations-information"
  "bearerToken": "current token"
}
```

## License
This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
