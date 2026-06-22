# Strike Platform — Purchase Architecture Audit

---

## 1. Entity Inventory

### `ActiveSubscription` — card-service
Table: `active_subscriptions`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long PK | Subscription ID |
| `userId` | Long NOT NULL | User who purchased |
| `cardDefinitionId` | Long NOT NULL | Card that was purchased |
| `storeId` | Long NOT NULL | Store where card is valid |
| `walletBalance` | BigDecimal(10,2) | **Remaining redeemable balance** |
| `status` | SubscriptionStatus (STRING) | Current lifecycle state |
| `purchasedAt` | LocalDateTime | Purchase timestamp (validity start) |
| `expiresAt` | LocalDateTime | **Validity end date** |
| `createdAt` | LocalDateTime (@CreationTimestamp) | Record creation |
| `updatedAt` | LocalDateTime (@UpdateTimestamp) | Last modification |

---

### `CardDefinition` — card-service
Table: `card_definitions`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long PK | Card ID |
| `vendorId` | Long | Owning vendor |
| `storeId` | Long | Store this card belongs to |
| `name` | String | Card name |
| `description` | String (1000) | Description |
| `cardPrice` | BigDecimal | **What user pays** |
| `walletAmount` | BigDecimal | **Value loaded into wallet on purchase** |
| `validityInDays` | Integer | Duration used to compute `expiresAt` |
| `imageUrl` | String | Card image |
| `isActive` | Boolean | Whether card is purchasable |
| `createdAt` | LocalDateTime | Creation time |
| `updatedAt` | LocalDateTime | Last modification |

---

### `CardCategoryMapping` — card-service
Table: `card_category_mappings`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long PK | — |
| `cardDefinitionId` | Long | FK to CardDefinition |
| `categoryId` | Long | FK to vendor-service category |
| `createdAt` | LocalDateTime | — |

Unique constraint: `(cardDefinitionId, categoryId)`

---

### `CardMenuItemMapping` — card-service
Table: `card_menu_item_mappings`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long PK | — |
| `cardDefinitionId` | Long | FK to CardDefinition |
| `menuItemId` | Long | FK to vendor-service menu item |
| `createdAt` | LocalDateTime | — |

Unique constraint: `(cardDefinitionId, menuItemId)`

---

### `IdempotencyRecord` — card-service
Table: `subscription_idempotency`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long PK | — |
| `idempotencyKey` | String(100), unique | Client-supplied key |
| `responseBody` | TEXT | Serialized response (NULL = in-flight) |
| `httpStatus` | int | HTTP status of stored response |
| `createdAt` | LocalDateTime | — |

---

### `RedemptionRecord` — redemption-service
Table: `redemption_records`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long PK | Redemption ID |
| `subscriptionId` | Long | FK to ActiveSubscription |
| `userId` | Long | User whose subscription was used |
| `storeId` | Long | Store where redemption occurred |
| `totalAmount` | BigDecimal(10,2) | Total deducted in this redemption |
| `status` | RedemptionStatus (STRING) | COMPLETED / FAILED / REVERSED |
| `failureReason` | String | Populated if FAILED |
| `items` | List\<RedemptionItem\> | Line items |
| `createdAt` | LocalDateTime | — |

---

### `RedemptionItem` — redemption-service
Table: `redemption_items`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long PK | — |
| `redemptionRecord` | @ManyToOne | Parent record |
| `menuItemId` | Long | Menu item redeemed |
| `menuItemName` | String | Name at time of redemption |
| `quantity` | Integer | Quantity |
| `unitPrice` | BigDecimal(10,2) | Price per unit |
| `totalPrice` | BigDecimal(10,2) | unitPrice × quantity |

---

### `Transaction` — ledger-service
Table: `ledger_transactions`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long PK | Transaction ID |
| `storeId` | Long | Store involved |
| `customerId` | Long | User ID |
| `subscriptionId` | Long | FK to ActiveSubscription |
| `transactionType` | TransactionType (STRING) | CARD_PURCHASE / REDEMPTION / REFUND / TOP_UP |
| `amount` | BigDecimal(10,2) | Amount of transaction |
| `remarks` | String | Description |
| `createdAt` | LocalDateTime (@CreationTimestamp, read-only) | — |

