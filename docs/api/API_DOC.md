# API_DOC for Frontend

## Base URL

- Production: `https://mnisthanh.page/auction/api/v1`
- Dev: `http://localhost:8080/api/v1`

## Auth Method

- Use `Bearer JWT` for endpoints that require authentication.
- Header:

```http
Authorization: Bearer <accessToken>
```

- Login returns `accessToken`.
- `refreshToken` is stored in an HTTP-only cookie named `refresh_token`.
- Browser requests that rely on the refresh cookie must include credentials:

```ts
fetch(url, { credentials: "include" })
```

## Endpoint List

### Authentication

| Method | Path |
|---|---|
| POST | `/auth/authenticate` |
| POST | `/auth/introspect` |
| POST | `/auth/refresh-token` |
| POST | `/auth/outbound/authenticate` |
| POST | `/auth/verify-otp` |
| POST | `/auth/logout` |

### Users

| Method | Path |
|---|---|
| GET | `/users` |
| POST | `/users` |
| POST | `/users/otp` |
| POST | `/users/create-password` |
| PUT | `/users/update-my-info` |
| GET | `/users/{id}` |
| GET | `/users/my-info` |
| DELETE | `/users/delete/{id}` |
| POST | `/users/change-password` |
| POST | `/users/forgot-password` |
| POST | `/users/reset-password` |
| GET | `/users/my-profile` |
| GET | `/users/{userId}/public-profile` |
| GET | `/users/admin/search` |
| PATCH | `/users/admin/{id}/active-status` |
| POST | `/users/admin-create` |
| PUT | `/users/{userId}/admin-update` |

### Products

| Method | Path |
|---|---|
| GET | `/products` |
| POST | `/products` |
| GET | `/products/my-products` |
| GET | `/products/search` |
| GET | `/products/{id}` |
| PUT | `/products/{id}` |
| PATCH | `/products/{id}` |
| PATCH | `/products/{id}/restore` |
| GET | `/products/admin/pending` |
| PATCH | `/products/admin/{id}/verify` |
| GET | `/products/admin/search` |
| PUT | `/products/admin/update/{id}` |

### Auction Sessions and Bids

| Method | Path |
|---|---|
| GET | `/auction-sessions` |
| POST | `/auction-sessions` |
| GET | `/auction-sessions/my-joined` |
| GET | `/auction-sessions/seller/{sellerId}/active` |
| GET | `/auction-sessions/top-popular` |
| GET | `/auction-sessions/my-sessions` |
| GET | `/auction-sessions/{id}` |
| GET | `/auction-sessions/active-desc` |
| GET | `/auction-sessions/schedule-desc` |
| POST | `/auction-sessions/{id}/buy-now` |
| PUT | `/auction-sessions/{id}/cancel` |
| PUT | `/auction-sessions/{id}/reactivate` |
| PUT | `/auction-sessions/update/{id}` |
| GET | `/auction-sessions/admin/search` |
| PUT | `/auction-sessions/admin/{id}` |
| GET | `/auction-sessions/{sessionId}/bids` |
| POST | `/auction-sessions/{sessionId}/bids` |
| GET | `/auction-sessions/count/{productId}` |

### Invoices and Payments

| Method | Path |
|---|---|
| GET | `/invoices/my-invoices` |
| GET | `/invoices/my-sales` |
| GET | `/invoices/my-listing-fees` |
| GET | `/invoices/sold-invoices` |
| GET | `/invoices/{id}` |
| POST | `/invoices/{id}/report-nonpayment` |
| POST | `/invoices/{id}/ship` |
| POST | `/invoices/{id}/confirm` |
| GET | `/invoices/dispute/{invoiceId}` |
| GET | `/invoices/seller-stats` |
| POST | `/invoices/{id}/dispute` |
| GET | `/invoices/admin/invoice/{invoiceId}` |
| POST | `/invoices/admin/disputes/{id}/resolve` |
| GET | `/invoices/admin/disputes` |
| PUT | `/invoices/admin/update/{id}` |
| GET | `/invoices/admin/search` |
| GET | `/payments/vn-pay` |
| GET | `/payments/vn-pay-callback` |

### Addresses, Feedback, and Notifications

| Method | Path |
|---|---|
| GET | `/address` |
| POST | `/address` |
| GET | `/address/{id}` |
| PUT | `/address/{id}` |
| DELETE | `/address/{id}` |
| PATCH | `/address/{id}/default` |
| POST | `/feedback/invoice/{invoiceId}` |
| PUT | `/feedback/{id}` |
| GET | `/feedback/my-total-feedback` |
| GET | `/feedback/public/{userId}` |
| GET | `/notifications` |
| PATCH | `/notifications/{id}/read` |
| GET | `/notifications/unread-count` |

