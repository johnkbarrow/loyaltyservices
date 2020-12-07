package com.sap.services.sbo.purchasetracker.controller;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.json.simple.JSONArray;
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

import com.sap.services.sbo.purchasetracker.cloudevents.SpringMessageFactory;
import com.sap.services.sbo.purchasetracker.model.Customer;
import com.sap.services.sbo.purchasetracker.repository.CustomerRepository;

import io.cloudevents.CloudEvent;

@RestController
@RequestMapping
public class PurchaseTrackerController {
	
	private final static Logger LOG = LoggerFactory.getLogger(PurchaseTrackerController.class);
	
	@Value("${CC_OCC_COMMERCE_WEBSERVICES_V2_409BF238_0D51_4EA0_8DB4_8710A648715D_GATEWAY_URL}")
	private String appGatewayUrl;
	
	@Value("${OUTBOUND_CONNECTED_SYSTEM}")
	private String connectedSystemId;
	
	@Value("${OUTBOUND_EVENT_TYPE}")
	private String eventType;
	
	@Value("${PROMOTION_ID}")
	private String loyaltyPromotionId;
	
	@Autowired
	private CustomerRepository repository;
	
	@PostMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void eventTrigger(HttpEntity<byte[]> requestEntity) {	
		
		/*
		 * 1. Marshall event payload into CloudEvent
		 */
		CloudEvent cloudEvent = SpringMessageFactory.createReader(requestEntity).toEvent();
		String eventData = new String(cloudEvent.getData());
		LOG.info("eventData: " + eventData); // {"baseSiteUid":"electronics","orderCode":"00009036"}
		
		/*
		 * 2. Retrieve order id and base site
		 */
		JSONParser parser = new JSONParser();
		try {
			JSONObject jsonEventResponse = (JSONObject)parser.parse(eventData);
			String orderCode = (String)jsonEventResponse.get("orderCode");
			String baseSite = (String)jsonEventResponse.get("baseSiteUid");
			LOG.info("Order ID: " + orderCode + ", Base site: " + baseSite);
        
			/*
			 * 3. Call OCC with order id
			 */
			CloseableHttpClient client = HttpClients.createDefault();
        
			HttpGet httpGet = new HttpGet(appGatewayUrl + "/" + baseSite + "/orders/" + orderCode);
			httpGet.addHeader("Accept", "application/json");
			httpGet.addHeader("Content-Type", "application/json");
			
			CloseableHttpResponse response = client.execute(httpGet);
			LOG.info("OCC Response = " + response.getStatusLine().getStatusCode());
			
			String responseStr = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			//LOG.info("OCC Response = " + responseStr);
			
			if (response.getStatusLine().getStatusCode() == 200) {
				/*
				 * 4. Parse response into JSON object to retrieve 
				 *    a) products bought and how many
				 *    b) customer id
				 *    c) if loyalty promotion was applied to this order
				 */
				
				JSONObject jsonResponse = (JSONObject)parser.parse(responseStr);
			
				//TODO Look at https://stackoverflow.com/questions/28982412/how-to-search-find-in-json-with-java
				
				JSONArray entryArray = (JSONArray) jsonResponse.get("entries");
				Map<String, Long> productsBought = new HashMap<String, Long>();
				for (int j=0; j<entryArray.size(); j++) {
					JSONObject entry = (JSONObject) entryArray.get(j);
					JSONObject product = (JSONObject) entry.get("product");
					String productId = (String)product.get("code");
					Long quantity = (Long)entry.get("quantity");
					LOG.info("PRODUCT QUANTITY: " + quantity);
					productsBought.put(productId, quantity);
				}
				
				JSONObject user = (JSONObject)jsonResponse.get("user");
				String userId = (String)user.get("uid");
				
				LOG.info(userId + " bought " + productsBought.toString());
				
				boolean promotionApplied = false;
				JSONArray orderPromotionArray = (JSONArray) jsonResponse.get("appliedOrderPromotions");
				if (!orderPromotionArray.isEmpty()) {
					for (int k=0; k<entryArray.size(); k++) {
						JSONObject orderPromotion = (JSONObject) orderPromotionArray.get(k);
						JSONObject promotion = (JSONObject) orderPromotion.get("promotion");
						String promotionId = (String)promotion.get("code");
						if (promotionId.equalsIgnoreCase(loyaltyPromotionId)) {
							promotionApplied = true;
							break;
						}
					}
				}
				
				if (promotionApplied) {
					LOG.info(loyaltyPromotionId + " WAS applied");
				}
				else {
					LOG.info(loyaltyPromotionId + " WAS NOT applied");
				}
				
				/*
				 * 5. Look for existing customer in our mongodb repository
				 * 	  otherwise create new
				 */
				Customer customer = repository.findByCustomerId(userId);
				if (customer == null) {
					customer = new Customer(userId);
				}
			
				/*
				 * 6. Update purchase history for this customer
				 */
				Set<String> productList = productsBought.keySet();
				Iterator<String> productIter = productList.iterator();
				while (productIter.hasNext()) {
					String productCode = productIter.next();
					Long productQuantity = productsBought.get(productCode);
					customer.addToPurchaseHistory(productCode, Double.valueOf(productQuantity.doubleValue()));
				}
				
				repository.save(customer);
				
				/*
				 * 7. Create and send event to loyaltytracker service
				 *    to notify customer purchase history updated
				 */
				final String eventId = UUID.randomUUID().toString();
				final URI src = URI.create(connectedSystemId);
				
				
				HttpPost httpPost = new HttpPost("http://default-broker");
				httpPost.addHeader("Content-Type", "application/json");
				httpPost.addHeader("CE-ID", eventId);
				httpPost.addHeader("CE-Type", eventType);
				httpPost.addHeader("CE-SpecVersion", "1.0");
				httpPost.addHeader("CE-Source", src.toString());
				httpPost.addHeader("CE-eventtypeversion", "v1");
				httpPost.addHeader("X-B3-Flags", "1");
		    
				String content = "{" + 
					"\"customerId\": \"" + userId + "\"," + 
					"\"promotionApplied\": \"" + promotionApplied + "\"" + 
					"}";
				
				StringEntity postingString = new StringEntity(content, ContentType.APPLICATION_JSON);
				httpPost.setEntity(postingString);
				
				response = client.execute(httpPost);
				
				LOG.info("BROKER Response Code = " + response.getStatusLine().getStatusCode());
			}
		}
		catch (IOException ioe) {
			LOG.error("IO Exception", ioe);
		}
		catch (ParseException pe) {
        	LOG.error("JSON Parse Exception", pe);
        }
		
	}
	
	@GetMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
	public String ping() {
		return "Hello purchasetracker";
	}

}