---

### `UserProfile` — user-service
Table: `user_profiles`

| Field | Type | Purpose |
|---|---|---|
| `userId` | Long PK (not auto-generated) | User ID from auth-service |
| `name` | String | Display name |
| `email` | String | Email |
| `profilePicUrl` | String | Profile picture |
| `mobileNumber` | String(10) | Mobile |
| `latitude` | Double | Location |
| `longitude` | Double | Location |
| `lastLocationAt` | LocalDateTime | Last known location timestamp |
| `createdAt` | LocalDateTime | — |
| `updatedAt` | LocalDateTime | — |

> **Note:** user-service does NOT own any subscription or wallet data. It is a profile + location store only. Subscriptions are entirely owned by card-service.

---

## 2. Enum Inventory

### `SubscriptionStatus` — card-service
```
ACTIVE      → Initial state after purchase
EXPIRED     → expiresAt passed (scheduled job, on-the-fly check, or redemption guard)
EXHAUSTED   → walletBalance reached 0 after a redemption
CANCELLED   → User explicitly cancelled
```

### `RedemptionStatus` — redemption-service
```
COMPLETED   → Redemption processed successfully (only status currently written)
FAILED      → Defined but never written (missing: graceful failure path)
REVERSED    → Defined but never written (missing: reversal logic)
```

### `TransactionType` — ledger-service
```
CARD_PURCHASE  → Written when user purchases a card
REDEMPTION     → Written when vendor redeems
REFUND         → Defined but never written
TOP_UP         → Defined but never written
```

---

## 3. DTO Inventory

### Purchase / Subscription DTOs — card-service

| Class | Fields |
|---|---|
| `PurchaseSubscriptionRequest` | `cardDefinitionId` (Long), `storeId` (Long) |
| `SubscriptionResponse` | `id`, `userId`, `cardDefinitionId`, `cardName`, `storeId`, `walletBalance`, `status`, `purchasedAt`, `expiresAt`, `createdAt` |
| `BalanceResponse` | `subscriptionId`, `walletBalance` |
| `DeductBalanceRequest` | `amount` (BigDecimal, min 0.01) |
| `SubscriptionRedemptionContext` | `userId`, `storeId`, `cardDefinitionId`, `status`, `eligibleCategoryIds`, `eligibleMenuItemIds` |
| `EligibleMenuResponse` | `subscriptionId`, `cardName`, `categories` (nested: categoryId, categoryName, items[]) |

### Card Definition DTOs — card-service

| Class | Key Fields |
|---|---|
| `CreateCardRequest` | `storeId`, `name`, `description`, `cardPrice`, `walletAmount`, `validityInDays`, `imageUrl`, `categoryIds` (≥1), `eligibleMenuItemIds` (optional) |
| `UpdateCardRequest` | All fields of CreateCardRequest, all optional |
| `CardDefinitionResponse` | All definition fields + `bonusAmount` (walletAmount - cardPrice), `categoryIds`, `eligibleMenuItemIds`, `eligibleItems` (rich: id/name/price) |
| `CardPreviewResponse` | `cardId`, `cardName`, `description`, `cardPrice`, `walletAmount`, `validityInDays`, `eligibleMenus` (nested categories + items) |

### Redemption DTOs — redemption-service

| Class | Key Fields |
|---|---|
| `RedemptionRequest` | `subscriptionId`, `storeId`, `items[]` (menuItemId, quantity) |
| `RedemptionResponse` | `id`, `subscriptionId`, `userId`, `storeId`, `totalAmount`, `remainingBalance`, `status`, `items[]`, `createdAt` |
| `RedemptionItemResponse` | `menuItemId`, `menuItemName`, `quantity`, `unitPrice`, `totalPrice` |

### Ledger DTOs — ledger-service

