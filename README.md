# ⚡ TradePulse — Complete Java Desktop Trading Platform

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)](https://adoptium.net)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.4-orange?logo=java)](https://openjfx.io)
[![Database](https://img.shields.io/badge/Database-SQLite%20%7C%20PostgreSQL-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org)
[![Build](https://img.shields.io/badge/Build-Maven%203.9-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![Tests](https://img.shields.io/badge/Tests-JUnit%205%20Passing-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**TradePulse** is an advanced, production-grade Java desktop trading terminal and financial simulation ecosystem built with **Java 21 LTS**, **JavaFX 21**, **SQLite / PostgreSQL**, and **Maven**. Designed as a high-performance portfolio application, it delivers professional fintech visual design, realistic multi-threaded market simulation, atomic virtual order execution, interactive candlestick charting, social trading community with private messaging, and administrative moderation.

---

## 📸 Key Features & Architecture

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                       TRADEPULSE TERMINAL                                  │
├─────────────────┬────────────────────────────────────────────────────────────────┬───────────────┤
│    SIDEBAR      │                         MAIN WORKSPACE                         │ MARKET INSIGHT│
│  ─────────────  │  ────────────────────────────────────────────────────────────  │ ───────────── │
│  📊 Dashboard   │  • Top Metric Cards (Portfolio Value, Cash, Today P&L, Return) │ 📰 Live News  │
│  📈 Market (66) │  • Interactive Canvas Candlestick Chart (OHLC + SMA + Vol)    │    Marquee    │
│  💼 Portfolio   │  • Real-Time Order Execution Panel (Buy/Sell, Fees, Balances)  │ 🟢 Top 5      │
│  📜 Orders      │  • Asset Allocation Breakdown & Sector Pie Charts              │    Gainers    │
│  👥 Social Chat │  • Target Price Alert Engine & In-App Notification Center      │ 🔴 Top 5      │
│  📰 News Feed   │  • Searchable 66-Stock Market Watch with Sector Filters        │    Losers     │
│  🔔 Alerts      │  • Social Community Directory & 1-to-1 Private Chat Threads    │ 🏆 Trader     │
│  💬 Feedback    │  • Role-Based Admin Console (Feedback Review & User Status)    │    Ranking    │
│  ⚙️ Settings    │  • Instant Dark / Light Theme Toggle (Custom Modern CSS)       │ 📊 Index Sum  │
└─────────────────┴────────────────────────────────────────────────────────────────┴───────────────┘
```

### 1. 📈 Market Watch (66 Listed Companies)
- **66 Real-World Bluechip Companies** spanning **Technology, Semiconductors, Indian Bluechips, Global Banking & Finance, Healthcare, Retail, Energy, and Industrials** (e.g., AAPL, NVDA, MSFT, RELIANCE, TCS, HDFCBANK, INFY, JPM, LLY, TSLA, CAT, GE).
- Real-time search by ticker symbol or company name.
- Sector filter dropdown (*All Sectors, Technology, Banking & Finance, IT Services, Healthcare, Energy, etc.*) and movement filter (*Gainers, Losers*).
- Live pricing, daily range, volume, change percent badges, and watchlist star controls.

### 2. 🕯️ Interactive Candlestick Chart Canvas
- Custom high-performance JavaFX `Canvas` renderer with double buffering and responsive resizing.
- Crisp green (`#10B981`) and red (`#EF4444`) OHLC candlestick bodies with upper/lower wicks.
- **20-Period Moving Average (SMA)** overlay line in cyan (`#06B6D4`).
- **Volume Histogram** dynamically rendered in the lower 25% of the chart height.
- **Interactive Crosshair & Hover HUD**: Pinpoints date, Open, High, Low, Close, and Volume directly under the cursor with precise axis price tags.
- Selectable timeframes: `1m`, `5m`, `1h`, `1D`.

### 3. 💳 Virtual Trading Engine & Average Cost Accounting
- **Atomic Database Transactions** ensuring balance and holding consistency.
- **Buying**: Live cash verification, 0.05% brokerage fee calculation, atomic virtual balance deduction, weighted-average cost basis accounting (`(oldShares*oldAvg + newShares*price) / totalShares`), order creation, transaction logging, and automated execution notification.
- **Selling**: Holdings quantity verification, proceeds calculation, atomic cash credit, holding shares reduction or cleanup, realized P&L tracking, and transaction logging.
- **Duplicate Execution Prevention**: Thread-safe user lock coordination (`ConcurrentHashMap<Integer, Object>`).
- Decoupled financial state: Cash, Invested Capital, Market Value, Realized P&L, and Unrealized P&L are strictly isolated.

### 4. 💼 Portfolio Analytics & Allocation
- Real-time valuation metric cards: Total Portfolio Value, Available Cash, Invested Capital, Unrealized P&L, Realized P&L, and Total Return %.
- **Sector Allocation Pie Chart** (`PieChart`) breaking down portfolio distribution by industry.
- Comprehensive holdings table with ticker, name, shares, average cost, current price, total cost, current value, unrealized P&L ($ and %), allocation %, and quick trade actions.
- Account reset option allowing users to restore virtual funds to ₹1,00,000 for fresh testing.

### 5. 👥 Social Community & Private 1-to-1 Messaging
- **Public Trader Directory**: View other traders, avatar initials, display names, @handles, bios, return percentages, win rates, and total trade counts.
- **Fair Leaderboard Algorithm**: Traders are ranked strictly by performance formula:
  $$\text{Return \%} = \frac{\text{Total Portfolio Value} - \text{Initial Balance}}{\text{Initial Balance}} \times 100$$
  *(Prevents users from gaming ranks simply by depositing extra virtual cash!)*
- **Private 1-to-1 Chat**: Send instant messages to other traders with real-time bubble styling, timestamps, unread badges, and server-side authorization checks.

### 6. 🔔 Target Price Alerts & Notification Center
- Create alerts for stocks rising above ($\ge$) or dropping below ($\le$) target prices.
- Background evaluator continuously monitors price movements and fires immediate in-app toast banners and notification center updates upon trigger.
- Notification bell in the header bar with unread count badge and interactive popover.

### 7. 📰 Live Financial News & Side Insights Panel
- Support for live **NewsAPI** integration via Java's non-blocking `HttpClient`.
- Embedded, realistic financial intelligence fallback with curated articles from Bloomberg, Reuters, Mint, and WSJ.
- Independent right-side market insights panel with auto-scrolling news ticker that **pauses on mouse hover**, top 5 gainers, top 5 losers, and trader leaderboard.

### 8. 🛡️ Administrative Moderation Dashboard
- Strict **Role-Based Access Control (RBAC)**: Only accounts with `Role.ADMIN` can access admin endpoints.
- User feedback submission: 5-star rating, category (Bug, Feature, Suggestion, General), and comments.
- Admin dashboard: submission count, average platform rating, rating distribution breakdown, review queue with "Mark Reviewed" button, and user deactivation/activation controls.

### 9. 🎨 Modern Dark & Light Design System
- Polished dark terminal aesthetic (`#0B0F19`, `#111827`, `#1E293B`) and clean light mode (`#F8FAFC`, `#FFFFFF`, `#E2E8F0`).
- Seamless live theme switching without application restarts.

---

## 🛠️ Technology Stack

| Component | Technology | Version |
|---|---|---|
| **Runtime** | OpenJDK / Eclipse Temurin | 21 LTS |
| **GUI Framework** | OpenJFX (Controls, Graphics, FXML, Base) | 21.0.4 |
| **Build Tool** | Apache Maven | 3.9.9 |
| **Default Database** | SQLite JDBC (Zero-setup local persistence) | 3.46.1.0 |
| **Enterprise Database** | PostgreSQL JDBC Driver | 42.7.4 |
| **Connection Pooling** | HikariCP | 5.1.0 |
| **Security / Crypto** | jBCrypt (Salted Password Hashing) | 0.4 |
| **JSON Handling** | Jackson Databind & JSR310 Datatype | 2.17.2 |
| **Testing** | JUnit 5 (Jupiter API, Engine, Params) | 5.11.0 |
| **Logging** | SLF4J & Logback Classic | 2.0.16 / 1.5.8 |

---

## 🚀 Getting Started

### Prerequisites
1. **Java 21 LTS** installed and configured on your system (`java -version`).
2. **Maven 3.9+** installed (`mvn -version`).

*(On this development environment, Java 21 LTS is located at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` and Maven is located at `C:\Users\Admin\.gemini\antigravity\tools\apache-maven-3.9.9\bin`)*.

### Environment Setup
A `.env.example` file is included in the project root:
```properties
# Database Configuration (SQLITE by default for zero-config setup)
DB_TYPE=SQLITE
DB_PATH=tradepulse.db

# Optional: If connecting to PostgreSQL:
# DB_TYPE=POSTGRES
# DB_URL=jdbc:postgresql://localhost:5432/tradepulse
# DB_USER=postgres
# DB_PASSWORD=your_password

# Optional: Real NewsAPI Key (https://newsapi.org)
NEWS_API_KEY=your_news_api_key_here
```

---

## 💻 Running the Application

### Option A: Run directly with JavaFX Maven Plugin
```powershell
mvn javafx:run
```

### Option B: Run the Pre-packaged Standalone Executable JAR
```powershell
java -jar target/tradepulse-nexus-1.0.0.jar
```

---

## 🧪 Running Automated Tests
The project features automated unit tests covering authentication, BCrypt hashing, atomic trade execution, insufficient funds/holdings validation, P&L calculations, price alerts, role-based security, and private chat access control.

Run the test suite:
```powershell
mvn test
```

Test Results:
```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🔑 Default Seed Accounts

The application automatically seeds initial data on first launch:

| Role | Username | Password | Purpose |
|---|---|---|---|
| **Administrator** | `admin` | `Admin@123` | Full administrative console access & moderation |
| **Active Trader** | `alex_trader` | `Trader@123` | Active portfolio with pre-seeded positions & trades |
| **Quant Trader** | `sophia_quant` | `Trader@123` | High-return community trader for chat & leaderboard |
| **Value Investor** | `rahul_invests` | `Trader@123` | Indian bluechips portfolio |
| **Portfolio Mgr** | `elena_capital` | `Trader@123` | Macro investor in healthcare & energy |

*You can also register any new account directly via the registration tab!*

---

## 📁 Project Structure

```
tradepulse-nexus/
├── pom.xml                                    # Maven project configuration & dependencies
├── README.md                                  # Complete platform documentation
├── .env.example                               # Environment configuration template
├── .gitignore                                 # Git ignore rules
├── src/
│   ├── main/
│   │   ├── java/com/tradepulse/
│   │   │   ├── MainApp.java                   # JavaFX Application lifecycle entry point
│   │   │   ├── Launcher.java                  # Standalone JAR launcher
│   │   │   ├── database/
│   │   │   │   ├── DatabaseConfig.java        # DB configuration resolver (SQLite/PostgreSQL)
│   │   │   │   ├── DatabaseManager.java       # HikariCP connection pool & schema runner
│   │   │   │   └── DatabaseSeeder.java        # Seeds 66 stocks, users, portfolios, feedback
│   │   │   ├── model/                         # Domain models (User, StockQuote, Holding, Order, etc.)
│   │   │   ├── service/                       # Decoupled business logic & concurrency
│   │   │   │   ├── AuthService.java           # BCrypt registration, login, and profiles
│   │   │   │   ├── MarketDataService.java     # Multi-threaded market price simulation
│   │   │   │   ├── TradingEngine.java         # Atomic buy/sell engine with fee accounting
│   │   │   │   ├── PortfolioService.java      # Position tracking, P&L, and watchlists
│   │   │   │   ├── AlertService.java          # Price threshold evaluation & triggers
│   │   │   │   ├── NotificationService.java   # In-app notification center
│   │   │   │   ├── NewsService.java           # Asynchronous NewsAPI & financial cache
│   │   │   │   ├── ChatService.java           # Private 1-to-1 messaging with RBAC
│   │   │   │   ├── AdminService.java          # Role-based feedback & user administration
│   │   │   │   ├── LeaderboardService.java    # Fair % return rankings calculation
│   │   │   │   └── ServiceRegistry.java       # Central dependency coordinator
│   │   │   ├── ui/
│   │   │   │   ├── MainLayout.java            # App shell coordinating header, sidebar, views
│   │   │   │   ├── components/                # CandlestickChartCanvas, Sidebar, HeaderBar,
│   │   │   │   │                              # MarketInsightsPanel, ToastNotification, NotificationModal
│   │   │   │   └── views/                     # DashboardView, MarketWatchView, CompanyDetailView,
│   │   │   │                                  # PortfolioView, OrdersView, SocialView, NewsView,
│   │   │   │                                  # AlertsView, FeedbackView, AdminView, SettingsView
│   │   │   └── util/
│   │   │       └── ThemeManager.java          # Live Dark & Light theme switcher
│   │   └── resources/com/tradepulse/
│   │       ├── css/
│   │       │   ├── dark.css                   # Premium Dark theme stylesheet
│   │       │   └── light.css                  # Clean Light theme stylesheet
│   │       └── schema/
│   │           ├── schema-sqlite.sql          # SQLite schema DDL
│   │           └── schema-postgres.sql        # PostgreSQL schema DDL
│   └── test/java/com/tradepulse/
│       ├── TestDbHelper.java                  # Isolated SQLite testing harness
│       ├── AuthServiceTest.java               # Auth & BCrypt unit tests
│       ├── TradingEngineTest.java             # Buy/Sell, fees, averaging, & P&L tests
│       └── AdminAndSecurityTest.java          # Role-based access & chat privacy tests
```

---

## 🔒 Security & Data Privacy
- **Salted BCrypt Hashing**: Passwords are never stored in plain text.
- **Strict Data Isolation**: User portfolios, orders, private messages, and notifications are scoped strictly to the authenticated account ID.
- **SQL Injection Prevention**: All queries use parameterized `PreparedStatement` interfaces.
- **Authorization Verification**: Chat conversations and administrative functions verify identity and roles on the service layer, preventing UI-level privilege escalation.
