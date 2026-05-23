# DTO Requests and Responses

## Requests

### AddressRequest
| Field | Type |
|---|---|
| recipientName | String |
| phoneNumber | String |
| street | String |
| ward | String |
| district | String |
| city | String |
| isDefault | Boolean |

### AdminCreationRequest
| Field | Type |
|---|---|
| username | String |
| password | String |
| firstName | String |
| lastName | String |
| email | String |
| phoneNumber | String |
| isActive | Boolean |
| roles | Set<String> |
| createdAt | LocalDateTime |

### AdminUpdateInvoiceRequest
| Field | Type |
|---|---|
| status | InvoiceStatus |
| trackingCode | String |
| carrier | String |
| recipientName | String |
| recipientPhone | String |
| shippingAddress | String |
| note | String |

### AdminUpdateRequest
| Field | Type |
|---|---|
| password | String |
| firstName | String |
| lastName | String |
| email | String |
| phoneNumber | String |
| isActive | Boolean |
| strikeCount | Integer |
| reputationScore | Integer |
| roles | Set<String> |
| updatedAt | LocalDateTime |

### AdminUpdateSessionRequest
| Field | Type |
|---|---|
| startTime | LocalDateTime |
| endTime | LocalDateTime |
| startPrice | BigDecimal |
| reservePrice | BigDecimal |
| buyNowPrice | BigDecimal |
| status | AuctionStatus |

### AuctionSessionAdminSearchRequest
| Field | Type |
|---|---|
| productName | String |
| status | AuctionStatus |
| sort | String |

### AuctionSessionRequest
| Field | Type |
|---|---|
| productId | Long |
| startTime | LocalDateTime |
| endTime | LocalDateTime |
| reservePrice | BigDecimal |
| buyNowPrice | BigDecimal |

### AuthenticationRequest
| Field | Type |
|---|---|
| username | String |
| email | String |
| password | String |

### BidRequest
| Field | Type |
|---|---|
| amount | BigDecimal |

### CategoryRequest
| Field | Type |
|---|---|
| name | String |
| description | String |

### ChangePassRequest
| Field | Type |
|---|---|
| currentPassword | String |
| newPassword | String |

### DisputeRequest
| Field | Type |
|---|---|
| reason | String |

### DisputeSearchRequest
| Field | Type |
|---|---|
| decision | String |
| sort | String |

### ExchangeTokenRequest
JSON naming: `snake_case`.

| Field | JSON Field | Type |
|---|---|---|
| code | code | String |
| clientId | client_id | String |
| clientSecret | client_secret | String |
| redirectUri | redirect_uri | String |
| grantType | grant_type | String |

### FeedbackRequest
| Field | Type |
|---|---|
| rating | FeedbackRating |
| comment | String |

### ForgotPassRequest
| Field | Type |
|---|---|
| email | String |

### IntrospectRequest
| Field | Type |
|---|---|
| token | String |

### InvoiceAdminSearchRequest
| Field | Type |
|---|---|
| keyword | String |
| status | InvoiceStatus |
| type | InvoiceType |
| sort | String |

### LogoutRequest
| Field | Type |
|---|---|
| accessToken | String |
| refreshToken | String |

### NotificationRequest
| Field | Type |
|---|---|
| message | String |
| link | String |

### OtpVerificationRequest
| Field | Type |
|---|---|
| email | String |
| otp | String |

### PasswordCreationRequest
| Field | Type |
|---|---|
| password | String |

### PermissionRequest
| Field | Type |
|---|---|
| name | String |
| description | String |

### ProductRequest
| Field | Type |
|---|---|
| name | String |
| description | String |
| startPrice | BigDecimal |
| categoryId | Long |
| attributes | String |
| imageIds | List<Integer> |

### ProductSearchRequest
| Field | Type |
|---|---|
| keyword | String |
| categoryId | Long |
| minPrice | BigDecimal |
| maxPrice | BigDecimal |
| sort | String |
| status | ProductStatus |
| sellerId | String |
| isActive | Boolean |