| Class | Key Fields |
|---|---|
| `RecordTransactionRequest` | `storeId`, `customerId`, `subscriptionId`, `transactionType`, `amount`, `remarks` |
| `TransactionResponse` | All above fields + `id` + `createdAt` |

---

## 4. Complete API Inventory

### card-service (`/api/cards`, `/api/subscriptions`)

| Method | Path | Role | Purpose |
|---|---|---|---|
| `POST` | `/api/cards` | VENDOR | Create card definition |
| `GET` | `/api/cards/my` | VENDOR | Vendor's own cards |
| `PUT` | `/api/cards/{id}` | VENDOR | Update card |
| `DELETE` | `/api/cards/{id}` | VENDOR | Deactivate card |
| `GET` | `/api/cards/{id}/preview` | VENDOR | Card preview with menu |
| `GET` | `/api/cards/subscriptions/store/{storeId}` | VENDOR | Subscriptions at store |
| `GET` | `/api/cards/subscriptions/{subscriptionId}` | VENDOR / USER / ADMIN | Get subscription (role-filtered) |
| `GET` | `/api/cards/{id}` | PUBLIC | Card details |
| `GET` | `/api/cards/store/{storeId}` | PUBLIC | Active cards at store |
| `POST` | `/api/subscriptions` | USER | Purchase card (idempotent) |
| `GET` | `/api/subscriptions/my` | USER | User's subscriptions (paginated) |
| `GET` | `/api/subscriptions/my/active` | USER | User's ACTIVE subscriptions (with lazy expiry) |
| `GET` | `/api/subscriptions/{id}` | USER | Get own subscription |
| `PATCH` | `/api/subscriptions/{id}/cancel` | USER | Cancel subscription |
| `GET` | `/api/subscriptions/{subscriptionId}/menu` | USER | Eligible menu for redemption |

### card-service internal

| Method | Path | Called By |
|---|---|---|
| `GET` | `/internal/subscriptions/{id}/balance` | redemption-service |
| `POST` | `/internal/subscriptions/{id}/deduct` | redemption-service |
| `GET` | `/internal/subscriptions/{id}/eligible-category-ids` | redemption-service |
| `GET` | `/internal/subscriptions/{id}/redemption-context` | redemption-service |
| `POST` | `/internal/subscriptions/expire` | ops / admin |
| `GET` | `/internal/cards/{cardDefinitionId}/category-ids` | vendor-service |
| `GET` | `/internal/cards/{cardDefinitionId}/menu-item-ids` | vendor-service |
| `GET` | `/internal/cards/category-mappings/active/{categoryId}` | vendor-service |

### redemption-service (`/api/redemptions`)

| Method | Path | Role | Purpose |
|---|---|---|---|
| `POST` | `/api/redemptions` | VENDOR | Process redemption (idempotent) |
| `GET` | `/api/redemptions/store/{storeId}` | VENDOR / ADMIN | Redemptions at store |
| `GET` | `/api/redemptions/user/{userId}` | USER / ADMIN | User's redemption history |
| `GET` | `/api/redemptions/subscription/{subscriptionId}` | USER / ADMIN | Redemptions for subscription |
| `GET` | `/api/redemptions/{id}` | USER / VENDOR / ADMIN | Single redemption |

### ledger-service (`/api/ledger`)

| Method | Path | Role | Purpose |
|---|---|---|---|
| `GET` | `/api/ledger/user/{userId}` | USER / ADMIN | User's transaction history |
| `GET` | `/api/ledger/subscription/{subscriptionId}` | USER / ADMIN | Transactions for subscription |
| `POST` | `/internal/transactions` | card-service / redemption-service | Record transaction |

---

## 5. Lifecycle Diagram

