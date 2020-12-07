package com.sap.services.sbo.loyaltyreward.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sap.services.sbo.loyaltyreward.model.Customer;

public interface CustomerRepository extends MongoRepository<Customer, String> {

	public Customer findByCustomerId(String customerId);

}