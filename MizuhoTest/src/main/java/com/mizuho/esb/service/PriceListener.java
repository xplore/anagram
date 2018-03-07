package com.mizuho.esb.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.lang.NonNull;

import com.mizuho.esb.entity.VendorPrice;

public class PriceListener {
	
	public final String listenerId;
	public final Map<String, Set<String>> interest;

	public PriceListener(String id) {
		super();
		listenerId = id;
		interest = new HashMap<String, Set<String>>();
	}
	
	public void addInterest(@NonNull String vendor, @NonNull String instrument) {
		interest.computeIfAbsent(vendor, k -> new HashSet<String>()).add(instrument);
	}
	

	/**
	 * @param price
	 * Sub classes of PriceListener can override notifyPrice to perform custom actions
	 * or have callback functions with client references. For the purpose of this task
	 * we simply print out the notifications
	 */
	public void notifyPrice(@NonNull VendorPrice price) {
		Logger.getGlobal().info(String.format("listener:%s :: %s", listenerId, price));
	}
	
}
