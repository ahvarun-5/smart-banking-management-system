# Smart Banking Management System - REST API Documentation

## 1. Overview
The **Smart Banking Management System** exposes RESTful endpoints following standard HTTP status codes, JSON payload formatting, and stateless JWT Bearer token authentication.

- **Base URL**: `http://localhost:8080/api`
- **Authentication**: `Authorization: Bearer <jwt-token>`
- **Content-Type**: `application/json`

---

## 2. Global Response Formats

### Success Response (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

### Error Response (`ErrorResponse`)
```json
{
  "timestamp": "2026-11-15T20:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error explanation",
  "path": "/api/transactions/withdraw",
  "validationErrors": {
    "amount": "Deposit amount must be positive"
  }
}
```

### Common HTTP Status Codes
| Code | Meaning | Typical Scenario |
| :--- | :--- | :--- |
| `200 OK` | Success | Successful read or update operation |
| `201 Created` | Created | Successful user registration or account creation |
| `400 Bad Request` | Validation / Business Logic Error | Insufficient balance, invalid input, negative amounts |
| `401 Unauthorized` | Authentication Failure | Missing, expired, or malformed JWT token; bad credentials |
| `403 Forbidden` | Authorization Failure | Customer trying to access admin endpoint or unowned account |
| `404 Not Found` | Resource Missing | Non-existent account, transaction, or customer ID |
| `409 Conflict` | Resource Collision | Registering with an already-registered email address |
| `500 Server Error` | Unexpected Server Error | Unhandled server exceptions |

---

## 3. Authentication Endpoints (`/api/auth`)

### 3.1. Register Customer
- **Endpoint**: `POST /api/auth/register`
- **Access**: Public
- **Request Body**:
```json
{
  "fullName": "Aarav Patel",
  "email": "aarav.patel@example.com",
  "phoneNumber": "9876543210",
  "address": "45 Marine Drive, Mumbai",
  "password": "Password@123",
  "confirmPassword": "Password@123"
}
```
- **Response (201 Created)**:
```json
{
  "success": true,
  "message": "Registration successful! Welcome to Smart Bank.",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "fullName": "Aarav Patel",
      "email": "aarav.patel@example.com",
      "phoneNumber": "9876543210",
      "address": "45 Marine Drive, Mumbai",
      "role": "ROLE_CUSTOMER",
      "enabled": true,
      "createdAt": "2026-11-15T20:30:00"
    }
  }
}
```

