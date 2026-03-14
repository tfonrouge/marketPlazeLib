# MarketPlazeLib — Test Plan

## Unit Tests (commonLib:jvmTest)

### MoneyTest
| Test | Validates |
|------|-----------|
| addition works correctly | `Money + Money` |
| subtraction works correctly | `Money - Money` |
| multiplication by int | `Money * Int` |
| multiplication by long | `Money * Long` |
| division works correctly | `Money / Int` |
| unary minus | `-Money` |
| ZERO constant | `Money.ZERO.cents == 0` |
| comparison | `<`, `>`, `==` via Comparable |
| fromDecimal | `Money.fromDecimal(100, 2) == Money(10000)` |
| toDecimal | `Money(1550).toDecimal(2) == 15.50` |

### PricingServiceTest
| Test | Validates |
|------|-----------|
| ProfitCalculator computes correct breakdown | Commission %, fixed fees, totals, margin |
| MarketplaceComparator ranks correctly | bestByProfit picks cheapest marketplace |
| PriceSuggester minimum price | Inverse calculation: cost / (1 - comm% - margin%) |
| PriceSuggester competitive pricing | MATCH_LOWEST, MATCH_AVERAGE, BELOW_AVERAGE strategies |

## Integration Tests (shopifyLib:jvmTest)

### ShopifyTest
| Test | Validates |
|------|-----------|
| shopify product properties test | @ShopifyField annotations match GraphQL node properties |

## Test Execution

```bash
./gradlew :commonLib:jvmTest              # Money + Pricing tests
./gradlew :shopifyLib:jvmTest             # @ShopifyField reflection test
./gradlew jvmTest                         # All JVM tests
./gradlew build                           # Full build including JS compilation
```
