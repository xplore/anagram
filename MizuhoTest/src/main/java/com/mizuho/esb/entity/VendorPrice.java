package com.mizuho.esb.entity;

import java.time.Instant;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class VendorPrice {

	public final String vendor;
	public final String instrument;
	public final Double price;
	public final Instant instant;
	
	private final static ObjectMapper mapper = new ObjectMapper();

	public VendorPrice(@NonNull String vendor, @NonNull String instrument, @NonNull Double price) {
		super();
		this.vendor = vendor;
		this.instrument = instrument;
		this.price = price;
		this.instant = Instant.now();
	}

	@Override
	public String toString() {
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}

	}

}
