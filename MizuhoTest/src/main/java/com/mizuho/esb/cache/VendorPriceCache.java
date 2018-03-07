package com.mizuho.esb.cache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.springframework.lang.NonNull;

import com.mizuho.esb.entity.VendorPrice;

public final class VendorPriceCache {
	
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, TreeMap<Instant, VendorPrice>>> vendorMap;
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, TreeMap<Instant,VendorPrice>>> instrumentMap;
	
	private final ScheduledExecutorService exec;
	
	public VendorPriceCache() {
		super();
		
		vendorMap = new ConcurrentHashMap<>();
		instrumentMap  = new ConcurrentHashMap<>();
		exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				
				Instant expired = Instant.now().minus(30, ChronoUnit.DAYS);
				
				vendorMap.forEach((vendor, instMap)->{
					instMap.forEach((instrument, priceMap)->{
						priceMap.entrySet().removeIf( 
								(Predicate<? super Entry<Instant, VendorPrice>>) entry -> {
									return entry.getKey().isBefore(expired);
						});
					});
				});
				
			}
			
		}, 1, 1, TimeUnit.HOURS);
	}

	public void addVendorPrice(@NonNull VendorPrice price) {
		
		vendorMap
		.computeIfAbsent(price.vendor, k -> new ConcurrentHashMap<String,TreeMap<Instant, VendorPrice>>())
		.computeIfAbsent(price.instrument, k -> new TreeMap<Instant, VendorPrice>())
		.put(price.instant, price);
		
		instrumentMap
		.computeIfAbsent(price.instrument, k -> new ConcurrentHashMap<String,TreeMap<Instant, VendorPrice>>())
		.computeIfAbsent(price.vendor, k -> new TreeMap<Instant, VendorPrice>())
		.put(price.instant, price);
		
	}
	
	public List<VendorPrice> getVendorPriceForInstruments(@NonNull String vendor, @NonNull List<String> instruments){
		List<VendorPrice> results  = new ArrayList<VendorPrice>();
		if(!vendorMap.containsKey(vendor)) {
			return results;
		}
		
		ConcurrentHashMap<String, TreeMap<Instant, VendorPrice>> instMap = vendorMap.get(vendor);
		
		instruments.forEach(instrument -> {
			if(instMap.containsKey(instrument)) results.add(instMap.get(instrument).lastEntry().getValue());
		});
		
		return results;
	}
	
	public List<VendorPrice> getInstrumentPriceForVendors(@NonNull String instrument, @NonNull List<String> vendors){
		List<VendorPrice> results  = new ArrayList<VendorPrice>();
		if(!instrumentMap.containsKey(instrument)) {
			return results;
		}
		
		ConcurrentHashMap<String, TreeMap<Instant, VendorPrice>> vMap = instrumentMap.get(instrument);
		
		vendors.forEach(vendor -> {
			if(vMap.containsKey(vendor)) results.add(vMap.get(vendor).lastEntry().getValue());
		});
		
		return results;
	}
	
	public List<VendorPrice> getVendorPriceForAllInstruments(@NonNull String vendor){
		List<VendorPrice> results  = new ArrayList<VendorPrice>();
		if(!vendorMap.containsKey(vendor)) {
			return results;
		}
		ConcurrentHashMap<String, TreeMap<Instant, VendorPrice>> instMap = vendorMap.get(vendor);
		
		instMap.forEach( (k, v) -> {
			results.add(v.lastEntry().getValue());
		} );
		
		return results;
		
	}
	
	public List<VendorPrice> getInstrumentPriceForAllVendors(@NonNull String instrument){
		List<VendorPrice> results  = new ArrayList<VendorPrice>();
		if(!instrumentMap.containsKey(instrument)) {
			return results;
		}
		ConcurrentHashMap<String, TreeMap<Instant, VendorPrice>> vMap = instrumentMap.get(instrument);
		
		vMap.forEach( (k, v) -> {
			results.add(v.lastEntry().getValue());
		} );
		
		return results;
		
	}
	
	public void shutdown() throws InterruptedException {
		exec.shutdown();
		try {
			if (!exec.awaitTermination(800, TimeUnit.MILLISECONDS)) {
				exec.shutdownNow();
			}
		} catch (InterruptedException e) {
			exec.shutdownNow();
		}
		Logger.getGlobal()
				.info(String.format("finished cache shutdown :: %s :: %s", exec.isTerminated(), exec.isShutdown()));
	}
	
}
