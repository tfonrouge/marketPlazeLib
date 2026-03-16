# MarketPlazeLib — Test Plan

## Test Coverage Matrix

| REQ | Description | Test ID | Module |
|-----|-------------|---------|--------|
| FR-01 | Money arithmetic | TEST-001 to TEST-010 | commonLib |
| FR-06 | ProfitCalculator | TEST-011 | commonLib |
| FR-07 | MarketplaceComparator | TEST-012 | commonLib |
| FR-08 | PriceSuggester min price | TEST-013 | commonLib |
| FR-09 | PriceSuggester competitive | TEST-014 | commonLib |
| FR-11 | SyncScheduler lifecycle | TEST-015 to TEST-022 | commonLib |
| FR-19 | @ShopifyField mapping | TEST-023 | shopifyLib |
| FR-01..FR-14 | Integration scenarios | TEST-024 to TEST-037 | samples |

## Unit Tests: Money (commonLib:jvmTest — MoneyTest)

### TEST-001: addition works correctly
- **Covers**: FR-01
- **Steps**: Money(1000) + Money(2500)
- **Expected**: Money(3500)

### TEST-002: subtraction works correctly
- **Covers**: FR-01
- **Steps**: Money(5000) - Money(2000)
- **Expected**: Money(3000)

### TEST-003: multiplication by int
- **Covers**: FR-01
- **Steps**: Money(1500) * 3
- **Expected**: Money(4500)

### TEST-004: multiplication by long
- **Covers**: FR-01
- **Steps**: Money(1500) * 3L
- **Expected**: Money(4500)

### TEST-005: division works correctly
- **Covers**: FR-01
- **Steps**: Money(9000) / 3
- **Expected**: Money(3000)

### TEST-006: unary minus
- **Covers**: FR-01
- **Steps**: -Money(1000)
- **Expected**: Money(-1000)

### TEST-007: ZERO constant
- **Covers**: FR-01
- **Steps**: Money.ZERO.cents
- **Expected**: 0L

### TEST-008: comparison
- **Covers**: FR-01
- **Steps**: Money(1000) < Money(2000), Money(3000) > Money(1000)
- **Expected**: true, true

### TEST-009: fromDecimal
- **Covers**: FR-01
- **Steps**: Money.fromDecimal(100, 2)
- **Expected**: Money(10000)

### TEST-010: toDecimal
- **Covers**: FR-01
- **Steps**: Money(1550).toDecimal(2)
- **Expected**: 15.50

## Unit Tests: Pricing Services (commonLib:jvmTest — PricingServiceTest)

### TEST-011: ProfitCalculator computes correct breakdown without taxes
- **Covers**: FR-06, BR-02, BR-03
- **Steps**: calculate(salePrice=10000, baseCost=4000, costStructure with 2.9%+30 commission + 2900 fee)
- **Expected**: commission=320, fee=2900, totalCosts=7220, netProfit=2780, margin=27.8%, healthLevel=HEALTHY

### TEST-012: ProfitCalculator with MX tax withholdings
- **Covers**: FR-06, BR-09, BR-10, BR-11, BR-12, BR-13
- **Steps**: calculate with ML marketplace (ivaRate=0.08, isrRate=0.025, ivaIncluded=true), $500 sale, 16% commission, price-based shipping tiers
- **Expected**: taxableBase=43103, ivaWithheld=3448, isrWithheld=1077, shippingCost=0 (free above $299), healthLevel=EXCELLENT

### TEST-013: ProfitCalculator with discount
- **Covers**: FR-06, BR-09
- **Steps**: calculate with 10% discount on $100 sale
- **Expected**: finalPrice=9000, salePrice=10000 (original preserved)

### TEST-014: ProfitCalculator with presentation per-piece metrics
- **Covers**: FR-06, FR-23, BR-14
- **Steps**: calculate with Display 10×50 (500 pieces), $1000 sale
- **Expected**: piecesCount=500, pricePerPiece=200

### TEST-015: resolveShippingCost finds correct tier
- **Covers**: FR-21, BR-13
- **Steps**: 3 price-based tiers ($0-99→$25, $99-299→$37, $299+→free), test $50/$150/$500
- **Expected**: $25, $37, $0

### TEST-016: HealthLevel classification
- **Covers**: FR-22
- **Steps**: classify 5%, 12%, 20%, 35%, -5%
- **Expected**: CRITICAL, ACCEPTABLE, HEALTHY, EXCELLENT, CRITICAL

