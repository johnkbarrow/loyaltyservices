package com.sap.services.sbo.loyaltyreward.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

@Configuration
@EnableMongoRepositories(basePackages = "com.sap.services.sbo.loyaltyreward.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {
	
	@Value("${host}")
	private String mongoUri;
		
	@Value("${port}")
	private int port;
		
	@Value("${username}")
	private String username;
		
	@Value("${password}")
	private String password;

	@Override
	protected String getDatabaseName() {
			
		return "customers";
	}

	@Override
	protected void configureClientSettings(Builder builder) {

		builder
		  	.credential(MongoCredential.createCredential(username, getDatabaseName(), password.toCharArray()))
		  	.applyToClusterSettings(settings  -> {
		  	    settings.hosts(Collections.singletonList(new ServerAddress(mongoUri, port)));
		  	})
		  	.applyToClusterSettings(settings  -> {
		  	    settings.requiredClusterType(ClusterType.REPLICA_SET);
		  	})
		  	.applyToClusterSettings(settings  -> {
		  		settings.mode(ClusterConnectionMode.MULTIPLE);
		  	})
			.applyToClusterSettings(settings  -> {
				settings.requiredReplicaSetName("globaldb");
			})
			.applyToSslSettings(settings -> {
				settings.enabled(true);
			});
		}

}
