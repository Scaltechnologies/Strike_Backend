# STRIKE PLATFORM — MASTER BACKEND DOCUMENTATION

> **Generated:** 2026-06-20  
> **Stack:** Java 17 · Spring Boot 3.5.11 · PostgreSQL · Spring Cloud Gateway  
> **Source of truth:** Live codebase at `D:\Back_End\Strike\strike`

---

## TABLE OF CONTENTS

1. [System Overview](#1-system-overview)
2. [Complete Route Map](#2-complete-route-map)
3. [Auth Module](#3-auth-module)
4. [User Module](#4-user-module)
5. [Vendor Module](#5-vendor-module)
6. [Card Module](#6-card-module)
7. [Redemption Module](#7-redemption-module)
8. [Ledger Module](#8-ledger-module)
9. [Admin Module](#9-admin-module)
10. [Notification Module](#10-notification-module)
11. [DTO Reference](#11-dto-reference)
12. [Entity Reference](#12-entity-reference)
13. [Enum Reference](#13-enum-reference)
14. [Business Rules](#14-business-rules)
15. [Frontend Screen Mapping](#15-frontend-screen-mapping)
16. [Implementation Priority](#16-implementation-priority)

---

# 1. SYSTEM OVERVIEW

## Architecture

Strike is a meal-card microservices platform. Users purchase prepaid subscription cards from vendors and redeem meals against their wallet balance. Nine Spring Boot services communicate synchronously via REST; all sit behind a single Spring Cloud Gateway.

```
┌─────────────────────────────────────────────────────┐
│                  strike-gateway :8080               │
│              (JWT validation, routing)              │
└───┬───────┬───────┬──────┬──────┬──────┬──────┬────┘
    │       │       │      │      │      │      │
  8081    8082    8083    8084   8085   8086   8087   8088
auth-   user-  vendor- card-  redem- ledger admin- notif-
service service service service ption  service service ication
                              service
```

## Service Responsibilities

| Service | Port | DB Schema Tables | Primary Role |
|---|---|---|---|
| **strike-gateway** | 8080 | — | JWT auth filter, path-based routing |
| **auth-service** | 8081 | `user_auth`, `vendors`, `otp_codes`, `refresh_tokens` | OTP login for users and vendors, JWT issuance, refresh-token rotation |
| **user-service** | 8082 | `user_profiles` | User profile management; proxy aggregator for subscriptions, redemptions, transactions |
| **vendor-service** | 8083 | `vendor_profiles`, `stores`, `store_timings`, `store_holidays`, `categories`, `menu_items` | Vendor profile, store CRUD, menu management, analytics |
| **card-service** | 8084 | `card_definitions`, `active_subscriptions`, `card_category_mappings`, `card_menu_item_mappings`, `idempotency_records` | Card product definitions, subscription purchase, wallet balance, eligibility |
| **redemption-service** | 8085 | `redemption_records`, `redemption_items`, `idempotency_records` | Redemption request lifecycle (PENDING → COMPLETED / REJECTED) |
| **ledger-service** | 8086 | `ledger_transactions` | Immutable financial transaction log |
| **admin-service** | 8087 | `admins`, `admin_refresh_tokens`, `admin_vendor_records`, `commission_records` | Admin auth, vendor approval workflow, commission management, platform analytics |
| **notification-service** | 8088 | `notification_logs` | OTP SMS/email delivery, vendor status notifications, subscription/redemption confirmations |

## Inter-Service Communication

All calls are synchronous HTTP via `RestTemplate` (Spring WebClient not used). Internal endpoints (`/internal/**`) bypass gateway JWT enforcement — they are accessible only within the private network.

```
auth-service ──PATCH──► vendor-service   /internal/vendors/{id}/init
auth-service ──POST──►  admin-service    /internal/admin/vendors
auth-service ──POST──►  notification-service /internal/notify/otp

admin-service ──PATCH──► auth-service    /internal/vendors/{id}/approve|reject|suspend|reactivate
admin-service ──GET──►  vendor-service   /internal/vendors/{id}/store
admin-service ──GET──►  card-service     /api/admin/cards/**
admin-service ──GET──►  redemption-service /api/admin/redemptions/**
admin-service ──GET──►  ledger-service   /api/admin/ledger/**
admin-service ──POST──► notification-service /internal/notify/vendor-status

card-service ──POST──►  ledger-service   /internal/transactions   (CARD_PURCHASE)
card-service ──GET──►   vendor-service   /internal/categories/validate
card-service ──GET──►   vendor-service   /internal/menu-items/validate

redemption-service ──POST──► card-service    /internal/subscriptions/{id}/deduct
redemption-service ──POST──► ledger-service  /internal/transactions   (REDEMPTION)
redemption-service ──POST──► notification-service /internal/notify/redemption
redemption-service ──GET──►  vendor-service  /internal/vendors/{id}/owns-store/{storeId}

admin-service ──POST──► internal/admin/commission  (commission recording after card purchase)
```

## Authentication Flow

### User Login
```
1. POST /api/auth/user/send-otp   { mobileNumber }
   → OTP generated, sent via notification-service SMS/email
   
2. POST /api/auth/user/verify-otp { mobileNumber, otp }
   → Returns: { token, refreshToken, expiresIn, userId, newUser }
   
3. All subsequent calls: Authorization: Bearer <token>
   Gateway validates JWT, adds X-User-Id + X-User-Role headers
```

### Vendor Login
```
1. POST /api/auth/vendor/register  { hotelName, address, mobileNumber, email, lat, lng }
   → Vendor created with status=PENDING
   
2. POST /api/auth/vendor/login     { mobileNumber }
   → OTP sent (only if status=ACTIVE)
   
3. POST /api/auth/vendor/verify    { mobileNumber, otp }
   → If ACTIVE: returns { token, refreshToken, ... }
   → If PENDING/VERIFIED: returns status message, no token
   
4. Admin approves → vendor gets ACTIVE status → can now login
```

### Admin Login
```
1. POST /api/admin/auth/setup      (first time only, creates SUPER_ADMIN)
2. POST /api/admin/auth/login      { email, password }
   → Returns: { token, refreshToken, role, ... }
```

### Token Lifecycle
- **Access token:** 1 hour (HS256 JWT, secret: `STRIKE_SUPER_SECRET_KEY_2026_STRIKE_PLATFORM`)
- **Refresh token:** 30 days, stored in `refresh_tokens` table
- **Rotation:** Each refresh call issues a new refresh token, old one is deleted
- **JWT claims:** `sub` (userId/vendorId), `role` (USER|VENDOR|ADMIN|SUPER_ADMIN), `mobile`

## Gateway Routing

| Path Prefix | Downstream Service | Port |
|---|---|---|
| `/api/auth/**` | auth-service | 8081 |
| `/api/user/**` | user-service | 8082 |
| `/api/vendor/**`, `/api/stores/**`, `/api/menu/**`, `/api/dashboard/**`, `/api/analytics/**` | vendor-service | 8083 |
| `/api/cards/**`, `/api/subscriptions/**` | card-service | 8084 |
| `/api/redemptions/**` | redemption-service | 8085 |
| `/api/ledger/**` | ledger-service | 8086 |
| `/api/admin/**` | admin-service | 8087 |

Gateway performs JWT validation before forwarding. On success, it injects `X-User-Id` and `X-User-Role` headers. Internal paths (`/internal/**`) are **not** routed through the gateway.

---

# 2. COMPLETE ROUTE MAP

## 2.1 auth-service (Port 8081)

### UserAuthController — `/api/auth/user`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `/send-otp` | No | — | `SendOtpRequest` | `String` |
| POST | `/verify-otp` | No | — | `VerifyOtpRequest` | `AuthResponse` |

### VendorAuthController — `/api/auth/vendor`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `/register` | No | — | `RegisterVendorRequest` | `Map<String,Object>` |
| POST | `/login` | No | — | `VendorLoginRequest` | `Map<String,Object>` |
| POST | `/verify` | No | — | `VerifyOtpRequest` | `VendorAuthResponse` |

### AuthRefreshController — `/api/auth`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `/refresh` | No | — | `RefreshRequest` | `Map<String,Object>` (token, refreshToken, expiresIn) |
| POST | `/logout` | No | — | `RefreshRequest` | `String` |

### InternalUserController — `/internal/users`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `` | Internal | — | `page`, `size` query params | `PageResponse<UserAuthResponse>` |
| GET | `/count` | Internal | — | — | `long` |
| GET | `/{userId}` | Internal | — | — | `UserAuthResponse` |
| PATCH | `/{userId}/ban` | Internal | — | — | `Void` |
| PATCH | `/{userId}/unban` | Internal | — | — | `Void` |

### InternalVendorController — `/internal/vendors`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| PATCH | `/{vendorId}/approve` | Internal | — | — | `Void` |
| PATCH | `/{vendorId}/reject` | Internal | — | — | `Void` |
| PATCH | `/{vendorId}/suspend` | Internal | — | — | `Void` |
| PATCH | `/{vendorId}/reactivate` | Internal | — | — | `Void` |

---

## 2.2 user-service (Port 8082)

### UserProfileController — `/api/user`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| GET | `/me` | Yes | USER | — (X-User-Id header) | `UserProfileResponse` |
| PUT | `/me` | Yes | USER | `UserProfileRequest` | `UserProfileResponse` |
| PATCH | `/me/location` | Yes | USER | `UpdateLocationRequest` | `UserProfileResponse` |
| POST | `/me/subscriptions/purchase` | Yes | USER | `PurchaseSubscriptionRequest` | Proxy → card-service |
| GET | `/me/subscriptions` | Yes | USER | `page`, `size` | Proxy → card-service |
| GET | `/me/subscriptions/active` | Yes | USER | — | Proxy → card-service |
| GET | `/me/subscriptions/{id}` | Yes | USER | — | Proxy → card-service |
| PATCH | `/me/subscriptions/{id}/cancel` | Yes | USER | — | Proxy → card-service |
| GET | `/stores/{storeId}/cards` | Yes | USER | — | Proxy → card-service |
| GET | `/me/redemptions` | Yes | USER | `page`, `size` | Proxy → redemption-service |
| GET | `/me/redemptions/{id}` | Yes | USER | — | Proxy → redemption-service |
| GET | `/me/subscriptions/{id}/redemptions` | Yes | USER | `page`, `size` | Proxy → redemption-service |
| GET | `/me/transactions` | Yes | USER | `page`, `size` | Proxy → ledger-service |
| GET | `/me/subscriptions/{id}/transactions` | Yes | USER | `page`, `size` | Proxy → ledger-service |

---

## 2.3 vendor-service (Port 8083)

### PublicStoreController — `/api/stores`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `` | No | — | — | `ApiResponse<List<StoreResponse>>` |
| GET | `/nearby` | No | — | `lat`, `lng`, `radiusKm` query params | `List<StoreResponse>` |
| GET | `/search` | No | — | `q`, `category` query params | `List<StoreResponse>` |
| GET | `/{storeId}` | No | — | — | `StoreResponse` |
| GET | `/{storeId}/menu` | No | — | — | `List<CategoryWithItemsResponse>` |

### StoreController — `/api/vendor/stores`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| GET | `/my` | Yes | VENDOR | — | `StoreResponse` |
| PUT | `/my` | Yes | VENDOR | `StoreDetailsRequest` | `StoreResponse` |
| PATCH | `/my/location` | Yes | VENDOR | `UpdateStoreLocationRequest` | `StoreResponse` |
| PATCH | `/my/status` | Yes | VENDOR | `StoreStatus` (body) | `StoreResponse` |

### VendorProfileController — `/api/vendor/profile`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| GET | `` | Yes | VENDOR | — | `VendorProfileResponse` |
| PUT | `` | Yes | VENDOR | `UpdateVendorProfileRequest` | `VendorProfileResponse` |
| PATCH | `` | Yes | VENDOR | `UpdateVendorProfileRequest` | `VendorProfileResponse` (partial) |
| GET | `/status` | Yes | VENDOR | — | `Map<String,Object>` (status, rejectionReason, commissionRate) |

### StoreTimingController — `/api/vendor/stores/{id}/timings` & `/holidays`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `/{id}/timings` | Yes | VENDOR | `StoreTimingRequest` | `StoreTimingResponse` |
| GET | `/{id}/timings` | Yes | VENDOR | — | `List<StoreTimingResponse>` |
| DELETE | `/{id}/timings/{timingId}` | Yes | VENDOR | — | `void` |
| POST | `/{id}/holidays` | Yes | VENDOR | `StoreHolidayRequest` | `StoreHolidayResponse` |
| GET | `/{id}/holidays` | Yes | VENDOR | — | `List<StoreHolidayResponse>` |
| DELETE | `/{id}/holidays/{holidayId}` | Yes | VENDOR | — | `void` |

### DashboardController — `/api/vendor/dashboard`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/my` | Yes | VENDOR | — | `DashboardSummaryResponse` |
| GET | `/store/{storeId}` | Yes | VENDOR | — | `DashboardSummaryResponse` |

### AnalyticsController — `/api/analytics`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/my` | Yes | VENDOR | — | `AnalyticsResponse` |
| GET | `/store/{storeId}` | Yes | VENDOR | — | `AnalyticsResponse` |

### CategoryController — `/api/menu/categories`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `` | Yes | VENDOR | `CreateCategoryRequest` | `CategoryResponse` |
| GET | `` | Yes | VENDOR | — | `List<CategoryResponse>` |
| GET | `/{categoryId}` | Yes | VENDOR | — | `CategoryResponse` |
| PUT | `/{categoryId}` | Yes | VENDOR | `UpdateCategoryRequest` | `CategoryResponse` |
| DELETE | `/{categoryId}` | Yes | VENDOR | — | `void` |

### MenuItemController — `/api/menu/items`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `` | Yes | VENDOR | `CreateMenuItemRequest` | `MenuItemResponse` |
| GET | `` | Yes | VENDOR | — | `List<MenuItemResponse>` |
| GET | `/{itemId}` | Yes | VENDOR | — | `MenuItemResponse` |
| GET | `/by-category/{categoryId}` | Yes | VENDOR | — | `List<MenuItemResponse>` |
| PUT | `/{itemId}` | Yes | VENDOR | `UpdateMenuItemRequest` | `MenuItemResponse` |
| DELETE | `/{itemId}` | Yes | VENDOR | — | `void` |

### Internal — vendor-service

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/internal/vendors/{vendorId}/init` | `InitVendorProfileRequest` | `ResponseEntity<?>` |
| GET | `/internal/vendors/{vendorId}/store` | — | `StoreResponse` |
| GET | `/internal/vendors/{vendorId}/owns-store/{storeId}` | — | `Map<String,Boolean>` |
| GET | `/internal/categories/validate` | `categoryIds`, `storeId` query params | `List<Long>` |
| GET | `/internal/categories/by-ids` | `ids` query param | `List<CategoryWithItemsResponse>` |
| GET | `/internal/menu-items/store/{storeId}` | — | `List<MenuItemResponse>` |
| GET | `/internal/menu-items/validate` | `itemIds`, `storeId`, `categoryIds` query params | `List<Long>` |
| GET | `/internal/menu-items/by-ids` | `ids` query param | `List<MenuItemResponse>` |

---

## 2.4 card-service (Port 8084)

### UserCardController — `/api/cards` (Public)

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/{id}` | No | — | — | `ApiResponse<CardDefinitionResponse>` |
| GET | `/store/{storeId}` | No | — | — | `List<CardDefinitionResponse>` |

### VendorCardController — `/api/cards`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `` | Yes | VENDOR | `CreateCardRequest` | `CardDefinitionResponse` |
| GET | `/my` | Yes | VENDOR | — | `List<CardDefinitionResponse>` |
| PUT | `/{id}` | Yes | VENDOR | `UpdateCardRequest` | `CardDefinitionResponse` |
| DELETE | `/{id}` | Yes | VENDOR | — | `void` (deactivates) |
| GET | `/{id}/preview` | Yes | VENDOR | — | `CardPreviewResponse` |
| GET | `/subscriptions/store/{storeId}` | Yes | VENDOR | `page`, `size` | `PageResponse<SubscriptionResponse>` |
| GET | `/subscriptions/{subscriptionId}` | Yes | VENDOR/ADMIN | — | `SubscriptionResponse` |

### AdminCardController — `/api/admin/cards`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/{id}` | Yes | ADMIN | — | `CardDefinitionResponse` |
| GET | `/vendor/{vendorId}` | Yes | ADMIN | — | `List<CardDefinitionResponse>` |
| GET | `/subscriptions/store/{storeId}` | Yes | ADMIN | `page`, `size` | `PageResponse<SubscriptionResponse>` |
| GET | `/subscriptions/{id}` | Yes | ADMIN | — | `SubscriptionResponse` |

### UserSubscriptionController — `/api/subscriptions`

| Method | Path | Auth | Role | Request DTO / Headers | Response |
|---|---|---|---|---|---|
| POST | `` | Yes | USER/ADMIN | `PurchaseSubscriptionRequest`, `Idempotency-Key` header | `SubscriptionResponse` |
| GET | `/my` | Yes | USER/ADMIN | `page`, `size` | `PageResponse<SubscriptionResponse>` |
| GET | `/my/active` | Yes | USER/ADMIN | — | `List<SubscriptionResponse>` |
| GET | `/{id}` | Yes | USER/ADMIN | — | `SubscriptionResponse` |
| PATCH | `/{id}/cancel` | Yes | USER/ADMIN | — | `SubscriptionResponse` |
| GET | `/{subscriptionId}/menu` | Yes | USER/ADMIN | — | `EligibleMenuResponse` |

### Internal — card-service

| Method | Path | Request | Response |
|---|---|---|---|
| GET | `/internal/cards/{cardDefinitionId}/category-ids` | — | `List<Long>` |
| GET | `/internal/cards/{cardDefinitionId}/menu-item-ids` | — | `List<Long>` |
| GET | `/internal/cards/category-mappings/active/{categoryId}` | — | `Map<String,Boolean>` |
| GET | `/internal/subscriptions/{id}/balance` | — | `BalanceResponse` |
| POST | `/internal/subscriptions/{id}/deduct` | `DeductBalanceRequest` | `BalanceResponse` |
| GET | `/internal/subscriptions/{id}/eligible-category-ids` | — | `List<Long>` |
| GET | `/internal/subscriptions/{id}/redemption-context` | — | `SubscriptionRedemptionContext` |
| POST | `/internal/subscriptions/expire` | — | `Map<String,Object>` |

---

## 2.5 redemption-service (Port 8085)

### VendorRedemptionController — `/api/redemptions`

| Method | Path | Auth | Role | Request DTO / Headers | Response |
|---|---|---|---|---|---|
| POST | `` | Yes | VENDOR | `RedemptionRequest`, `Idempotency-Key` header | `RedemptionResponse` (direct POS redemption) |
| GET | `/store/{storeId}/queue` | Yes | VENDOR | — | `List<RedemptionQueueResponse>` |
| POST | `/{id}/approve` | Yes | VENDOR | vendorId (X-User-Id) | `RedemptionResponse` |
| POST | `/{id}/reject` | Yes | VENDOR | `reason` query param, vendorId (X-User-Id) | `RedemptionResponse` |
| GET | `/store/{storeId}` | Yes | VENDOR/ADMIN | `page`, `size` | `PageResponse<RedemptionResponse>` |

### UserRedemptionController — `/api/redemptions`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `/request` | Yes | USER | `RedemptionRequest` | `RedemptionResponse` (status=PENDING) |
| GET | `/user/{userId}` | Yes | USER/ADMIN | `page`, `size` | `PageResponse<RedemptionResponse>` |
| GET | `/subscription/{subscriptionId}` | Yes | USER/ADMIN | `page`, `size` | `PageResponse<RedemptionResponse>` |
| GET | `/{id}` | Yes | USER/ADMIN | — | `RedemptionResponse` |

### AdminRedemptionController — `/api/admin/redemptions`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/stats` | Yes | ADMIN | — | `Map<String,Object>` (totalRedemptions, totalAmount) |
| GET | `/all` | Yes | ADMIN | `page`, `size` | `PageResponse<RedemptionResponse>` |
| GET | `/{id}` | Yes | ADMIN | — | `RedemptionResponse` |
| GET | `/store/{storeId}` | Yes | ADMIN | `page`, `size` | `PageResponse<RedemptionResponse>` |
| GET | `/user/{userId}` | Yes | ADMIN | `page`, `size` | `PageResponse<RedemptionResponse>` |
| GET | `/subscription/{subscriptionId}` | Yes | ADMIN | `page`, `size` | `PageResponse<RedemptionResponse>` |

---

## 2.6 ledger-service (Port 8086)

### UserTransactionController — `/api/ledger`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/user/{userId}` | Yes | USER/ADMIN | `page`, `size` | `PageResponse<TransactionResponse>` |
| GET | `/subscription/{subscriptionId}` | Yes | USER/ADMIN | `page`, `size` | `PageResponse<TransactionResponse>` |

### VendorTransactionController — `/api/ledger`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/store/{storeId}` | Yes | VENDOR/ADMIN | `page`, `size` | `PageResponse<TransactionResponse>` |

### AdminTransactionController — `/api/admin/ledger`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/stats` | Yes | ADMIN | — | `Map<String,Object>` (totalTransactions, totalRevenue) |
| GET | `/all` | Yes | ADMIN | `page`, `size` | `PageResponse<TransactionResponse>` |
| GET | `/subscription/{subscriptionId}` | Yes | ADMIN | `page`, `size` | `PageResponse<TransactionResponse>` |
| GET | `/store/{storeId}` | Yes | ADMIN | `page`, `size` | `PageResponse<TransactionResponse>` |
| GET | `/user/{userId}` | Yes | ADMIN | `page`, `size` | `PageResponse<TransactionResponse>` |

### Internal — ledger-service

| Method | Path | Request DTO | Response |
|---|---|---|---|
| POST | `/internal/transactions` | `RecordTransactionRequest` | `TransactionResponse` |

---

## 2.7 admin-service (Port 8087)

### AdminAuthController — `/api/admin/auth`

| Method | Path | Auth | Role | Request DTO | Response |
|---|---|---|---|---|---|
| POST | `/setup` | No | — | `AdminRegisterRequest` | `Map<String,Object>` |
| POST | `/login` | No | — | `AdminLoginRequest` | `AdminLoginResponse` |
| POST | `/refresh` | No | — | `AdminRefreshRequest` | `Map<String,Object>` |
| POST | `/logout` | No | — | `AdminRefreshRequest` | `Map<String,String>` |
| GET | `/me` | Yes | ADMIN/SUPER_ADMIN | — | `Map<String,Object>` |
| PATCH | `/change-password` | Yes | ADMIN/SUPER_ADMIN | `ChangePasswordRequest` | `Map<String,String>` |
| POST | `/register` | Yes | SUPER_ADMIN | `AdminRegisterRequest` | `Map<String,Object>` |
| GET | `/admins` | Yes | SUPER_ADMIN | — | `Map<String,Object>` (list) |
| PATCH | `/admins/{adminId}/deactivate` | Yes | SUPER_ADMIN | — | `Map<String,String>` |
| PATCH | `/admins/{adminId}/activate` | Yes | SUPER_ADMIN | — | `Map<String,String>` |

### AdminVendorController — `/api/admin/vendors`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | `PageResponse<VendorRecord>` |
| GET | `/pending` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | `PageResponse<VendorRecord>` |
| GET | `/active` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | `PageResponse<VendorRecord>` |
| GET | `/suspended` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | `PageResponse<VendorRecord>` |
| GET | `/rejected` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | `PageResponse<VendorRecord>` |
| GET | `/{vendorId}` | Yes | ADMIN/SUPER_ADMIN | — | `VendorRecord` |
| PATCH | `/{vendorId}/approve` | Yes | ADMIN/SUPER_ADMIN | — | `String` |
| PATCH | `/{vendorId}/reject` | Yes | ADMIN/SUPER_ADMIN | `{ reason }` body | `String` |
| PATCH | `/{vendorId}/suspend` | Yes | ADMIN/SUPER_ADMIN | `{ reason }` body | `String` |
| PATCH | `/{vendorId}/reactivate` | Yes | ADMIN/SUPER_ADMIN | — | `String` |
| PATCH | `/{vendorId}/commission-rate` | Yes | ADMIN/SUPER_ADMIN | `{ commissionRate }` body | `String` |
| GET | `/{vendorId}/store` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → vendor-service |
| GET | `/{vendorId}/cards` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → card-service |
| GET | `/{vendorId}/subscriptions` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → card-service |
| GET | `/{vendorId}/redemptions` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → redemption-service |
| GET | `/{vendorId}/transactions` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → ledger-service |

### AdminUserController — `/api/admin/users`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | Proxy → auth-service |
| GET | `/{userId}` | Yes | ADMIN/SUPER_ADMIN | — | `Map<String,Object>` (auth + profile) |
| GET | `/{userId}/subscriptions` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → card-service |
| GET | `/{userId}/subscriptions/active` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → card-service |
| GET | `/{userId}/redemptions` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → redemption-service |
| GET | `/{userId}/transactions` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → ledger-service |
| PATCH | `/{userId}/ban` | Yes | ADMIN/SUPER_ADMIN | — | `String` |
| PATCH | `/{userId}/unban` | Yes | ADMIN/SUPER_ADMIN | — | `String` |

### AdminCardsController — `/api/admin/cards`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/{id}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → card-service |
| GET | `/vendor/{vendorId}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → card-service |
| GET | `/subscriptions/store/{storeId}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → card-service |
| GET | `/subscriptions/{id}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → card-service |

### AdminRedemptionsController — `/api/admin/redemptions`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/all` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → redemption-service |
| GET | `/{id}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → redemption-service |
| GET | `/store/{storeId}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → redemption-service |
| GET | `/user/{userId}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → redemption-service |
| GET | `/subscription/{subscriptionId}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → redemption-service |

### AdminLedgerController — `/api/admin/ledger`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `/all` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → ledger-service |
| GET | `/subscription/{subscriptionId}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → ledger-service |
| GET | `/store/{storeId}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → ledger-service |
| GET | `/user/{userId}` | Yes | ADMIN/SUPER_ADMIN | — | Proxy → ledger-service |

### AdminCommissionController — `/api/admin/commissions`

| Method | Path | Auth | Role | Request | Response |
|---|---|---|---|---|---|
| GET | `` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | `PageResponse<CommissionRecordResponse>` |
| GET | `/pending` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | `PageResponse<CommissionRecordResponse>` |
| GET | `/vendor/{vendorId}` | Yes | ADMIN/SUPER_ADMIN | `page`, `size` | `PageResponse<CommissionRecordResponse>` |
| GET | `/stats` | Yes | ADMIN/SUPER_ADMIN | — | `Map<String,Object>` |
| PATCH | `/{id}/settle` | Yes | ADMIN/SUPER_ADMIN | — | `CommissionRecordResponse` |
| PATCH | `/vendor/{vendorId}/settle-all` | Yes | ADMIN/SUPER_ADMIN | — | `Map<String,Object>` |

### AdminDashboardController — `/api/admin/dashboard`

| Method | Path | Auth | Role | Response |
|---|---|---|---|---|
| GET | `` | Yes | ADMIN/SUPER_ADMIN | Platform stats + pending approvals + recent vendor registrations |

### AdminAnalyticsController — `/api/admin/analytics`

| Method | Path | Auth | Role | Response |
|---|---|---|---|---|
| GET | `/overview` | Yes | ADMIN/SUPER_ADMIN | Users, vendors, subscriptions, revenue, redemptions summary |
| GET | `/revenue` | Yes | ADMIN/SUPER_ADMIN | Monthly revenue + commission breakdown |
| GET | `/vendor-performance` | Yes | ADMIN/SUPER_ADMIN | Per-vendor subscription revenue, commission, count |
| GET | `/commissions` | Yes | ADMIN/SUPER_ADMIN | Pending/settled totals + per-vendor breakdown |
| GET | `/orders` | Yes | ADMIN/SUPER_ADMIN | Total redemptions and order amount |

### AdminPlatformController — `/api/admin/platform`

| Method | Path | Auth | Role | Response |
|---|---|---|---|---|
| GET | `/stats` | Yes | ADMIN/SUPER_ADMIN | Users, vendors by status, transaction stats, redemption stats |

### Internal — admin-service

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/internal/admin/vendors` | `VendorRegistrationRequest` | `Void` (upsert) |
| GET | `/internal/admin/vendors/{vendorId}` | — | `VendorRecord` |
| GET | `/internal/admin/vendors/{vendorId}/status` | — | `Map<String,Object>` (status, rejectionReason, commissionRate) |
| POST | `/internal/admin/commission` | `RecordCommissionRequest` | `CommissionRecordResponse` |

---

## 2.8 notification-service (Port 8088)

### InternalNotificationController — `/internal/notify`

| Method | Path | Auth | Request DTO | Response |
|---|---|---|---|---|
| POST | `/otp` | Internal | `OtpNotificationRequest` | `String` |
| POST | `/vendor-status` | Internal | `VendorStatusNotificationRequest` | `String` |
| POST | `/subscription` | Internal | `SubscriptionNotificationRequest` | `String` |
| POST | `/redemption` | Internal | `RedemptionNotificationRequest` | `String` |
| GET | `/logs` | Internal | — | `List<NotificationLog>` |
| GET | `/logs/recipient/{recipientId}` | Internal | `type` query param (`USER`\|`VENDOR`) | `List<NotificationLog>` |

---

# 3. AUTH MODULE

## 3.1 User Login (OTP)

**Flow:**
1. `POST /api/auth/user/send-otp` — Validates mobile format, checks user not banned, generates 6-digit OTP, stores in `otp_codes` table with 5-minute TTL, calls notification-service to deliver OTP.
2. `POST /api/auth/user/verify-otp` — Validates OTP exists and is not expired. If user does not exist in `user_auth`, creates one (`verified=true`). Generates JWT access token and refresh token. Returns `AuthResponse` with `newUser=true` on first login.

**Rate Limits:**
- 60-second cooldown between OTP requests for the same mobile number
- Maximum 6 OTP requests per hour per mobile number
- OTP expires after 5 minutes

## 3.2 Vendor Registration & Login

**Registration:**
1. `POST /api/auth/vendor/register` — Creates `Vendor` entity with `status=PENDING`. Calls `POST /internal/admin/vendors` to create `VendorRecord` in admin-service.

**Login (OTP):**
2. `POST /api/auth/vendor/login` — Sends OTP **only if** vendor `status=ACTIVE`. Returns error for other statuses.
3. `POST /api/auth/vendor/verify` — Validates OTP. Response varies by status:
   - `ACTIVE` → issues JWT + refresh token
   - `PENDING` / `VERIFIED` → returns status message, no token
   - `SUSPENDED` / `REJECTED` → returns status message, no token

## 3.3 Admin Login

**Email + Password (not OTP):**
1. `POST /api/admin/auth/setup` — One-time super admin creation. Fails if any admin already exists.
2. `POST /api/admin/auth/login` — Validates email + BCrypt-hashed password. Issues admin JWT with role `ADMIN` or `SUPER_ADMIN`.

## 3.4 JWT

- **Algorithm:** HS256
- **Secret:** `STRIKE_SUPER_SECRET_KEY_2026_STRIKE_PLATFORM` (same across all services)
- **Expiry:** 3,600,000 ms (1 hour)
- **Claims:** `sub` (userId/vendorId/adminId as String), `role`, `mobile` (for users/vendors), issued-at, expiry
- **Transport:** `Authorization: Bearer <token>` header

## 3.5 Refresh Tokens

- **Storage:** `refresh_tokens` table (auth-service) / `admin_refresh_tokens` table (admin-service)
- **TTL:** 30 days
- **Rotation:** Every call to `/refresh` deletes the old token and issues a new one
- **Revocation:** `POST /api/auth/logout` deletes the refresh token
- **Invalidation on status change:** If user is banned or vendor is suspended, refresh is rejected and old token is deleted

## 3.6 Gateway JWT Filter

The gateway's `JwtAuthFilter` (reactive WebFlux filter):
1. Reads `Authorization` header
2. Validates JWT signature and expiry
3. Extracts `sub` and `role` claims
4. Adds `X-User-Id` and `X-User-Role` headers to the forwarded request
5. All downstream services extract identity from these headers (not the raw JWT)

**Public paths bypassed by gateway filter:** `/api/auth/**`, Swagger paths

---

# 4. USER MODULE

## 4.1 Profile APIs

| Action | Endpoint | Notes |
|---|---|---|
| Get/create profile | GET `/api/user/me` | Auto-creates profile on first access |
| Update profile | PUT `/api/user/me` | Full update |
| Update location | PATCH `/api/user/me/location` | Coordinates only |

## 4.2 Store Discovery APIs

Routed through user-service proxy → vendor-service:

| Action | Endpoint |
|---|---|
| Browse all stores | GET `/api/stores` (public) |
| Search stores | GET `/api/stores/search?q=&category=` (public) |
| Nearby stores | GET `/api/stores/nearby?lat=&lng=&radiusKm=` (public) |
| Store detail | GET `/api/stores/{storeId}` (public) |
| Store menu | GET `/api/stores/{storeId}/menu` (public) |
| Cards for store | GET `/api/user/stores/{storeId}/cards` (authenticated) |

## 4.3 Card / Subscription APIs

All proxied from user-service to card-service:

| Action | Endpoint |
|---|---|
| Purchase subscription | POST `/api/user/me/subscriptions/purchase` |
| My subscriptions | GET `/api/user/me/subscriptions` |
| My active subscriptions | GET `/api/user/me/subscriptions/active` |
| Subscription detail | GET `/api/user/me/subscriptions/{id}` |
| Cancel subscription | PATCH `/api/user/me/subscriptions/{id}/cancel` |
| Eligible menu for subscription | GET `/api/subscriptions/{subscriptionId}/menu` |

## 4.4 Redemption APIs

Proxied from user-service to redemption-service:

| Action | Endpoint |
|---|---|
| Request redemption | POST `/api/redemptions/request` |
| My redemptions | GET `/api/user/me/redemptions` |
| Redemption detail | GET `/api/user/me/redemptions/{id}` |
| Redemptions for subscription | GET `/api/user/me/subscriptions/{id}/redemptions` |

## 4.5 Transaction APIs

Proxied from user-service to ledger-service:

| Action | Endpoint |
|---|---|
| My transactions | GET `/api/user/me/transactions` |
| Transactions for subscription | GET `/api/user/me/subscriptions/{id}/transactions` |

---

# 5. VENDOR MODULE

## 5.1 Vendor Profile

| Action | Endpoint | Notes |
|---|---|---|
| Get profile | GET `/api/vendor/profile` | Returns `VendorProfileResponse` |
| Full update | PUT `/api/vendor/profile` | All fields |
| Partial update | PATCH `/api/vendor/profile` | Only provided fields |
| Check approval status | GET `/api/vendor/profile/status` | Returns status, rejectionReason, commissionRate |

## 5.2 Store Management

| Action | Endpoint |
|---|---|
| Get my store | GET `/api/vendor/stores/my` |
| Update store details | PUT `/api/vendor/stores/my` |
| Update location | PATCH `/api/vendor/stores/my/location` |
| Update status | PATCH `/api/vendor/stores/my/status` |

## 5.3 Store Timings

| Action | Endpoint | Notes |
|---|---|---|
| Add/update timing | POST `/api/vendor/stores/{id}/timings` | Upserts by day |
| List timings | GET `/api/vendor/stores/{id}/timings` | — |
| Delete timing | DELETE `/api/vendor/stores/{id}/timings/{timingId}` | — |

## 5.4 Store Holidays

| Action | Endpoint |
|---|---|
| Add holiday | POST `/api/vendor/stores/{id}/holidays` |
| List holidays | GET `/api/vendor/stores/{id}/holidays` |
| Delete holiday | DELETE `/api/vendor/stores/{id}/holidays/{holidayId}` |

## 5.5 Store Status

Valid values: `ACTIVE`, `INACTIVE`, `TEMPORARILY_CLOSED`  
Updated via: `PATCH /api/vendor/stores/my/status`

## 5.6 Store Location

`PATCH /api/vendor/stores/my/location` — `{ latitude, longitude }`

## 5.7 Menu Categories

| Action | Endpoint |
|---|---|
| Create category | POST `/api/menu/categories` |
| List my categories | GET `/api/menu/categories` |
| Get category | GET `/api/menu/categories/{categoryId}` |
| Update category | PUT `/api/menu/categories/{categoryId}` |
| Delete category | DELETE `/api/menu/categories/{categoryId}` |

## 5.8 Menu Items

| Action | Endpoint |
|---|---|
| Create item | POST `/api/menu/items` |
| List my items | GET `/api/menu/items` |
| Get item | GET `/api/menu/items/{itemId}` |
| Items by category | GET `/api/menu/items/by-category/{categoryId}` |
| Update item | PUT `/api/menu/items/{itemId}` |
| Delete item | DELETE `/api/menu/items/{itemId}` |

## 5.9 Dashboard APIs

| Action | Endpoint | Returns |
|---|---|---|
| My dashboard | GET `/api/vendor/dashboard/my` | `DashboardSummaryResponse` (revenue, subscriptions, redemptions, recent transactions) |
| Store dashboard | GET `/api/vendor/dashboard/store/{storeId}` | Same as above |
| My analytics | GET `/api/analytics/my` | `AnalyticsResponse` |
| Store analytics | GET `/api/analytics/store/{storeId}` | `AnalyticsResponse` |

---

# 6. CARD MODULE

## 6.1 Card Creation

`POST /api/cards` (VENDOR)

Card creation validates:
- `storeId` must belong to the authenticated vendor (via `/internal/vendors/{vendorId}/owns-store/{storeId}`)
- `categoryIds` must all be active and belong to the store (via `/internal/categories/validate`)
- `eligibleMenuItemIds` (if provided) must belong to valid categories in the store (via `/internal/menu-items/validate`)

On success: creates `CardDefinition` + `CardCategoryMapping` rows + `CardMenuItemMapping` rows (if item IDs provided).

## 6.2 Card Updates

`PUT /api/cards/{id}` (VENDOR)

Ownership check: card's `vendorId` must match authenticated vendor. Updates card fields + re-validates and replaces category/item mappings if provided.

## 6.3 Card Deletion (Deactivation)

`DELETE /api/cards/{id}` (VENDOR)

Sets `isActive=false`. Does not delete from DB. Existing subscriptions remain valid.

## 6.4 Card Assignment (Subscriptions)

`POST /api/subscriptions` (USER)

- Creates `ActiveSubscription` with `walletBalance = card.walletAmount`, `status=ACTIVE`, `expiresAt = now + card.validityInDays`
- Uses `Idempotency-Key` header to prevent double-purchases
- Records `CARD_PURCHASE` transaction in ledger-service
- Records commission in admin-service commission records

## 6.5 Card Subscriptions (Listing)

| Endpoint | Who | Notes |
|---|---|---|
| GET `/api/subscriptions/my` | USER | Paginated, all statuses |
| GET `/api/subscriptions/my/active` | USER | Only ACTIVE |
| GET `/api/cards/subscriptions/store/{storeId}` | VENDOR | All subscriptions for store |

## 6.6 Card Balances

- GET `/internal/subscriptions/{id}/balance` → `{ subscriptionId, walletBalance }`
- POST `/internal/subscriptions/{id}/deduct` → deducts `amount` from `walletBalance`
- If `walletBalance` reaches 0 after deduction, status is set to `EXHAUSTED`

## 6.7 Card History

Subscription history per user: GET `/api/subscriptions/my?page=&size=`  
Eligible items for a subscription: GET `/api/subscriptions/{subscriptionId}/menu`

---

# 7. REDEMPTION MODULE

## 7.1 Queue

`GET /api/redemptions/store/{storeId}/queue` (VENDOR)

Returns all `PENDING` redemptions for the store in list form (`RedemptionQueueResponse`). Vendor sees: `id`, `subscriptionId`, `userId`, `totalAmount`, `createdAt`.

## 7.2 Pending Redemptions

A redemption enters `PENDING` status via `POST /api/redemptions/request` (USER flow). Balance is **not** deducted. The vendor must manually approve or reject.

## 7.3 Accepted (Completed) Redemptions

`POST /api/redemptions/{id}/approve` (VENDOR)

Validates:
- Redemption exists and is `PENDING`
- Vendor owns the store in the redemption record
- Subscription is still `ACTIVE`

On approval:
1. Sets `status=COMPLETED`, sets `approvedAt`
2. Calls card-service `/internal/subscriptions/{id}/deduct` to deduct wallet balance
3. Calls ledger-service `/internal/transactions` to record `REDEMPTION` transaction
4. Optionally sends redemption notification

## 7.4 Rejected Redemptions

`POST /api/redemptions/{id}/reject?reason=...` (VENDOR)

- Sets `status=REJECTED`, sets `rejectedAt`, stores `failureReason`
- Balance is **not** deducted
- User can retry

## 7.5 Approval Flow (User-Initiated)

```
User → POST /api/redemptions/request
         ↓
  status = PENDING (no balance deducted)
         ↓
  Vendor sees in queue: GET /api/redemptions/store/{id}/queue
         ↓
  Vendor → POST /api/redemptions/{id}/approve
         ↓
  status = COMPLETED
  Balance deducted, REDEMPTION recorded in ledger
```

## 7.6 Rejection Flow

```
Vendor → POST /api/redemptions/{id}/reject?reason=...
         ↓
  status = REJECTED
  Balance NOT deducted
  failureReason stored
```

## 7.7 Direct POS Redemption (Vendor-Initiated)

`POST /api/redemptions` (VENDOR, legacy)

Vendor creates the redemption directly (e.g., scans card at POS). Bypasses approval queue. Status immediately set to `COMPLETED`. Balance deducted immediately.

## 7.8 Status Transitions

```
PENDING ──approve──► COMPLETED
PENDING ──reject───► REJECTED
COMPLETED ──reverse► REVERSED   (not yet exposed via API)
any ──error──────►   FAILED
```

---

# 8. LEDGER MODULE

## 8.1 Transactions

All financial events are recorded as immutable rows in `ledger_transactions`.

### Transaction Types

| Type | Triggered by | Amount |
|---|---|---|
| `CARD_PURCHASE` | card-service on subscription purchase | Card price paid |
| `REDEMPTION` | redemption-service on approval | Redemption total amount |
| `REFUND` | (defined but not yet called) | Refund amount |
| `TOP_UP` | (defined but not yet called) | Top-up amount |

### Recording a Transaction

Internal only: `POST /internal/transactions` with `RecordTransactionRequest`

## 8.2 Revenue Queries

| Scope | Endpoint | Role |
|---|---|---|
| By customer | GET `/api/ledger/user/{userId}` | USER/ADMIN |
| By subscription | GET `/api/ledger/subscription/{subscriptionId}` | USER/ADMIN |
| By store | GET `/api/ledger/store/{storeId}` | VENDOR/ADMIN |
| All (admin) | GET `/api/admin/ledger/all` | ADMIN |
| Stats | GET `/api/admin/ledger/stats` | ADMIN — returns totalTransactions, totalRevenue |

## 8.3 Wallet

The subscription wallet lives in `active_subscriptions.walletBalance`. The ledger records what happened; the card-service tracks current balance.

## 8.4 Reports / Analytics

Admin analytics assembled by admin-service from commission records:
- Monthly revenue breakdown: GET `/api/admin/analytics/revenue`
- Vendor performance ranking: GET `/api/admin/analytics/vendor-performance`
- Commission summary: GET `/api/admin/analytics/commissions`
- Platform stats: GET `/api/admin/platform/stats`

---

# 9. ADMIN MODULE

## 9.1 Admin Account Management

| Action | Endpoint | Role Required |
|---|---|---|
| Initial super admin setup | POST `/api/admin/auth/setup` | None (one-time) |
| Login | POST `/api/admin/auth/login` | None |
| Refresh token | POST `/api/admin/auth/refresh` | None |
| Logout | POST `/api/admin/auth/logout` | None |
| Get own profile | GET `/api/admin/auth/me` | ADMIN |
| Change password | PATCH `/api/admin/auth/change-password` | ADMIN |
| Register new admin | POST `/api/admin/auth/register` | SUPER_ADMIN |
| List all admins | GET `/api/admin/auth/admins` | SUPER_ADMIN |
| Deactivate admin | PATCH `/api/admin/auth/admins/{id}/deactivate` | SUPER_ADMIN |
| Activate admin | PATCH `/api/admin/auth/admins/{id}/activate` | SUPER_ADMIN |

**Note:** An admin cannot deactivate their own account.

## 9.2 Vendor Management

| Action | Endpoint |
|---|---|
| All vendors (paginated) | GET `/api/admin/vendors` |
| Filter by status | GET `/api/admin/vendors/pending|active|suspended|rejected` |
| Vendor detail | GET `/api/admin/vendors/{vendorId}` |
| Approve vendor | PATCH `/api/admin/vendors/{vendorId}/approve` |
| Reject vendor | PATCH `/api/admin/vendors/{vendorId}/reject` + `{ reason }` |
| Suspend vendor | PATCH `/api/admin/vendors/{vendorId}/suspend` + `{ reason }` |
| Reactivate vendor | PATCH `/api/admin/vendors/{vendorId}/reactivate` |
| Set commission rate | PATCH `/api/admin/vendors/{vendorId}/commission-rate` + `{ commissionRate }` |
| Vendor's store | GET `/api/admin/vendors/{vendorId}/store` |
| Vendor's cards | GET `/api/admin/vendors/{vendorId}/cards` |
| Vendor's subscriptions | GET `/api/admin/vendors/{vendorId}/subscriptions` |
| Vendor's redemptions | GET `/api/admin/vendors/{vendorId}/redemptions` |
| Vendor's transactions | GET `/api/admin/vendors/{vendorId}/transactions` |

**Approval side-effects:** On approve/reject/suspend/reactivate, admin-service calls auth-service `/internal/vendors/{id}/approve|reject|suspend|reactivate` to sync status, then sends vendor notification.

## 9.3 User Management

| Action | Endpoint |
|---|---|
| All users | GET `/api/admin/users` |
| User detail | GET `/api/admin/users/{userId}` (auth + profile merged) |
| User subscriptions | GET `/api/admin/users/{userId}/subscriptions` |
| Active subscriptions | GET `/api/admin/users/{userId}/subscriptions/active` |
| User redemptions | GET `/api/admin/users/{userId}/redemptions` |
| User transactions | GET `/api/admin/users/{userId}/transactions` |
| Ban user | PATCH `/api/admin/users/{userId}/ban` |
| Unban user | PATCH `/api/admin/users/{userId}/unban` |

## 9.4 Commission Management

| Action | Endpoint |
|---|---|
| All commission records | GET `/api/admin/commissions` |
| Pending commissions | GET `/api/admin/commissions/pending` |
| By vendor | GET `/api/admin/commissions/vendor/{vendorId}` |
| Commission stats | GET `/api/admin/commissions/stats` |
| Settle one record | PATCH `/api/admin/commissions/{id}/settle` |
| Settle all for vendor | PATCH `/api/admin/commissions/vendor/{vendorId}/settle-all` |

**Default commission rate:** 10.00% per vendor, configurable per vendor.

## 9.5 Dashboard

`GET /api/admin/dashboard`

Returns in a single call:
- Admin identity (email, role)
- Total users count
- Vendor breakdown (total, pending, active, suspended, rejected)
- Total subscription revenue
- Total commission earned
- Pending commission
- Total subscriptions count
- Total redemptions count
- Up to 10 pending vendor approvals
- Last 5 vendor registrations

## 9.6 Analytics

| Endpoint | Returns |
|---|---|
| GET `/api/admin/analytics/overview` | Users, vendors by status, subscriptions, revenue, commission |
| GET `/api/admin/analytics/revenue` | Monthly subscription revenue + commission (from commission_records) |
| GET `/api/admin/analytics/vendor-performance` | Per-vendor: revenue, commission, subscription count (sorted desc by commission) |
| GET `/api/admin/analytics/commissions` | Pending/settled totals + per-vendor commission breakdown |
| GET `/api/admin/analytics/orders` | Total redemption count and amount |

## 9.7 Cards / Subscriptions (Admin View)

| Endpoint |
|---|
| GET `/api/admin/cards/{id}` |
| GET `/api/admin/cards/vendor/{vendorId}` |
| GET `/api/admin/cards/subscriptions/store/{storeId}` |
| GET `/api/admin/cards/subscriptions/{id}` |

## 9.8 Redemptions (Admin View)

| Endpoint |
|---|
| GET `/api/admin/redemptions/all` |
| GET `/api/admin/redemptions/{id}` |
| GET `/api/admin/redemptions/store/{storeId}` |
| GET `/api/admin/redemptions/user/{userId}` |
| GET `/api/admin/redemptions/subscription/{subscriptionId}` |

## 9.9 Ledger (Admin View)

| Endpoint |
|---|
| GET `/api/admin/ledger/all` |
| GET `/api/admin/ledger/subscription/{subscriptionId}` |
| GET `/api/admin/ledger/store/{storeId}` |
| GET `/api/admin/ledger/user/{userId}` |

---

# 10. NOTIFICATION MODULE

All endpoints are internal (`/internal/notify/**`) — not exposed through gateway.

## 10.1 OTP Notifications

`POST /internal/notify/otp`

```json
{ "mobile": "9876543210", "otp": "123456", "recipientType": "USER" }
```

Delivers OTP via SMS (Twilio, if `notification.sms.enabled=true`) or email. Logs delivery in `notification_logs`.

## 10.2 Vendor Status Notifications

`POST /internal/notify/vendor-status`

```json
{
  "vendorId": 1,
  "mobile": "9876543210",
  "email": "vendor@example.com",
  "hotelName": "My Hotel",
  "status": "APPROVED",
  "reason": null
}
```

Status values: `APPROVED`, `REJECTED`, `SUSPENDED`, `REACTIVATED`

## 10.3 Subscription Notifications

`POST /internal/notify/subscription`

```json
{
  "userId": 10,
  "subscriptionId": 55,
  "cardName": "Monthly Lunch Card",
  "amount": 500.00,
  "expiresAt": "2026-07-20T00:00:00"
}
```

## 10.4 Redemption Notifications

`POST /internal/notify/redemption`

```json
{
  "userId": 10,
  "redemptionId": 88,
  "status": "COMPLETED",
  "amount": 120.00
}
```

Status values: `COMPLETED`, `REJECTED`, `PENDING`

## 10.5 SMS Notifications

Implemented via Twilio. Enabled by setting environment variables:
- `NOTIFICATION_SMS_ENABLED=true`
- `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`

## 10.6 Email Notifications

Implemented via Spring Mail (SMTP). Enabled by `NOTIFICATION_EMAIL_ENABLED=true`.  
Default from address: `noreply@strikeapp.com`

## 10.7 Notification Logs

All notifications (sent or failed) are logged to `notification_logs` table.

Query logs:
- GET `/internal/notify/logs` — all logs
- GET `/internal/notify/logs/recipient/{recipientId}?type=USER|VENDOR` — logs by recipient

---

# 11. DTO REFERENCE

## auth-service DTOs

### SendOtpRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| mobileNumber | String | Yes | `@NotBlank`, `@Pattern(^[6-9]\\d{9}$)` |

### VerifyOtpRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| mobileNumber | String | Yes | `@NotBlank` |
| otp | String | Yes | `@NotBlank` |

### AuthResponse
| Field | Type | Notes |
|---|---|---|
| userId | Long | |
| mobileNumber | String | |
| token | String | Access JWT |
| refreshToken | String | |
| expiresIn | long | Seconds |
| newUser | Boolean | `true` on first-ever login |
| message | String | |

### UserAuthResponse
| Field | Type |
|---|---|
| id | Long |
| mobileNumber | String |
| verified | Boolean |
| banned | Boolean |
| createdAt | LocalDateTime |
| updatedAt | LocalDateTime |

### RegisterVendorRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| hotelName | String | Yes | `@NotBlank` |
| address | String | Yes | `@NotBlank` |
| mobileNumber | String | Yes | `@NotBlank`, `@Pattern(^[6-9]\\d{9}$)` |
| email | String | No | `@Email` |
| latitude | Double | Yes | `@NotNull` |
| longitude | Double | Yes | `@NotNull` |

### VendorLoginRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| mobileNumber | String | Yes | `@NotBlank`, `@Pattern(^[6-9]\\d{9}$)` |

### VendorAuthResponse
| Field | Type | Notes |
|---|---|---|
| token | String | Access JWT (null if not ACTIVE) |
| refreshToken | String | |
| expiresIn | long | Seconds |
| vendorId | Long | |
| hotelName | String | |
| mobileNumber | String | |
| email | String | |
| address | String | |
| status | String | PENDING / VERIFIED / ACTIVE / REJECTED / SUSPENDED |
| message | String | |

### RefreshRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| refreshToken | String | Yes | `@NotBlank` |

---

## user-service DTOs

### UserProfileRequest
| Field | Type | Required |
|---|---|---|
| name | String | No |
| phone | String | No |
| email | String | No |
| latitude | Double | No |
| longitude | Double | No |

### UserProfileResponse
| Field | Type |
|---|---|
| id | Long |
| userId | Long |
| name | String |
| phone | String |
| email | String |
| latitude | Double |
| longitude | Double |
| createdAt | LocalDateTime |
| updatedAt | LocalDateTime |

### UpdateLocationRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| latitude | Double | Yes | `@NotNull` |
| longitude | Double | Yes | `@NotNull` |

---

## vendor-service DTOs

### InitVendorProfileRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| vendorId | Long | Yes | `@NotNull` |
| shopName | String | Yes | `@NotBlank` |
| mobile | String | Yes | `@NotBlank` |
| address | String | Yes | `@NotBlank` |
| email | String | No | `@Email` |
| latitude | Double | Yes | `@DecimalMin("-90") @DecimalMax("90")` |
| longitude | Double | Yes | `@DecimalMin("-180") @DecimalMax("180")` |

### StoreDetailsRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank`, `@Size(max=100)` |
| address | String | Yes | `@NotBlank`, `@Size(max=300)` |
| phone | String | No | `@Pattern` |
| email | String | No | `@Email` |
| category | String | No | `@Size(max=100)` |
| description | String | No | `@Size(max=1000)` |
| logoUrl | String | No | `@Size(max=500)` |
| latitude | Double | No | |
| longitude | Double | No | |

### UpdateStoreLocationRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| latitude | Double | Yes | `@NotNull`, `@DecimalMin("-90") @DecimalMax("90")` |
| longitude | Double | Yes | `@NotNull`, `@DecimalMin("-180") @DecimalMax("180")` |

### StoreResponse
| Field | Type |
|---|---|
| id | Long |
| vendorId | Long |
| name | String |
| address | String |
| phone | String |
| email | String |
| category | String |
| description | String |
| logoUrl | String |
| latitude | Double |
| longitude | Double |
| distanceKm | Double |
| status | StoreStatus |
| timings | List\<StoreTimingResponse\> |
| holidays | List\<StoreHolidayResponse\> |
| createdAt | LocalDateTime |
| updatedAt | LocalDateTime |

### UpdateVendorProfileRequest
| Field | Type | Validation |
|---|---|---|
| shopName | String | `@Size(min=2, max=100)` |
| ownerName | String | `@Size(max=100)` |
| mobile | String | `@Size(max=15)` |
| email | String | `@Email` |
| address | String | `@Size(max=300)` |
| category | String | `@Size(max=100)` |
| description | String | `@Size(max=1000)` |
| logoUrl | String | |

### VendorProfileResponse
| Field | Type |
|---|---|
| id | Long |
| vendorId | Long |
| shopName | String |
| ownerName | String |
| mobile | String |
| email | String |
| address | String |
| category | String |
| description | String |
| logoUrl | String |
| createdAt | LocalDateTime |
| updatedAt | LocalDateTime |

### StoreTimingRequest
| Field | Type | Required | Notes |
|---|---|---|---|
| dayOfWeek | DayOfWeek | Yes | `@NotNull` |
| openTime | LocalTime | No | Null if `isClosed=true` |
| closeTime | LocalTime | No | Null if `isClosed=true` |
| isClosed | Boolean | No | Default `false` |

### StoreTimingResponse
| Field | Type |
|---|---|
| id | Long |
| storeId | Long |
| dayOfWeek | DayOfWeek |
| openTime | LocalTime |
| closeTime | LocalTime |
| isClosed | Boolean |

### StoreHolidayRequest
| Field | Type | Required |
|---|---|---|
| date | LocalDate | Yes `@NotNull` |
| reason | String | No |

### StoreHolidayResponse
| Field | Type |
|---|---|
| id | Long |
| storeId | Long |
| date | LocalDate |
| reason | String |
| createdAt | LocalDateTime |

### CreateCategoryRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank` |
| description | String | No | |
| imageUrl | String | No | |
| displayOrder | Integer | No | |

### UpdateCategoryRequest
| Field | Type | Validation |
|---|---|---|
| name | String | `@Size(min=2, max=100)` |
| description | String | `@Size(max=500)` |
| imageUrl | String | `@Size(max=500)` |
| displayOrder | Integer | |

### CategoryResponse
| Field | Type |
|---|---|
| id | Long |
| name | String |
| description | String |
| imageUrl | String |
| displayOrder | Integer |
| status | CategoryStatus |
| storeId | Long |

### CategoryWithItemsResponse
| Field | Type |
|---|---|
| id | Long |
| name | String |
| description | String |
| imageUrl | String |
| displayOrder | Integer |
| items | List\<MenuItemResponse\> |

### CreateMenuItemRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank` |
| description | String | No | |
| price | BigDecimal | Yes | `@NotNull` |
| imageUrl | String | No | |
| itemType | ItemType | No | VEG / NON_VEG |
| availabilityStatus | ItemAvailabilityStatus | No | AVAILABLE / OUT_OF_STOCK |
| categoryId | Long | Yes | `@NotNull` |

### UpdateMenuItemRequest
| Field | Type | Validation |
|---|---|---|
| name | String | `@Size(max=100)` |
| description | String | `@Size(max=500)` |
| price | BigDecimal | `@DecimalMin("0.01")` |
| imageUrl | String | `@Size(max=500)` |
| itemType | ItemType | |
| availabilityStatus | ItemAvailabilityStatus | |
| categoryId | Long | |

### MenuItemResponse
| Field | Type |
|---|---|
| id | Long |
| name | String |
| description | String |
| price | BigDecimal |
| imageUrl | String |
| itemType | ItemType |
| availabilityStatus | ItemAvailabilityStatus |
| categoryId | Long |
| storeId | Long |
| createdAt | LocalDateTime |
| updatedAt | LocalDateTime |

### DashboardSummaryResponse
| Field | Type |
|---|---|
| storeId | Long |
| totalRevenue | BigDecimal |
| totalRedemptionAmount | BigDecimal |
| totalCardsSold | Long |
| totalActiveSubscriptions | Long |
| totalSubscriptions | Long |
| totalRedemptions | Long |
| recentTransactions | List\<Object\> |
| recentRedemptions | List\<Object\> |
| activeCards | List\<Object\> |

---

## card-service DTOs

### CreateCardRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| storeId | Long | Yes | `@NotNull` |
| name | String | Yes | `@NotBlank` |
| description | String | No | |
| cardPrice | BigDecimal | Yes | `@NotNull`, `@DecimalMin("0.01")` |
| walletAmount | BigDecimal | Yes | `@NotNull`, `@DecimalMin("0.01")` |
| validityInDays | Integer | Yes | `@NotNull`, `@Min(1)` |
| imageUrl | String | No | |
| categoryIds | List\<Long\> | Yes | `@NotNull`, `@Size(min=1)` |
| eligibleMenuItemIds | List\<Long\> | No | `@Size(min=1)` if provided |

### UpdateCardRequest
| Field | Type | Validation |
|---|---|---|
| name | String | `@Size(min=2, max=100)` |
| description | String | `@Size(max=500)` |
| cardPrice | BigDecimal | `@DecimalMin("0.01")` |
| walletAmount | BigDecimal | `@DecimalMin("0.01")` |
| validityInDays | Integer | `@Min(1)` |
| imageUrl | String | `@Size(max=500)` |
| isActive | Boolean | |
| categoryIds | List\<Long\> | `@Size(min=1)` |
| eligibleMenuItemIds | List\<Long\> | `@Size(min=1)` |

### CardDefinitionResponse
| Field | Type | Notes |
|---|---|---|
| id | Long | |
| vendorId | Long | |
| storeId | Long | |
| name | String | |
| description | String | |
| cardPrice | BigDecimal | |
| walletAmount | BigDecimal | |
| bonusAmount | BigDecimal | `walletAmount - cardPrice` |
| validityInDays | Integer | |
| imageUrl | String | |
| isActive | Boolean | |
| categoryIds | List\<Long\> | |
| eligibleMenuItemIds | List\<Long\> | ID list only |
| eligibleItems | List\<EligibleItemInfo\> | Populated in create/update/single-get |
| createdAt | LocalDateTime | |

### PurchaseSubscriptionRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| cardDefinitionId | Long | Yes | `@NotNull` |
| storeId | Long | Yes | `@NotNull` |

### SubscriptionResponse
| Field | Type |
|---|---|
| id | Long |
| userId | Long |
| cardDefinitionId | Long |
| cardName | String |
| storeId | Long |
| walletBalance | BigDecimal |
| status | SubscriptionStatus |
| purchasedAt | LocalDateTime |
| expiresAt | LocalDateTime |
| createdAt | LocalDateTime |

### BalanceResponse
| Field | Type |
|---|---|
| subscriptionId | Long |
| walletBalance | BigDecimal |

### DeductBalanceRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| amount | BigDecimal | Yes | `@NotNull` |

### CardPreviewResponse
| Field | Type |
|---|---|
| cardId | Long |
| cardName | String |
| description | String |
| cardPrice | BigDecimal |
| walletAmount | BigDecimal |
| validityInDays | Integer |
| eligibleMenus | List\<MenuCategoryPreview\> |

### EligibleMenuResponse
| Field | Type |
|---|---|
| subscriptionId | Long |
| cardName | String |
| categories | List\<EligibleCategory\> |

Each `EligibleCategory`: `{ categoryId, categoryName, items: [{ itemId, name, price, itemType, availabilityStatus }] }`  
Only `AVAILABLE` items are returned.

### SubscriptionRedemptionContext
| Field | Type |
|---|---|
| userId | Long |
| storeId | Long |
| cardDefinitionId | Long |
| status | String |
| eligibleCategoryIds | List\<Long\> |
| eligibleMenuItemIds | List\<Long\> |

---

## redemption-service DTOs

### RedemptionRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| subscriptionId | Long | Yes | `@NotNull` |
| storeId | Long | Yes | `@NotNull` |
| items | List\<RedemptionItemRequest\> | Yes | `@Valid`, `@NotEmpty` |

### RedemptionItemRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| menuItemId | Long | Yes | `@NotNull` |
| quantity | Integer | Yes | `@NotNull` |

### RedemptionResponse
| Field | Type |
|---|---|
| id | Long |
| subscriptionId | Long |
| userId | Long |
| storeId | Long |
| totalAmount | BigDecimal |
| remainingBalance | BigDecimal |
| status | RedemptionStatus |
| initiatedBy | String |
| items | List\<RedemptionItemResponse\> |
| createdAt | LocalDateTime |
| approvedAt | LocalDateTime |
| rejectedAt | LocalDateTime |
| failureReason | String |

### RedemptionItemResponse
| Field | Type |
|---|---|
| id | Long |
| redemptionId | Long |
| menuItemId | Long |
| itemName | String |
| quantity | Integer |
| unitPrice | BigDecimal |
| totalPrice | BigDecimal |

### RedemptionQueueResponse
| Field | Type |
|---|---|
| id | Long |
| subscriptionId | Long |
| userId | Long |
| totalAmount | BigDecimal |
| createdAt | LocalDateTime |

---

## ledger-service DTOs

### RecordTransactionRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| storeId | Long | Yes | `@NotNull` |
| customerId | Long | Yes | `@NotNull` |
| subscriptionId | Long | Yes | `@NotNull` |
| transactionType | TransactionType | Yes | `@NotNull` |
| amount | BigDecimal | Yes | `@NotNull` |
| remarks | String | No | |

### TransactionResponse
| Field | Type |
|---|---|
| id | Long |
| storeId | Long |
| customerId | Long |
| subscriptionId | Long |
| transactionType | TransactionType |
| amount | BigDecimal |
| remarks | String |
| createdAt | LocalDateTime |

---

## admin-service DTOs

### AdminLoginRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| email | String | Yes | `@Email` |
| password | String | Yes | `@NotBlank` |

### AdminLoginResponse
| Field | Type |
|---|---|
| id | Long |
| email | String |
| name | String |
| role | String |
| token | String |
| refreshToken | String |
| expiresIn | long |
| message | String |

### AdminRegisterRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| email | String | Yes | `@Email` |
| name | String | Yes | `@NotBlank` |
| password | String | Yes | `@NotBlank`, `@Size(min=8)` |

### AdminRefreshRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| refreshToken | String | Yes | `@NotBlank` |

### ChangePasswordRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| currentPassword | String | Yes | `@NotBlank` |
| newPassword | String | Yes | `@NotBlank`, `@Size(min=8)` |

### CommissionRecordResponse
| Field | Type |
|---|---|
| id | Long |
| vendorId | Long |
| storeId | Long |
| subscriptionId | Long |
| userId | Long |
| subscriptionAmount | BigDecimal |
| commissionRate | BigDecimal |
| commissionAmount | BigDecimal |
| status | String |
| settledAt | LocalDateTime |
| createdAt | LocalDateTime |

### RecordCommissionRequest
| Field | Type | Required |
|---|---|---|
| vendorId | Long | Yes |
| storeId | Long | Yes |
| subscriptionId | Long | Yes |
| userId | Long | Yes |
| subscriptionAmount | BigDecimal | Yes |
| commissionRate | BigDecimal | Yes |

---

## notification-service DTOs

### OtpNotificationRequest
| Field | Type | Required |
|---|---|---|
| mobile | String | Yes |
| otp | String | Yes |
| recipientType | String | Yes (`USER` / `VENDOR`) |

### VendorStatusNotificationRequest
| Field | Type |
|---|---|
| vendorId | Long |
| mobile | String |
| email | String |
| hotelName | String |
| status | String (`APPROVED` / `REJECTED` / `SUSPENDED` / `REACTIVATED`) |
| reason | String |

### SubscriptionNotificationRequest
| Field | Type |
|---|---|
| userId | Long |
| subscriptionId | Long |
| cardName | String |
| amount | BigDecimal |
| expiresAt | LocalDateTime |

### RedemptionNotificationRequest
| Field | Type |
|---|---|
| userId | Long |
| redemptionId | Long |
| status | String (`COMPLETED` / `REJECTED` / `PENDING`) |
| amount | BigDecimal |

---

# 12. ENTITY REFERENCE

## auth-service Entities

### UserAuth (`user_auth`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| mobileNumber | String(10) | NOT NULL |
| verified | Boolean | NOT NULL |
| banned | Boolean | NOT NULL, default `false` |
| createdAt | LocalDateTime | set on `@PrePersist` |
| updatedAt | LocalDateTime | set on `@PrePersist` / `@PreUpdate` |

### Vendor (`vendors`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| hotelName | String | — |
| address | String | — |
| mobileNumber | String | — |
| email | String | — |
| latitude | Double | — |
| longitude | Double | — |
| status | String | PENDING / VERIFIED / ACTIVE / REJECTED / SUSPENDED |
| createdAt | LocalDateTime | — |

### OtpCode (`otp_codes`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| mobile | String | — |
| otpCode | String | — |
| expiresAt | LocalDateTime | — |
| verified | Boolean | — |
| createdAt | LocalDateTime | — |

### RefreshToken (`refresh_tokens`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| token | String | NOT NULL, UNIQUE, index: `idx_refresh_token` |
| userId | Long | null for VENDOR tokens |
| vendorId | Long | null for USER tokens |
| role | String(10) | NOT NULL (USER / VENDOR) |
| expiresAt | LocalDateTime | NOT NULL |
| createdAt | LocalDateTime | — |

---

## vendor-service Entities

### VendorProfile (`vendor_profiles`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| vendorId | Long | NOT NULL, UNIQUE |
| shopName | String | NOT NULL |
| ownerName | String | — |
| mobile | String | — |
| email | String | — |
| address | String | — |
| category | String | — |
| description | String(1000) | — |
| logoUrl | String | — |
| createdAt | LocalDateTime | `@CreationTimestamp` |
| updatedAt | LocalDateTime | `@UpdateTimestamp` |

### Store (`stores`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| vendorId | Long | NOT NULL |
| name | String | NOT NULL |
| address | String | NOT NULL |
| phone | String | — |
| email | String | — |
| category | String | — |
| description | String(1000) | — |
| logoUrl | String | — |
| latitude | Double | — |
| longitude | Double | — |
| status | StoreStatus | NOT NULL, default ACTIVE |
| createdAt | LocalDateTime | `@CreationTimestamp` |
| updatedAt | LocalDateTime | `@UpdateTimestamp` |

### StoreTiming (`store_timings`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| storeId | Long | NOT NULL |
| dayOfWeek | DayOfWeek | NOT NULL |
| openTime | LocalTime | — |
| closeTime | LocalTime | — |
| isClosed | Boolean | NOT NULL, default `false` |

### StoreHoliday (`store_holidays`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| storeId | Long | NOT NULL |
| date | LocalDate | NOT NULL |
| reason | String | — |
| createdAt | LocalDateTime | `@CreationTimestamp` |

### Category (`categories`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| name | String(100) | NOT NULL |
| description | String(500) | — |
| imageUrl | String | — |
| displayOrder | Integer | — |
| status | CategoryStatus | ACTIVE / INACTIVE |
| storeId | Long | NOT NULL |
| createdAt | LocalDateTime | `@CreationTimestamp` |
| updatedAt | LocalDateTime | `@UpdateTimestamp` |

### MenuItem (`menu_items`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| name | String | NOT NULL |
| description | String(1000) | — |
| price | BigDecimal | NOT NULL |
| imageUrl | String | — |
| itemType | ItemType | VEG / NON_VEG |
| availabilityStatus | ItemAvailabilityStatus | AVAILABLE / OUT_OF_STOCK |
| categoryId | Long | NOT NULL |
| storeId | Long | NOT NULL |
| createdAt | LocalDateTime | `@CreationTimestamp` |
| updatedAt | LocalDateTime | `@UpdateTimestamp` |

---

## card-service Entities

### CardDefinition (`card_definitions`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| vendorId | Long | NOT NULL |
| storeId | Long | NOT NULL |
| name | String | NOT NULL |
| description | String(1000) | — |
| cardPrice | BigDecimal | NOT NULL |
| walletAmount | BigDecimal | NOT NULL |
| validityInDays | Integer | NOT NULL |
| imageUrl | String | — |
| isActive | Boolean | NOT NULL |
| createdAt | LocalDateTime | `@CreationTimestamp` |
| updatedAt | LocalDateTime | `@UpdateTimestamp` |

### ActiveSubscription (`active_subscriptions`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| userId | Long | NOT NULL |
| cardDefinitionId | Long | NOT NULL |
| storeId | Long | NOT NULL |
| walletBalance | BigDecimal(10,2) | NOT NULL |
| status | SubscriptionStatus | NOT NULL |
| purchasedAt | LocalDateTime | NOT NULL |
| expiresAt | LocalDateTime | NOT NULL |
| createdAt | LocalDateTime | `@CreationTimestamp` |
| updatedAt | LocalDateTime | `@UpdateTimestamp` |

### CardCategoryMapping (`card_category_mappings`)
| Column | Type | Notes |
|---|---|---|
| id | Long | PK |
| cardDefinitionId | Long | FK to card_definitions |
| categoryId | Long | FK to categories (vendor-service) |

### CardMenuItemMapping (`card_menu_item_mappings`)
| Column | Type | Notes |
|---|---|---|
| id | Long | PK |
| cardDefinitionId | Long | FK to card_definitions |
| menuItemId | Long | FK to menu_items (vendor-service) |

### IdempotencyRecord (`idempotency_records`)
| Column | Type | Notes |
|---|---|---|
| id | String (UUID) | PK (the idempotency key) |
| status | String | PROCESSING / COMPLETED |
| responseBody | String (JSON) | Cached response |
| httpStatus | Integer | Cached HTTP status code |
| createdAt | LocalDateTime | |

---

## redemption-service Entities

### RedemptionRecord (`redemption_records`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| version | Long | `@Version` — optimistic locking, default 0 |
| subscriptionId | Long | NOT NULL |
| userId | Long | NOT NULL |
| storeId | Long | NOT NULL |
| totalAmount | BigDecimal(10,2) | NOT NULL |
| status | RedemptionStatus | NOT NULL |
| initiatedBy | String | USER / VENDOR |
| failureReason | String | — |
| approvedAt | LocalDateTime | — |
| rejectedAt | LocalDateTime | — |
| items | List\<RedemptionItem\> | `@OneToMany`, cascaded |
| createdAt | LocalDateTime | `@CreationTimestamp` |

### RedemptionItem (`redemption_items`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| redemptionId | Long | NOT NULL |
| menuItemId | Long | NOT NULL |
| itemName | String | — |
| quantity | Integer | NOT NULL |
| unitPrice | BigDecimal(10,2) | NOT NULL |
| totalPrice | BigDecimal(10,2) | NOT NULL |
| redemptionRecord | RedemptionRecord | `@ManyToOne` |

---

## ledger-service Entities

### Transaction (`ledger_transactions`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| storeId | Long | NOT NULL |
| customerId | Long | NOT NULL |
| subscriptionId | Long | NOT NULL |
| transactionType | TransactionType | NOT NULL |
| amount | BigDecimal(10,2) | NOT NULL |
| remarks | String | — |
| createdAt | LocalDateTime | `@CreationTimestamp` |

---

## user-service Entities

### UserProfile (`user_profiles`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| userId | Long | NOT NULL, UNIQUE |
| name | String | — |
| phone | String | — |
| email | String | — |
| latitude | Double | — |
| longitude | Double | — |
| createdAt | LocalDateTime | `@CreationTimestamp` |
| updatedAt | LocalDateTime | `@UpdateTimestamp` |

---

## admin-service Entities

### Admin (`admins`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| email | String | NOT NULL, UNIQUE |
| name | String | NOT NULL |
| password | String | NOT NULL (BCrypt hashed) |
| role | String | ADMIN / SUPER_ADMIN |
| active | Boolean | NOT NULL, default `true` |
| lastLoginAt | LocalDateTime | — |
| createdAt | LocalDateTime | — |

### AdminRefreshToken (`admin_refresh_tokens`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| token | String | NOT NULL, UNIQUE |
| adminId | Long | NOT NULL |
| expiresAt | LocalDateTime | NOT NULL |
| createdAt | LocalDateTime | — |

### VendorRecord (`admin_vendor_records`)
| Column | Type | Constraints |
|---|---|---|
| vendorId | Long | PK (synced from auth-service) |
| hotelName | String | NOT NULL |
| mobileNumber | String | — |
| email | String | — |
| status | String | NOT NULL (PENDING / ACTIVE / REJECTED / SUSPENDED) |
| rejectionReason | String | — |
| commissionRate | BigDecimal(5,2) | Default 10.00 |
| createdAt | LocalDateTime | `@CreationTimestamp` |
| updatedAt | LocalDateTime | `@UpdateTimestamp` |

### CommissionRecord (`commission_records`)
| Column | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| vendorId | Long | NOT NULL |
| storeId | Long | NOT NULL |
| subscriptionId | Long | NOT NULL |
| userId | Long | NOT NULL |
| subscriptionAmount | BigDecimal(12,2) | NOT NULL |
| commissionRate | BigDecimal(5,2) | NOT NULL |
| commissionAmount | BigDecimal(12,2) | NOT NULL |
| status | String | NOT NULL, default PENDING |
| settledAt | LocalDateTime | — |
| createdAt | LocalDateTime | `@CreationTimestamp` |

---

## notification-service Entities

### NotificationLog (`notification_logs`)
| Column | Type | Notes |
|---|---|---|
| id | Long | PK |
| recipientId | Long | userId or vendorId |
| recipientType | String | USER / VENDOR |
| channel | String | SMS / EMAIL |
| type | String | OTP / VENDOR_STATUS / SUBSCRIPTION / REDEMPTION |
| status | String | SENT / FAILED |
| createdAt | LocalDateTime | |

---

# 13. ENUM REFERENCE

## auth-service

### Vendor Status (String column — not Java enum)
| Value | Meaning |
|---|---|
| `PENDING` | Just registered; awaiting OTP verification |
| `VERIFIED` | OTP verified; awaiting admin approval |
| `ACTIVE` | Admin approved; can login and operate |
| `REJECTED` | Admin rejected application |
| `SUSPENDED` | Admin suspended active vendor |

---

## vendor-service

### StoreStatus
| Value | Meaning | Usage |
|---|---|---|
| `ACTIVE` | Store open for business | Public store listing shows this store |
| `INACTIVE` | Vendor manually set offline | Hidden from public listing |
| `TEMPORARILY_CLOSED` | Short-term closure | Hidden from public listing |

### CategoryStatus
| Value | Meaning |
|---|---|
| `ACTIVE` | Category visible and eligible for card mappings |
| `INACTIVE` | Category hidden; not selectable for new cards |

### ItemType
| Value | Meaning |
|---|---|
| `VEG` | Vegetarian item |
| `NON_VEG` | Non-vegetarian item |

### ItemAvailabilityStatus
| Value | Meaning | Impact |
|---|---|---|
| `AVAILABLE` | Item is in stock | Returned in eligible-menu API |
| `OUT_OF_STOCK` | Item unavailable | Filtered out from eligible-menu API |

---

## card-service

### SubscriptionStatus
| Value | Meaning | Transitions |
|---|---|---|
| `ACTIVE` | Subscription is valid, balance available | → EXPIRED, EXHAUSTED, CANCELLED |
| `EXPIRED` | Past `expiresAt` date (set by nightly job) | Terminal |
| `EXHAUSTED` | `walletBalance <= 0` after deduction | Terminal |
| `CANCELLED` | User cancelled | Terminal |

---

## redemption-service

### RedemptionStatus
| Value | Meaning | Balance Impact |
|---|---|---|
| `PENDING` | Submitted by user, awaiting vendor approval | None |
| `COMPLETED` | Vendor approved; items fulfilled | Balance deducted |
| `REJECTED` | Vendor rejected | None |
| `FAILED` | Technical error during approval | None |
| `REVERSED` | Completed then reversed | Balance restored (rare) |

---

## ledger-service

### TransactionType
| Value | Meaning | Triggered By |
|---|---|---|
| `CARD_PURCHASE` | User purchased subscription | card-service |
| `REDEMPTION` | Wallet balance spent at store | redemption-service |
| `REFUND` | Amount refunded to wallet | (defined, not yet triggered) |
| `TOP_UP` | Wallet topped up | (defined, not yet triggered) |

---

## admin-service

### Admin Role (String column)
| Value | Meaning |
|---|---|
| `ADMIN` | Standard admin, all vendor/user management |
| `SUPER_ADMIN` | All ADMIN permissions + admin account management |

### CommissionRecord Status (String column)
| Value | Meaning |
|---|---|
| `PENDING` | Commission earned but not yet paid to platform |
| `SETTLED` | Commission has been settled/paid |

---

# 14. BUSINESS RULES

## OTP Rules
1. Mobile number must match pattern `^[6-9]\\d{9}$` (Indian mobile format)
2. OTP is 6 digits, randomly generated
3. OTP expires in 5 minutes
4. Minimum 60-second cooldown between OTP requests for the same mobile
5. Maximum 6 OTP requests per mobile per hour
6. Banned users cannot request OTPs

## Vendor Rules
1. Vendor can only login (OTP sent) if `status=ACTIVE`
2. Vendor registration always starts with `status=PENDING`
3. Admin must approve → sets `status=ACTIVE`; only then can vendor login
4. Vendor can be rejected → status=REJECTED (cannot login)
5. Active vendor can be suspended → status=SUSPENDED (cannot login)
6. Suspended vendor can be reactivated → status=ACTIVE (can login again)
7. Commission rate default is 10.00%; can be changed per-vendor by admin

## Store Rules
1. One vendor has one store (1:1 relationship via `vendorId`)
2. Store status defaults to `ACTIVE` on creation
3. Store must be `ACTIVE` for cards to be purchasable (implied by card listing)
4. Categories and menu items belong to a specific store via `storeId`

## Card Rules
1. Card must have at least 1 eligible category (`categoryIds` cannot be empty)
2. All `categoryIds` must be ACTIVE categories belonging to the vendor's store
3. If `eligibleMenuItemIds` provided, all items must belong to valid categories in the store
4. `walletAmount` must be > 0; `cardPrice` must be > 0; `validityInDays` must be >= 1
5. Deactivating a card (`isActive=false`) does not affect existing subscriptions
6. A category cannot be deleted if it is mapped to an active card (checked via `/internal/cards/category-mappings/active/{categoryId}`)
7. Public GET `/api/cards/store/{storeId}` only returns cards with `isActive=true`

## Subscription Rules
1. Subscription purchase is idempotent: same `Idempotency-Key` returns the same response
2. `walletBalance` is set to `card.walletAmount` at purchase time
3. `expiresAt = purchasedAt + card.validityInDays`
4. A subscription can only be used at the store it was purchased for (storeId is fixed)
5. Only `ACTIVE` subscriptions can be used for redemptions
6. Nightly job marks all subscriptions where `expiresAt < now` as `EXPIRED`
7. After a balance deduction, if `walletBalance <= 0`, status is set to `EXHAUSTED`
8. User can cancel an `ACTIVE` subscription → status becomes `CANCELLED`

## Redemption Rules
1. Redemption request must reference an `ACTIVE` subscription
2. Subscription's `storeId` must match the redemption's `storeId`
3. All redeemed items must be in the card's eligible categories (verified via subscription context)
4. Only `AVAILABLE` menu items can be redeemed (filtered at eligibility check)
5. `totalAmount` = sum of `(quantity × item.price)` for all items
6. Balance is NOT deducted when status is `PENDING`; deducted only on approval
7. Optimistic locking (`@Version`) on `RedemptionRecord` prevents concurrent double-approvals
8. Redemption is idempotent at request creation via `Idempotency-Key` header (vendor POS flow)
9. Vendor must own the store (`/internal/vendors/{id}/owns-store/{storeId}`) to approve/reject

## Commission Rules
1. Commission is calculated as `subscriptionAmount × commissionRate / 100`
2. Default commission rate: 10.00% per vendor
3. Commission is recorded at subscription purchase time (`CARD_PURCHASE`)
4. Commission starts with `status=PENDING`
5. Admin can settle individual records or all records for a vendor
6. Analytics revenue = sum of all `subscriptionAmount` in commission_records (total subscription revenue)

## Admin Rules
1. Only one super admin can be created via `/setup` endpoint; fails if any admin exists
2. Super admins can create regular admins; regular admins cannot create other admins
3. An admin cannot deactivate their own account
4. Deactivated admins cannot refresh tokens (existing refresh tokens are revoked on next refresh attempt)
5. Admin JWT uses separate `AdminJwtUtil` with same secret as other services

---

# 15. FRONTEND SCREEN MAPPING

## User App

| Screen | API Calls |
|---|---|
| **Splash / Auth** | POST `/api/auth/user/send-otp` |
| **OTP Verification** | POST `/api/auth/user/verify-otp` |
| **Home / Store Discovery** | GET `/api/stores` (public list) |
| **Nearby Stores** | GET `/api/stores/nearby?lat=&lng=&radiusKm=` |
| **Store Search** | GET `/api/stores/search?q=&category=` |
| **Store Detail** | GET `/api/stores/{storeId}`, GET `/api/stores/{storeId}/menu` |
| **Store Cards** | GET `/api/user/stores/{storeId}/cards` |
| **Card Detail / Preview** | GET `/api/cards/{id}` |
| **Purchase Card** | POST `/api/user/me/subscriptions/purchase` (with `Idempotency-Key`) |
| **My Subscriptions** | GET `/api/user/me/subscriptions` |
| **Active Subscriptions** | GET `/api/user/me/subscriptions/active` |
| **Subscription Detail** | GET `/api/user/me/subscriptions/{id}` |
| **Eligible Menu (Redeem)** | GET `/api/subscriptions/{subscriptionId}/menu` |
| **Request Redemption** | POST `/api/redemptions/request` |
| **My Redemptions** | GET `/api/user/me/redemptions` |
| **Redemption Detail** | GET `/api/user/me/redemptions/{id}` |
| **Subscription Redemptions** | GET `/api/user/me/subscriptions/{id}/redemptions` |
| **Transactions** | GET `/api/user/me/transactions` |
| **Subscription Transactions** | GET `/api/user/me/subscriptions/{id}/transactions` |
| **Profile** | GET `/api/user/me` |
| **Edit Profile** | PUT `/api/user/me` |
| **Update Location** | PATCH `/api/user/me/location` |
| **Cancel Subscription** | PATCH `/api/user/me/subscriptions/{id}/cancel` |
| **Token Refresh** | POST `/api/auth/refresh` |
| **Logout** | POST `/api/auth/logout` |

---

## Vendor App

| Screen | API Calls |
|---|---|
| **Registration** | POST `/api/auth/vendor/register` |
| **Login (OTP Request)** | POST `/api/auth/vendor/login` |
| **OTP Verify** | POST `/api/auth/vendor/verify` |
| **Awaiting Approval** | GET `/api/vendor/profile/status` |
| **Dashboard** | GET `/api/vendor/dashboard/my` |
| **Analytics** | GET `/api/analytics/my` |
| **Store Profile** | GET `/api/vendor/stores/my` |
| **Edit Store** | PUT `/api/vendor/stores/my` |
| **Update Location** | PATCH `/api/vendor/stores/my/location` |
| **Store Status** | PATCH `/api/vendor/stores/my/status` |
| **Store Timings** | GET/POST/DELETE `/api/vendor/stores/{id}/timings` |
| **Store Holidays** | GET/POST/DELETE `/api/vendor/stores/{id}/holidays` |
| **Menu Categories** | GET/POST/PUT/DELETE `/api/menu/categories/**` |
| **Menu Items** | GET/POST/PUT/DELETE `/api/menu/items/**` |
| **Items by Category** | GET `/api/menu/items/by-category/{categoryId}` |
| **My Cards** | GET `/api/cards/my` |
| **Create Card** | POST `/api/cards` |
| **Card Detail/Preview** | GET `/api/cards/{id}/preview` |
| **Update Card** | PUT `/api/cards/{id}` |
| **Deactivate Card** | DELETE `/api/cards/{id}` |
| **Store Subscriptions** | GET `/api/cards/subscriptions/store/{storeId}` |
| **Redemption Queue** | GET `/api/redemptions/store/{storeId}/queue` |
| **Approve Redemption** | POST `/api/redemptions/{id}/approve` |
| **Reject Redemption** | POST `/api/redemptions/{id}/reject?reason=` |
| **Store Redemptions (History)** | GET `/api/redemptions/store/{storeId}` |
| **Store Transactions** | GET `/api/ledger/store/{storeId}` |
| **Profile** | GET/PUT/PATCH `/api/vendor/profile` |
| **Token Refresh** | POST `/api/auth/refresh` |
| **Logout** | POST `/api/auth/logout` |

---

## Admin App

| Screen | API Calls |
|---|---|
| **Initial Setup** | POST `/api/admin/auth/setup` |
| **Login** | POST `/api/admin/auth/login` |
| **Token Refresh** | POST `/api/admin/auth/refresh` |
| **Logout** | POST `/api/admin/auth/logout` |
| **Profile (Me)** | GET `/api/admin/auth/me` |
| **Change Password** | PATCH `/api/admin/auth/change-password` |
| **Dashboard** | GET `/api/admin/dashboard` |
| **Platform Stats** | GET `/api/admin/platform/stats` |
| **Analytics Overview** | GET `/api/admin/analytics/overview` |
| **Revenue Analytics** | GET `/api/admin/analytics/revenue` |
| **Vendor Performance** | GET `/api/admin/analytics/vendor-performance` |
| **Commission Analytics** | GET `/api/admin/analytics/commissions` |
| **Order Analytics** | GET `/api/admin/analytics/orders` |
| **All Vendors** | GET `/api/admin/vendors` |
| **Pending Vendors** | GET `/api/admin/vendors/pending` |
| **Vendor Detail** | GET `/api/admin/vendors/{vendorId}` |
| **Approve Vendor** | PATCH `/api/admin/vendors/{vendorId}/approve` |
| **Reject Vendor** | PATCH `/api/admin/vendors/{vendorId}/reject` |
| **Suspend Vendor** | PATCH `/api/admin/vendors/{vendorId}/suspend` |
| **Reactivate Vendor** | PATCH `/api/admin/vendors/{vendorId}/reactivate` |
| **Set Commission** | PATCH `/api/admin/vendors/{vendorId}/commission-rate` |
| **Vendor Store** | GET `/api/admin/vendors/{vendorId}/store` |
| **Vendor Cards** | GET `/api/admin/vendors/{vendorId}/cards` |
| **Vendor Subscriptions** | GET `/api/admin/vendors/{vendorId}/subscriptions` |
| **Vendor Redemptions** | GET `/api/admin/vendors/{vendorId}/redemptions` |
| **Vendor Transactions** | GET `/api/admin/vendors/{vendorId}/transactions` |
| **All Users** | GET `/api/admin/users` |
| **User Detail** | GET `/api/admin/users/{userId}` |
| **User Subscriptions** | GET `/api/admin/users/{userId}/subscriptions` |
| **User Redemptions** | GET `/api/admin/users/{userId}/redemptions` |
| **User Transactions** | GET `/api/admin/users/{userId}/transactions` |
| **Ban User** | PATCH `/api/admin/users/{userId}/ban` |
| **Unban User** | PATCH `/api/admin/users/{userId}/unban` |
| **All Cards** | GET `/api/admin/cards/vendor/{vendorId}` |
| **Card Detail** | GET `/api/admin/cards/{id}` |
| **Store Subscriptions** | GET `/api/admin/cards/subscriptions/store/{storeId}` |
| **All Redemptions** | GET `/api/admin/redemptions/all` |
| **Redemption Detail** | GET `/api/admin/redemptions/{id}` |
| **All Transactions** | GET `/api/admin/ledger/all` |
| **Commissions** | GET `/api/admin/commissions` |
| **Settle Commission** | PATCH `/api/admin/commissions/{id}/settle` |
| **Settle All (Vendor)** | PATCH `/api/admin/commissions/vendor/{vendorId}/settle-all` |
| **Manage Admins** (SUPER_ADMIN) | GET `/api/admin/auth/admins` |
| **Register Admin** (SUPER_ADMIN) | POST `/api/admin/auth/register` |
| **Activate/Deactivate Admin** (SUPER_ADMIN) | PATCH `/api/admin/auth/admins/{id}/activate|deactivate` |

---

# 16. IMPLEMENTATION PRIORITY

## Vendor App — Implementation Order

### Phase 1: Auth & Onboarding
1. Registration screen → `POST /api/auth/vendor/register`
2. Login screen (OTP request) → `POST /api/auth/vendor/login`
3. OTP verification → `POST /api/auth/vendor/verify`
4. Approval waiting screen → `GET /api/vendor/profile/status`
5. Token refresh + logout flow

### Phase 2: Store Setup
6. Store profile view/edit → GET/PUT `/api/vendor/stores/my`
7. Store location update → `PATCH /api/vendor/stores/my/location`
8. Store timings → GET/POST/DELETE `/api/vendor/stores/{id}/timings`
9. Store holidays → GET/POST/DELETE `/api/vendor/stores/{id}/holidays`
10. Store status toggle → `PATCH /api/vendor/stores/my/status`

### Phase 3: Menu Management
11. Categories list/create/edit/delete → `/api/menu/categories/**`
12. Menu items list/create/edit/delete → `/api/menu/items/**`
13. Items by category → `GET /api/menu/items/by-category/{categoryId}`

### Phase 4: Card Management
14. Create card → `POST /api/cards` (requires categories to exist)
15. My cards list → `GET /api/cards/my`
16. Card preview → `GET /api/cards/{id}/preview`
17. Edit/deactivate card

### Phase 5: Redemption Operations
18. Redemption queue → `GET /api/redemptions/store/{storeId}/queue`
19. Approve redemption → `POST /api/redemptions/{id}/approve`
20. Reject redemption → `POST /api/redemptions/{id}/reject`
21. Redemption history → `GET /api/redemptions/store/{storeId}`

### Phase 6: Dashboard & Analytics
22. Dashboard → `GET /api/vendor/dashboard/my`
23. Store subscriptions → `GET /api/cards/subscriptions/store/{storeId}`
24. Store transactions → `GET /api/ledger/store/{storeId}`
25. Analytics → `GET /api/analytics/my`

---

## User App — Implementation Order

### Phase 1: Auth
1. Phone input + OTP request → `POST /api/auth/user/send-otp`
2. OTP verification → `POST /api/auth/user/verify-otp`
3. Token refresh + logout

### Phase 2: Store Discovery
4. Home / store list → `GET /api/stores`
5. Store search → `GET /api/stores/search`
6. Nearby stores → `GET /api/stores/nearby`
7. Store detail → `GET /api/stores/{storeId}`
8. Store menu → `GET /api/stores/{storeId}/menu`

### Phase 3: Cards & Purchase
9. Cards for store → `GET /api/user/stores/{storeId}/cards`
10. Card detail → `GET /api/cards/{id}`
11. Purchase card → `POST /api/user/me/subscriptions/purchase`
12. My subscriptions → `GET /api/user/me/subscriptions`
13. Active subscriptions → `GET /api/user/me/subscriptions/active`
14. Subscription detail → `GET /api/user/me/subscriptions/{id}`
15. Cancel subscription

### Phase 4: Redemptions
16. Eligible menu → `GET /api/subscriptions/{subscriptionId}/menu`
17. Request redemption → `POST /api/redemptions/request`
18. My redemptions → `GET /api/user/me/redemptions`
19. Redemption detail
20. Subscription redemptions

### Phase 5: Transactions & Profile
21. Transactions → `GET /api/user/me/transactions`
22. Profile view → `GET /api/user/me`
23. Edit profile → `PUT /api/user/me`
24. Update location → `PATCH /api/user/me/location`

---

## Admin App — Implementation Order

### Phase 1: Auth & Setup
1. Initial setup (one-time) → `POST /api/admin/auth/setup`
2. Login → `POST /api/admin/auth/login`
3. Token refresh + logout + profile

### Phase 2: Dashboard
4. Main dashboard → `GET /api/admin/dashboard`
5. Platform stats → `GET /api/admin/platform/stats`

### Phase 3: Vendor Management (Core Function)
6. Vendor list + filter by status → `GET /api/admin/vendors` / `/pending` / `/active`
7. Vendor detail view → `GET /api/admin/vendors/{vendorId}`
8. Approve vendor → `PATCH /api/admin/vendors/{vendorId}/approve`
9. Reject vendor → `PATCH /api/admin/vendors/{vendorId}/reject`
10. Suspend / reactivate vendor
11. Vendor's store / cards / subscriptions / redemptions / transactions

### Phase 4: User Management
12. User list → `GET /api/admin/users`
13. User detail → `GET /api/admin/users/{userId}`
14. Ban / unban user
15. User subscriptions / redemptions / transactions

### Phase 5: Analytics
16. Overview → `GET /api/admin/analytics/overview`
17. Revenue analytics → `GET /api/admin/analytics/revenue`
18. Vendor performance → `GET /api/admin/analytics/vendor-performance`
19. Order analytics → `GET /api/admin/analytics/orders`

### Phase 6: Commission Management
20. Commission list + pending → `GET /api/admin/commissions`
21. Commission stats
22. Settle individual / all for vendor

### Phase 7: Content Views
23. All cards, redemptions, transactions (read-only views)

### Phase 8: Admin Account Management (SUPER_ADMIN only)
24. Admin list → `GET /api/admin/auth/admins`
25. Register admin → `POST /api/admin/auth/register`
26. Activate / deactivate admin
27. Commission rate configuration per vendor

---

*End of MASTER_BACKEND_DOCUMENTATION.md*
