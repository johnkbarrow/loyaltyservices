package com.sap.services.sbo.loyaltyreward.model;

import java.util.HashMap;

import org.springframework.data.annotation.Id;

public class Customer {
	
	@Id
	public String id;
	
	public String customerId;
	public HashMap<String, Double> purchaseMap;

	public Customer() {}
	
	public Customer(String customerId) {
		this.customerId = customerId;
		this.purchaseMap = new HashMap<String, Double>();
	}
	
	public HashMap<String, Double> getPurchaseMap() {
		return purchaseMap;
	}
	
	public void addToPurchaseHistory(String productId, Double productQuantity) {
		
		Double originalQuantity = purchaseMap.get(productId);
		if (originalQuantity == null) {
			purchaseMap.put(productId, productQuantity);
		}
		else {
			originalQuantity += productQuantity;
			purchaseMap.put(productId, originalQuantity);
		}
	}
	
	public void resetPurchaseHistory(String productId) {
		purchaseMap.put(productId, null);
	}
	
	@Override
	public String toString() {
		return String.format(
				"Customer[id=%s, customerId='%s']",
				id, customerId);
	}

}
