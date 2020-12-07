package com.sap.services.sbo.purchasetracker.cloudevents;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class HttpHeaderInterceptor implements ClientHttpRequestInterceptor {

	private final String headerKey;
	private final String headerValue;

	public HttpHeaderInterceptor(String headerKey, String headerValue) {
		this.headerKey = headerKey;
		this.headerValue = headerValue;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
										ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
		httpRequest.getHeaders().set(this.headerKey, this.headerValue);
		return clientHttpRequestExecution.execute(httpRequest, bytes);
	}
}