```
CARD DEFINITION (vendor creates)
         │
         │  User calls POST /api/subscriptions
         ▼
    ┌─────────────────────────────────────────────────────────┐
    │  ActiveSubscription created                             │
    │  walletBalance = card.walletAmount                      │
    │  expiresAt     = now + card.validityInDays              │
    │  status        = ACTIVE                                 │
    └─────────────────────────────┬───────────────────────────┘
                                  │
         ┌────────────────────────┼──────────────────────────────┐
         │                        │                              │
         ▼                        ▼                              ▼
  expiresAt <= now        walletBalance = 0            User cancels
  (job / lazy check /     after redemption             /subscriptions/{id}/cancel
   redemption guard)
         │                        │                              │
         ▼                        ▼                              ▼
      EXPIRED               EXHAUSTED                      CANCELLED
    (terminal)          (can be cancelled)               (terminal)


REDEMPTION FLOW (vendor calls POST /api/redemptions)
  │
  ├─ 1. verifyVendorOwnsStore           → vendor-service
  ├─ 2. getRedemptionContext            → card-service (status, eligibleCategories, eligibleItems)
  ├─ 3. Validate: status=ACTIVE, storeId match, categories configured
  ├─ 4. getMenuItems(storeId)           → vendor-service
  ├─ 5. Validate each item: exists, eligible category, eligible item, not OUT_OF_STOCK
  ├─ 6. Create RedemptionRecord (in-memory)
  ├─ 7. deductBalance(subscriptionId, totalAmount) → card-service
  │       └─ If balance hits 0 → status = EXHAUSTED
  ├─ 8. Save RedemptionRecord to DB
  ├─ 9. recordRedemption()              → ledger-service (non-blocking)
  └─ 10. sendNotification()             → notification-service (best-effort)


EXPIRY JOB (hourly, @Scheduled)
  SubscriptionExpiryJob
  └─ expireOverdueSubscriptions()
       ├─ Fetch ACTIVE where expiresAt < now
       ├─ Send expiry notifications (best-effort)
       └─ Bulk UPDATE status='EXPIRED' WHERE status='ACTIVE' AND expiresAt < now
```

---

## 6. Business Rules

**Purchase rules:**
- Card must be `isActive = true`
- `storeId` in request must match `cardDefinition.storeId`
- No payment validation — subscription is created immediately (no payment gateway)
- Wallet loaded with `card.walletAmount`, NOT `card.cardPrice`
- Bonus is implicit: `walletAmount - cardPrice` = bonus value loaded into wallet

**Balance rules:**
- `walletBalance` is always `≥ 0`
- Deduction: `newBalance = walletBalance - redemptionAmount`
- If `balance < redemptionAmount`: `InsufficientBalanceException` thrown from card-service
- If `newBalance == 0` after deduction: status set to `EXHAUSTED`

**Validity rules:**
- `expiresAt = purchasedAt + validityInDays`
- Expiry checked in 4 places: scheduled job, `deductBalance()`, `getActiveByUser()`, manual ops endpoint
- An EXPIRED subscription cannot be redeemed

**Redemption eligibility rules:**
- Subscription must be `ACTIVE` and not expired
- Subscription must be bound to the requesting store's `storeId`
- Vendor must own the store (`verifyVendorOwnsStore`)
- Each item must belong to an eligible category (`eligibleCategoryIds`)
- If card has item-level restrictions (`eligibleMenuItemIds`), item must be in that set
- Item must not be `OUT_OF_STOCK`

**Cancellation rules:**
- User can cancel `ACTIVE` or `EXHAUSTED` subscriptions
- Cannot cancel already `CANCELLED` subscriptions
- User must own the subscription (ownership check enforced)
- No refund is issued on cancel

**Idempotency rules:**
- Both purchase and redemption support `Idempotency-Key` header
- 3-phase: check → reserve (NULL body = in-flight) → complete (non-NULL body)
- In-flight key returns HTTP 409
- Completed key returns original response with original HTTP status
- On error: key is cancelled (deleted) to allow safe retry

---

## 7. Questions Answered

### Q1. How does a user purchase a card?

`POST /api/subscriptions` with body `{cardDefinitionId, storeId}` and header `X-User-Id`.