### ProductUpdateRequest
| Field | Type |
|---|---|
| name | String |
| description | String |
| startPrice | BigDecimal |
| categoryId | Long |
| attributes | String |
| imageIdsToAdd | List<Integer> |
| imageIdsToRemove | List<Integer> |

### ResetPassRequest
| Field | Type |
|---|---|
| email | String |
| otp | String |
| newPassword | String |

### ResolveDisputeRequest
| Field | Type |
|---|---|
| decision | DisputeDecision |
| adminNote | String |

### RoleRequest
| Field | Type |
|---|---|
| name | String |
| description | String |
| permissions | Set<String> |

### ShipInvoiceRequest
| Field | Type |
|---|---|
| trackingCode | String |
| carrier | String |

### UpdateAuctionSessionRequest
| Field | Type |
|---|---|
| startTime | LocalDateTime |
| endTime | LocalDateTime |
| startPrice | BigDecimal |
| reservePrice | BigDecimal |
| buyNowPrice | BigDecimal |

### UserCreationRequest
| Field | Type |
|---|---|
| username | String |
| firstName | String |
| lastName | String |
| password | String |
| email | String |
| phoneNumber | String |
| isActive | Boolean |
| createdAt | LocalDateTime |

### UserUpdateRequest
| Field | Type |
|---|---|
| firstName | String |
| lastName | String |
| email | String |
| phoneNumber | String |

## Responses

### AddressResponse
| Field | Type |
|---|---|
| id | Long |
| recipientName | String |
| phoneNumber | String |
| street | String |
| ward | String |
| district | String |
| city | String |
| isDefault | Boolean |
| fullAddress | String |

### AdminAuctionSessionResponse
| Field | Type |
|---|---|
| id | Long |
| startTime | LocalDateTime |
| endTime | LocalDateTime |
| startPrice | BigDecimal |
| currentPrice | BigDecimal |
| reservePrice | BigDecimal |
| buyNowPrice | BigDecimal |
| highestMaxBid | BigDecimal |
| status | AuctionStatus |
| product | SimpleProductResponse |
| highestBidder | SimpleUserResponse |
| createdAt | LocalDateTime |
| updatedAt | LocalDateTime |

### AuctionSessionResponse
| Field | Type |
|---|---|
| id | Long |
| startTime | LocalDateTime |
| endTime | LocalDateTime |
| startPrice | BigDecimal |
| currentPrice | BigDecimal |
| buyNowPrice | BigDecimal |
| status | AuctionStatus |
| product | ProductResponse |
| highestBidder | SimpleUserResponse |
| reservePriceMet | boolean |
| myMaxBid | BigDecimal |

### AuthenticationResponse
| Field | Type |
|---|---|
| accessToken | String |
| refreshToken | String |
| authenticated | boolean |

### BidResponse
| Field | Type |
|---|---|
| id | Long |
| displayedAmount | BigDecimal |
| bidTime | LocalDateTime |
| user | SimpleUserResponse |
| auctionSessionId | Long |

### CategoryResponse
| Field | Type |
|---|---|
| id | Long |
| name | String |
| description | String |

### CreateAuctionSessionResponse
| Field | Type |
|---|---|
| message | String |
| paymentUrl | String |
| sessionDetails | AuctionSessionResponse |

### DisputeResponse
| Field | Type |
|---|---|
| id | Long |
| invoiceId | Long |
| reason | String |
| decision | DisputeDecision |
| adminNote | String |
| createdAt | LocalDateTime |
| resolvedAt | LocalDateTime |

### ExchangeTokenResponse
JSON naming: `snake_case`.

| Field | JSON Field | Type |
|---|---|---|
| accessToken | access_token | String |
| expiresIn | expires_in | Long |
| refreshToken | refresh_token | String |
| scope | scope | String |
| tokenType | token_type | String |

