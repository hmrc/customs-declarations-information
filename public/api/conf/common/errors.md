We use standard HTTP status codes to show whether an API request succeeded or not. 

**Status endpoint error scenarios**

<table>
  <tbody>
    <tr>
      <td><b>Scenario</b></td>
      <td><b>HTTP Status</b></td>
      <td><b>Code</b></td>
      <td><b>Message</b></td>
    </tr>
    <tr>
      <td>Declaration not found for given MRN, DUCR, UCR or Inventory Reference</td>
      <td>404 (Not Found)</td>
      <td>CDS60001</td>
      <td>Declaration not found</td>
    </tr>
    <tr>
      <td>Invalid MRN, DUCR, UCR or Inventory Reference</td>
      <td>400 (Bad Request)</td>
      <td>CDS60002</td>
      <td>Search parameter invalid</td>
    </tr>
    <tr>
      <td>Missing MRN, DUCR, UCR or Inventory Reference</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>Missing search parameter</td>
    </tr>
    <tr>
      <td>MRN, DUCR, UCR or Inventory Reference too long</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>Search parameter too long</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier and X-Badge-Identifier http headers are missing</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>Both X-Submitter-Identifier and X-Badge-Identifier headers are missing</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier is invalid</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>X-Submitter-Identifier header is invalid</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier header is invalid</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>X-Badge-Identifier header is invalid</td>
    </tr>
    <tr>
        <td>A firewall rejected the request</td>
        <td>403 (Payload Forbidden)</td>
        <td>PAYLOAD_FORBIDDEN</td>
        <td>A firewall rejected the request</td>
    </tr>
    <tr>
      <td>Internal server error</td>
      <td>500</td>
      <td>CDS60003 or INTERNAL_SERVER_ERROR</td>
      <td>Internal server error</td>
    </tr>
  </tbody>
</table>
<br>

**Version endpoint error scenarios**

<table>
  <tbody>
    <tr>
      <td><b>Scenario</b></td>
      <td><b>HTTP Status</b></td>
      <td><b>Code</b></td>
      <td><b>Message</b></td>
    </tr>
    <tr>
      <td>Declaration not found for given MRN</td>
      <td>404 (Not Found)</td>
      <td>CDS60001</td>
      <td>Declaration not found</td>
    </tr>
    <tr>
      <td>Invalid MRN</td>
      <td>400 (Bad Request)</td>
      <td>CDS60002</td>
      <td>MRN parameter invalid</td>
    </tr>
    <tr>
      <td>Missing MRN</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>Missing MRN parameter</td>
    </tr>
    <tr>
      <td>MRN too long</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>MRN parameter too long</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier and X-Badge-Identifier http headers are missing</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>Both X-Submitter-Identifier and X-Badge-Identifier headers are missing</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier is invalid</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>X-Submitter-Identifier header is invalid</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier header is invalid</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>X-Badge-Identifier header is invalid</td>
    </tr>
    <tr>
        <td>A firewall rejected the request</td>
        <td>403 (Payload Forbidden)</td>
        <td>PAYLOAD_FORBIDDEN</td>
        <td>A firewall rejected the request</td>
    </tr>
    <tr>
      <td>Internal server error</td>
      <td>500</td>
      <td>CDS60003 or INTERNAL_SERVER_ERROR</td>
      <td>Internal server error</td>
    </tr>
  </tbody>
</table>
<br>

**Full endpoint error scenarios**

