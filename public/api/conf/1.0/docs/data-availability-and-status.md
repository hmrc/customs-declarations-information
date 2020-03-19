Only declarations received by CDS in the last 180 days will be returned by this API.

The API returns a response containing these fields:

<table style = "width 100%">
  <tr><td></td><td></td></tr>
  <tr>
    <td>AcceptanceDateTime/DateTimeString</td>
    <td>Date and time on which the declaration was legally accepted.     
      <br><br>This is only populated on an arrived declaration.
    </td>
  </tr>
  <tr>
    <td>ID</td>
    <td>Movement Reference Number (MRN)</td>
  </tr>
  <tr>
    <td>VersionID</td>
    <td>Declaration version number. This will be the latest version.</td>
  </tr>
  <tr>
    <td>ReceivedDateTime/DateTimeString</td>
    <td>Date and time on which the declaration was received</td>
  </tr>
  <tr>
    <td>GoodsReleasedDateTime/DateTimeString</td>
    <td>Date and time on which the goods were released.</td>
  </tr>
  <tr>
    <td>ROE</td>
    <td>Customs route of entry.</td>
  </tr>
  <tr>
    <td>ICS</td>
    <td>Import clearance status.</td>
  </tr>
  <tr>
    <td>IRC</td>
    <td>Inventory return code. This is the error response from inventory linking </td>
  </tr>
  <tr>
    <td>FunctionCode</td>
    <td>Function code is always set to 9.</td>
  </tr>
  <tr>
    <td>TypeCode</td>
    <td>Declaration type. UCC data elements 1/1 + 1/2.</td>
  </tr>
  <tr>
    <td>GoodsItemQuantity</td>
    <td>Total number of items. UCC data element 1/9.</td>
  </tr>
  <tr>
    <td>TotalPackageQuantity </td>
    <td>Total number of packages. UCC data element 6/18.</td>
  </tr>
  <tr>
    <td>Submitter/ID</td>
    <td>Submitting Trader. This is the ID of the initiating trader if the declaration is submitted via a CSP.</td>
  </tr>
  <tr>
    <td>PreviousDocument/ID</td>
    <td>The ID of a previous document. A DUCR or MUCR.</td>
  </tr>
  <tr>
    <td>PreviousDocument/TypeCode</td>
    <td>The type of a previous document: <br><br>        
        DCR = DUCR<br>
        MCR = MUCR
     </td>
  </tr>
  <tr>
    <td>UCR/TraderAssignedReferenceID </td>
    <td>Reference Number or UCR. UCC data element 2/4.</td>
  </tr>
</table>