### 3.2. User Login
- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
```json
{
  "email": "aarav.patel@example.com",
  "password": "Password@123"
}
```
- **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Login successful!",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "fullName": "Aarav Patel",
      "email": "aarav.patel@example.com",
      "role": "ROLE_CUSTOMER"
    }
  }
}
```

### 3.3. Current User Profile
- **Endpoint**: `GET /api/auth/me`
- **Access**: Authenticated (`ROLE_CUSTOMER` or `ROLE_ADMIN`)
- **Headers**: `Authorization: Bearer <token>`
- **Response (200 OK)**:
```json
{
  "success": true,
  "message": "User profile fetched successfully",
  "data": {
    "id": 1,
    "fullName": "Aarav Patel",
    "email": "aarav.patel@example.com",
    "phoneNumber": "9876543210",
    "address": "45 Marine Drive, Mumbai",
    "role": "ROLE_CUSTOMER",
    "enabled": true
  }
}
```

### 3.4. Logout
- **Endpoint**: `POST /api/auth/logout`
- **Access**: Authenticated
- **Headers**: `Authorization: Bearer <token>`
- **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

---

## 4. Account Management Endpoints (`/api/accounts`)

### 4.1. Open New Account
- **Endpoint**: `POST /api/accounts`
- **Access**: `ROLE_CUSTOMER`
- **Request Body**:
```json
{
  "accountType": "SAVINGS",
  "initialDeposit": 1000.00
}
```
- **Response (201 Created)**:
```json
{
  "id": 10,
  "accountNumber": "SB-1002345678",
  "accountType": "SAVINGS",
  "balance": 1000.00,
  "accountStatus": "ACTIVE",
  "createdAt": "2026-11-15T20:30:00"
}
```

### 4.2. List Customer Accounts
- **Endpoint**: `GET /api/accounts`
- **Access**: `ROLE_CUSTOMER`
- **Response (200 OK)**:
```json
[
  {
    "id": 10,
    "accountNumber": "SB-1002345678",
    "accountType": "SAVINGS",
    "balance": 1000.00,
    "accountStatus": "ACTIVE",
    "createdAt": "2026-11-15T20:30:00"
  }
]
```

### 4.3. Get Account by Account Number
- **Endpoint**: `GET /api/accounts/number/{accountNumber}`
- **Access**: `ROLE_CUSTOMER` (Account owner only)
- **Response (200 OK)**: Returns account details JSON.

---

## 5. Transaction Endpoints (`/api/transactions`)

### 5.1. Deposit Money
- **Endpoint**: `POST /api/transactions/deposit`
- **Access**: `ROLE_CUSTOMER` (Account owner only)
- **Request Body**:
```json
{
  "accountNumber": "SB-1002345678",
  "amount": 500.00,
  "description": "Salary deposit"
}
```
- **Response (200 OK)**:
```json
{
  "id": 101,
  "transactionReference": "TXN-20261115-DEP01",
  "accountNumber": "SB-1002345678",
  "relatedAccountNumber": null,
  "transactionType": "DEPOSIT",
  "amount": 500.00,
  "transactionStatus": "SUCCESS",
  "description": "Salary deposit",
  "transactionDate": "2026-11-15T20:32:00"
}
```

### 5.2. Withdraw Money
- **Endpoint**: `POST /api/transactions/withdraw`
- **Access**: `ROLE_CUSTOMER` (Account owner only)
- **Request Body**:
```json
{
  "accountNumber": "SB-1002345678",
  "amount": 200.00,
  "description": "ATM withdrawal"
}
```
- **Response (200 OK)**: Returns `TransactionResponse` with type `WITHDRAWAL`.

### 5.3. Fund Transfer
- **Endpoint**: `POST /api/transactions/transfer`
- **Access**: `ROLE_CUSTOMER` (Sender account owner only)
- **Request Body**:
```json
{
  "fromAccountNumber": "SB-1002345678",
  "toAccountNumber": "CA-2003456789",
  "amount": 300.00,
  "description": "Monthly rent share"
}
```
- **Response (200 OK)**: Returns `TransactionResponse` with type `TRANSFER_OUT`.

### 5.4. Customer Transaction History
- **Endpoint**: `GET /api/transactions?type=DEPOSIT&startDate=...&endDate=...&reference=...&page=0&size=10`
- **Access**: `ROLE_CUSTOMER`
- **Response (200 OK)**: Spring Data `Page<TransactionResponse>`.

---

## 6. Customer Profile Endpoints (`/api/customers`)

### 6.1. Get Profile
- **Endpoint**: `GET /api/customers/profile`
- **Access**: `ROLE_CUSTOMER`
- **Response (200 OK)**: `UserResponse` with full name, email, phone, and address.

### 6.2. Update Profile
- **Endpoint**: `PUT /api/customers/profile`
- **Access**: `ROLE_CUSTOMER`
- **Request Body**:
```json
{
  "fullName": "Aarav K. Patel",
  "phoneNumber": "9876500000",
  "address": "100 Marine Lines, Mumbai"
}
```
- **Response (200 OK)**: Updated `UserResponse`.

### 6.3. Change Password
- **Endpoint**: `PUT /api/customers/change-password`
- **Access**: `ROLE_CUSTOMER`
- **Request Body**:
```json
{
  "currentPassword": "Password@123",
  "newPassword": "NewPassword@456",
  "confirmNewPassword": "NewPassword@456"
}
```
- **Response (200 OK)**: `{"success": true, "message": "Password updated successfully"}`.

---

## 7. Administrator Endpoints (`/api/admin/*`)
All `/api/admin/*` endpoints strictly require JWT Bearer authentication with `ROLE_ADMIN` authority.

### 7.1. Dashboard Analytics
- **Endpoint**: `GET /api/admin/dashboard`
- **Response (200 OK)**:
```json
{
  "totalCustomers": 12,
  "totalActiveCustomers": 11,
  "totalAccounts": 18,
  "totalActiveAccounts": 16,
  "totalAccountBalances": 345000.00,
  "totalTransactions": 142,
  "totalDepositVolume": 450000.00,
  "totalWithdrawalVolume": 105000.00,
  "totalTransferVolume": 78000.00
}
```

### 7.2. Customer Management
- `GET /api/admin/customers?query=...&page=0&size=10`: Search and list all customers.
- `GET /api/admin/customers/{id}`: Detailed customer portfolio with all owned accounts and total combined balance.
- `PATCH /api/admin/customers/{id}/status`: Enable/disable customer access.
  ```json
  { "enabled": false, "reason": "KYC review pending" }
  ```

### 7.3. Account Management
- `GET /api/admin/accounts?type=SAVINGS&status=ACTIVE&query=...&page=0&size=10`: Search and filter accounts.
- `PATCH /api/admin/accounts/{id}/status`: Change account status (`ACTIVE`, `INACTIVE`, `FROZEN`).
  ```json
  { "status": "FROZEN", "reason": "Suspicious transaction review" }
  ```

### 7.4. System-Wide Transaction Audit
- `GET /api/admin/transactions?type=...&status=...&accountNumber=...&customerQuery=...&startDate=...&endDate=...&reference=...&page=0&size=20`:
  Complete system transaction audit with dynamic multi-criteria filtering, pagination, and sorting.
