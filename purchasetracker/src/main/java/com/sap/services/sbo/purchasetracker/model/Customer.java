package com.sap.services.sbo.purchasetracker.model;

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
	
	@Override
	public String toString() {
		return String.format(
				"Customer[id=%s, customerId='%s']",
				id, customerId);
	}

}
