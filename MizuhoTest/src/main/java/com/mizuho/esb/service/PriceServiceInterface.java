package com.mizuho.esb.service;

import org.springframework.lang.NonNull;

import com.mizuho.esb.entity.VendorPrice;

public interface PriceServiceInterface {
	
	public void publishPrice(@NonNull VendorPrice price);
	
	public void addPriceListener(PriceListener listener);
	
	public void removePriceListener(@NonNull String id);

}