<table>
  <tbody>
    <tr>
      <td><b>Scenario</b></td>
      <td><b>HTTP Status</b></td>
      <td><b>Code</b></td>
      <td><b>Message</b></td>
    </tr>
    <tr>
      <td>Declaration not found for given MRN</td>
      <td>404 (Not Found)</td>
      <td>CDS60001</td>
      <td>Declaration not found</td>
    </tr>
    <tr>
      <td>Invalid MRN</td>
      <td>400 (Bad Request)</td>
      <td>CDS60002</td>
      <td>MRN parameter invalid</td>
    </tr>
    <tr>
      <td>Missing MRN</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>Missing MRN parameter</td>
    </tr>
    <tr>
      <td>MRN too long</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>MRN parameter too long</td>
    </tr>
    <tr>
      <td>Invalid declarationVersion parameter</td>
      <td>400</td>
      <td>BAD_REQUEST</td>
      <td>Invalid declarationVersion parameter</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier and X-Badge-Identifier http headers are missing</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>Both X-Submitter-Identifier and X-Badge-Identifier headers are missing</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier is invalid</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>X-Submitter-Identifier header is invalid</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier header is invalid</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>X-Badge-Identifier header is invalid</td>
    </tr>
    <tr>
        <td>A firewall rejected the request</td>
        <td>403 (Payload Forbidden)</td>
        <td>PAYLOAD_FORBIDDEN</td>
        <td>A firewall rejected the request</td>
    </tr>
    <tr>
      <td>Internal server error</td>
      <td>500</td>
      <td>CDS60003 or INTERNAL_SERVER_ERROR</td>
      <td>Internal server error</td>
    </tr>
  </tbody>
</table>
<br>

**Search endpoint error scenarios**

<table>
  <tbody>
    <tr>
      <td><b>Scenario</b></td>
      <td><b>HTTP Status</b></td>
      <td><b>Code</b></td>
      <td><b>Message</b></td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier and X-Badge-Identifier http headers are missing</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>Both X-Submitter-Identifier and X-Badge-Identifier headers are missing</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier is invalid</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>X-Submitter-Identifier header is invalid</td>
    </tr>
    <tr>
      <td>For CSPs only, X-Submitter-Identifier header is invalid</td>
      <td>400 (Bad Request)</td>
      <td>BAD_REQUEST</td>
      <td>X-Badge-Identifier header is invalid</td>
    </tr>
    <tr>
      <td>Internal server error</td>
      <td>500</td>
      <td>CDS60003 or INTERNAL_SERVER_ERROR</td>
      <td>Internal server error</td>
    </tr>
    <tr>
      <td>Page number is out of bounds</td>
      <td>400 (Bad Request)</td>
      <td>CDS60005</td>
      <td>pageNumber parameter out of bounds</td>
    </tr>
    <tr>
      <td>Invalid declarationStatus parameter</td>
      <td>400 (Bad Request)</td>
      <td>CDS60006</td>
      <td>Invalid declarationStatus parameter</td>
    </tr>
    <tr>
      <td>Invalid declarationStatus parameter</td>
      <td>400 (Bad Request)</td>
      <td>CDS60007</td>
      <td>Invalid declarationStatus parameter</td>
    </tr>
    <tr>
      <td>Invalid declarationCategory parameter</td>
      <td>400 (Bad Request)</td>
      <td>CDS60008</td>
      <td>Invalid declarationCategory parameter</td>
    </tr>
    <tr>
      <td>Invalid date parameters</td>
      <td>400 (Bad Request)</td>
      <td>CDS60009</td>
      <td>Invalid date parameters</td>
    </tr>
    <tr>
      <td>Invalid goodsLocationCode parameter</td>
      <td>400 (Bad Request)</td>
      <td>CDS60010</td>
      <td>Invalid goodsLocationCode parameter</td>
    </tr>
    <tr>
      <td>Invalid pageNumber parameter</td>
      <td>400 (Bad Request)</td>
      <td>CDS60012</td>
      <td>Invalid pageNumber parameter</td>
    </tr>
    <tr>
        <td>A firewall rejected the request</td>
        <td>403 (Payload Forbidden)</td>
        <td>PAYLOAD_FORBIDDEN</td>
        <td>A firewall rejected the request</td>
    </tr>
  </tbody>
</table>
<br>

See our [reference guide](/api-documentation/docs/reference-guide#errors) for more on errors.
