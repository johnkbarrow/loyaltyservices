package com.sap.services.sbo.purchasetracker.cloudevents;

import io.cloudevents.SpecVersion;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.rw.CloudEventWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class SpringMessageWriterImpl implements MessageWriter<CloudEventWriter<ResponseEntity<Void>>,
		ResponseEntity<Void>>, CloudEventWriter<ResponseEntity<Void>> {
	private final RestTemplate restTemplate;
	private final String url;

	SpringMessageWriterImpl(RestTemplate restTemplate, String url) {
		this.restTemplate = restTemplate;
		this.url = url;
	}

	@Override
	public CloudEventWriter<ResponseEntity<Void>> create(SpecVersion specVersion) {
		this.putHeader(CloudEventsHeaders.SPEC_VERSION, specVersion.toString());
		return this;
	}

	@Override
	public ResponseEntity<Void> setEvent(EventFormat eventFormat, byte[] bytes) {
		this.putHeader(HttpHeaders.CONTENT_TYPE, eventFormat.serializedContentType());
		return this.end(bytes);
	}

	@Override
	public ResponseEntity<Void> end(byte[] bytes) {
		HttpEntity<String> request = null;
		if (bytes != null)
			request = new HttpEntity<>(new String(bytes));

		return this.restTemplate.postForEntity(url, request, Void.class);
	}

	@Override
	public ResponseEntity<Void> end() {
		return this.end(null);
	}

	@Override
	public void setAttribute(String name, String value) {
		this.putHeader(CloudEventsHeaders.ATTRIBUTES_TO_HEADERS.get(name), value);
	}

	@Override
	public void setExtension(String name, String value) {
		this.putHeader(CloudEventsHeaders.CE_PREFIX + name, value);
	}

	public void putHeader(String headerKey, String headerValue) {
		List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();

		if (CollectionUtils.isEmpty(interceptors)) {
			interceptors = new ArrayList<>();
		}
		interceptors.add(new HttpHeaderInterceptor(headerKey, headerValue));
		restTemplate.setInterceptors(interceptors);
	}
}