`UserSubscriptionController` → `SubscriptionServiceImpl.purchase()`:
1. Validate card exists, is active, storeId matches
2. Create `ActiveSubscription` (balance = walletAmount, status = ACTIVE, expiresAt = now + validityInDays)
3. POST `/internal/transactions` to ledger-service (CARD_PURCHASE, amount = cardPrice)
4. POST `/internal/admin/commission` to admin-service
5. POST notification to notification-service
6. Return HTTP 201 + `SubscriptionResponse`

**No payment is processed.** The subscription is created with full balance immediately.

---

### Q2. What API creates a subscription?

`POST /api/subscriptions` — `UserSubscriptionController` in card-service.

---

### Q3. What entity stores purchased cards?

`ActiveSubscription` (table: `active_subscriptions`) in card-service.

---

### Q4. What fields track remaining balance?

`ActiveSubscription.walletBalance` (BigDecimal, precision 10, scale 2) — sole source of truth.
Decremented atomically by `SubscriptionService.deductBalance()` on every redemption.

`RedemptionResponse.remainingBalance` returns the post-deduction balance to the API caller but is not persisted — it is computed at deduction time.

---

### Q5. What fields track validity?

| Field | Where | What it stores |
|---|---|---|
| `validityInDays` | `CardDefinition` | Duration configured by vendor |
| `purchasedAt` | `ActiveSubscription` | Validity start |
| `expiresAt` | `ActiveSubscription` | Validity end (= purchasedAt + validityInDays) |

---

### Q6. How does redemption affect balances?

`RedemptionServiceImpl.redeem()` calls `CardServiceClient.deductBalance(subscriptionId, totalAmount)` → `POST /internal/subscriptions/{id}/deduct`.

card-service atomically deducts from `walletBalance`. If balance hits 0, `status` is set to `EXHAUSTED`. The new balance is returned and placed in `RedemptionResponse.remainingBalance`.

---

### Q7. How is expiry handled?

- **Primary:** `SubscriptionExpiryJob` — `@Scheduled(cron = "0 0 * * * *")` (top of every hour). Sends notifications, then bulk UPDATE to EXPIRED.
- **At redemption:** `deductBalance()` checks `expiresAt < now` before allowing deduction. If expired, saves EXPIRED status and throws.
- **At user query:** `getActiveByUser()` lazy-checks each ACTIVE subscription and marks EXPIRED if past due.
- **Manual ops:** `POST /internal/subscriptions/expire` — same logic, on-demand.

---

### Q8. Which APIs already exist?

See Section 4 — all endpoints are implemented and functioning:
- card-service: 15 public/user-facing endpoints + 8 internal endpoints
- redemption-service: 5 endpoints
- ledger-service: 2 user-facing + 1 internal

---

### Q9. Which APIs are missing?

See Section 9 — Missing Pieces below.

---

### Q10. What backend changes are required before Phase 4?

See Section 10 — Recommended Phase 4 Scope below.

---

## 8. Inter-Service Call Map

| From | To | Method | Endpoint | Purpose |
|---|---|---|---|---|
| card-service | ledger-service | POST | `/internal/transactions` | Record CARD_PURCHASE |
| card-service | admin-service | POST | `/internal/admin/commission` | Record vendor commission |
| card-service | vendor-service | GET | `/internal/categories/validate` | Validate category IDs on card create |
| card-service | vendor-service | GET | `/internal/categories/by-ids` | Fetch categories with items |
| card-service | vendor-service | GET | `/internal/menu-items/validate` | Validate menu item IDs on card create |
| card-service | vendor-service | GET | `/internal/menu-items/by-ids` | Fetch menu items by ID |
| card-service | notification-service | POST | `/internal/notify/subscription` | Purchase notification |
| card-service | notification-service | POST | `/internal/notify/subscription-expired` | Expiry notification |
| redemption-service | card-service | GET | `/internal/subscriptions/{id}/balance` | Get wallet balance |
| redemption-service | card-service | POST | `/internal/subscriptions/{id}/deduct` | Deduct balance |
| redemption-service | card-service | GET | `/internal/subscriptions/{id}/eligible-category-ids` | Get eligible categories |
| redemption-service | card-service | GET | `/internal/subscriptions/{id}/redemption-context` | Get full subscription context |
| redemption-service | vendor-service | GET | `/internal/menu-items/store/{storeId}` | Fetch all store menu items |
| redemption-service | vendor-service | GET | `/internal/vendors/{vendorId}/owns-store/{storeId}` | Verify vendor owns store |
| redemption-service | ledger-service | POST | `/internal/transactions` | Record REDEMPTION |
| redemption-service | notification-service | POST | `/internal/notify/redemption` | Redemption confirmation |

