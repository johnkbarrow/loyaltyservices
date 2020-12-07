package com.sap.services.sbo.loyaltyreward.controller;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sap.services.sbo.loyaltyreward.cloudevent.SpringMessageFactory;
import com.sap.services.sbo.loyaltyreward.model.Customer;
import com.sap.services.sbo.loyaltyreward.repository.CustomerRepository;

import io.cloudevents.CloudEvent;

@RestController
@RequestMapping
public class LoyaltyRewardController {
	
	private final static Logger LOG = LoggerFactory.getLogger(LoyaltyRewardController.class);
	
	@Value("${CUSTOMER_SERVICE_43507EB8_D017_4001_813D_7F7A07481F39_GATEWAY_URL}")
	private String ioAppGatewayUrl;
	
	@Value("${PROMO_GROUP}")
	private String promotionGroupId;
	
	@Value("${PROMO_PRODUCT}")
	private String loyaltyProduct;
	
	@Value("${PROMO_PRODUCT_QUANTITY}")
	private Double loyaltyProductQuantity;
	
	@Autowired
	private CustomerRepository repository;
	
	@PostMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void eventTrigger(HttpEntity<byte[]> requestEntity) {
		
		/*
		 * 1. Marshall event payload to CloudEvent
		 */
		CloudEvent cloudEvent = SpringMessageFactory.createReader(requestEntity).toEvent();
		String eventData = new String(cloudEvent.getData());
		LOG.info("eventData: " + eventData); //{"customerId": "john.barrow@sap.com","promotionApplied": "true"}
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject jsonEventResponse = (JSONObject)parser.parse(eventData);
			
			/*
			 * 2. Retrieve customer id and if promotion applied
			 */
			//String customerId = phu.getCustomerId();
			String customerId = (String) jsonEventResponse.get("customerId");
			LOG.info("Customer ID: " + customerId);
		
			String promotionApplied = (String) jsonEventResponse.get("promotionApplied");
			LOG.info("Promotion Applied: " + promotionApplied);
		
			/*
			 * 3. Retrieve existing customer from our mongodb repository
			 */
			Customer customer = repository.findByCustomerId(customerId);
			if (customer != null) {
			
				/*
				 * 4. Retrieve customer purchase history
				 */
				HashMap<String, Double> purchaseHistory = customer.getPurchaseMap();
		
				/*
				 * 5. Test for Loyalty products and quantities
				 */
				if (purchaseHistory.containsKey(loyaltyProduct)) {
					Double existingQuantity = purchaseHistory.get(loyaltyProduct);
					LOG.info("existingQuantity: " + existingQuantity);
					if (existingQuantity != null) {
						if (existingQuantity.compareTo(loyaltyProductQuantity) < 0) { //a value less than 0 if this Double is numerically less than anotherDouble
							LOG.info("Product " + loyaltyProduct + " has quantity " + existingQuantity + " and therefore DOES NOT qualify for loyalty");
						}
						else {
							LOG.info("Product " + loyaltyProduct + " has quantity " + existingQuantity + " and therefore DOES qualify for loyalty");
				
							/*
							 * 6. Apply loyalty logic...
							 *    ...add customer to promotion group via IntegrationObject API
							 */
							CloseableHttpClient client = HttpClients.createDefault();
						
							HttpPost httpPost = new HttpPost(ioAppGatewayUrl + "/Customers");
							String postBody = "{\n" + 
									"\"uid\": \"" + customerId + "\",\n" + 
									"\"groups\": [{\n" + 
									"\"uid\": \"" + promotionGroupId + "\"\n" + 
									"}]\n" + 
									"}";
						
							httpPost.setEntity(new StringEntity(postBody, ContentType.APPLICATION_JSON));
						
							try {
								CloseableHttpResponse response = client.execute(httpPost);
								LOG.info("Commerce IO Response = " + response.getStatusLine().getStatusCode());
							
								if (response.getStatusLine().getStatusCode() == 201) {
									/*
									 * 7. Reset purchase history for this product
									 */
									customer.resetPurchaseHistory(loyaltyProduct);
									repository.save(customer);
								}
							}
							catch (ClientProtocolException e) {
								e.printStackTrace();
							}
							catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					else {
						LOG.info("loyalty product purchase history is null");
					}
				}
				else {
					LOG.info("purchase history does NOT contain loyalty product");
				}
			}
		}
		catch (ParseException pe) {
        	LOG.error("JSON Parse Exception", pe);
        }
		
	}
	
	@GetMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
	public String ping() {
		return "Hello loyaltyreward";
	}

}