### TEST-017: MarketplaceComparator ranks correctly
- **Covers**: FR-07
- **Steps**: compare with MarketplaceCostPair (cheap 1% vs expensive 15% + IVA/ISR)
- **Expected**: bestByProfit = cheap marketplace

### TEST-018: PriceSuggester minimum price for margin
- **Covers**: FR-08, BR-04
- **Steps**: minimumPriceForMargin(baseCost=5000, 10% commission, targetMargin=20%)
- **Expected**: Money(7142)

### TEST-019: PriceSuggester with MX taxes raises minimum price
- **Covers**: FR-08, BR-11, BR-12
- **Steps**: same calculation with vs without ML marketplace taxes
- **Expected**: minPriceWithTax > minPriceNoTax

### TEST-020: PriceSuggester competitive pricing
- **Covers**: FR-09
- **Steps**: 3 competitor prices (8000, 10000, 12000), test all 3 strategies
- **Expected**: MATCH_LOWEST=8000, MATCH_AVERAGE=10000, BELOW_AVERAGE=9500

## Unit Tests: SyncScheduler (commonLib:jvmTest — SyncSchedulerTest)

### TEST-021: schedule runs sync task
- **Covers**: FR-11, BR-05
- **Steps**: schedule task with 50ms interval, wait 130ms
- **Expected**: syncCount >= 2

### TEST-022: schedule returns task ID
- **Covers**: FR-11
- **Steps**: schedule task, inspect returned ID
- **Expected**: contains marketplace._id.id + engine class name

### TEST-023: isRunning returns true for active task
- **Covers**: FR-11
- **Steps**: schedule task, check isRunning after 10ms
- **Expected**: true

### TEST-024: cancel stops a task
- **Covers**: FR-11
- **Steps**: schedule → wait → cancel → check isRunning
- **Expected**: false

### TEST-025: cancelAll stops all tasks
- **Covers**: FR-11
- **Steps**: schedule 2 tasks → cancelAll → check activeTaskIds
- **Expected**: empty set

### TEST-026: onResult callback receives sync results
- **Covers**: FR-11, BR-05
- **Steps**: schedule with onResult callback, wait for 2+ cycles
- **Expected**: results list has >= 2 entries, all State.Ok

### TEST-027: failing sync engine does not crash scheduler
- **Covers**: FR-11, BR-05
- **Steps**: schedule with FailingSyncEngine (throws RuntimeException), wait 130ms
- **Expected**: scheduler still running, results contain errors

### TEST-028: reschedule replaces existing task
- **Covers**: FR-11, BR-06
- **Steps**: schedule engine1, wait, reschedule with engine2, wait
- **Expected**: engine1 stopped incrementing, engine2 started, same taskId

## Integration Test: ShopifyField (shopifyLib:jvmTest — ShopifyTest)

### TEST-029: shopify product properties test
- **Covers**: FR-19, BR-08
- **Steps**: compare ShopifyProduct @ShopifyField-annotated properties vs GetProductsQuery.Node properties via reflection
- **Expected**: property name lists match exactly

## Integration Tests: Samples (samples:jvmTest — SampleIntegrationTest)

### TEST-030 to TEST-043: End-to-end scenarios
- **Covers**: FR-01 to FR-14 (models, filters, pricing, serialization)
- 14 tests covering: model creation+serialization, filter copy pattern, ProfitCalculator on Shopify/MercadoLibre, MarketplaceComparator ranking, PriceSuggester strategies, Money arithmetic+serialization, full end-to-end scenario

## Test Execution

```bash
./gradlew :commonLib:jvmTest     # TEST-001 to TEST-028 (28 tests: 10 Money + 10 Pricing + 8 SyncScheduler)
./gradlew :shopifyLib:jvmTest    # TEST-029 (1 test)
./gradlew :samples:jvmTest       # TEST-030 to TEST-043 (14 tests)
./gradlew jvmTest                # All 43 tests
./gradlew build                  # Full build including JS compilation
```

## Not Tested (known gaps)

- KVision column definitions, filter views, form builders — no JS unit tests (visual components, tested by consuming app)
- Shopify GraphQL live sync — requires real Shopify store credentials
- MercadoLibre/Amazon sync — abstract implementations, tested by consumer
- MongoDB Coll persistence — tested in consuming app with real/embedded MongoDB