---

## 9. Missing Pieces

| Gap | Severity | Detail |
|---|---|---|
| **Payment gateway integration** | CRITICAL | `POST /api/subscriptions` creates the subscription with full balance immediately — no payment is collected. `CARD_PURCHASE` is recorded in the ledger for `cardPrice` but the user is never charged. |
| **Commission settlement** | HIGH | `AdminServiceClient.recordCommission()` creates a `CommissionRecord` in admin-service but it stays `PENDING` forever. No settlement job, no payout logic, no status transition to SETTLED. |
| **Refund flow** | MEDIUM | `TransactionType.REFUND` exists in the enum but is never written. Cancelling a subscription produces no refund. No payment reversal logic exists. |
| **Top-up / wallet reload** | MEDIUM | `TransactionType.TOP_UP` exists but is never written. Once a subscription is EXHAUSTED, users must purchase a new card. No `POST /api/subscriptions/{id}/top-up` endpoint exists. |
| **Redemption failure path** | MEDIUM | `RedemptionStatus.FAILED` and `REVERSED` are defined but never used. If `deductBalance()` throws, no `RedemptionRecord` is persisted — no audit trail for failed attempts. |
| **Admin subscription issuance** | MEDIUM | No admin endpoint to create a subscription for a user (for promos, gifts, vouchers). Only users can purchase via the standard flow. |
| **Ledger reconciliation** | MEDIUM | No job or endpoint to verify that the sum of all `REDEMPTION` transactions for a subscription equals `walletAmount - walletBalance`. Partial failures could cause divergence. |
| **Non-atomic ledger write** | MEDIUM | Both purchase and redemption write to the ledger synchronously but on failure the ledger entry is silently dropped with no retry. The subscription/redemption proceeds even if the ledger write fails. |
| **Subscription pause/freeze** | LOW | No PAUSED status. No ability to temporarily freeze a subscription and resume it later. |
| **Card inventory / stock limits** | LOW | `CardDefinition` has no `maxPurchases` field. Cards can be purchased unlimited times. Vendor must manually deactivate the card if stock is exhausted. |

---

## 10. Recommended Phase 4 Scope

### Priority 1 — Revenue (Launch Blocker)
- Integrate payment gateway (Razorpay recommended for India) before `SubscriptionServiceImpl.purchase()` persists the subscription
- Flow: initiate payment order → user pays → verify callback → create subscription
- Record `CARD_PURCHASE` in ledger only after payment confirmation

### Priority 2 — Data Integrity
- Implement redemption failure path: wrap `deductBalance()` in try-catch inside `RedemptionServiceImpl.redeem()`, persist `RedemptionRecord` with `status = FAILED` + `failureReason` when it throws
- Add ledger retry mechanism (outbox pattern or at-least-once retry) so failed ledger writes are not silently dropped

### Priority 3 — Vendor Payout
- Commission settlement job in admin-service: transition `CommissionRecord` from PENDING → SETTLED
- Trigger vendor payout via payment gateway disbursement API

### Priority 4 — User Experience
- Refund API: `POST /api/subscriptions/{id}/refund` — validates cancellation eligibility, reverses payment, records `REFUND` in ledger
- Top-up API: `POST /api/subscriptions/{id}/top-up` — adds balance to EXHAUSTED subscription, records `TOP_UP` in ledger

### Priority 5 — Operations
- Admin subscription issuance: `POST /api/admin/subscriptions` — create subscription for a user without payment (promo / gift / voucher flow)
- Ledger reconciliation endpoint or nightly batch job to detect balance drift