### Categories, Images, Roles, and Admin

| Method | Path |
|---|---|
| GET | `/categories` |
| POST | `/categories` |
| GET | `/categories/{id}` |
| POST | `/categories/{id}` |
| DELETE | `/categories/{id}` |
| POST | `/images/upload` |
| DELETE | `/images/{id}` |
| GET | `/roles` |
| POST | `/roles` |
| DELETE | `/roles/{role}` |
| GET | `/roles/permissions` |
| POST | `/roles/permissions` |
| DELETE | `/roles/permissions/{permission}` |
| GET | `/admin/settings` |
| PUT | `/admin/settings/{key}` |
| GET | `/admin/statistics` |
| GET | `/admin/logs` |

## Request/Response Examples

### Login

`POST /auth/authenticate`

```json
{
  "username": "seller01",
  "password": "P@ssw0rd123"
}
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": null,
  "authenticated": true
}
```

### Refresh Token

`POST /auth/refresh-token`

Request uses the `refresh_token` cookie.

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": null,
  "authenticated": true
}
```

### Create Product

`POST /products`

```json
{
  "name": "Seiko mechanical watch",
  "description": "Used watch in good working condition.",
  "startPrice": 1200000,
  "categoryId": 3,
  "attributes": "{\"brand\":\"Seiko\"}",
  "imageIds": [1, 2]
}
```

```json
{
  "id": 10,
  "name": "Seiko mechanical watch",
  "startPrice": 1200000,
  "status": "WAITING_FOR_APPROVAL",
  "isActive": true
}
```

### Paginated List

`GET /products?page=1&size=10`

```json
{
  "currentPage": 1,
  "totalPages": 5,
  "pageSize": 10,
  "totalElements": 48,
  "data": []
}
```

### Create Auction Session

`POST /auction-sessions`

```json
{
  "productId": 10,
  "startTime": "2026-06-01T09:00:00",
  "endTime": "2026-06-01T12:00:00",
  "reservePrice": 3000000,
  "buyNowPrice": 5000000
}
```

```json
{
  "message": "Auction session created successfully.",
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "sessionDetails": {
    "id": 100,
    "status": "WAITING_PAYMENT",
    "currentPrice": 1200000
  }
}
```

### Place Bid

`POST /auction-sessions/{sessionId}/bids`

```json
{
  "amount": 1500000
}
```

```json
{
  "id": 501,
  "displayedAmount": 1500000,
  "bidTime": "2026-06-01T09:10:00",
  "auctionSessionId": 100
}
```

### VNPay Payment

`GET /payments/vn-pay?invoiceId=9001&addressId=20`

```text
https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...
```

`GET /payments/vn-pay-callback?...`

```json
{
  "code": "00",
  "message": "Payment successful.",
  "paymentTime": "20260601121500",
  "transactionId": "14587963",
  "invoiceId": "9001"
}
```

### Update Shipping

`POST /invoices/{id}/ship`

```json
{
  "trackingCode": "GHN123456789",
  "carrier": "GHN"
}
```

```json
{
  "message": "Shipping information updated successfully."
}
```

### Create Address

`POST /address`

```json
{
  "recipientName": "Nguyen Van An",
  "phoneNumber": "0901234567",
  "street": "123 Nguyen Hue",
  "ward": "Ben Nghe",
  "district": "District 1",
  "city": "Ho Chi Minh City",
  "isDefault": true
}
```

```json
{
  "id": 20,
  "recipientName": "Nguyen Van An",
  "fullAddress": "123 Nguyen Hue, Ben Nghe, District 1, Ho Chi Minh City",
  "isDefault": true
}
```

### Feedback

`POST /feedback/invoice/{invoiceId}`

```json
{
  "rating": "POSITIVE",
  "comment": "The seller shipped the item as described."
}
```

```json
{
  "message": "Feedback created successfully."
}
```

### Upload Image

`POST /images/upload`

Content-Type: `multipart/form-data`

```text
file=<binary>
```

```json
{
  "id": 1,
  "publicId": "auction/products/seiko_001",
  "url": "https://res.cloudinary.com/demo/image/upload/v1/seiko.jpg"
}
```

## Error Codes

| Code | Meaning |
|---|---|
| 200 | Success with response body. |
| 204 | Success with no response body. |
| 400 | Invalid request or validation error. |
| 401 | Not authenticated, missing token, or invalid token. |
| 403 | Permission denied for the requested operation. |
| 404 | Resource not found. |
| 409 | Data conflict, for example a resource already exists. |
| 500 | Server error or business error not mapped to a specific status. |

Common error response:

```json
{
  "status": 400,
  "message": "Start price must be greater than 0",
  "timestamp": "2026-05-15T10:30:00",
  "exception": "RuntimeException"
}
```
