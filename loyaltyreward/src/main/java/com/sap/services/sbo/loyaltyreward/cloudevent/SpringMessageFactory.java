package com.sap.services.sbo.loyaltyreward.cloudevent;

import io.cloudevents.core.message.MessageReader;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.core.message.impl.GenericStructuredMessageReader;
import io.cloudevents.core.message.impl.MessageUtils;
import io.cloudevents.core.message.impl.UnknownEncodingMessageReader;
import io.cloudevents.rw.CloudEventWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class SpringMessageFactory {

	private SpringMessageFactory() {
	}

	public static MessageWriter<CloudEventWriter<ResponseEntity<Void>>, ResponseEntity<Void>> createWriter(
			RestTemplate restTemplate, String url) {
		return new SpringMessageWriterImpl(restTemplate, url);
	}

	public static MessageWriter<CloudEventWriter<ResponseEntity<Void>>, ResponseEntity<Void>> createWriter(
			RestTemplate restTemplate, String url, String authHeader) {
		SpringMessageWriterImpl springMessageWriter = new SpringMessageWriterImpl(restTemplate, url);
		springMessageWriter.putHeader(HttpHeaders.AUTHORIZATION, authHeader);
		return springMessageWriter;
	}

	public static MessageReader createReader(byte[] body, MultiValueMap<String, String> headers) {
		return MessageUtils.parseStructuredOrBinaryMessage(
				() -> headers.getFirst(HttpHeaders.CONTENT_TYPE),
				format -> new GenericStructuredMessageReader(format, body),
				() -> headers.getFirst(String.valueOf(CloudEventsHeaders.SPEC_VERSION)),
				sv -> new BinarySpringMessageReaderImpl(sv, body, headers),
				UnknownEncodingMessageReader::new);
	}

	public static MessageReader createReader(byte[] body, HttpHeaders headers) {
		return createReader(body, (MultiValueMap<String, String>) headers);
	}

	public static MessageReader createReader(HttpEntity<byte[]> requestEntity) {
		return createReader(requestEntity.getBody(), requestEntity.getHeaders());
	}
}
