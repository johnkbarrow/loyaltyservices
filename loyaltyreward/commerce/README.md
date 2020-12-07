# Configure Customer Integration Object API

In this step we will add an oData service to obtain inforamation about the customer.  This will appear within Kyma as Customer Service.

1. Copy the file [customer_io.impex](commerce/customer_io.impex) and update the value of `<ENTER YOUR DESTINATION HERE!!!!>` to your destination 
   target prior to importing the content
2. Open the commerce admin console impex import - `https://<commerce domain>/hac/console/impex/import`
3. Paste the updated content, then choose `Import content`
4. Open the commerce backoffice `https://<commerce domain>/backoffice`
5. Choose System -> API -> Endpoints 
6. Choose the cc-customer Endpoint and change the Specification URL to

   `https://ODATAUSER:odata1234@<commerce domain>/odata2webservices/Customer/$metadata`

7.  Choose System -> API -> Destinations -> Exposed Destinations
8.  Verify that the Exposed Destination `cc-customer` is using the credential `odatauser-credential`
9. Choose the Exposed Destination `cc-customer` and then the option to `Register Exposed Destination Action`

# Configure Promotion

In this step we create a promotion that is only available for a specific usergroup (loyaltycustomers)

1. Open the commerce admin console impex import - `https://<commerce domain>/hac/console/impex/import`
2. Copy the contents of [promotion.impex](commerce/promotion.impex) and then choose `Import content`
3. Login to the backoffice - `https://<commerce domain>/backoffice`
4. Find the promotion that was just created ('surveycustomer_percentage_discount_cart') and publish the promotion

# To Reset
1. Open the commerce backoffice `https://<commerce domain>/backoffice`
2. Choose the menu `User ->  Customers` and choose the desired user
3. In the `General` tab remove the user from the `[loyaltycustomers]` group and save the change