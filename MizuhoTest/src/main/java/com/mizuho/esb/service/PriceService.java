package com.mizuho.esb.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.mizuho.esb.cache.VendorPriceCache;
import com.mizuho.esb.entity.VendorPrice;

public class PriceService implements PriceServiceInterface {

	private static final Map<String, PriceListener> listeners = new ConcurrentHashMap<String, PriceListener>();

	private static final Map<String, List<String>> listenerMap = new ConcurrentHashMap<String, List<String>>();

	private static final BlockingQueue<VendorPrice> priceQueue = new LinkedBlockingQueue<VendorPrice>();

	private VendorPriceCache cache = new VendorPriceCache();

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	private AtomicBoolean shutdown = new AtomicBoolean(false);
	
	private JdbcTemplate template;
	private PreparedStatement preparedStatement;
	
	private static final String insertPriceQuery = "insert into price values ( ?, ?, ?, ? ) ";

	public PriceService() {
		super();
		setupPublishingThread();
		template = setupJdbcTemplate();
		initializeCacheFromDatabase();
	}

	private void setupPublishingThread() {
		executor.execute(new Runnable() {

			@Override
			public void run() {

				while (!shutdown.get()) {
					Logger.getGlobal().info(String.format("shutdown :: %s", shutdown.get()));
					try {
						VendorPrice price = priceQueue.poll(1, TimeUnit.SECONDS);
						if (price != null) {

							String key = price.vendor.concat(price.instrument);
							if (listenerMap.containsKey(key)) {

								listenerMap.get(key).forEach(listenerID -> {
									if (listeners.containsKey(listenerID)) {
										listeners.get(listenerID).notifyPrice(price);
									}
								});

							}

						}

					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

			}

		});
	}
	
	
	public void initializeCacheFromDatabase() {
		//TODO use the jdbc template to load the existing prices into the cache using cache.addVendorPrice() method.
	}

	@Override
	public void publishPrice(final VendorPrice price) {
		insertPriceToDB(price);
		cache.addVendorPrice(price);
		priceQueue.add(price);
	}

	private void insertPriceToDB(VendorPrice price) {
		template.update(new PreparedStatementCreator() {
 
            @Override
			public PreparedStatement createPreparedStatement(Connection con)
                    throws SQLException {
                PreparedStatement stmt = con.prepareStatement(insertPriceQuery);
                stmt.setString(1, price.vendor);
                stmt.setString(2, price.instrument);
                stmt.setDouble(3, price.price);
                stmt.setTimestamp(4, Timestamp.from(price.instant));
                return stmt;
            }
        });
		
		List<VendorPrice> priceList = 
		template.query("select * from price", new RowMapper<VendorPrice>() {

			@Override
			public VendorPrice mapRow(ResultSet arg0, int arg1) throws SQLException {
				VendorPrice p = new VendorPrice(
						arg0.getString("vendor"),
						arg0.getString("instrument"),
						arg0.getDouble("price")
						);
				return p;
			}
			
		});
		
		Logger.getGlobal().info(String.format("total number of records :: %s", priceList.size()));
	}

	@Override
	public void addPriceListener(PriceListener listener) {
		listeners.put(listener.listenerId, listener);
		listener.interest.forEach((vendor, instruments) -> {
			instruments.forEach(instrument -> {
				listenerMap.computeIfAbsent(vendor.concat(instrument), v -> new ArrayList<String>())
						.add(listener.listenerId);
			});
		});
	}

	@Override
	public void removePriceListener(String id) {
		PriceListener listener = listeners.remove(id);
		listener.interest.forEach((vendor, instruments) -> {
			instruments.forEach(instrument -> {
				String key = vendor.concat(instrument);
				if (listenerMap.containsKey(key)) {
					listenerMap.get(key).remove(id);
				}
			});
		});
	}
	
	private DataSource getDatasource() {

		//TODO ideally use spring configuration to inject the appropriate datasource. This is for illustration purpose.
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder
			.setType(EmbeddedDatabaseType.DERBY) 
			.addScript("db/sql/create-db.sql")
			.addScript("db/sql/insert-data.sql")
			.build();
		return db;
	}
	
	private JdbcTemplate setupJdbcTemplate() {
		return new JdbcTemplate(getDatasource());
	}

	public void shutdown() throws InterruptedException {
		shutdown.set(true);
		Logger.getGlobal().info(String.format("shutdown set to true :: %s", shutdown.get()));
		executor.shutdown();
		try {
			if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
		}
		Logger.getGlobal()
				.info(String.format("finished shutdown :: %s :: %s", executor.isTerminated(), executor.isShutdown()));
		cache.shutdown();
	}

}