### FeedbackDto
| Field | Type |
|---|---|
| id | Long |
| invoiceId | Long |
| fromUsername | String |
| toUsername | String |
| rating | FeedbackRating |
| comment | String |
| createdAt | LocalDateTime |
| reviewAs | String |

### IntrospectResponse
| Field | Type |
|---|---|
| valid | boolean |

### InvoiceResponse
| Field | Type |
|---|---|
| id | Long |
| user | SimpleUserResponse |
| product | SimpleProductResponse |
| auctionSessionId | Long |
| finalPrice | BigDecimal |
| status | InvoiceStatus |
| createdAt | LocalDateTime |
| dueDate | LocalDateTime |
| type | InvoiceType |
| shippingAddress | String |
| recipientName | String |
| recipientPhone | String |
| trackingCode | String |
| carrier | String |
| shippedAt | LocalDateTime |
| paymentTime | LocalDateTime |
| hasFeedback | Boolean |

### MessageResponse
| Field | Type |
|---|---|
| message | String |

### NotificationResponse
| Field | Type |
|---|---|
| id | Long |
| message | String |
| isRead | Boolean |
| link | String |
| createdAt | LocalDateTime |

### OutboundUserResponse
JSON naming: `snake_case`.

| Field | JSON Field | Type |
|---|---|---|
| id | id | String |
| email | email | String |
| verifiedEmail | verified_email | boolean |
| name | name | String |
| givenName | given_name | String |
| familyName | family_name | String |
| picture | picture | String |
| locale | locale | String |

### PageResponse<T>
| Field | Type |
|---|---|
| currentPage | int |
| totalPages | int |
| pageSize | int |
| totalElements | long |
| data | List<T> |

### PermissionResponse
| Field | Type |
|---|---|
| name | String |
| description | String |

### ProductResponse
| Field | Type |
|---|---|
| id | Long |
| name | String |
| description | String |
| startPrice | BigDecimal |
| createdAt | LocalDateTime |
| category | CategoryResponse |
| seller | SimpleUserResponse |
| status | ProductStatus |
| attributes | String |
| isActive | Boolean |
| images | Set<Image> |

### PublicUserProfileResponse
| Field | Type |
|---|---|
| id | String |
| username | String |
| firstName | String |
| lastName | String |
| reputationScore | Integer |
| createdAt | LocalDateTime |

### RoleResponse
| Field | Type |
|---|---|
| name | String |
| description | String |
| permissions | Set<PermissionResponse> |

### SellerRevenueResponse
| Field | Type |
|---|---|
| totalAuctionSessions | long |
| totalRevenue | Long |

### SimpleProductResponse
| Field | Type |
|---|---|
| id | Long |
| name | String |
| seller | SimpleUserResponse |
| startPrice | BigDecimal |
| images | Set<Image> |

### SimpleUserResponse
| Field | Type |
|---|---|
| id | String |
| username | String |
| firstName | String |
| lastName | String |
| email | String |
| phoneNumber | String |

### StatisticResponse
| Field | Type |
|---|---|
| totalUsers | long |
| activeAuctions | long |
| pendingProducts | long |
| totalRevenue | BigDecimal |
| totalGMV | BigDecimal |
| totalListingFee | BigDecimal |
| commissionRevenue | BigDecimal |

### UserProfileResponse
| Field | Type |
|---|---|
| id | String |
| username | String |
| firstName | String |
| lastName | String |
| email | String |
| phoneNumber | String |
| noPassword | boolean |
| isActive | Boolean |
| strikeCount | Integer |
| reputationScore | Integer |
| createdAt | LocalDateTime |
| roles | Set<RoleResponse> |

### UserResponse
| Field | Type |
|---|---|
| id | String |
| username | String |
| firstName | String |
| lastName | String |
| noPassword | boolean |
| email | String |
| phoneNumber | String |
| isActive | Boolean |
| createdAt | LocalDateTime |
| updatedAt | LocalDateTime |
| roles | Set<RoleResponse> |
