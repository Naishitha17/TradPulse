package com.tradepulse.database;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseSeeder {
    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    public static void seed(DatabaseManager dbManager) {
        try (Connection conn = dbManager.getConnection()) {
            seedStocks(conn);
            seedUsersAndProfiles(conn);
            seedFeedback(conn);
        } catch (Exception e) {
            log.error("Error during database seeding: {}", e.getMessage(), e);
        }
    }

    private static void seedStocks(Connection conn) throws Exception {
        String countSql = "SELECT COUNT(*) FROM stocks";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                log.info("Stocks table already populated ({} items). Skipping seed.", rs.getInt(1));
                return;
            }
        }

        String insertSql = "INSERT INTO stocks (ticker, name, sector, description, market_cap, pe_ratio, high_52w, low_52w, base_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // 60+ real companies across multiple sectors
        String[][] stockData = {
            // Tech & Software
            {"AAPL", "Apple Inc.", "Technology", "Designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories.", "$3.45T", "33.8", "237.23", "164.08", "231.50"},
            {"MSFT", "Microsoft Corporation", "Technology", "Develops and supports software, services, devices, and enterprise cloud solutions worldwide.", "$3.15T", "35.2", "468.35", "366.50", "428.20"},
            {"NVDA", "NVIDIA Corporation", "Technology", "Pioneer in GPU-accelerated computing and artificial intelligence infrastructure.", "$3.38T", "54.1", "144.42", "45.11", "141.54"},
            {"GOOGL", "Alphabet Inc.", "Technology", "Global tech leader in internet search, cloud computing, software, and autonomous driving.", "$2.12T", "24.6", "191.75", "129.40", "168.90"},
            {"AMZN", "Amazon.com Inc.", "Technology", "E-commerce giant, cloud computing provider (AWS), digital streaming, and artificial intelligence.", "$1.95T", "43.2", "201.20", "118.35", "186.40"},
            {"META", "Meta Platforms Inc.", "Technology", "Builds technologies that help people connect, find communities, and grow businesses.", "$1.48T", "28.5", "602.95", "279.40", "591.20"},
            {"TSLA", "Tesla Inc.", "Automotive & Clean Energy", "Designs and manufactures electric vehicles, battery energy storage, and solar panels.", "$812B", "68.4", "271.00", "138.80", "255.60"},
            {"AVGO", "Broadcom Inc.", "Semiconductors", "Global technology leader designing semiconductor and infrastructure software solutions.", "$820B", "37.5", "185.16", "86.10", "176.40"},
            {"ORCL", "Oracle Corporation", "Technology", "Enterprise database software, cloud-engineered systems, and enterprise software products.", "$475B", "41.2", "178.60", "99.26", "172.30"},
            {"ADBE", "Adobe Inc.", "Technology", "Diversified software company offering digital media and marketing solutions worldwide.", "$225B", "42.1", "638.25", "433.90", "502.80"},
            {"CRM", "Salesforce Inc.", "Technology", "Customer relationship management technology connecting companies with their clients.", "$278B", "48.2", "318.71", "193.35", "291.50"},
            {"AMD", "Advanced Micro Devices", "Semiconductors", "High-performance computing, graphics, and visualization technologies.", "$235B", "102.4", "227.30", "116.05", "154.20"},
            {"INTC", "Intel Corporation", "Semiconductors", "Designs and manufactures microprocessors and foundry semiconductor technologies.", "$98B", "26.1", "51.28", "18.51", "22.80"},
            {"CSCO", "Cisco Systems Inc.", "Networking", "Designs and sells internet protocol based networking and communications products.", "$228B", "24.3", "58.20", "44.50", "56.40"},
            {"IBM", "International Business Machines", "Technology", "Hybrid cloud infrastructure, enterprise AI consulting, and software solutions.", "$205B", "22.8", "237.37", "135.88", "218.10"},
            {"NOW", "ServiceNow Inc.", "Technology", "Enterprise cloud provider optimizing digital workflows for global business operations.", "$192B", "58.7", "955.00", "527.20", "925.30"},
            {"INTU", "Intuit Inc.", "Financial Software", "Provides financial management and compliance solutions including TurboTax and QuickBooks.", "$178B", "57.4", "682.90", "473.10", "632.40"},
            {"UBER", "Uber Technologies Inc.", "Consumer Mobility", "Global platform connecting consumers with ridesharing, grocery delivery, and freight.", "$158B", "36.2", "87.00", "39.40", "78.90"},
            {"ABNB", "Airbnb Inc.", "Consumer Services", "Operates a global online marketplace for short-term homestays and experiences.", "$85B", "31.8", "170.10", "110.20", "134.50"},

            // Indian Market Leaders (Bluechips)
            {"RELIANCE", "Reliance Industries Ltd", "Conglomerate", "India's largest company spanning energy, retail, telecom (Jio), and digital services.", "₹18.5T", "24.8", "3217.90", "2221.05", "2745.50"},
            {"TCS", "Tata Consultancy Services", "IT Services", "Global IT services, consulting, and business solutions leader.", "₹14.8T", "29.2", "4592.25", "3311.00", "4060.00"},
            {"HDFCBANK", "HDFC Bank Ltd", "Banking & Finance", "India's leading private sector bank providing personal and commercial banking.", "₹13.2T", "19.5", "1794.00", "1363.55", "1740.20"},
            {"INFY", "Infosys Limited", "IT Services", "Next-generation digital services, consulting, and enterprise software engineering.", "₹7.8T", "27.4", "1991.45", "1358.35", "1855.00"},
            {"ICICIBANK", "ICICI Bank Ltd", "Banking & Finance", "Prominent Indian financial services institution offering diversified retail banking.", "₹8.9T", "18.1", "1300.00", "913.80", "1265.40"},
            {"SBIN", "State Bank of India", "Banking & Finance", "India's largest public sector commercial bank and financial services conglomerate.", "₹7.3T", "11.2", "912.00", "555.25", "815.30"},
            {"BHARTIARTL", "Bharti Airtel Ltd", "Telecommunications", "Leading global telecommunications company operating across Asia and Africa.", "₹9.4T", "58.6", "1712.00", "890.00", "1675.00"},
            {"ITC", "ITC Limited", "Consumer Goods", "Major Indian diversified conglomerate in FMCG, hotels, paperboards, and agribusiness.", "₹6.1T", "28.3", "528.55", "399.30", "482.15"},
            {"LT", "Larsen & Toubro Ltd", "Engineering & Infra", "Global engineering, procurement, construction, manufacturing, and tech conglomerate.", "₹4.9T", "34.1", "3948.00", "2868.00", "3580.00"},
            {"TATAMOTORS", "Tata Motors Ltd", "Automotive", "Global automobile manufacturer producing commercial vehicles, passenger cars, and Jaguar Land Rover.", "₹3.3T", "10.4", "1179.05", "621.50", "882.40"},
            {"MARUTI", "Maruti Suzuki India", "Automotive", "India's premier passenger car manufacturer and mobility solutions provider.", "₹3.6T", "26.5", "13680.00", "9900.00", "11450.00"},
            {"SUNPHARMA", "Sun Pharmaceutical Ind.", "Healthcare", "India's largest specialty generic pharmaceutical company.", "₹4.5T", "39.4", "1960.35", "1072.00", "1890.00"},
            {"BAJFINANCE", "Bajaj Finance Ltd", "Financial Services", "India's foremost non-banking financial institution specializing in consumer lending.", "₹4.3T", "29.8", "8190.00", "6360.00", "6980.00"},
            {"TITAN", "Titan Company Ltd", "Consumer Goods", "India's leading lifestyle company with leadership in jewelry, watches, and eyewear.", "₹3.1T", "85.2", "3868.00", "3055.00", "3320.00"},
            {"WIPRO", "Wipro Limited", "IT Services", "Information technology, consulting, and business process services company.", "₹2.8T", "23.4", "579.00", "375.00", "550.00"},

            // Global Finance
            {"JPM", "JPMorgan Chase & Co.", "Banking & Finance", "Largest bank in the United States and premier global financial services firm.", "$635B", "12.6", "225.48", "138.80", "220.10"},
            {"V", "Visa Inc.", "Financial Services", "World leader in digital payments facilitating transactions across consumers and merchants.", "$560B", "30.4", "293.07", "227.68", "282.50"},
            {"MA", "Mastercard Inc.", "Financial Services", "Global technology company in the payments industry connecting consumers and banks.", "$468B", "36.2", "519.80", "371.40", "501.20"},
            {"BAC", "Bank of America Corp", "Banking & Finance", "American multinational investment bank and financial services company.", "$328B", "14.2", "44.44", "24.96", "41.90"},
            {"MS", "Morgan Stanley", "Financial Services", "Global investment bank and wealth management services provider.", "$192B", "18.3", "120.40", "71.10", "118.20"},
            {"GS", "The Goldman Sachs Group", "Financial Services", "Leading global investment banking, securities, and investment management firm.", "$172B", "16.1", "525.00", "289.36", "512.40"},
            {"BLK", "BlackRock Inc.", "Asset Management", "World's largest asset manager providing investment and risk management solutions.", "$148B", "24.5", "1015.00", "670.00", "995.00"},
            {"PYPL", "PayPal Holdings Inc.", "Fintech", "Leading digital payments platform enabling digital commerce across 200+ markets.", "$86B", "19.8", "82.50", "50.25", "81.40"},
            {"SQ", "Block Inc.", "Fintech", "Builds ecosystem tools for merchants (Square) and peer-to-peer finance (Cash App).", "$44B", "34.5", "87.52", "39.20", "72.80"},
            {"COIN", "Coinbase Global Inc.", "Fintech & Crypto", "Premier cryptocurrency exchange and institutional crypto financial infrastructure.", "$53B", "38.1", "283.48", "72.80", "214.50"},

            // Healthcare & Life Sciences
            {"LLY", "Eli Lilly and Company", "Healthcare", "Discovers, develops, and delivers life-changing medicines in oncology, diabetes, and obesity.", "$815B", "108.5", "972.53", "531.00", "892.40"},
            {"UNH", "UnitedHealth Group Inc.", "Healthcare", "Diversified healthcare company providing benefits and health technology services (Optum).", "$520B", "27.8", "606.38", "436.38", "565.00"},
            {"JNJ", "Johnson & Johnson", "Healthcare", "Researches, develops, and manufactures innovative medical devices and pharmaceuticals.", "$385B", "24.1", "168.96", "143.16", "161.40"},
            {"ABBV", "AbbVie Inc.", "Healthcare", "Discovers and develops advanced therapies in immunology, oncology, and neuroscience.", "$340B", "48.2", "200.40", "136.20", "192.30"},
            {"MRK", "Merck & Co. Inc.", "Healthcare", "Global pharmaceutical company developing innovative health solutions and vaccines.", "$265B", "19.4", "134.63", "98.24", "104.80"},
            {"AZN", "AstraZeneca PLC", "Healthcare", "Biopharmaceutical company focusing on oncology, cardiovascular, and rare diseases.", "$240B", "38.1", "87.00", "60.40", "77.50"},
            {"NVO", "Novo Nordisk A/S", "Healthcare", "Global healthcare pioneer specialized in diabetes and obesity treatments.", "$510B", "39.8", "147.20", "88.10", "116.40"},

            // Retail & Consumer Goods
            {"WMT", "Walmart Inc.", "Retail", "World's largest omnichannel retailer serving hundreds of millions of customers weekly.", "$660B", "34.2", "83.34", "49.85", "82.40"},
            {"PG", "Procter & Gamble Co.", "Consumer Goods", "Provides branded consumer packaged goods across beauty, grooming, and health.", "$395B", "26.4", "177.94", "141.45", "167.80"},
            {"COST", "Costco Wholesale Corp", "Retail", "Operates membership warehouses offering branded and private-label merchandise.", "$402B", "54.8", "924.00", "525.00", "908.50"},
            {"KO", "The Coca-Cola Company", "Beverages", "World's largest nonalcoholic beverage company with products sold in 200+ countries.", "$290B", "27.5", "73.53", "51.55", "66.90"},
            {"PEP", "PepsiCo Inc.", "Beverages & Snacks", "Global food and beverage company with leading brands in snacks and soft drinks.", "$235B", "24.6", "182.40", "155.80", "171.20"},
            {"MCD", "McDonald's Corp", "Consumer Food", "World's leading global foodservice retailer with over 40,000 locations worldwide.", "$212B", "25.8", "317.90", "243.50", "295.40"},
            {"NKE", "NIKE Inc.", "Apparel & Footwear", "World's premier athletic footwear, apparel, equipment, and accessories company.", "$124B", "23.9", "123.39", "70.75", "82.30"},
            {"DIS", "The Walt Disney Company", "Entertainment", "Diversified family entertainment and media enterprise spanning parks, studios, and streaming.", "$175B", "36.4", "123.74", "78.73", "95.60"},
            {"HD", "The Home Depot Inc.", "Retail", "World's largest home improvement specialty retailer.", "$398B", "26.7", "415.56", "274.26", "401.30"},

            // Energy & Industrials
            {"XOM", "Exxon Mobil Corporation", "Energy", "Exploration and production of crude oil and natural gas, and petrochemicals manufacturing.", "$475B", "14.8", "126.34", "95.77", "119.50"},
            {"CVX", "Chevron Corporation", "Energy", "Integrated energy operations including exploration, refining, and renewable fuels.", "$285B", "15.2", "167.11", "136.00", "155.80"},
            {"SHEL", "Shell PLC", "Energy", "Global energy and petrochemicals company exploring lower-carbon energy solutions.", "$218B", "11.6", "74.33", "60.40", "67.80"},
            {"CAT", "Caterpillar Inc.", "Industrial Machinery", "Leading manufacturer of construction and mining equipment, diesel engines, and gas turbines.", "$192B", "17.9", "401.00", "223.76", "388.20"},
            {"GE", "GE Aerospace", "Aerospace & Defense", "World-leading provider of commercial and military aircraft engines and systems.", "$205B", "34.5", "194.00", "84.50", "186.50"},
            {"BA", "The Boeing Company", "Aerospace & Defense", "Leading manufacturer of commercial jetliners, defense, space, and security systems.", "$95B", "-31.2", "267.54", "140.00", "154.20"}
        };

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (String[] stock : stockData) {
                pstmt.setString(1, stock[0]);
                pstmt.setString(2, stock[1]);
                pstmt.setString(3, stock[2]);
                pstmt.setString(4, stock[3]);
                pstmt.setString(5, stock[4]);
                pstmt.setDouble(6, Double.parseDouble(stock[5]));
                pstmt.setDouble(7, Double.parseDouble(stock[6]));
                pstmt.setDouble(8, Double.parseDouble(stock[7]));
                pstmt.setDouble(9, Double.parseDouble(stock[8]));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            log.info("Seeded {} stocks successfully.", stockData.length);
        }
    }

    private static void seedUsersAndProfiles(Connection conn) throws Exception {
        String countSql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String userSql = "INSERT INTO users (username, email, password_hash, display_name, role) VALUES (?, ?, ?, ?, ?)";
        String profileSql = "INSERT INTO profiles (user_id, bio, avatar_seed, virtual_balance, initial_balance, currency, theme) VALUES (?, ?, ?, ?, ?, ?, ?)";

        String[][] users = {
            {"admin", "admin@tradepulse.internal", "Admin@123", "System Administrator", "ADMIN", "TradePulse Nexus Core Moderator & Administrator", "avatar_admin", "500000.00", "500000.00", "INR", "DARK"},
            {"alex_trader", "alex@fintech.dev", "Trader@123", "Alex Morgan", "USER", "Swing trader focusing on large-cap tech and breakout momentum setups.", "avatar_alex", "145250.00", "100000.00", "INR", "DARK"},
            {"sophia_quant", "sophia@quant.ai", "Trader@123", "Sophia Chen", "USER", "Algorithmic momentum trader and macro market researcher.", "avatar_sophia", "182400.00", "100000.00", "INR", "DARK"},
            {"rahul_invests", "rahul@mumbai.co", "Trader@123", "Rahul Sharma", "USER", "Long-term value investor analyzing fundamental earnings and Indian bluechips.", "avatar_rahul", "124800.00", "100000.00", "INR", "DARK"},
            {"elena_capital", "elena@vanguard.org", "Trader@123", "Elena Rostova", "USER", "Macro portfolio manager balancing healthcare dividends and energy equities.", "avatar_elena", "116500.00", "100000.00", "INR", "LIGHT"},
            {"marcus_hedge", "marcus@london.ac", "Trader@123", "Marcus Vance", "USER", "High-beta tech equity analyst and derivatives specialist.", "avatar_marcus", "94200.00", "100000.00", "INR", "DARK"}
        };

        for (String[] u : users) {
            String hash = BCrypt.hashpw(u[2], BCrypt.gensalt(10));
            int userId;
            try (PreparedStatement pstmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, u[0]);
                pstmt.setString(2, u[1]);
                pstmt.setString(3, hash);
                pstmt.setString(4, u[3]);
                pstmt.setString(5, u[4]);
                pstmt.executeUpdate();
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        userId = keys.getInt(1);
                    } else {
                        continue;
                    }
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(profileSql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, u[5]);
                pstmt.setString(3, u[6]);
                pstmt.setDouble(4, Double.parseDouble(u[7]));
                pstmt.setDouble(5, Double.parseDouble(u[8]));
                pstmt.setString(6, u[9]);
                pstmt.setString(7, u[10]);
                pstmt.executeUpdate();
            }

            // Seed sample watchlist for user 2 (alex_trader)
            if (u[0].equals("alex_trader")) {
                seedTraderPortfolio(conn, userId);
            }
        }
        log.info("Seeded initial users, admin account, and profiles.");
    }

    private static void seedTraderPortfolio(Connection conn, int userId) throws Exception {
        // Seed some sample holdings and transactions for alex_trader
        String holdingSql = "INSERT INTO holdings (user_id, ticker, shares, average_buy_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(holdingSql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, "NVDA");
            pstmt.setInt(3, 50);
            pstmt.setDouble(4, 115.00);
            pstmt.addBatch();

            pstmt.setInt(1, userId);
            pstmt.setString(2, "AAPL");
            pstmt.setInt(3, 40);
            pstmt.setDouble(4, 210.00);
            pstmt.addBatch();

            pstmt.setInt(1, userId);
            pstmt.setString(2, "RELIANCE");
            pstmt.setInt(3, 20);
            pstmt.setDouble(4, 2550.00);
            pstmt.addBatch();

            pstmt.executeBatch();
        }

        // Seed sample watchlist
        String watchSql = "INSERT INTO watchlists (user_id, ticker) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(watchSql)) {
            String[] list = {"AAPL", "NVDA", "MSFT", "RELIANCE", "TCS", "TSLA"};
            for (String t : list) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, t);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // Seed sample orders & transactions
        String orderSql = "INSERT INTO orders (user_id, ticker, type, status, quantity, price, fee, total_amount, realized_pnl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(orderSql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, "NVDA");
            pstmt.setString(3, "BUY");
            pstmt.setString(4, "COMPLETED");
            pstmt.setInt(5, 50);
            pstmt.setDouble(6, 115.00);
            pstmt.setDouble(7, 2.88);
            pstmt.setDouble(8, 5752.88);
            pstmt.setDouble(9, 0.0);
            pstmt.executeUpdate();
        }
    }

    private static void seedFeedback(Connection conn) throws Exception {
        String countSql = "SELECT COUNT(*) FROM feedback";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        String insertSql = "INSERT INTO feedback (user_id, rating, category, comment, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setInt(1, 2); // alex_trader
            pstmt.setInt(2, 5);
            pstmt.setString(3, "GENERAL");
            pstmt.setString(4, "TradePulse Nexus is exceptional! The dark mode candlestick terminal and simulated execution speed are top notch.");
            pstmt.setString(5, "REVIEWED");
            pstmt.addBatch();

            pstmt.setInt(1, 3); // sophia_quant
            pstmt.setInt(2, 5);
            pstmt.setString(3, "SUGGESTION");
            pstmt.setString(4, "Would love to see exponential moving average (EMA 20/50) overlays directly on the candlestick canvas chart!");
            pstmt.setString(5, "PENDING");
            pstmt.addBatch();

            pstmt.setInt(1, 4); // rahul_invests
            pstmt.setInt(2, 4);
            pstmt.setString(3, "FEATURE");
            pstmt.setString(4, "Great platform for paper trading Indian equities. Please add trailing stop-loss simulation when possible.");
            pstmt.setString(5, "PENDING");
            pstmt.addBatch();

            pstmt.executeBatch();
            log.info("Seeded initial community feedback.");
        }
    }
}
