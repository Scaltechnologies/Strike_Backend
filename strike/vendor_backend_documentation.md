# Vendor Backend Documentation
## Strike Platform — Complete API Reference

> **Source of Truth for Vendor Frontend Rebuild**
> Generated from live codebase. Every endpoint verified against controller code.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Authentication](#2-authentication)
3. [Vendor Profile Module](#3-vendor-profile-module)
4. [Store Management Module](#4-store-management-module)
5. [Menu Management Module](#5-menu-management-module)
6. [Card Management Module](#6-card-management-module)
7. [Redemption Module](#7-redemption-module)
8. [Transactions / Ledger Module](#8-transactions--ledger-module)
9. [Complete DTO Reference](#9-complete-dto-reference)
10. [Complete Frontend Mapping](#10-complete-frontend-mapping)
11. [Swagger Verification Notes](#11-swagger-verification-notes)

---

## 1. System Overview

### Architecture

All frontend traffic routes through the **API Gateway** on port 8080. The gateway validates JWT tokens, extracts user identity, and forwards requests to the appropriate microservice.

```
Frontend → strike-gateway:8080 → [microservice]
```

### Service Registry

| Service | Port | Responsibility |
|---|---|---|
| strike-gateway | 8080 | JWT validation, routing, header injection |
| auth-service | 8081 | Vendor registration, OTP, token management |
| user-service | 8082 | Customer accounts (not vendor-facing) |
| vendor-service | 8083 | Vendor profile + store management |
| card-service | 8084 | Card definitions + customer subscriptions |
| redemption-service | 8085 | Redemption queue + approval flow |
| ledger-service | 8086 | Transaction history |
| admin-service | 8087 | Admin approval, commissions (internal) |

### Gateway Route Table

| URL Prefix | Routes To |
|---|---|
| `/api/auth/**` | auth-service:8081 |
| `/api/vendor/**` | vendor-service:8083 |
| `/api/stores/**` | vendor-service:8083 |
| `/api/cards/**` | card-service:8084 |
| `/api/subscriptions/**` | card-service:8084 |
| `/api/redemptions/**` | redemption-service:8085 |
| `/api/ledger/**` | ledger-service:8086 |
| `/api/admin/**` | admin-service:8087 |
| `/api/menu/**` | menu-service |

### Purpose of Each Service

**Vendor Service** manages vendor identity: the profile the vendor sees about themselves (shop name, logo, contact info) and their physical store location. It also exposes internal endpoints that other services use to verify store ownership.

**Store Service** (part of vendor-service) manages the operational store: address, timings, holidays, geolocation, and public-facing status. A vendor has exactly one store.

**Menu Service** manages the vendor's catalog: categories and menu items. Categories group items. Items have a price, type (VEG/NON_VEG), and availability status.

**Card Service** manages the card product the vendor sells to customers. A card definition specifies price, wallet amount, validity, and which menu categories/items customers can redeem. When a customer buys a card, a subscription record is created.

**Redemption Service** manages the act of spending wallet balance. In the legacy (vendor-initiated) flow, the vendor POS posts items and balance is deducted immediately. In the Phase 5 (user-initiated) flow, customers submit requests that appear in the vendor's queue for approval or rejection.

**Ledger/Transaction Service** is the audit trail. Every card purchase and redemption creates a ledger entry for reporting.

---

## 2. Authentication

**Base URL:** `http://localhost:8080/api/auth/vendor`

All authentication endpoints are **unauthenticated** (no JWT required). JWT tokens are returned after OTP verification.

### Common HTTP Headers (Auth Endpoints)

| Header | Value |
|---|---|
| Content-Type | application/json |

---

### 2.1 Register Vendor

```
POST /api/auth/vendor/register
```

**Request Body**

```json
{
  "hotelName": "Spice Garden",
  "address": "123 Main Street, Chennai",
  "mobileNumber": "9876543210",
  "email": "spice@garden.com",
  "latitude": 13.0827,
  "longitude": 80.2707
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| hotelName | String | Yes | `@NotBlank` |
| address | String | Yes | `@NotBlank` |
| mobileNumber | String | Yes | `@NotBlank`, Pattern: `^[6-9]\d{9}$` (Indian mobile) |
| email | String | No | `@Email` |
| latitude | Double | Yes | `@NotNull` |
| longitude | Double | Yes | `@NotNull` |

**Success Response** `200 OK`

```json
"Registration successful. Check server console for OTP."
```

**What Happens**
- Creates vendor record with `status = PENDING`
- Generates a 6-digit OTP (logged to server console in dev mode)

**Error Responses**

| Status | Condition |
|---|---|
| 400 | Validation failure (missing fields, bad phone format) |
| 409 | Mobile number already registered |

---

### 2.2 Request Login OTP

```
POST /api/auth/vendor/login
```

**Request Body**

```json
{
  "mobileNumber": "9876543210"
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| mobileNumber | String | Yes | `@NotBlank`, Pattern: `^[6-9]\d{9}$` |

**Success Response** `200 OK`

```json
"OTP sent. Check server console for OTP."
```

**Error Responses**

| Status | Condition |
|---|---|
| 400 | Invalid mobile format |
| 403 | Vendor status is not ACTIVE (still PENDING or VERIFIED) |
| 404 | Mobile number not registered |

---

### 2.3 Verify OTP

```
POST /api/auth/vendor/verify
```

**Request Body**

```json
{
  "mobileNumber": "9876543210",
  "otp": "123456"
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| mobileNumber | String | Yes | `@NotBlank`, Pattern: `^[6-9]\d{9}$` |
| otp | String | Yes | `@NotBlank` |

**Success Response** `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600,
  "vendorId": 42,
  "hotelName": "Spice Garden",
  "mobileNumber": "9876543210",
  "email": "spice@garden.com",
  "address": "123 Main Street, Chennai",
  "status": "ACTIVE",
  "message": "Login successful"
}
```

**Token Details**

| Field | Value |
|---|---|
| Access token validity | 1 hour (3600 seconds) |
| Refresh token validity | 30 days |
| JWT claim: sub | vendorId |
| JWT claim: authorities | `ROLE_VENDOR` |

**Vendor Status State Machine**

```
PENDING   → (OTP verified on first registration) → VERIFIED
VERIFIED  → (Admin approves in admin panel)       → ACTIVE
ACTIVE    → (Admin suspends)                      → SUSPENDED
ACTIVE    → (Admin rejects)                       → REJECTED
```

Only vendors with `status = ACTIVE` can log in. Registration OTP verification moves the vendor to VERIFIED but login still fails until admin approves.

**Error Responses**

| Status | Condition |
|---|---|
| 400 | OTP is expired or incorrect |
| 403 | Vendor not ACTIVE |
| 404 | Mobile not registered |

---

### 2.4 Check Approval Status

```
GET /api/vendor/profile/status
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role — usable when VERIFIED)

**Success Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "status": "VERIFIED",
    "rejectionReason": null
  }
}
```

**Use Case:** Poll this after registration OTP verification to show the vendor their approval state while they wait for admin approval.

---

## 3. Vendor Profile Module

**Base URL:** `http://localhost:8080/api/vendor/profile`

All endpoints require `Authorization: Bearer {token}` with `ROLE_VENDOR`.

The vendor ID is always extracted from the JWT token server-side — the frontend never needs to send it.

---

### 3.1 Get Vendor Profile

```
GET /api/vendor/profile
```

**Success Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "vendorId": 42,
    "shopName": "Spice Garden",
    "ownerName": "Rajan Kumar",
    "mobile": "9876543210",
    "email": "spice@garden.com",
    "address": "123 Main Street, Chennai",
    "category": "Indian",
    "description": "Authentic South Indian cuisine",
    "logoUrl": "https://cdn.example.com/logo.png",
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-06-01T14:22:00"
  },
  "message": null
}
```

---

### 3.2 Update Vendor Profile (Full Replace)

```
PUT /api/vendor/profile
```

**Request Body** — all fields optional; send only what you want to update:

```json
{
  "shopName": "Spice Garden Deluxe",
  "ownerName": "Rajan Kumar",
  "mobile": "9876543210",
  "email": "spice@garden.com",
  "address": "456 New Street, Chennai",
  "category": "Indian",
  "description": "Updated description",
  "logoUrl": "https://cdn.example.com/new-logo.png"
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| shopName | String | No | `@Size(min=2, max=100)` |
| ownerName | String | No | `@Size(max=100)` |
| mobile | String | No | `@Size(max=15)` |
| email | String | No | `@Email` |
| address | String | No | `@Size(max=300)` |
| category | String | No | `@Size(max=100)` |
| description | String | No | `@Size(max=1000)` |
| logoUrl | String | No | None |

**Success Response** `200 OK`

```json
{
  "success": true,
  "data": { /* VendorProfileResponse */ },
  "message": "Profile updated successfully"
}
```

---

### 3.3 Partial Update Vendor Profile

```
PATCH /api/vendor/profile
```

Same request body as PUT. Difference: only non-null fields are updated (true partial update). PUT replaces all fields.

**Success Response** `200 OK` — same structure as PUT.

---

## 4. Store Management Module

**Base URL:** `http://localhost:8080/api/stores`

A vendor has exactly one store. The store is created automatically when the vendor's account is activated by admin.

---

### 4.1 Get My Store

```
GET /api/stores/my
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Success Response** `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 10,
    "vendorId": 42,
    "name": "Spice Garden",
    "address": "123 Main Street, Chennai",
    "phone": "+919876543210",
    "email": "spice@garden.com",
    "category": "Indian",
    "description": "Authentic South Indian cuisine",
    "logoUrl": "https://cdn.example.com/logo.png",
    "latitude": 13.0827,
    "longitude": 80.2707,
    "distanceKm": null,
    "status": "ACTIVE",
    "timings": [
      {
        "id": 1,
        "storeId": 10,
        "dayOfWeek": "MONDAY",
        "openTime": "09:00",
        "closeTime": "22:00",
        "isClosed": false
      }
    ],
    "holidays": [],
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-06-01T14:22:00"
  }
}
```

---

### 4.2 Update My Store

```
PUT /api/stores/my
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Request Body**

```json
{
  "name": "Spice Garden",
  "address": "456 New Street, Chennai",
  "phone": "+919876543210",
  "email": "store@spice.com",
  "category": "Indian",
  "description": "Updated description",
  "logoUrl": "https://cdn.example.com/logo.png",
  "latitude": 13.0827,
  "longitude": 80.2707
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank`, `@Size(max=100)` |
| address | String | Yes | `@NotBlank`, `@Size(max=300)` |
| phone | String | No | Pattern: `^[+\d][\d\s\-]{6,14}$` |
| email | String | No | `@Email` |
| category | String | No | `@Size(max=100)` |
| description | String | No | `@Size(max=1000)` |
| logoUrl | String | No | `@Size(max=500)` |
| latitude | Double | No | `@DecimalMin("-90.0")`, `@DecimalMax("90.0")` |
| longitude | Double | No | `@DecimalMin("-180.0")`, `@DecimalMax("180.0")` |

**Success Response** `200 OK`

```json
{
  "success": true,
  "data": { /* StoreResponse */ },
  "message": "Store updated successfully"
}
```

---

### 4.3 Update Store Location Only

```
PATCH /api/stores/my/location
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Request Body**

```json
{
  "latitude": 13.0832,
  "longitude": 80.2712
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| latitude | Double | Yes | `@NotNull`, `@DecimalMin("-90.0")`, `@DecimalMax("90.0")` |
| longitude | Double | Yes | `@NotNull`, `@DecimalMin("-180.0")`, `@DecimalMax("180.0")` |

**Success Response** `200 OK`

```json
{
  "success": true,
  "data": { /* StoreResponse */ },
  "message": "Store location updated"
}
```

---

### 4.4 Update Store Status

```
PATCH /api/stores/my/status?status=TEMPORARILY_CLOSED
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Query Parameter**

| Param | Type | Values |
|---|---|---|
| status | StoreStatus | `ACTIVE`, `INACTIVE`, `TEMPORARILY_CLOSED` |

**Success Response** `200 OK`

```json
{
  "success": true,
  "data": { /* StoreResponse */ },
  "message": "Store status updated"
}
```

---

### 4.5 Add / Update Store Timing

```
POST /api/stores/{storeId}/timings
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

Creates or replaces the timing for a specific day of the week (upsert behavior).

**Request Body**

```json
{
  "dayOfWeek": "MONDAY",
  "openTime": "09:00",
  "closeTime": "22:00",
  "isClosed": false
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| dayOfWeek | DayOfWeek | Yes | `@NotNull` — MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY |
| openTime | LocalTime | Conditional | Required when `isClosed = false`. Format: `HH:mm` |
| closeTime | LocalTime | Conditional | Required when `isClosed = false`. Format: `HH:mm` |
| isClosed | Boolean | No | Defaults to `false` |

**Success Response** `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 5,
    "storeId": 10,
    "dayOfWeek": "MONDAY",
    "openTime": "09:00",
    "closeTime": "22:00",
    "isClosed": false
  },
  "message": "Timing saved successfully"
}
```

---

### 4.6 Get Store Timings

```
GET /api/stores/{storeId}/timings
```

**Authentication:** Not required (public)

**Success Response** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 5,
      "storeId": 10,
      "dayOfWeek": "MONDAY",
      "openTime": "09:00",
      "closeTime": "22:00",
      "isClosed": false
    },
    {
      "id": 6,
      "storeId": 10,
      "dayOfWeek": "SUNDAY",
      "openTime": null,
      "closeTime": null,
      "isClosed": true
    }
  ]
}
```

---

### 4.7 Delete Store Timing

```
DELETE /api/stores/{storeId}/timings/{timingId}
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Success Response** `204 No Content`

---

### 4.8 Add Store Holiday

```
POST /api/stores/{storeId}/holidays
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Request Body**

```json
{
  "date": "2026-08-15",
  "reason": "Independence Day"
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| date | LocalDate | Yes | `@NotNull`. Format: `YYYY-MM-DD` |
| reason | String | No | None |

**Error:** 400 if a holiday already exists for the given date.

**Success Response** `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 3,
    "storeId": 10,
    "date": "2026-08-15",
    "reason": "Independence Day",
    "createdAt": "2026-06-20T10:00:00"
  },
  "message": "Holiday added successfully"
}
```

---

### 4.9 Get Store Holidays

```
GET /api/stores/{storeId}/holidays
```

**Authentication:** Not required (public)

**Success Response** `200 OK` — list of `StoreHolidayResponse`

---

### 4.10 Delete Store Holiday

```
DELETE /api/stores/{storeId}/holidays/{holidayId}
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Success Response** `204 No Content`

---

## 5. Menu Management Module

**Base URL:** `http://localhost:8080/api/menu`

All vendor-facing menu endpoints require `Authorization: Bearer {token}` with `ROLE_VENDOR`.

The menu is organized as: **Store → Categories → Menu Items**

---

### 5.1 Category APIs

#### Create Category

```
POST /api/menu/categories
```

**Request Body**

```json
{
  "name": "Starters",
  "description": "Appetizers and snacks",
  "imageUrl": "https://cdn.example.com/starters.png",
  "displayOrder": 1
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank` |
| description | String | No | None |
| imageUrl | String | No | None |
| displayOrder | Integer | No | None |

**Success Response** `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 7,
    "name": "Starters",
    "description": "Appetizers and snacks",
    "imageUrl": "https://cdn.example.com/starters.png",
    "displayOrder": 1,
    "status": "ACTIVE",
    "storeId": 10
  },
  "message": "Category created successfully"
}
```

---

#### Get All My Categories

```
GET /api/menu/categories
```

Returns only `ACTIVE` categories for the vendor's store.

**Success Response** `200 OK` — list of `CategoryResponse`

---

#### Get Category by ID

```
GET /api/menu/categories/{categoryId}
```

**Error:** 404 if the category does not belong to the vendor's store.

**Success Response** `200 OK` — single `CategoryResponse`

---

#### Update Category

```
PUT /api/menu/categories/{categoryId}
```

**Request Body**

```json
{
  "name": "Starters & Snacks",
  "description": "Updated description",
  "imageUrl": "https://cdn.example.com/new.png",
  "displayOrder": 2
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | No | `@Size(min=2, max=100)` |
| description | String | No | `@Size(max=500)` |
| imageUrl | String | No | `@Size(max=500)` |
| displayOrder | Integer | No | None |

**Success Response** `200 OK` — `CategoryResponse` with `message: "Category updated successfully"`

---

#### Delete Category

```
DELETE /api/menu/categories/{categoryId}
```

**Behavior:** Soft delete — sets `status = INACTIVE`. The category remains in the database.

**Restriction:** Cannot delete a category that is mapped to an active card. You must remove the category from all cards first.

**Success Response** `204 No Content`

---

### 5.2 Menu Item APIs

#### Create Menu Item

```
POST /api/menu/items
```

**Request Body**

```json
{
  "name": "Masala Dosa",
  "description": "Crispy dosa with spiced potato filling",
  "price": 120.00,
  "imageUrl": "https://cdn.example.com/dosa.png",
  "itemType": "VEG",
  "availabilityStatus": "AVAILABLE",
  "categoryId": 7
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank` |
| description | String | No | None |
| price | BigDecimal | Yes | `@NotNull` |
| imageUrl | String | No | None |
| itemType | ItemType | No | `VEG` or `NON_VEG` |
| availabilityStatus | ItemAvailabilityStatus | No | `AVAILABLE` or `OUT_OF_STOCK` |
| categoryId | Long | Yes | `@NotNull` |

**Success Response** `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 55,
    "name": "Masala Dosa",
    "description": "Crispy dosa with spiced potato filling",
    "price": 120.00,
    "imageUrl": "https://cdn.example.com/dosa.png",
    "itemType": "VEG",
    "availabilityStatus": "AVAILABLE",
    "categoryId": 7,
    "storeId": 10,
    "createdAt": "2026-06-20T11:00:00",
    "updatedAt": "2026-06-20T11:00:00"
  },
  "message": "Item created successfully"
}
```

---

#### Get All My Menu Items

```
GET /api/menu/items
```

Returns all items across all categories for the vendor's store.

**Success Response** `200 OK` — list of `MenuItemResponse`

---

#### Get Menu Item by ID

```
GET /api/menu/items/{itemId}
```

**Error:** 404 if item doesn't belong to vendor's store.

**Success Response** `200 OK` — single `MenuItemResponse`

---

#### Get Items by Category

```
GET /api/menu/items/by-category/{categoryId}
```

Returns all items in the given category that belong to the vendor's store.

**Success Response** `200 OK` — list of `MenuItemResponse`

---

#### Update Menu Item

```
PUT /api/menu/items/{itemId}
```

**Request Body** — all fields optional:

```json
{
  "name": "Ghee Masala Dosa",
  "description": "Extra crispy with ghee",
  "price": 145.00,
  "imageUrl": "https://cdn.example.com/ghee-dosa.png",
  "itemType": "VEG",
  "availabilityStatus": "AVAILABLE",
  "categoryId": 7
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | No | `@Size(max=100)` |
| description | String | No | `@Size(max=500)` |
| price | BigDecimal | No | `@DecimalMin("0.01")` |
| imageUrl | String | No | `@Size(max=500)` |
| itemType | ItemType | No | `VEG` or `NON_VEG` |
| availabilityStatus | ItemAvailabilityStatus | No | `AVAILABLE` or `OUT_OF_STOCK` |
| categoryId | Long | No | None |

**Success Response** `200 OK` — `MenuItemResponse` with `message: "Item updated successfully"`

---

#### Delete Menu Item

```
DELETE /api/menu/items/{itemId}
```

**Success Response** `204 No Content`

---

#### Toggle Item Availability

Use the **Update Menu Item** endpoint with only the `availabilityStatus` field:

```
PUT /api/menu/items/{itemId}
```

```json
{
  "availabilityStatus": "OUT_OF_STOCK"
}
```

or

```json
{
  "availabilityStatus": "AVAILABLE"
}
```

---

### 5.3 Frontend Menu Screen Mapping

| Screen Action | Endpoint | Notes |
|---|---|---|
| Load all categories | `GET /api/menu/categories` | Active categories only |
| Load items in category | `GET /api/menu/items/by-category/{id}` | |
| Load all items | `GET /api/menu/items` | |
| Create category | `POST /api/menu/categories` | |
| Edit category | `PUT /api/menu/categories/{id}` | |
| Delete category | `DELETE /api/menu/categories/{id}` | Soft delete; blocked if mapped to active card |
| Create menu item | `POST /api/menu/items` | Requires valid categoryId |
| Edit menu item | `PUT /api/menu/items/{id}` | |
| Delete menu item | `DELETE /api/menu/items/{id}` | |
| Toggle item availability | `PUT /api/menu/items/{id}` | Send `{ "availabilityStatus": "OUT_OF_STOCK" }` |
| View store's public menu | `GET /api/stores/{storeId}/menu` | Public endpoint; returns grouped by category |

---

## 6. Card Management Module

**Base URL:** `http://localhost:8080/api/cards`

A card definition is the product the vendor sells to customers. It defines:
- The purchase price (`cardPrice`)
- The wallet amount loaded (`walletAmount` — can be > cardPrice to give a bonus)
- How long the card is valid (`validityInDays`)
- Which menu categories and items can be redeemed with the card

---

### 6.1 Create Card

```
POST /api/cards
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Request Body**

```json
{
  "storeId": 10,
  "name": "Lunch Saver",
  "description": "Best value lunch combo card",
  "cardPrice": 500.00,
  "walletAmount": 600.00,
  "validityInDays": 30,
  "imageUrl": "https://cdn.example.com/card.png",
  "categoryIds": [7, 8],
  "eligibleMenuItemIds": [55, 56, 57]
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| storeId | Long | Yes | `@NotNull` |
| name | String | Yes | `@NotBlank` |
| description | String | No | None |
| cardPrice | BigDecimal | Yes | `@NotNull`, `@DecimalMin("0.01")` |
| walletAmount | BigDecimal | Yes | `@NotNull`, `@DecimalMin("0.01")` |
| validityInDays | Integer | Yes | `@NotNull`, `@Min(1)` |
| imageUrl | String | No | None |
| categoryIds | List\<Long\> | Yes | `@NotNull`, `@Size(min=1)` — at least one category required |
| eligibleMenuItemIds | List\<Long\> | No | `@Size(min=1)` if provided — if omitted, all items in the categories are eligible |

**Validation Logic (Server-Side)**
- All `categoryIds` must belong to the vendor's store and be ACTIVE
- All `eligibleMenuItemIds` (if provided) must belong to the vendor's store and be in one of the specified categories

**Success Response** `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 3,
    "vendorId": 42,
    "storeId": 10,
    "name": "Lunch Saver",
    "description": "Best value lunch combo card",
    "cardPrice": 500.00,
    "walletAmount": 600.00,
    "bonusAmount": 100.00,
    "validityInDays": 30,
    "imageUrl": "https://cdn.example.com/card.png",
    "isActive": true,
    "categoryIds": [7, 8],
    "eligibleMenuItemIds": [55, 56, 57],
    "eligibleItems": [
      { "id": 55, "name": "Masala Dosa", "price": 120.00 },
      { "id": 56, "name": "Idli Sambar", "price": 80.00 },
      { "id": 57, "name": "Vada", "price": 60.00 }
    ],
    "createdAt": "2026-06-20T12:00:00"
  }
}
```

**Note:** `bonusAmount = walletAmount - cardPrice`. This is calculated server-side.

---

### 6.2 Get My Cards

```
GET /api/cards/my
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

Returns all card definitions created by this vendor (both active and inactive).

**Success Response** `200 OK` — list of `CardDefinitionResponse`

---

### 6.3 Update Card

```
PUT /api/cards/{id}
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Request Body** — all fields optional:

```json
{
  "name": "Super Lunch Saver",
  "description": "Updated description",
  "cardPrice": 499.00,
  "walletAmount": 620.00,
  "validityInDays": 45,
  "imageUrl": "https://cdn.example.com/card-new.png",
  "isActive": true,
  "categoryIds": [7, 8, 9],
  "eligibleMenuItemIds": [55, 56, 57, 58]
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | No | `@Size(min=2, max=100)` |
| description | String | No | `@Size(max=500)` |
| cardPrice | BigDecimal | No | `@DecimalMin("0.01")` |
| walletAmount | BigDecimal | No | `@DecimalMin("0.01")` |
| validityInDays | Integer | No | `@Min(1)` |
| imageUrl | String | No | `@Size(max=500)` |
| isActive | Boolean | No | None |
| categoryIds | List\<Long\> | No | `@Size(min=1)` if provided |
| eligibleMenuItemIds | List\<Long\> | No | `@Size(min=1)` if provided |

**Authorization:** The vendor must own the card.

**Success Response** `200 OK` — `CardDefinitionResponse`

---

### 6.4 Deactivate Card

```
DELETE /api/cards/{id}
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Behavior:** Soft delete — sets `isActive = false`. Does not delete the card or affect existing customer subscriptions.

**Authorization:** Vendor must own the card.

**Success Response** `204 No Content`

---

### 6.5 Preview Card

```
GET /api/cards/{id}/preview
```

Shows the menu structure that a customer would see when buying this card.

**Authentication:** Not required (public)

**Success Response** `200 OK`

```json
{
  "cardId": 3,
  "cardName": "Lunch Saver",
  "description": "Best value lunch combo card",
  "cardPrice": 500.00,
  "walletAmount": 600.00,
  "validityInDays": 30,
  "eligibleMenus": [
    {
      "categoryId": 7,
      "categoryName": "Starters",
      "items": [
        {
          "itemId": 55,
          "name": "Masala Dosa",
          "price": 120.00,
          "itemType": "VEG",
          "availabilityStatus": "AVAILABLE"
        }
      ]
    }
  ]
}
```

---

### 6.6 Get Subscriptions for a Store

```
GET /api/cards/subscriptions/store/{storeId}?page=0&size=20
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

Shows all customer subscriptions purchased for this store's cards.

**Query Parameters**

| Param | Type | Default | Description |
|---|---|---|---|
| page | int | 0 | Zero-based page number |
| size | int | 20 | Items per page |

**Success Response** `200 OK`

```json
{
  "content": [
    {
      "id": 101,
      "userId": 200,
      "cardDefinitionId": 3,
      "storeId": 10,
      "walletBalance": 480.00,
      "status": "ACTIVE",
      "purchasedAt": "2026-06-01T09:00:00",
      "expiresAt": "2026-07-01T09:00:00",
      "createdAt": "2026-06-01T09:00:00",
      "updatedAt": "2026-06-01T09:00:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 6.7 Get Subscription by ID

```
GET /api/cards/subscriptions/{subscriptionId}
```

**Headers:**
- `Authorization: Bearer {token}`
- `X-User-Role: VENDOR` (injected by gateway)

**Success Response** `200 OK` — single `SubscriptionResponse`

---

### 6.8 Frontend Card Screen Mapping

| Screen Action | Endpoint | Notes |
|---|---|---|
| Load card list | `GET /api/cards/my` | |
| Create new card | `POST /api/cards` | Validate categoryIds exist first |
| Edit card | `PUT /api/cards/{id}` | |
| Deactivate card | `DELETE /api/cards/{id}` | Soft delete |
| Preview card (as customer sees it) | `GET /api/cards/{id}/preview` | |
| View who bought the card | `GET /api/cards/subscriptions/store/{storeId}` | Paginated |
| View single subscription detail | `GET /api/cards/subscriptions/{subscriptionId}` | |

---

## 7. Redemption Module

**Base URL:** `http://localhost:8080/api/redemptions`

The redemption module supports two flows:

**Legacy Flow (Vendor-initiated POS):** Vendor selects items on their POS device for the customer. Balance is deducted immediately. Status goes straight to COMPLETED.

**Phase 5 Flow (Customer-initiated):** Customer submits a redemption request from their app. The request lands in the vendor's queue as PENDING. Vendor reviews and approves or rejects. Balance is only deducted on approval.

---

### Redemption Status Enum

| Status | Meaning |
|---|---|
| `PENDING` | Customer submitted; awaiting vendor approval |
| `COMPLETED` | Approved by vendor or completed via POS |
| `REJECTED` | Vendor rejected the request |
| `FAILED` | System error during processing |
| `REVERSED` | Refund was processed |

---

### 7.1 Vendor-Initiated Redemption (POS Flow)

```
POST /api/redemptions
```

**Headers:**
- `Authorization: Bearer {token}` (VENDOR role)
- `Idempotency-Key: {unique-key}` (optional — prevents double submission)

**Request Body**

```json
{
  "subscriptionId": 101,
  "storeId": 10,
  "items": [
    { "menuItemId": 55, "quantity": 2 },
    { "menuItemId": 57, "quantity": 1 }
  ]
}
```

**Field Validation**

| Field | Type | Required | Validation |
|---|---|---|---|
| subscriptionId | Long | Yes | `@NotNull` |
| storeId | Long | Yes | `@NotNull` |
| items | List | Yes | `@Valid`, `@NotEmpty` |
| items[].menuItemId | Long | Yes | `@NotNull` |
| items[].quantity | Integer | Yes | `@NotNull`, `@Min(1)` |

**Server-Side Checks**
- Vendor must own the given `storeId`
- Subscription must be `ACTIVE`
- Subscription's `storeId` must match the request's `storeId`
- Items must be in the subscription's eligible categories/items
- Items must have `availabilityStatus = AVAILABLE`
- Subscription must have sufficient balance

**Success Response** `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 500,
    "subscriptionId": 101,
    "userId": 200,
    "storeId": 10,
    "totalAmount": 300.00,
    "remainingBalance": 300.00,
    "status": "COMPLETED",
    "initiatedBy": "VENDOR",
    "items": [
      {
        "menuItemId": 55,
        "menuItemName": "Masala Dosa",
        "quantity": 2,
        "unitPrice": 120.00,
        "totalPrice": 240.00
      },
      {
        "menuItemId": 57,
        "menuItemName": "Vada",
        "quantity": 1,
        "unitPrice": 60.00,
        "totalPrice": 60.00
      }
    ],
    "createdAt": "2026-06-20T13:00:00",
    "approvedAt": "2026-06-20T13:00:00",
    "rejectedAt": null,
    "failureReason": null
  }
}
```

---

### 7.2 Get Redemption Queue (Phase 5)

```
GET /api/redemptions/store/{storeId}/queue
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

Returns all `PENDING` redemption requests for the store. These were submitted by customers via their app.

**Success Response** `200 OK`

```json
[
  {
    "id": 501,
    "subscriptionId": 102,
    "userId": 201,
    "customerName": "Priya Sharma",
    "storeId": 10,
    "totalAmount": 180.00,
    "status": "PENDING",
    "initiatedBy": "USER",
    "items": [
      {
        "menuItemId": 56,
        "menuItemName": "Idli Sambar",
        "quantity": 2,
        "unitPrice": 80.00,
        "totalPrice": 160.00
      },
      {
        "menuItemId": 57,
        "menuItemName": "Vada",
        "quantity": 1,
        "unitPrice": 60.00,
        "totalPrice": 60.00
      }
    ],
    "createdAt": "2026-06-20T13:10:00"
  }
]
```

---

### 7.3 Approve Redemption (Phase 5)

```
POST /api/redemptions/{id}/approve
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**No request body.**

**What Happens**
1. Deducts `totalAmount` from subscription wallet
2. Sets `status = COMPLETED`
3. Records a `REDEMPTION` transaction in ledger
4. Sends notification to customer

**Restriction:** Only `PENDING` redemptions can be approved.

**Success Response** `200 OK` — `RedemptionResponse` with `status: "COMPLETED"`

**Error Responses**

| Status | Condition |
|---|---|
| 400 | Redemption is not in PENDING state |
| 400 | Insufficient balance in subscription |
| 404 | Redemption ID not found |

---

### 7.4 Reject Redemption (Phase 5)

```
POST /api/redemptions/{id}/reject
```

**Headers:** `Authorization: Bearer {token}` (VENDOR role)

**Request Body** (optional)

```json
{
  "reason": "Item out of stock"
}
```

**What Happens**
1. Sets `status = REJECTED`
2. Populates `failureReason`
3. No balance is deducted
4. Sends rejection notification to customer

**Restriction:** Only `PENDING` redemptions can be rejected.

**Success Response** `200 OK` — `RedemptionResponse` with `status: "REJECTED"`

---

### 7.5 Get Store Redemption History

```
GET /api/redemptions/store/{storeId}?page=0&size=20
```

**Headers:** `Authorization: Bearer {token}` (VENDOR or ADMIN role)

Returns paginated redemption history for the store (all statuses).

**Query Parameters**

| Param | Type | Default |
|---|---|---|
| page | int | 0 |
| size | int | 20 |

**Success Response** `200 OK` — `PageResponse<RedemptionResponse>`

---

### 7.6 Get Single Redemption

```
GET /api/redemptions/{id}
```

**Headers:** `Authorization: Bearer {token}` (VENDOR, USER, or ADMIN)

**Success Response** `200 OK` — `RedemptionResponse`

---

### 7.7 Redemption Flow Diagrams

**Legacy (POS) Flow:**
```
Vendor opens customer's subscription
  → Vendor selects items on POS
  → POST /api/redemptions
  → Server validates & deducts balance
  → Status: COMPLETED immediately
  → Customer notified
```

**Phase 5 (Queue) Flow:**
```
Customer submits request (via user app)
  → POST /api/redemptions/request   [USER role]
  → Status: PENDING, no deduction
  → Appears in vendor queue

Vendor reviews queue
  → GET /api/redemptions/store/{storeId}/queue
  → Sees customer name, items, amount

Vendor approves
  → POST /api/redemptions/{id}/approve
  → Balance deducted from subscription
  → Status: COMPLETED
  → Customer notified

Vendor rejects
  → POST /api/redemptions/{id}/reject  { reason: "..." }
  → Status: REJECTED, no deduction
  → Customer notified with reason
```

---

### 7.8 Frontend Redemption Screen Mapping

| Screen Action | Endpoint | Notes |
|---|---|---|
| Load redemption queue | `GET /api/redemptions/store/{storeId}/queue` | PENDING only |
| Approve queue item | `POST /api/redemptions/{id}/approve` | No body |
| Reject queue item | `POST /api/redemptions/{id}/reject` | Optional reason body |
| Create POS redemption | `POST /api/redemptions` | Vendor-initiated |
| View redemption history | `GET /api/redemptions/store/{storeId}` | Paginated, all statuses |
| View single redemption | `GET /api/redemptions/{id}` | |

---

## 8. Transactions / Ledger Module

**Base URL:** `http://localhost:8080/api/ledger`

The ledger is a read-only audit trail for the vendor. Entries are created automatically when:
- A customer purchases a card → `CARD_PURCHASE`
- A vendor approves or completes a redemption → `REDEMPTION`
- A refund is processed → `REFUND`

---

### Transaction Type Enum

| Value | Meaning |
|---|---|
| `CARD_PURCHASE` | Customer purchased a card definition |
| `REDEMPTION` | Wallet balance was spent on items |
| `REFUND` | Refund credited back |
| `TOP_UP` | Balance topped up (future feature) |

---

### 8.1 Get Store Transactions

```
GET /api/ledger/store/{storeId}?page=0&size=20
```

**Headers:** `Authorization: Bearer {token}` (VENDOR or ADMIN role)

**Query Parameters**

| Param | Type | Default |
|---|---|---|
| page | int | 0 |
| size | int | 20 |

**Success Response** `200 OK`

```json
{
  "content": [
    {
      "id": 1001,
      "storeId": 10,
      "customerId": 200,
      "subscriptionId": 101,
      "transactionType": "REDEMPTION",
      "amount": 300.00,
      "remarks": "Redemption at Spice Garden",
      "createdAt": "2026-06-20T13:00:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

### 8.2 Frontend Transaction / Ledger Screen Mapping

| Screen Action | Endpoint | Notes |
|---|---|---|
| Load transaction history | `GET /api/ledger/store/{storeId}` | Paginated |
| Load next page | `GET /api/ledger/store/{storeId}?page=1` | |

---

## 9. Complete DTO Reference

### 9.1 Request DTOs

#### RegisterVendorRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| hotelName | String | Yes | `@NotBlank` |
| address | String | Yes | `@NotBlank` |
| mobileNumber | String | Yes | `@NotBlank`, Pattern `^[6-9]\d{9}$` |
| email | String | No | `@Email` |
| latitude | Double | Yes | `@NotNull` |
| longitude | Double | Yes | `@NotNull` |

#### VendorLoginRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| mobileNumber | String | Yes | `@NotBlank`, Pattern `^[6-9]\d{9}$` |

#### VerifyOtpRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| mobileNumber | String | Yes | `@NotBlank`, Pattern `^[6-9]\d{9}$` |
| otp | String | Yes | `@NotBlank` |

#### UpdateVendorProfileRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| shopName | String | No | `@Size(min=2, max=100)` |
| ownerName | String | No | `@Size(max=100)` |
| mobile | String | No | `@Size(max=15)` |
| email | String | No | `@Email` |
| address | String | No | `@Size(max=300)` |
| category | String | No | `@Size(max=100)` |
| description | String | No | `@Size(max=1000)` |
| logoUrl | String | No | None |

#### StoreDetailsRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank`, `@Size(max=100)` |
| address | String | Yes | `@NotBlank`, `@Size(max=300)` |
| phone | String | No | Pattern `^[+\d][\d\s\-]{6,14}$` |
| email | String | No | `@Email` |
| category | String | No | `@Size(max=100)` |
| description | String | No | `@Size(max=1000)` |
| logoUrl | String | No | `@Size(max=500)` |
| latitude | Double | No | `@DecimalMin("-90.0")`, `@DecimalMax("90.0")` |
| longitude | Double | No | `@DecimalMin("-180.0")`, `@DecimalMax("180.0")` |

#### UpdateStoreLocationRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| latitude | Double | Yes | `@NotNull`, `@DecimalMin("-90.0")`, `@DecimalMax("90.0")` |
| longitude | Double | Yes | `@NotNull`, `@DecimalMin("-180.0")`, `@DecimalMax("180.0")` |

#### StoreTimingRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| dayOfWeek | DayOfWeek | Yes | `@NotNull` (MONDAY–SUNDAY) |
| openTime | LocalTime | Conditional | Required if `isClosed = false`. Format `HH:mm` |
| closeTime | LocalTime | Conditional | Required if `isClosed = false`. Format `HH:mm` |
| isClosed | Boolean | No | Default `false` |

#### StoreHolidayRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| date | LocalDate | Yes | `@NotNull`. Format `YYYY-MM-DD` |
| reason | String | No | None |

#### CreateCategoryRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank` |
| description | String | No | None |
| imageUrl | String | No | None |
| displayOrder | Integer | No | None |

#### UpdateCategoryRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | No | `@Size(min=2, max=100)` |
| description | String | No | `@Size(max=500)` |
| imageUrl | String | No | `@Size(max=500)` |
| displayOrder | Integer | No | None |

#### CreateMenuItemRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | `@NotBlank` |
| description | String | No | None |
| price | BigDecimal | Yes | `@NotNull` |
| imageUrl | String | No | None |
| itemType | ItemType | No | `VEG` \| `NON_VEG` |
| availabilityStatus | ItemAvailabilityStatus | No | `AVAILABLE` \| `OUT_OF_STOCK` |
| categoryId | Long | Yes | `@NotNull` |

#### UpdateMenuItemRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | No | `@Size(max=100)` |
| description | String | No | `@Size(max=500)` |
| price | BigDecimal | No | `@DecimalMin("0.01")` |
| imageUrl | String | No | `@Size(max=500)` |
| itemType | ItemType | No | `VEG` \| `NON_VEG` |
| availabilityStatus | ItemAvailabilityStatus | No | `AVAILABLE` \| `OUT_OF_STOCK` |
| categoryId | Long | No | None |

#### CreateCardRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| storeId | Long | Yes | `@NotNull` |
| name | String | Yes | `@NotBlank` |
| description | String | No | None |
| cardPrice | BigDecimal | Yes | `@NotNull`, `@DecimalMin("0.01")` |
| walletAmount | BigDecimal | Yes | `@NotNull`, `@DecimalMin("0.01")` |
| validityInDays | Integer | Yes | `@NotNull`, `@Min(1)` |
| imageUrl | String | No | None |
| categoryIds | List\<Long\> | Yes | `@NotNull`, `@Size(min=1)` |
| eligibleMenuItemIds | List\<Long\> | No | `@Size(min=1)` if provided |

#### UpdateCardRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | No | `@Size(min=2, max=100)` |
| description | String | No | `@Size(max=500)` |
| cardPrice | BigDecimal | No | `@DecimalMin("0.01")` |
| walletAmount | BigDecimal | No | `@DecimalMin("0.01")` |
| validityInDays | Integer | No | `@Min(1)` |
| imageUrl | String | No | `@Size(max=500)` |
| isActive | Boolean | No | None |
| categoryIds | List\<Long\> | No | `@Size(min=1)` if provided |
| eligibleMenuItemIds | List\<Long\> | No | `@Size(min=1)` if provided |

#### RedemptionRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| subscriptionId | Long | Yes | `@NotNull` |
| storeId | Long | Yes | `@NotNull` |
| items | List | Yes | `@Valid`, `@NotEmpty` |
| items[].menuItemId | Long | Yes | `@NotNull` |
| items[].quantity | Integer | Yes | `@NotNull`, `@Min(1)` |

#### RejectRedemptionRequest
| Field | Type | Required | Validation |
|---|---|---|---|
| reason | String | No | None |

---

### 9.2 Response DTOs

#### VendorAuthResponse
| Field | Type | Description |
|---|---|---|
| token | String | JWT access token (1-hour validity) |
| refreshToken | String | Refresh token (30-day validity) |
| expiresIn | Long | Seconds until access token expires |
| vendorId | Long | Vendor's database ID |
| hotelName | String | Vendor's hotel/shop name |
| mobileNumber | String | Registered mobile number |
| email | String | Email address |
| address | String | Business address |
| status | String | `PENDING` / `VERIFIED` / `ACTIVE` |
| message | String | Human-readable status message |

#### VendorProfileResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Profile record ID |
| vendorId | Long | Vendor ID (same as JWT sub) |
| shopName | String | Shop display name |
| ownerName | String | Owner name |
| mobile | String | Contact number |
| email | String | Contact email |
| address | String | Physical address |
| category | String | Business category |
| description | String | Business description |
| logoUrl | String | Logo image URL |
| createdAt | LocalDateTime | Profile creation timestamp |
| updatedAt | LocalDateTime | Last update timestamp |

#### StoreResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Store ID |
| vendorId | Long | Owning vendor's ID |
| name | String | Store name |
| address | String | Physical address |
| phone | String | Contact phone |
| email | String | Contact email |
| category | String | Store category |
| description | String | Store description |
| logoUrl | String | Logo image URL |
| latitude | Double | GPS latitude |
| longitude | Double | GPS longitude |
| distanceKm | Double | Distance from user (only in nearby-search results) |
| status | StoreStatus | `ACTIVE` / `INACTIVE` / `TEMPORARILY_CLOSED` |
| timings | List\<StoreTimingResponse\> | Operating hours per day |
| holidays | List\<StoreHolidayResponse\> | Closed dates |
| createdAt | LocalDateTime | |
| updatedAt | LocalDateTime | |

#### StoreTimingResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Timing record ID |
| storeId | Long | Parent store ID |
| dayOfWeek | DayOfWeek | MONDAY–SUNDAY |
| openTime | LocalTime | Opening time (HH:mm) |
| closeTime | LocalTime | Closing time (HH:mm) |
| isClosed | Boolean | True if closed that day |

#### StoreHolidayResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Holiday record ID |
| storeId | Long | Parent store ID |
| date | LocalDate | Holiday date (YYYY-MM-DD) |
| reason | String | Reason for closure |
| createdAt | LocalDateTime | When the holiday was added |

#### CategoryResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Category ID |
| name | String | Category name |
| description | String | Description |
| imageUrl | String | Category image URL |
| displayOrder | Integer | Sort order in UI |
| status | CategoryStatus | `ACTIVE` / `INACTIVE` |
| storeId | Long | Parent store ID |

#### MenuItemResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Item ID |
| name | String | Item name |
| description | String | Item description |
| price | BigDecimal | Price (2 decimal places) |
| imageUrl | String | Item image URL |
| itemType | ItemType | `VEG` / `NON_VEG` |
| availabilityStatus | ItemAvailabilityStatus | `AVAILABLE` / `OUT_OF_STOCK` |
| categoryId | Long | Parent category ID |
| storeId | Long | Parent store ID |
| createdAt | LocalDateTime | |
| updatedAt | LocalDateTime | |

#### CardDefinitionResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Card definition ID |
| vendorId | Long | Owning vendor |
| storeId | Long | Associated store |
| name | String | Card name |
| description | String | Card description |
| cardPrice | BigDecimal | Purchase price |
| walletAmount | BigDecimal | Wallet credit amount |
| bonusAmount | BigDecimal | `walletAmount - cardPrice` |
| validityInDays | Integer | Days until expiry after purchase |
| imageUrl | String | Card image URL |
| isActive | Boolean | Whether card is currently on sale |
| categoryIds | List\<Long\> | Eligible category IDs |
| eligibleMenuItemIds | List\<Long\> | Specific item IDs (empty = all items in categories) |
| eligibleItems | List\<EligibleItemInfo\> | Item name + price for display |
| createdAt | LocalDateTime | |

#### EligibleItemInfo
| Field | Type | Description |
|---|---|---|
| id | Long | Menu item ID |
| name | String | Item name |
| price | BigDecimal | Item price |

#### SubscriptionResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Subscription ID |
| userId | Long | Customer ID |
| cardDefinitionId | Long | Which card was purchased |
| storeId | Long | Store this subscription is for |
| walletBalance | BigDecimal | Current remaining balance |
| status | SubscriptionStatus | `ACTIVE` / `EXPIRED` / `EXHAUSTED` / `CANCELLED` |
| purchasedAt | LocalDateTime | Purchase timestamp |
| expiresAt | LocalDateTime | Expiry timestamp |
| createdAt | LocalDateTime | |
| updatedAt | LocalDateTime | |

#### RedemptionResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Redemption record ID |
| subscriptionId | Long | Which subscription was used |
| userId | Long | Customer ID |
| storeId | Long | Store where redeemed |
| totalAmount | BigDecimal | Total amount deducted |
| remainingBalance | BigDecimal | Balance after deduction |
| status | RedemptionStatus | `PENDING` / `COMPLETED` / `REJECTED` / `FAILED` / `REVERSED` |
| initiatedBy | String | `VENDOR` or `USER` |
| items | List\<RedemptionItemResponse\> | Itemized list |
| createdAt | LocalDateTime | |
| approvedAt | LocalDateTime | When vendor approved (null if not yet) |
| rejectedAt | LocalDateTime | When vendor rejected (null if not) |
| failureReason | String | Rejection reason or error message |

#### RedemptionItemResponse
| Field | Type | Description |
|---|---|---|
| menuItemId | Long | Item ID |
| menuItemName | String | Item name at time of redemption |
| quantity | Integer | Number of units |
| unitPrice | BigDecimal | Price per unit at redemption time |
| totalPrice | BigDecimal | `unitPrice × quantity` |

#### RedemptionQueueResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Redemption ID |
| subscriptionId | Long | |
| userId | Long | |
| customerName | String | Customer display name |
| storeId | Long | |
| totalAmount | BigDecimal | |
| status | RedemptionStatus | Always `PENDING` in queue |
| initiatedBy | String | Always `USER` in queue |
| items | List\<RedemptionItemResponse\> | |
| createdAt | LocalDateTime | |

#### TransactionResponse
| Field | Type | Description |
|---|---|---|
| id | Long | Transaction ID |
| storeId | Long | Associated store |
| customerId | Long | Customer involved |
| subscriptionId | Long | Subscription involved |
| transactionType | TransactionType | `CARD_PURCHASE` / `REDEMPTION` / `REFUND` / `TOP_UP` |
| amount | BigDecimal | Transaction amount |
| remarks | String | Human-readable note |
| createdAt | LocalDateTime | |

### 9.3 Common Wrappers

#### ApiResponse\<T\>
```json
{
  "success": true,
  "data": { },
  "message": "Operation successful"
}
```

#### PageResponse\<T\>
```json
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

#### ErrorResponse
```json
{
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "statusCode": 400,
  "timestamp": "2026-06-20T13:00:00"
}
```

### 9.4 Enum Reference

#### StoreStatus
| Value | Meaning |
|---|---|
| `ACTIVE` | Store is open and accepting customers |
| `INACTIVE` | Store is disabled |
| `TEMPORARILY_CLOSED` | Short-term closure (e.g., holiday) |

#### CategoryStatus
| Value | Meaning |
|---|---|
| `ACTIVE` | Category is visible |
| `INACTIVE` | Soft-deleted |

#### ItemType
| Value | Meaning |
|---|---|
| `VEG` | Vegetarian |
| `NON_VEG` | Non-vegetarian |

#### ItemAvailabilityStatus
| Value | Meaning |
|---|---|
| `AVAILABLE` | Item is in stock |
| `OUT_OF_STOCK` | Item temporarily unavailable |

#### VendorStatus
| Value | Meaning |
|---|---|
| `PENDING` | Registered, OTP not yet verified |
| `VERIFIED` | OTP verified, awaiting admin approval |
| `ACTIVE` | Admin approved; can log in and use all APIs |
| `REJECTED` | Admin rejected the application |
| `SUSPENDED` | Admin suspended the account |

#### SubscriptionStatus
| Value | Meaning |
|---|---|
| `ACTIVE` | Active, balance available |
| `EXPIRED` | Past `expiresAt` date |
| `EXHAUSTED` | `walletBalance` reached zero |
| `CANCELLED` | User cancelled |

#### RedemptionStatus
| Value | Meaning |
|---|---|
| `PENDING` | Customer submitted; awaiting vendor decision |
| `COMPLETED` | Approved/completed, balance deducted |
| `REJECTED` | Vendor rejected |
| `FAILED` | System error |
| `REVERSED` | Refund applied |

#### TransactionType
| Value | Meaning |
|---|---|
| `CARD_PURCHASE` | Customer bought a card |
| `REDEMPTION` | Balance used for items |
| `REFUND` | Refund credited |
| `TOP_UP` | Balance added |

---

## 10. Complete Frontend Mapping

### 10.1 Screen → API → DTOs Table

| Frontend Screen | Backend APIs Used | DTOs Used |
|---|---|---|
| **Registration** | `POST /api/auth/vendor/register` | `RegisterVendorRequest` |
| **Login** | `POST /api/auth/vendor/login` | `VendorLoginRequest` |
| **OTP Verify** | `POST /api/auth/vendor/verify` | `VerifyOtpRequest` → `VendorAuthResponse` |
| **Approval Pending** | `GET /api/vendor/profile/status` | `Map<status, rejectionReason>` |
| **Dashboard / Home** | `GET /api/stores/my`, `GET /api/ledger/store/{id}`, `GET /api/redemptions/store/{id}/queue` | `StoreResponse`, `PageResponse<TransactionResponse>`, `List<RedemptionQueueResponse>` |
| **Profile — View** | `GET /api/vendor/profile` | `VendorProfileResponse` |
| **Profile — Edit** | `PATCH /api/vendor/profile` | `UpdateVendorProfileRequest` → `VendorProfileResponse` |
| **Store — View** | `GET /api/stores/my` | `StoreResponse` |
| **Store — Edit** | `PUT /api/stores/my` | `StoreDetailsRequest` → `StoreResponse` |
| **Store — Update Location** | `PATCH /api/stores/my/location` | `UpdateStoreLocationRequest` → `StoreResponse` |
| **Store — Toggle Status** | `PATCH /api/stores/my/status?status=X` | `StoreResponse` |
| **Store Timings** | `GET /api/stores/{id}/timings`, `POST /api/stores/{id}/timings`, `DELETE /api/stores/{id}/timings/{timingId}` | `StoreTimingRequest`, `StoreTimingResponse` |
| **Store Holidays** | `GET /api/stores/{id}/holidays`, `POST /api/stores/{id}/holidays`, `DELETE /api/stores/{id}/holidays/{holidayId}` | `StoreHolidayRequest`, `StoreHolidayResponse` |
| **Menu — Categories** | `GET /api/menu/categories`, `POST /api/menu/categories`, `PUT /api/menu/categories/{id}`, `DELETE /api/menu/categories/{id}` | `CreateCategoryRequest`, `UpdateCategoryRequest`, `CategoryResponse` |
| **Menu — Items List** | `GET /api/menu/items` or `GET /api/menu/items/by-category/{id}` | `MenuItemResponse` |
| **Menu — Create Item** | `POST /api/menu/items` | `CreateMenuItemRequest` → `MenuItemResponse` |
| **Menu — Edit Item** | `PUT /api/menu/items/{id}` | `UpdateMenuItemRequest` → `MenuItemResponse` |
| **Menu — Delete Item** | `DELETE /api/menu/items/{id}` | — |
| **Menu — Toggle Availability** | `PUT /api/menu/items/{id}` | `{ availabilityStatus: "OUT_OF_STOCK" \| "AVAILABLE" }` |
| **Cards — List** | `GET /api/cards/my` | `List<CardDefinitionResponse>` |
| **Cards — Create** | `POST /api/cards` | `CreateCardRequest` → `CardDefinitionResponse` |
| **Cards — Edit** | `PUT /api/cards/{id}` | `UpdateCardRequest` → `CardDefinitionResponse` |
| **Cards — Deactivate** | `DELETE /api/cards/{id}` | — |
| **Cards — Preview** | `GET /api/cards/{id}/preview` | `CardPreviewResponse` |
| **Cards — Subscribers** | `GET /api/cards/subscriptions/store/{storeId}` | `PageResponse<SubscriptionResponse>` |
| **Redemption Queue** | `GET /api/redemptions/store/{storeId}/queue` | `List<RedemptionQueueResponse>` |
| **Redemption — Approve** | `POST /api/redemptions/{id}/approve` | `RedemptionResponse` |
| **Redemption — Reject** | `POST /api/redemptions/{id}/reject` | `RejectRedemptionRequest`, `RedemptionResponse` |
| **POS Redemption** | `POST /api/redemptions` | `RedemptionRequest` → `RedemptionResponse` |
| **Redemption History** | `GET /api/redemptions/store/{storeId}` | `PageResponse<RedemptionResponse>` |
| **Transactions** | `GET /api/ledger/store/{storeId}` | `PageResponse<TransactionResponse>` |

---

### 10.2 HTTP Headers Reference

Every authenticated request must include:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

For idempotent operations (card purchase, POS redemption):

```
Idempotency-Key: {client-generated-uuid}
```

The gateway automatically injects `X-User-Id` and `X-User-Role` from the JWT. You do not need to set these in the frontend.

---

### 10.3 Pagination Convention

All paginated endpoints share the same query parameters and response shape:

```
GET /api/.../store/{storeId}?page=0&size=20
```

```json
{
  "content": [...],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

## 11. Swagger Verification Notes

This section documents which endpoints were verified directly against controller source code. Every endpoint listed in this document corresponds to a verified `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, or `@DeleteMapping` annotation in the codebase.

### Verified Controllers

| Controller | File Location | Endpoints Verified |
|---|---|---|
| VendorAuthController | auth-service | `/api/auth/vendor/register`, `/api/auth/vendor/login`, `/api/auth/vendor/verify` |
| VendorProfileController | vendor-service | `GET /api/vendor/profile`, `PUT /api/vendor/profile`, `PATCH /api/vendor/profile`, `GET /api/vendor/profile/status` |
| StoreController | vendor-service | `GET /api/stores/my`, `PUT /api/stores/my`, `PATCH /api/stores/my/location`, `PATCH /api/stores/my/status` |
| PublicStoreController | vendor-service | `GET /api/stores`, `GET /api/stores/nearby`, `GET /api/stores/search`, `GET /api/stores/{id}`, `GET /api/stores/{id}/menu` |
| StoreTimingController | vendor-service | `POST /api/stores/{id}/timings`, `GET /api/stores/{id}/timings`, `DELETE /api/stores/{id}/timings/{timingId}` |
| StoreHolidayController | vendor-service | `POST /api/stores/{id}/holidays`, `GET /api/stores/{id}/holidays`, `DELETE /api/stores/{id}/holidays/{holidayId}` |
| CategoryController | menu-service | `POST /api/menu/categories`, `GET /api/menu/categories`, `GET /api/menu/categories/{id}`, `PUT /api/menu/categories/{id}`, `DELETE /api/menu/categories/{id}` |
| MenuItemController | menu-service | `POST /api/menu/items`, `GET /api/menu/items`, `GET /api/menu/items/{id}`, `GET /api/menu/items/by-category/{id}`, `PUT /api/menu/items/{id}`, `DELETE /api/menu/items/{id}` |
| VendorCardController | card-service | `POST /api/cards`, `GET /api/cards/my`, `PUT /api/cards/{id}`, `DELETE /api/cards/{id}`, `GET /api/cards/{id}/preview`, `GET /api/cards/subscriptions/store/{storeId}`, `GET /api/cards/subscriptions/{id}` |
| VendorRedemptionController | redemption-service | `POST /api/redemptions`, `GET /api/redemptions/store/{storeId}/queue`, `POST /api/redemptions/{id}/approve`, `POST /api/redemptions/{id}/reject`, `GET /api/redemptions/store/{storeId}`, `GET /api/redemptions/{id}` |
| VendorTransactionController | ledger-service | `GET /api/ledger/store/{storeId}` |

### Endpoints NOT Listed

The following exist in the codebase but are not vendor-facing and are excluded from this document:
- `/internal/**` — service-to-service only, not exposed through gateway
- `/api/admin/**` — admin panel only
- `/api/user/**` — customer app only
- `/api/subscriptions/**` — customer app only
- `/api/redemptions/request` — customer app only
- `/api/redemptions/user/{userId}` — customer app only
- `/api/redemptions/subscription/{subscriptionId}` — customer app only
- `/api/ledger/user/{userId}` — customer app only

---

*End of Vendor Backend Documentation*
