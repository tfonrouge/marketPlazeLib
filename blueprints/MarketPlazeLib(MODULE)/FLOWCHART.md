# MarketPlazeLib — Flowcharts

## 1. Happy Path: Shopify Pull Sync

```mermaid
sequenceDiagram
    participant App
    participant Scheduler as SyncScheduler
    participant Sync as IShopifyColl
    participant Apollo as Apollo GraphQL Client
    participant API as Shopify Admin API
    participant DB as MongoDB

    App->>Scheduler: schedule(SyncTask(marketplace, syncEngine, interval))
    loop Every interval
        Scheduler->>Sync: syncToLocal(marketplace)
        Sync->>Sync: buildApolloClient(marketplace.connectionConfig)
        Sync->>API: GetProductsQuery(first=250, after=null)
        loop while hasNextPage
            API-->>Sync: page {edges, pageInfo}
            Sync->>Sync: processSubNodes(media→images)
            Sync->>Sync: additionalFields(marketplaceId)
            Sync->>DB: bulkWrite(UpdateOneModel, upsert=true)
            Sync->>API: GetProductsQuery(first=250, after=endCursor)
        end
        Sync-->>Scheduler: SimpleState(isOk=true, "Updated N documents")
        Scheduler->>App: onResult(SimpleState)
    end
```

## 2. Decision Tree: Sync Engine Selection

```mermaid
flowchart TD
    A[App wants to sync marketplace] --> B{marketplace.type?}
    B -->|Shopify| C[IShopifyColl.syncToLocal]
    B -->|MercadoLibre| D[IMercadoLibreProductColl.syncToLocal]
    B -->|Amazon| E[IAmazonProductColl.syncToLocal]
    B -->|Unknown| F[Error: no sync engine for type]
    C --> G{connectionConfig has url + apiVersion + accessToken?}
    G -->|Yes| H[Build Apollo client → paginated fetch]
    G -->|No| I[Error: Missing connection config keys]
    D --> J{Consumer implemented fetchProducts?}
    J -->|Yes| K[Fetch → bulk upsert]
    J -->|No| L[Abstract method — consumer must implement]
    E --> J
```

## 3. Exception Handling: Sync Failures

```mermaid
flowchart TD
    A[syncToLocal called] --> B{Build Apollo client}
    B -->|Missing config key| C[Return simpleErrorState: Missing key in connectionConfig]
    B -->|Success| D{Execute GraphQL query}
    D -->|Network error| E[Return simpleErrorState: connection failed]
    D -->|Auth error 401| F[Return simpleErrorState: invalid access token]
    D -->|Rate limit 429| G[Return simpleErrorState: rate limited]
    D -->|Success| H{Parse response}
    H -->|No data| I[Return simpleErrorState: No data found]
    H -->|Parse error| J[Return simpleErrorState: deserialization failed]
    H -->|Success| K{Bulk write to MongoDB}
    K -->|Not acknowledged| L[Return SimpleState isOk=false: not acknowledged]
    K -->|Success| M[Return SimpleState isOk=true: Updated N docs]

    N[SyncScheduler catches exceptions] --> O{Exception type}
    O -->|Any| P[Wrap in SimpleState isOk=false]
    P --> Q[Call onResult callback]
    Q --> R[Continue scheduling — do NOT crash]
```

## 4. Profit Calculation Flow (with MX taxes)

```mermaid
flowchart TD
    A["salePrice + baseCost + costStructure + marketplace + discount + presentation?"] --> B["finalPrice = salePrice × (1 - discount)"]
    B --> C{"marketplace.ivaIncluded?"}
    C -->|true| D["taxableBase = finalPrice / 1.16"]
    C -->|false| E["taxableBase = finalPrice"]
    D --> F["ivaWithheld = taxableBase × ivaRate"]
    E --> F
    F --> G["isrWithheld = taxableBase × isrRate"]
    G --> H[Apply Commissions: finalPrice × % + fixed]
    H --> I[Apply Fees + Modality Fees]
    I --> J["shippingCost = resolveShippingCost(finalPrice, tiers)"]
    J --> K["totalCosts = baseCost + packagingCost + all lineItems"]
    K --> L["netProfit = finalPrice - totalCosts"]
    L --> M["margin% = netProfit / finalPrice × 100"]
    M --> N["healthLevel = classify(margin%)"]
    N --> O{"presentation.totalPieces > 0?"}
    O -->|yes| P["per-piece: price/cost/profit ÷ totalPieces"]
    O -->|no| Q[per-piece = null]
    P --> R[CostBreakdown]
    Q --> R
```

## 5. Marketplace Comparison Flow

```mermaid
flowchart TD
    A[Product + N CostStructures] --> B[MarketplaceComparator.compare]
    B --> C[For each CostStructure]
    C --> D[ProfitCalculator.calculate]
    D --> E[List of CostBreakdowns]
    E --> F[maxByOrNull netProfit → bestByProfit]
    E --> G[maxByOrNull profitMargin → bestByMargin]
    F --> H[ComparisonResult]
    G --> H
```

## 6. Price Suggestion Flow

```mermaid
flowchart TD
    A[Base Cost + CostStructure + Marketplace + Target Margin] --> B[PriceSuggester.minimumPriceForMargin]
    B --> C[fixedCosts = baseCost + fees + fixedCommissions + modalityFees]
    C --> D["taxRate = (ivaRate + isrRate) / 1.16 if ivaIncluded"]
    D --> D2["denominator = 1 - commissionRate - taxRate - marginRate"]
    D --> E{"denominator ≤ 0?"}
    E -->|Yes| F["Return Money(Long.MAX_VALUE) — impossible margin"]
    E -->|No| G["minPrice = fixedCosts / denominator"]
    G --> H[Money]

    I[Competitor Prices + Strategy] --> J[PriceSuggester.competitivePrice]
    J --> K{"prices empty?"}
    K -->|Yes| L[Return null]
    K -->|No| M{Strategy?}
    M -->|MATCH_LOWEST| N[min of prices]
    M -->|MATCH_AVERAGE| O[average of prices]
    M -->|BELOW_AVERAGE| P["95% of average"]
```

## 7. SyncScheduler Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Idle: SyncScheduler created
    Idle --> Running: schedule(SyncTask)
    Running --> Running: schedule(same marketplace) — cancel old + start new
    Running --> Running: schedule(different marketplace) — add concurrent
    Running --> PartialStop: cancel(taskId)
    PartialStop --> Idle: last task cancelled
    PartialStop --> Running: schedule(new task)
    Running --> Idle: cancelAll()
    Running --> Terminated: shutdown()
    Idle --> Terminated: shutdown()
    PartialStop --> Terminated: shutdown()

    state Running {
        [*] --> Syncing
        Syncing --> WaitingInterval: sync complete (ok or error)
        WaitingInterval --> Syncing: interval elapsed
    }
```
