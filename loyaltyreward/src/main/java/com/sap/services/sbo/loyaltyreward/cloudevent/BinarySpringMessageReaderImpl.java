package com.sap.services.sbo.loyaltyreward.cloudevent;

import io.cloudevents.SpecVersion;
import io.cloudevents.core.message.impl.BaseGenericBinaryMessageReaderImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import java.util.function.BiConsumer;

public class BinarySpringMessageReaderImpl extends BaseGenericBinaryMessageReaderImpl<String, String> {

	private final MultiValueMap<String, String> headers;

	protected BinarySpringMessageReaderImpl(SpecVersion version, byte[] body, MultiValueMap<String, String> headers) {
		super(version, body);
		this.headers = headers;
	}

	@Override
	protected boolean isContentTypeHeader(String key) {
		return key.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE);
	}

	@Override
	protected boolean isCloudEventsHeader(String key) {
		return key.length() > 3 && key.substring(0, CloudEventsHeaders.CE_PREFIX.length()).toLowerCase().startsWith(
				CloudEventsHeaders.CE_PREFIX);
	}

	@Override
	protected String toCloudEventsKey(String key) {
		return key.substring(CloudEventsHeaders.CE_PREFIX.length()).toLowerCase();
	}

	@Override
	protected void forEachHeader(BiConsumer<String, String> fn) {
		this.headers.forEach((e, list) -> list.forEach(v -> fn.accept(e, v)));
	}

	@Override
	protected String toCloudEventsValue(String s) {
		return s;
	}
}
