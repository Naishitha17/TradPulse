package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.OHLC;
import com.tradepulse.model.StockQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MarketDataService {
    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private final DatabaseManager dbManager;
    private final Map<String, StockQuote> quotes = new ConcurrentHashMap<>();
    private final List<Consumer<StockQuote>> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler;
    private final Random random = new Random();
    private boolean isSimulation = true;

    public MarketDataService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MarketSimulator-Thread");
            t.setDaemon(true);
            return t;
        });

        loadStocksFromDatabase();
        generateInitialHistoricalData();
        startMarketSimulation();
    }

    private void loadStocksFromDatabase() {
        String sql = "SELECT ticker, name, sector, description, market_cap, pe_ratio, high_52w, low_52w, base_price FROM stocks";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                StockQuote quote = new StockQuote(
                        rs.getString("ticker"),
                        rs.getString("name"),
                        rs.getString("sector"),
                        rs.getString("description"),
                        rs.getString("market_cap"),
                        rs.getDouble("pe_ratio"),
                        rs.getDouble("high_52w"),
                        rs.getDouble("low_52w"),
                        rs.getDouble("base_price")
                );
                quotes.put(quote.getTicker(), quote);
            }
            log.info("Loaded {} stocks into MarketDataService.", quotes.size());
        } catch (Exception e) {
            log.error("Failed to load stocks from database", e);
        }
    }

    private void generateInitialHistoricalData() {
        LocalDateTime now = LocalDateTime.now();
        for (StockQuote quote : quotes.values()) {
            double current = quote.getCurrentPrice();
            double walkPrice = current * (0.90 + (random.nextDouble() * 0.15));
            List<OHLC> candles = new ArrayList<>();

            // 60 historical bars representing prior trading periods
            for (int i = 60; i >= 1; i--) {
                LocalDateTime barTime = now.minusHours(i);
                double volatility = walkPrice * (0.005 + random.nextDouble() * 0.015);
                double open = walkPrice;
                double change = (random.nextGaussian() * volatility);
                double close = Math.max(1.0, open + change);
                double high = Math.max(open, close) + (random.nextDouble() * volatility * 0.8);
                double low = Math.min(open, close) - (random.nextDouble() * volatility * 0.8);
                low = Math.max(0.5, low);
                long vol = 10_000 + (long)(random.nextDouble() * 80_000);

                candles.add(new OHLC(barTime, round(open), round(high), round(low), round(close), vol));
                walkPrice = close;
            }

            // The final historical close meets the current quote's previousClose
            quote.setPreviousClose(round(walkPrice));
            quote.setHistory(candles);

            // Add the opening bar for today
            quote.addOHLC(new OHLC(now, quote.getCurrentPrice(), quote.getDayHigh(), quote.getDayLow(), quote.getCurrentPrice(), quote.getVolume()));
        }
    }

    private void startMarketSimulation() {
        scheduler.scheduleAtFixedRate(this::simulateMarketTick, 1, 2, TimeUnit.SECONDS);
        log.info("Market simulation engine started. Ticking every 2 seconds.");
    }

    private void simulateMarketTick() {
        try {
            // Pick a random subset of 8-15 stocks to update each tick for realistic market dynamism
            List<StockQuote> all = new ArrayList<>(quotes.values());
            if (all.isEmpty()) return;

            Collections.shuffle(all);
            int updateCount = Math.min(all.size(), 12 + random.nextInt(8));

            for (int i = 0; i < updateCount; i++) {
                StockQuote quote = all.get(i);
                double current = quote.getCurrentPrice();

                // Realistic percentage movement: mean 0, standard deviation 0.25%
                double drift = 0.0001; // slight positive market bias
                double shock = random.nextGaussian() * 0.0035;
                double pctChange = drift + shock;
                double newPrice = current * (1.0 + pctChange);
                newPrice = Math.max(1.0, round(newPrice));

                long tradeVolume = 500 + random.nextInt(4500);
                quote.updatePrice(newPrice, tradeVolume);

                // Update current candle
                OHLC currentCandle = new OHLC(LocalDateTime.now(), quote.getOpenPrice(), quote.getDayHigh(), quote.getDayLow(), newPrice, quote.getVolume());
                quote.addOHLC(currentCandle);

                // Notify subscribers
                for (Consumer<StockQuote> listener : listeners) {
                    try {
                        listener.accept(quote);
                    } catch (Exception ex) {
                        log.debug("Listener error on quote update: {}", ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during market tick simulation", e);
        }
    }

    public void addQuoteListener(Consumer<StockQuote> listener) {
        this.listeners.add(listener);
    }

    public void removeQuoteListener(Consumer<StockQuote> listener) {
        this.listeners.remove(listener);
    }

    public List<StockQuote> getAllStocks() {
        return new ArrayList<>(quotes.values());
    }

    public StockQuote getStock(String ticker) {
        if (ticker == null) return null;
        return quotes.get(ticker.toUpperCase());
    }

    public List<StockQuote> getTopGainers(int limit) {
        return quotes.values().stream()
                .sorted(Comparator.comparingDouble(StockQuote::getChangePercent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<StockQuote> getTopLosers(int limit) {
        return quotes.values().stream()
                .sorted(Comparator.comparingDouble(StockQuote::getChangePercent))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<String> getSectors() {
        return quotes.values().stream()
                .map(StockQuote::getSector)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<StockQuote> searchStocks(String query, String sector, String movement) {
        return quotes.values().stream()
                .filter(q -> {
                    if (query != null && !query.isBlank()) {
                        String qLower = query.trim().toLowerCase();
                        boolean matchTicker = q.getTicker().toLowerCase().contains(qLower);
                        boolean matchName = q.getName().toLowerCase().contains(qLower);
                        if (!matchTicker && !matchName) return false;
                    }
                    if (sector != null && !sector.equalsIgnoreCase("ALL")) {
                        if (!q.getSector().equalsIgnoreCase(sector)) return false;
                    }
                    if (movement != null && !movement.equalsIgnoreCase("ALL")) {
                        if (movement.equalsIgnoreCase("GAINERS") && q.getChange() <= 0) return false;
                        if (movement.equalsIgnoreCase("LOSERS") && q.getChange() >= 0) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(StockQuote::getTicker))
                .collect(Collectors.toList());
    }

    public Map<String, Double> getMarketIndices() {
        Map<String, Double> indexMap = new ConcurrentHashMap<>();
        // Tech Index
        double techAvg = quotes.values().stream()
                .filter(s -> s.getSector().equalsIgnoreCase("Technology") || s.getSector().equalsIgnoreCase("Semiconductors"))
                .mapToDouble(StockQuote::getChangePercent)
                .average()
                .orElse(0.0);

        // Bluechip Index
        double bluechipAvg = quotes.values().stream()
                .mapToDouble(StockQuote::getChangePercent)
                .average()
                .orElse(0.0);

        // Indian Leaders Index
        double indiaAvg = quotes.values().stream()
                .filter(s -> s.getTicker().matches("RELIANCE|TCS|HDFCBANK|INFY|ICICIBANK|SBIN|ITC|LT|TATAMOTORS|MARUTI"))
                .mapToDouble(StockQuote::getChangePercent)
                .average()
                .orElse(0.0);

        indexMap.put("Pulse Global 50", round(18420.50 * (1 + (bluechipAvg / 100))));
        indexMap.put("Pulse Tech 100", round(21340.20 * (1 + (techAvg / 100))));
        indexMap.put("NIFTY Pulse", round(24860.80 * (1 + (indiaAvg / 100))));
        return indexMap;
    }

    public boolean isSimulation() { return isSimulation; }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private double round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
