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
    <td>Invalid MRN</td>
    <td>400 (Bad Request)</td>
    <td>BAD_REQUEST</td>
  </tr>
  <tr>
    <td>Declaration not found for given MRN</td>
    <td>404 (Not Found)</td>
    <td>NOT_FOUND</td>
  </tr>
</table>
<br>

Errors specific to each API are shown in the Endpoints section, under Response. 
See our [reference guide](/api-documentation/docs/reference-guide#errors) for more on errors.

