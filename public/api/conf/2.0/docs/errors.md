We use standard HTTP status codes to show whether an API request succeeded or not.

**Error scenarios**

<table>
  <tr><td></td><td></td><td></td></tr>
  <tr>
    <td><strong>Scenario</strong></td>
    <td><strong>HTTP Status</strong></td>
    <td><strong>Message</strong></td>
  </tr>
  <tr>
    <td>Invalid MRN, DUCR, UCR or Inventory Reference</td>
    <td>400 (Bad Request)</td>
    <td>Invalid Search</td>
  </tr>
  <tr>
    <td>Declaration not found for given MRN, DUCR, UCR or Inventory Reference</td>
    <td>404 (Not Found)</td>
    <td>Declaration Not Found</td>
  </tr>
  <tr>
    <td>Missing MRN, DUCR, UCR or Inventory Reference</td>
    <td>404 (Not Found)</td>
    <td>Invalid Search</td>
  </tr>
</table>
<br>

Errors specific to each API are shown in the Endpoints section, under Response. 
See our [reference guide](/api-documentation/docs/reference-guide#errors) for more on errors.

