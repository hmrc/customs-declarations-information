This API is currently in beta - expect some breaking changes.

This API supersedes the /status-request/mrn/{mrn} endpoint on the Customs Declarations API, that was trialled in the Trade Test service.

The first release of this API provides a single endpoint for getting the status of a declaration by Movement Reference Number (MRN):

- /mrn/{mrn}/status

The MRN is the ID of the declaration assigned by customs.

Future releases of this API will provide endpoints for getting the status of a declaration by: 

- Declaration Unique Consignment Reference (DUCR)
- Unique Consignment Reference (UCR)
- Inventory Reference

The format of these endpoints will be:

- /ducr/{ducr}/status
- /ucr/{ucr}/status
- /inventory-reference/{inventory-reference}/status


If you would like to suggest other features or discuss priorities, [contact us](https://developer.service.hmrc.gov.uk/developer/support).
