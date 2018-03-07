package com.mizuho.esb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.mizuho.esb.entity.VendorPrice;
import com.mizuho.esb.service.PriceListener;
import com.mizuho.esb.service.PriceService;

@SpringBootApplication
public class MizuhoTestApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(MizuhoTestApplication.class, args);

		PriceListener oneAB = new PriceListener("L1");
		oneAB.addInterest("V1", "IA");
		oneAB.addInterest("V1", "IB");

		PriceListener twoBC = new PriceListener("L2");
		twoBC.addInterest("V1", "IB");
		twoBC.addInterest("V2", "IB");
		twoBC.addInterest("V1", "IC");
		twoBC.addInterest("V2", "IC");

		PriceListener threeC = new PriceListener("L3");
		threeC.addInterest("V1", "IC");
		threeC.addInterest("V2", "IC");
		threeC.addInterest("V3", "IC");

		List<PriceListener> listeners = new ArrayList<>();
		listeners.add(oneAB);
		listeners.add(twoBC);
		listeners.add(threeC);

		List<VendorPrice> prices = new ArrayList<VendorPrice>();
		
		prices.add(new VendorPrice("V3", "IA", 1.1));
		prices.add(new VendorPrice("V3", "IB", 1.2));
		prices.add(new VendorPrice("V3", "IC", 1.3));
		
		prices.add(new VendorPrice("V1", "IA", 1.1));
		prices.add(new VendorPrice("V1", "IB", 1.2));
		prices.add(new VendorPrice("V1", "IC", 1.3));

		prices.add(new VendorPrice("V2", "IA", 1.1));
		prices.add(new VendorPrice("V2", "IB", 1.2));
		prices.add(new VendorPrice("V2", "IC", 1.3));

		PriceService service = new PriceService();
		listeners.forEach(listener -> service.addPriceListener(listener));

		sendPricesToService(prices, service);

		service.removePriceListener("L1");
		service.removePriceListener("L2");
		
		sendPricesToService(prices, service);
		
		Thread.sleep(5000);
		

		service.shutdown();
		Logger.getGlobal().info("FINISHED");
	}

	private static void sendPricesToService(List<VendorPrice> prices, PriceService service) {
		prices.forEach(price -> {
			service.publishPrice(price);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}
}
