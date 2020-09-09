We use standard HTTP status codes to show whether an API request succeeded or not.

**Error scenarios**

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
      <td>Declaration Not Found</td>
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
      <td>Internal server error</td>
      <td>500</td>
      <td>CDS60003 or INTERNAL_SERVER_ERROR</td>
      <td>Internal server error</td>
    </tr>
  </tbody>
</table>
<br>

See our [reference guide](/api-documentation/docs/reference-guide#errors) for more on errors.
