package com.sap.services.sbo.loyaltyreward.cloudevent;

import io.cloudevents.core.message.impl.MessageUtils;
import io.cloudevents.core.v1.ContextAttributes;
import org.springframework.http.HttpHeaders;

import java.util.Map;

public enum CloudEventsHeaders {
	;

	public static final String CE_PREFIX = "ce-";

	protected static final Map<String, String> ATTRIBUTES_TO_HEADERS =
			MessageUtils.generateAttributesToHeadersMapping(v ->
					v.equals(ContextAttributes.DATACONTENTTYPE.toString()) ? HttpHeaders.CONTENT_TYPE :
							(CE_PREFIX + v)
			);
	public static final String SPEC_VERSION;

	static {
		SPEC_VERSION = ATTRIBUTES_TO_HEADERS.get(ContextAttributes.SPECVERSION.toString());
	}
}
