# RappiDrive Mobile API Documentation

**Versão da API**: v1  
**Data**: 16 de maio de 2026  
**Ambiente Base URL**: `https://api.rappidrive.com`  
**Staging URL**: `https://staging-api.rappidrive.com`  
**Documentação Swagger**: `/swagger-ui.html`

---

## Índice

1. [Introdução](#1-introdução)
2. [Autenticação e Segurança](#2-autenticação-e-segurança)
3. [Headers Obrigatórios](#3-headers-obrigatórios)
4. [Estrutura de Resposta](#4-estrutura-de-resposta)
5. [Códigos de Status HTTP](#5-códigos-de-status-http)
6. [Endpoints - Motorista (Driver)](#6-endpoints---motorista-driver)
7. [Endpoints - Passageiro (Passenger)](#7-endpoints---passageiro-passenger)
8. [Endpoints - Corrida (Trip)](#8-endpoints---corrida-trip)
9. [Endpoints - Veículo (Vehicle)](#9-endpoints---veículo-vehicle)
10. [Endpoints - Pagamento (Payment)](#10-endpoints---pagamento-payment)
11. [Endpoints - Avaliação (Rating)](#11-endpoints---avaliação-rating)
12. [Endpoints - Notificação (Notification)](#12-endpoints---notificação-notification)
13. [Áreas de Serviço (Service Areas)](#13-endpoints---áreas-de-serviço-service-areas)
14. [Aprovação de Motoristas (Admin)](#14-endpoints---aprovação-de-motoristas-admin)
15. [WebSockets e Eventos em Tempo Real](#15-websockets-e-eventos-em-tempo-real)
16. [Geolocalização e MapBox](#16-geolocalização-e-mapbox)
17. [Fluxos Principais](#17-fluxos-principais)
18. [Tratamento de Erros](#18-tratamento-de-erros)
19. [Paginação](#19-paginação)
20. [Rate Limiting](#20-rate-limiting)
21. [Versionamento](#21-versionamento)
22. [Ambiente de Desenvolvimento](#22-ambiente-de-desenvolvimento)
23. [Boas Práticas](#23-boas-práticas)
24. [SDKs e Bibliotecas Recomendadas](#24-sdks-e-bibliotecas-recomendadas)

---

## 1. Introdução

RappiDrive é uma plataforma white-label de mobilidade urbana. A API REST fornece todos os endpoints necessários para construir aplicativos móveis para **motoristas** e **passageiros**.

### 1.1 Características

- ✅ **RESTful** com JSON
- ✅ **OAuth 2.0** + JWT para autenticação (via Keycloak)
- ✅ **Multi-tenancy** (suporta múltiplas marcas/empresas)
- ✅ **PostGIS** para buscas geoespaciais
- ✅ **OpenAPI 3.0** (Swagger)
- ✅ **HTTPS** obrigatório em produção
- 🔜 **WebSockets** para eventos em tempo real (planejado)

### 1.2 Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────┐
│              Mobile Apps (iOS/Android)              │
├─────────────────────────────────────────────────────┤
│  Driver App           │       Passenger App         │
│  - Login              │       - Login               │
│  - Accept trips       │       - Request ride        │
│  - Navigation         │       - Track driver        │
│  - Earnings           │       - Payment             │
└────────┬──────────────┴─────────────┬───────────────┘
         │                             │
         │        HTTPS/REST           │
         ├─────────────────────────────┤
         ▼                             ▼
┌──────────────────────────────────────────────────────┐
│         API Gateway (Kong/Nginx) + Load Balancer     │
└────────┬─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│     Spring Boot Backend (Hexagonal Architecture)    │
│  - Virtual Threads (Java 21)                        │
│  - Multi-tenant support                             │
│  - Event-driven (Outbox Pattern)                    │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────────────┐
│      PostgreSQL 16 + PostGIS 3.4 (Geospatial)      │
└────────────────────────────────────────────────────┘
```

---

## 2. Autenticação e Segurança

A autenticação é gerenciada pelo **Keycloak** (servidor OAuth 2.0/OIDC externo ao backend). O app mobile não se comunica diretamente com o backend para login — ele obtém o token JWT do Keycloak e o usa nas chamadas à API.

### 2.1 Fluxo de Autenticação — Authorization Code + PKCE (recomendado para mobile)

O fluxo recomendado para aplicativos iOS e Android é **Authorization Code com PKCE**, que evita o envio de credenciais diretamente pelo app:

```
1. App gera code_verifier e code_challenge (SHA-256)

2. App abre o browser/webview para:
   GET https://auth.rappidrive.com/realms/{realm}/protocol/openid-connect/auth
     ?client_id=mobile-app
     &response_type=code
     &redirect_uri=rappidrive://callback
     &scope=openid profile
     &code_challenge={code_challenge}
     &code_challenge_method=S256

3. Usuário autentica no Keycloak (tela de login gerenciada pelo Keycloak)

4. Keycloak redireciona para:
   rappidrive://callback?code={authorization_code}

5. App troca o code pelo token:
   POST https://auth.rappidrive.com/realms/{realm}/protocol/openid-connect/token
   Content-Type: application/x-www-form-urlencoded

   grant_type=authorization_code
   &client_id=mobile-app
   &redirect_uri=rappidrive://callback
   &code={authorization_code}
   &code_verifier={code_verifier}

6. Keycloak retorna:
   {
     "access_token": "eyJhbGciOiJSUzI1NiIsInR...",
     "token_type": "Bearer",
     "expires_in": 300,
     "refresh_token": "eyJhbGciOiJIUzI1NiIsInR...",
     "refresh_expires_in": 1800,
     "scope": "openid profile"
   }

7. App usa access_token em todas as chamadas à API:
   Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR...
```

O JWT retornado contém o claim `tenant_id`, que o backend usa para resolver o contexto de tenant.

### 2.2 Refresh Token

```http
POST https://auth.rappidrive.com/realms/{realm}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&client_id=mobile-app
&refresh_token={refresh_token}
```

**Resposta**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR...",
  "refresh_expires_in": 1800
}
```

### 2.3 Logout / Revogar Token

```http
POST https://auth.rappidrive.com/realms/{realm}/protocol/openid-connect/logout
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer {access_token}

client_id=mobile-app
&refresh_token={refresh_token}
```

### 2.4 Discovery Endpoint (metadados OIDC)

```
GET https://auth.rappidrive.com/realms/{realm}/.well-known/openid-configuration
```

Retorna todos os endpoints do Keycloak (token, auth, logout, etc.).

---

## 3. Headers Obrigatórios

Todas as requisições autenticadas devem incluir:

```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
Accept: application/json
X-Tenant-ID: <UUID>
X-Request-Id: <UUID>  # Para tracking
X-Platform: ios | android
X-App-Version: 1.2.3
```

> **Atenção**: O header de tenant é `X-Tenant-ID` na maioria dos endpoints. O endpoint de notificações usa `X-Tenant-Id` (capitalização diferente — comportamento atual do código). O backend rejeita com 400 se o header estiver ausente, malformado, ou se o tenant não existir no banco.

**Exemplo**:
```http
GET /api/v1/drivers/650e8400-e29b-41d4-a716-446655440001
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR...
X-Tenant-ID: 550e8400-e29b-41d4-a716-446655440000
X-Request-Id: 123e4567-e89b-12d3-a456-426614174000
X-Platform: ios
X-App-Version: 1.2.3
```

---

## 4. Estrutura de Resposta

### 4.1 Resposta de Sucesso (200/201)

```json
{
  "id": "650e8400-e29b-41d4-a716-446655440001",
  "fullName": "João Silva",
  "email": "joao@email.com",
  "status": "ACTIVE",
  "createdAt": "2026-01-14T12:30:00Z"
}
```

### 4.2 Resposta de Erro (400/404/500)

```json
{
  "timestamp": "2026-01-14T12:30:45Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid CPF format",
  "path": "/api/v1/drivers",
  "traceId": "123e4567-e89b-12d3-a456-426614174000",
  "details": {
    "field": "cpf",
    "rejectedValue": "123",
    "reason": "CPF must have 11 digits"
  }
}
```

### 4.3 Resposta de Validação (422)

```json
{
  "timestamp": "2026-01-14T12:30:45Z",
  "status": 422,
  "error": "Validation Failed",
  "message": "Input validation errors",
  "path": "/api/v1/drivers",
  "errors": [
    {
      "field": "email",
      "message": "Email format is invalid"
    },
    {
      "field": "cpf",
      "message": "CPF already registered"
    }
  ]
}
```

---

## 5. Códigos de Status HTTP

| Código | Significado | Quando Usar |
|--------|-------------|-------------|
| **200** | OK | Request processada com sucesso |
| **201** | Created | Recurso criado (POST) |
| **204** | No Content | Sucesso sem retorno de dados (DELETE) |
| **400** | Bad Request | Dados inválidos enviados |
| **401** | Unauthorized | Token ausente ou inválido |
| **403** | Forbidden | Usuário sem permissão |
| **404** | Not Found | Recurso não encontrado |
| **409** | Conflict | Conflito (ex: email duplicado) |
| **422** | Unprocessable Entity | Validação de negócio falhou |
| **429** | Too Many Requests | Rate limit excedido |
| **500** | Internal Server Error | Erro no servidor |
| **503** | Service Unavailable | Serviço temporariamente indisponível |

---

## 6. Endpoints - Motorista (Driver)

### 6.1 Criar Motorista

```http
POST /api/v1/drivers
Content-Type: application/json
```

> **Nota**: O `tenantId` no body deve corresponder ao valor enviado no header `X-Tenant-ID`. O backend usa o header para isolamento; o campo no body é usado para associação do registro.

**Request Body**:
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "fullName": "João Silva",
  "email": "joao@email.com",
  "cpf": "12345678901",
  "phone": "+5511999999999",
  "driverLicense": {
    "number": "12345678901",
    "category": "B",
    "issueDate": "2020-01-15",
    "expirationDate": "2030-01-15"
  },
  "documentUrls": [
    "https://docs.exemplo.com/cnh.jpg",
    "https://docs.exemplo.com/crlv.jpg"
  ]
}
```

**Response (201 Created)**:
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440001",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "fullName": "João Silva",
  "email": "joao@email.com",
  "cpf": "123.456.789-01",
  "phone": "+55 11 99999-9999",
  "status": "INACTIVE",
  "driverLicense": {
    "number": "12345678901",
    "category": "B",
    "issueDate": "2020-01-15",
    "expirationDate": "2030-01-15"
  },
  "createdAt": "2026-01-14T12:30:00Z",
  "updatedAt": "2026-01-14T12:30:00Z"
}
```

> **Nota**: A criação do motorista automaticamente abre um processo de aprovação (`DriverApproval`). O motorista só pode ser ativado após aprovação por um administrador via `POST /api/v1/admin/approvals/{approvalId}/approve`.

### 6.2 Buscar Motorista por ID

```http
GET /api/v1/drivers/{id}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**:
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440001",
  "fullName": "João Silva",
  "email": "joao@email.com",
  "status": "ACTIVE",
  "currentLocation": {
    "latitude": -23.5505,
    "longitude": -46.6333
  },
  "rating": {
    "average": 4.8,
    "count": 1523
  },
  "totalTrips": 2341,
  "createdAt": "2026-01-01T10:00:00Z"
}
```

### 6.3 Buscar Dados do Motorista Atual (Me)

```http
GET /api/v1/drivers/me?tenantId={tenantId}
Authorization: Bearer <TOKEN>
```

**Query Parameters**:
- `tenantId` (required): UUID do tenant

**Response (200 OK)**: Objeto `DriverResponse` completo (mesmo formato de 6.2).

> **Nota**: O `keycloakId` é extraído automaticamente do JWT — o endpoint retorna o perfil do motorista vinculado à identidade do token atual.

### 6.4 Atualizar Localização do Motorista

```http
PUT /api/v1/drivers/{id}/location
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "location": {
    "latitude": -23.5505,
    "longitude": -46.6333
  }
}
```

**Response (200 OK)**: Objeto `DriverResponse` completo (mesmo formato de 6.2).

**⚠️ Importante**: Enviar localização a cada 5-10 segundos quando motorista estiver ACTIVE ou em corrida.

### 6.5 Ativar Motorista (Disponível para Corridas)

```http
PUT /api/v1/drivers/{id}/activate
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto `DriverResponse` completo (mesmo formato de 6.2) com `status: "ACTIVE"`.

### 6.6 Desativar Motorista (Offline)

> **Status**: 🔜 Não implementado. Para colocar o motorista offline, interrompa o envio de localização. Um mecanismo de timeout/desativação automática está planejado.

### 6.7 Buscar Motoristas Disponíveis (Próximos)

```http
GET /api/v1/drivers/nearby?tenantId={tenantId}&latitude=-23.5505&longitude=-46.6333&radiusKm=5
Authorization: Bearer <TOKEN>
```

**Query Parameters**:
- `tenantId` (required): UUID do tenant
- `latitude` (required): Latitude do ponto de partida
- `longitude` (required): Longitude do ponto de partida
- `radiusKm` (optional, default: 5.0): Raio de busca em km

**Response (200 OK)**:
```json
[
  {
    "id": "650e8400-e29b-41d4-a716-446655440001",
    "fullName": "João Silva",
    "currentLocation": {
      "latitude": -23.5505,
      "longitude": -46.6333
    },
    "rating": {
      "average": 4.8,
      "count": 1523
    }
  }
]
```

---

## 7. Endpoints - Passageiro (Passenger)

### 7.1 Criar Passageiro

```http
POST /api/v1/passengers
Content-Type: application/json
```

**Request Body**:
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "fullName": "Maria Silva",
  "email": "maria@email.com",
  "phone": "+5511988888888"
}
```

**Response (201 Created)**:
```json
{
  "id": "750e8400-e29b-41d4-a716-446655440002",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "fullName": "Maria Silva",
  "email": "maria@email.com",
  "phone": "+55 11 98888-8888",
  "status": "ACTIVE",
  "rating": {
    "average": 5.0,
    "count": 0
  },
  "totalTrips": 0,
  "createdAt": "2026-01-14T12:30:00Z"
}
```

### 7.2 Buscar Passageiro por ID

```http
GET /api/v1/passengers/{id}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Mesmo formato de 7.1

### 7.3 Buscar Dados do Passageiro Atual (Me)

```http
GET /api/v1/passengers/me?tenantId={tenantId}
Authorization: Bearer <TOKEN>
```

**Query Parameters**:
- `tenantId` (required): UUID do tenant

**Response (200 OK)**: Objeto `PassengerResponse` completo (mesmo formato de 7.1).

> **Nota**: O `keycloakId` é extraído automaticamente do JWT. Requer que a identidade Keycloak já esteja vinculada a um perfil de passageiro.

### 7.4 Atualizar Perfil do Passageiro

> **Status**: 🔜 Não implementado.

---

## 8. Endpoints - Corrida (Trip)

### 8.1 Solicitar Corrida

```http
POST /api/v1/trips
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "passengerId": "750e8400-e29b-41d4-a716-446655440002",
  "origin": {
    "latitude": -23.5505,
    "longitude": -46.6333,
    "address": "Rua das Flores, 123 - São Paulo, SP"
  },
  "destination": {
    "latitude": -23.5605,
    "longitude": -46.6555,
    "address": "Av. Paulista, 1000 - São Paulo, SP"
  },
  "vehicleType": "STANDARD",
  "paymentMethod": "CREDIT_CARD"
}
```

**Response (201 Created)**:
```json
{
  "id": "850e8400-e29b-41d4-a716-446655440003",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "passengerId": "750e8400-e29b-41d4-a716-446655440002",
  "status": "REQUESTED",
  "origin": {
    "latitude": -23.5505,
    "longitude": -46.6333,
    "address": "Rua das Flores, 123 - São Paulo, SP"
  },
  "destination": {
    "latitude": -23.5605,
    "longitude": -46.6555,
    "address": "Av. Paulista, 1000 - São Paulo, SP"
  },
  "estimatedFare": {
    "amount": 15.50,
    "currency": "BRL"
  },
  "estimatedDistanceKm": 5.2,
  "estimatedDurationMinutes": 12,
  "requestedAt": "2026-01-14T12:40:00Z"
}
```

### 8.2 Buscar Corrida por ID

```http
GET /api/v1/trips/{id}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**:
```json
{
  "id": "850e8400-e29b-41d4-a716-446655440003",
  "status": "DRIVER_ASSIGNED",
  "passenger": {
    "id": "750e8400-e29b-41d4-a716-446655440002",
    "fullName": "Maria Silva",
    "phone": "+55 11 98888-8888",
    "rating": 5.0
  },
  "driver": {
    "id": "650e8400-e29b-41d4-a716-446655440001",
    "fullName": "João Silva",
    "phone": "+55 11 99999-9999",
    "rating": 4.8,
    "currentLocation": {
      "latitude": -23.5505,
      "longitude": -46.6333
    },
    "vehicle": {
      "brand": "Toyota",
      "model": "Corolla",
      "color": "Preto",
      "licensePlate": "ABC-1234"
    }
  },
  "origin": {
    "latitude": -23.5505,
    "longitude": -46.6333,
    "address": "Rua das Flores, 123"
  },
  "destination": {
    "latitude": -23.5605,
    "longitude": -46.6555,
    "address": "Av. Paulista, 1000"
  },
  "estimatedFare": {
    "amount": 15.50,
    "currency": "BRL"
  },
  "actualFare": null,
  "estimatedDistanceKm": 5.2,
  "estimatedDurationMinutes": 12,
  "requestedAt": "2026-01-14T12:40:00Z",
  "assignedAt": "2026-01-14T12:42:00Z",
  "startedAt": null,
  "completedAt": null
}
```

### 8.3 Atribuir Motorista à Corrida

```http
PUT /api/v1/trips/{id}/assign-driver
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "driverId": "650e8400-e29b-41d4-a716-446655440001"
}
```

**Response (200 OK)**: Objeto `TripResponse` completo (mesmo formato de 8.2) com `status: "DRIVER_ASSIGNED"`.

### 8.4 Iniciar Corrida

```http
PUT /api/v1/trips/{id}/start
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto `TripResponse` completo com `status: "IN_PROGRESS"`.

### 8.5 Completar Corrida (sem cálculo automático)

```http
PUT /api/v1/trips/{id}/complete
Authorization: Bearer <TOKEN>
```

Completa a corrida sem processar pagamento. Use `8.5.1` para o fluxo integrado com cálculo de tarifa e pagamento automáticos.

**Response (200 OK)**: Objeto `TripResponse` com `status: "COMPLETED"`.

### 8.5.1 Completar Corrida com Pagamento

```http
POST /api/v1/trips/{id}/complete-with-payment
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "actualDistanceKm": 5.8,
  "actualDurationMinutes": 15,
  "paymentMethod": "CREDIT_CARD"
}
```

**Response (200 OK)**:
```json
{
  "trip": {
    "id": "850e8400-e29b-41d4-a716-446655440003",
    "status": "COMPLETED",
    "completedAt": "2026-01-14T13:00:00Z"
  },
  "fare": {
    "id": "950e8400-e29b-41d4-a716-446655440004",
    "baseFare": 5.00,
    "distanceFare": 14.50,
    "timeFare": 2.50,
    "totalFare": 22.00,
    "currency": "BRL"
  },
  "payment": {
    "id": "a50e8400-e29b-41d4-a716-446655440005",
    "amount": 22.00,
    "method": "CREDIT_CARD",
    "status": "COMPLETED",
    "processedAt": "2026-01-14T13:00:05Z"
  }
}
```

### 8.5.2 Buscar Detalhes de Pagamento da Corrida

```http
GET /api/v1/trips/{id}/payment-details
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Mesmo formato de 8.5.1.

### 8.6 Cancelar Corrida

```http
POST /api/v1/trips/{id}/cancel
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "reason": "PASSENGER_WAIT_TOO_LONG",
  "additionalNotes": "Motorista demorando muito"
}
```

**Response (200 OK)**:
```json
{
  "tripId": "850e8400-e29b-41d4-a716-446655440003",
  "cancelled": true,
  "cancelledBy": "PASSENGER",
  "reason": "PASSENGER_WAIT_TOO_LONG",
  "feeCharged": {
    "amount": 5.00,
    "currency": "BRL"
  },
  "cancelledAt": "2026-01-14T12:50:00Z",
  "message": "Trip cancelled successfully. Fee charged: R$ 5,00"
}
```

**Motivos de Cancelamento**:
- Passageiro: `PASSENGER_CHANGE_OF_PLANS`, `PASSENGER_PRICE_TOO_HIGH`, `PASSENGER_WAIT_TOO_LONG`, `PASSENGER_WRONG_LOCATION`, `PASSENGER_OTHER`
- Motorista: `DRIVER_PASSENGER_NOT_FOUND`, `DRIVER_UNSAFE_LOCATION`, `DRIVER_VEHICLE_ISSUE`, `DRIVER_OTHER`

**Política de Tarifa de Cancelamento**:
- **Status REQUESTED**:
  - < 5 minutos: grátis
  - ≥ 5 minutos: R$ 5,00
- **Status DRIVER_ASSIGNED**:
  - < 2 minutos (desde atribuição): grátis
  - ≥ 2 minutos: R$ 8,00
- **Motorista**: sempre grátis

### 8.7 Listar Corridas do Passageiro

> **Status**: 🔜 Não implementado.

### 8.8 Listar Corridas do Motorista

> **Status**: 🔜 Não implementado.

---

## 9. Endpoints - Veículo (Vehicle)

### 9.1 Cadastrar Veículo

```http
POST /api/v1/vehicles
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "driverId": "650e8400-e29b-41d4-a716-446655440001",
  "licensePlate": "ABC-1234",
  "brand": "Toyota",
  "model": "Corolla",
  "year": 2023,
  "color": "Preto",
  "vehicleType": "STANDARD"
}
```

**Response (201 Created)**:
```json
{
  "id": "b50e8400-e29b-41d4-a716-446655440006",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "driverId": "650e8400-e29b-41d4-a716-446655440001",
  "licensePlate": "ABC-1234",
  "brand": "Toyota",
  "model": "Corolla",
  "year": 2023,
  "color": "Preto",
  "vehicleType": "STANDARD",
  "status": "ACTIVE",
  "createdAt": "2026-01-14T12:30:00Z"
}
```

### 9.2 Buscar Veículo por ID

```http
GET /api/v1/vehicles/{id}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Mesmo formato de 9.1

### 9.3 Listar Veículos do Motorista

```http
GET /api/v1/vehicles/driver/{driverId}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**:
```json
[
  {
    "id": "b50e8400-e29b-41d4-a716-446655440006",
    "licensePlate": "ABC-1234",
    "brand": "Toyota",
    "model": "Corolla",
    "year": 2023,
    "color": "Preto",
    "vehicleType": "STANDARD",
    "status": "ACTIVE"
  }
]
```

### 9.4 Buscar Veículo Ativo do Motorista

```http
GET /api/v1/vehicles/driver/{driverId}/active
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto `VehicleResponse` do veículo atualmente ativo.

### 9.5 Atualizar Veículo

```http
PUT /api/v1/vehicles/{id}
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

### 9.6 Associar Veículo a Motorista

```http
PUT /api/v1/vehicles/{id}/assign
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "driverId": "650e8400-e29b-41d4-a716-446655440001"
}
```

**Response (200 OK)**: Objeto `VehicleResponse` atualizado.

### 9.7 Ativar Veículo

```http
PUT /api/v1/vehicles/{id}/activate
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto `VehicleResponse` com `status: "ACTIVE"`.

---

## 10. Endpoints - Pagamento (Payment)

### 10.1 Processar Pagamento

```http
POST /api/v1/payments
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "tripId": "850e8400-e29b-41d4-a716-446655440003",
  "amount": 22.00,
  "currency": "BRL",
  "method": "CREDIT_CARD",
  "cardToken": "tok_1234567890abcdef",
  "description": "Corrida São Paulo - Av. Paulista"
}
```

**Response (201 Created)**:
```json
{
  "id": "a50e8400-e29b-41d4-a716-446655440005",
  "tripId": "850e8400-e29b-41d4-a716-446655440003",
  "amount": 22.00,
  "currency": "BRL",
  "method": "CREDIT_CARD",
  "status": "COMPLETED",
  "transactionId": "ch_3MtFBaLkdIwHu7ix28a3tqPa",
  "processedAt": "2026-01-14T13:00:05Z"
}
```

### 10.2 Buscar Pagamento por ID

```http
GET /api/v1/payments/{id}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto `PaymentResponse` (mesmo formato de 10.1).

### 10.3 Buscar Pagamento por Corrida

```http
GET /api/v1/payments/trip/{tripId}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto `PaymentResponse`.

### 10.4 Solicitar Reembolso

```http
POST /api/v1/payments/refund
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "paymentId": "a50e8400-e29b-41d4-a716-446655440005",
  "amount": 22.00,
  "reason": "Trip cancelled by driver after start"
}
```

**Response (200 OK)**: Objeto `PaymentResponse` com `status: "REFUNDED"`.

---

## 11. Endpoints - Avaliação (Rating)

### 11.1 Criar Avaliação

```http
POST /api/v1/ratings?rateeId={rateeId}
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Query Parameters**:
- `rateeId` (required): UUID de quem está sendo avaliado (motorista ou passageiro)

**Request Body**:
```json
{
  "tripId": "850e8400-e29b-41d4-a716-446655440003",
  "type": "DRIVER_BY_PASSENGER",
  "score": 5,
  "comment": "Excelente motorista! Muito educado e dirigiu com segurança."
}
```

**Valores de `type`**:
- `DRIVER_BY_PASSENGER` — passageiro avalia o motorista
- `PASSENGER_BY_DRIVER` — motorista avalia o passageiro

> **Nota**: O `raterId` (quem avalia) é inferido automaticamente do JWT. O campo `score` aceita valores de 1 a 5. O `comment` é opcional (máx. 500 caracteres).

**Response (201 Created)**:
```json
{
  "id": "d50e8400-e29b-41d4-a716-446655440008",
  "tripId": "850e8400-e29b-41d4-a716-446655440003",
  "type": "DRIVER_BY_PASSENGER",
  "score": 5,
  "comment": "Excelente motorista! Muito educado e dirigiu com segurança.",
  "createdAt": "2026-01-14T13:10:00Z"
}

### 11.2 Resumo de Avaliações do Motorista

```http
GET /api/v1/ratings/drivers/{driverId}/summary
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**:
```json
{
  "averageScore": 4.8,
  "totalRatings": 1523
}
```

### 11.3 Avaliação do Passageiro

```http
GET /api/v1/ratings/passengers/{passengerId}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto com informações de rating do passageiro.

### 11.4 Avaliações de uma Corrida

```http
GET /api/v1/ratings/trips/{tripId}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto com as avaliações da corrida (passageiro → motorista e motorista → passageiro).

### 11.5 Resumo de Avaliações do Motorista (detalhado)

O endpoint `GET /api/v1/ratings/drivers/{driverId}/summary` retorna um resumo estendido incluindo distribuição por nota e últimas avaliações (não apenas média e contagem).

### 11.6 Reportar Avaliação

```http
POST /api/v1/ratings/{ratingId}/report
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "reason": "Conteúdo ofensivo ou inadequado"
}
```

**Response (204 No Content)**

---

## 12. Endpoints - Notificação (Notification)

> **Atenção**: Todos os endpoints de notificação requerem o header `X-Tenant-Id` (capitalização com 'Id', diferente da maioria dos outros endpoints que usam `X-Tenant-ID`).

### 12.1 Listar Notificações

```http
GET /api/v1/notifications?status=UNREAD&page=0&size=20
Authorization: Bearer <TOKEN>
X-Tenant-Id: <UUID>
```

**Query Parameters**:
- `status` (optional): Filtro por status — `UNREAD` para somente não lidas. Omitir retorna todas.
- `page` (default: 0): Página (zero-based)
- `size` (default: 20): Itens por página

> **Nota**: O usuário é inferido automaticamente do JWT — não envie `userId` como parâmetro.

**Response (200 OK)**:
```json
[
  {
    "id": "e50e8400-e29b-41d4-a716-446655440009",
    "type": "TRIP_ASSIGNED",
    "title": "Nova corrida disponível!",
    "message": "Passageiro Maria Silva solicitou uma corrida próxima a você.",
    "status": "UNREAD",
    "createdAt": "2026-01-14T12:40:30Z"
  }
]
```

### 12.2 Contagem de Não Lidas

```http
GET /api/v1/notifications/unread-count
Authorization: Bearer <TOKEN>
X-Tenant-Id: <UUID>
```

**Response (200 OK)**:
```json
{
  "unreadCount": 3
}
```

### 12.3 Marcar Notificação como Lida

```http
PATCH /api/v1/notifications/{id}/read
Authorization: Bearer <TOKEN>
X-Tenant-Id: <UUID>
```

**Response (204 No Content)**

> **Nota**: O backend valida que a notificação pertence ao usuário autenticado antes de marcá-la como lida.

### 12.4 Enviar Notificação (Admin)

```http
POST /api/v1/notifications
Authorization: Bearer <TOKEN>
X-Tenant-Id: <UUID>
Content-Type: application/json
```

> **Requer role `ADMIN`**. Endpoint para envio de notificações pelo sistema/administrador — não é destinado ao app mobile de usuário final.

### 12.5 Registrar Token de Push Notification

> **Status**: 🔜 Não implementado. Push notifications via FCM/APNs não estão integrados na v1.

---

## 13. Endpoints - Áreas de Serviço (Service Areas)

Áreas de operação do tenant. O app mobile pode usar esses endpoints para exibir zonas de cobertura e validar se uma origem/destino está dentro da área operacional.

### 13.1 Listar Todas as Áreas de Serviço

```http
GET /api/v1/service-areas
Authorization: Bearer <TOKEN>
X-Tenant-ID: <UUID>
```

**Response (200 OK)**:
```json
[
  {
    "id": "f50e8400-e29b-41d4-a716-446655440010",
    "name": "São Paulo Centro",
    "active": true
  }
]
```

### 13.2 Listar Áreas de Serviço Ativas

```http
GET /api/v1/service-areas/active
Authorization: Bearer <TOKEN>
X-Tenant-ID: <UUID>
```

**Response (200 OK)**: Lista de `ServiceAreaResponse` com `active: true`.

### 13.3 Buscar Área de Serviço por ID

```http
GET /api/v1/service-areas/{id}
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**: Objeto `ServiceAreaResponse`.

---

## 14. Endpoints - Aprovação de Motoristas (Admin)

Fluxo administrativo de aprovação. Após um motorista se cadastrar, um `DriverApproval` é criado automaticamente com status `PENDING`. Um administrador precisa aprovar ou rejeitar antes que o motorista possa ser ativado.

### 14.1 Listar Aprovações Pendentes

```http
GET /api/v1/admin/approvals/pending?page=0&size=10
Authorization: Bearer <TOKEN>
```

**Response (200 OK)**:
```json
{
  "content": [
    {
      "approvalId": "...",
      "driverId": "650e8400-e29b-41d4-a716-446655440001",
      "driverName": "João Silva",
      "submittedAt": "2026-01-14T12:30:00Z",
      "status": "PENDING"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 3,
  "totalPages": 1
}
```

### 14.2 Aprovar Motorista

```http
POST /api/v1/admin/approvals/{approvalId}/approve
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "adminId": "admin-uuid-opcional-se-autenticado"
}
```

> **Nota**: `adminId` é extraído automaticamente do JWT quando o usuário está autenticado. O campo no body é ignorado nesse caso.

**Response (200 OK)**:
```json
{
  "approvalId": "...",
  "driverId": "650e8400-e29b-41d4-a716-446655440001",
  "status": "APPROVED"
}
```

### 14.3 Rejeitar Motorista

```http
POST /api/v1/admin/approvals/{approvalId}/reject
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "adminId": "admin-uuid-opcional-se-autenticado",
  "rejectionReason": "Documentos ilegíveis ou inválidos",
  "permanentBan": false
}
```

**Response (200 OK)**:
```json
{
  "approvalId": "...",
  "driverId": "650e8400-e29b-41d4-a716-446655440001",
  "status": "REJECTED",
  "rejectionReason": "Documentos ilegíveis ou inválidos"
}
```

---

## 15. WebSockets e Eventos em Tempo Real

> **Status**: 🔜 Planejado — não disponível na v1 atual. Use polling nos endpoints REST.
>
> O backend v1 não possui suporte a WebSockets. Eventos em tempo real (localização do motorista, mudança de status da corrida) devem ser obtidos via **polling** nos endpoints REST até a implementação do WebSocket.
>
> A especificação de eventos abaixo representa o contrato planejado para a v2.

### 15.1 Conectar ao WebSocket (planejado)

```javascript
// URL: wss://api.rappidrive.com/ws
const socket = new WebSocket('wss://api.rappidrive.com/ws');

socket.onopen = () => {
  socket.send(JSON.stringify({
    type: 'AUTH',
    token: 'Bearer eyJhbGciOiJSUzI1NiIsInR...',
    userId: '650e8400-e29b-41d4-a716-446655440001',
    userType: 'DRIVER'
  }));
};
```

### 15.2 Eventos para Motorista (planejado)

**15.2.1 Nova Corrida Disponível**
```json
{
  "type": "TRIP_REQUEST",
  "data": {
    "tripId": "850e8400-e29b-41d4-a716-446655440003",
    "passenger": {
      "name": "Maria Silva",
      "rating": 5.0
    },
    "origin": {
      "latitude": -23.5505,
      "longitude": -46.6333,
      "address": "Rua das Flores, 123"
    },
    "destination": {
      "latitude": -23.5605,
      "longitude": -46.6555,
      "address": "Av. Paulista, 1000"
    },
    "estimatedFare": 15.50,
    "distanceKm": 5.2,
    "expiresIn": 30
  }
}
```

**15.2.2 Corrida Cancelada pelo Passageiro**
```json
{
  "type": "TRIP_CANCELLED",
  "data": {
    "tripId": "850e8400-e29b-41d4-a716-446655440003",
    "cancelledBy": "PASSENGER",
    "reason": "PASSENGER_WAIT_TOO_LONG"
  }
}
```

### 15.3 Eventos para Passageiro (planejado)

**15.3.1 Motorista Atribuído**
```json
{
  "type": "DRIVER_ASSIGNED",
  "data": {
    "tripId": "850e8400-e29b-41d4-a716-446655440003",
    "driver": {
      "id": "650e8400-e29b-41d4-a716-446655440001",
      "name": "João Silva",
      "phone": "+55 11 99999-9999",
      "rating": 4.8,
      "currentLocation": {
        "latitude": -23.5505,
        "longitude": -46.6333
      },
      "vehicle": {
        "brand": "Toyota",
        "model": "Corolla",
        "color": "Preto",
        "licensePlate": "ABC-1234"
      }
    },
    "estimatedArrivalMinutes": 5
  }
}
```

**15.3.2 Atualização de Localização do Motorista**
```json
{
  "type": "DRIVER_LOCATION_UPDATE",
  "data": {
    "tripId": "850e8400-e29b-41d4-a716-446655440003",
    "location": {
      "latitude": -23.5510,
      "longitude": -46.6340
    },
    "heading": 180.5,
    "speed": 35.2,
    "estimatedArrivalMinutes": 4
  }
}
```

**15.3.3 Motorista Chegou**
```json
{
  "type": "DRIVER_ARRIVED",
  "data": {
    "tripId": "850e8400-e29b-41d4-a716-446655440003",
    "arrivedAt": "2026-01-14T12:44:00Z"
  }
}
```

**15.3.4 Corrida Iniciada**
```json
{
  "type": "TRIP_STARTED",
  "data": {
    "tripId": "850e8400-e29b-41d4-a716-446655440003",
    "startedAt": "2026-01-14T12:45:00Z"
  }
}
```

**15.3.5 Corrida Concluída**
```json
{
  "type": "TRIP_COMPLETED",
  "data": {
    "tripId": "850e8400-e29b-41d4-a716-446655440003",
    "completedAt": "2026-01-14T13:00:00Z",
    "fare": {
      "amount": 22.00,
      "currency": "BRL"
    }
  }
}
```

---

## 16. Geolocalização e MapBox

### 16.1 Calcular Tarifa Estimada

```http
POST /api/v1/fares/calculate
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "origin": {
    "latitude": -23.5505,
    "longitude": -46.6333
  },
  "destination": {
    "latitude": -23.5605,
    "longitude": -46.6555
  },
  "vehicleType": "STANDARD"
}
```

**Response (200 OK)**:
```json
{
  "baseFare": 5.00,
  "distanceFare": 13.00,
  "timeFare": 0.00,
  "totalFare": 18.00,
  "currency": "BRL",
  "estimatedDistanceKm": 5.2,
  "estimatedDurationMinutes": 12,
  "breakdown": {
    "baseFareDescription": "Tarifa mínima",
    "distanceFareDescription": "R$ 2,50 por km × 5.2 km",
    "timeFareDescription": "Tempo estimado não aplicado"
  }
}
```

### 16.2 Buscar Endereço por Coordenadas (Reverse Geocoding)

> **Status**: 🔜 Planejado — não disponível na v1 atual. Implemente no lado cliente usando o SDK MapBox diretamente.

### 16.3 Buscar Coordenadas por Endereço (Geocoding)

> **Status**: 🔜 Planejado — não disponível na v1 atual. Implemente no lado cliente usando o SDK MapBox diretamente.

---

## 17. Fluxos Principais

### 17.1 Fluxo Completo - Passageiro

```
1.  Passageiro abre app → GET /api/v1/passengers/{id}
2.  Passageiro seleciona origem e destino → POST /api/v1/fares/calculate
3.  Passageiro confirma → POST /api/v1/trips (status: REQUESTED)
4.  [v1] App faz polling em GET /api/v1/trips/{id} aguardando motorista
    [v2] WebSocket: aguardando evento DRIVER_ASSIGNED
5.  App exibe: Motorista João chegando em 5 min
6.  [v1] App faz polling em GET /api/v1/trips/{id} e GET /api/v1/drivers/{driverId} para atualizar posição
    [v2] WebSocket: eventos DRIVER_LOCATION_UPDATE (a cada 5s)
7.  App atualiza mapa com localização do motorista
8.  [v1] Polling detecta status DRIVER_ARRIVED
    [v2] WebSocket recebe: DRIVER_ARRIVED
9.  App mostra: "Motorista chegou!"
10. [v1] Polling detecta status IN_PROGRESS
    [v2] WebSocket recebe: TRIP_STARTED
11. App entra em modo "Em viagem"
12. [v1] Polling detecta status COMPLETED
    [v2] WebSocket recebe: TRIP_COMPLETED
13. App exibe resumo da corrida e solicita pagamento
14. Passageiro confirma → POST /api/v1/trips/{id}/complete-with-payment
15. App solicita avaliação → POST /api/v1/ratings
16. Fim
```

### 17.2 Fluxo Completo - Motorista

```
1.  Motorista abre app → GET /api/v1/drivers/{id}
2.  Motorista clica "Ficar Online" → PUT /api/v1/drivers/{id}/activate
3.  App inicia envio de localização → PUT /api/v1/drivers/{id}/location (a cada 5-10s)
4.  [v1] App faz polling aguardando corrida atribuída
    [v2] WebSocket recebe: TRIP_REQUEST
5.  App exibe: "Nova corrida! Aceitar?"
6.  Motorista aceita → PUT /api/v1/trips/{id}/assign-driver
7.  App entra em modo "Indo buscar passageiro"
8.  Motorista chega → App detecta proximidade (Geofence no cliente)
9.  [v2] App envia: POST /api/v1/trips/{id}/notify-arrival (planejado)
10. Motorista clica "Iniciar" → PUT /api/v1/trips/{id}/start
11. App entra em modo "Em viagem"
12. Motorista segue rota até destino
13. Motorista clica "Finalizar" → POST /api/v1/trips/{id}/complete-with-payment
14. App exibe: "Corrida concluída! R$ 22,00"
15. App solicita avaliação → POST /api/v1/ratings
16. Motorista fica disponível novamente
17. Fim
```

### 17.3 Fluxo de Cancelamento

**Por Passageiro**:
```
1. Passageiro cancela → POST /api/v1/trips/{id}/cancel
2. Backend calcula tarifa (se aplicável)
3. Backend processa pagamento da taxa
4. [v2] WebSocket notifica motorista: TRIP_CANCELLED
5. Motorista fica disponível novamente
```

**Por Motorista**:
```
1. Motorista cancela → POST /api/v1/trips/{id}/cancel
2. Backend registra cancelamento (sem taxa para motorista)
3. [v2] WebSocket notifica passageiro: TRIP_CANCELLED
4. Passageiro pode solicitar nova corrida
```

---

## 18. Tratamento de Erros

### 18.1 Erros Comuns

**400 Bad Request** - Header de tenant ausente ou inválido
```json
{
  "error": "Missing required header: X-Tenant-ID",
  "status": 400,
  "timestamp": "2026-01-14T12:30:45Z"
}
```

**401 Unauthorized** - Token inválido ou expirado
```json
{
  "timestamp": "2026-01-14T12:30:45Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token has expired",
  "path": "/api/v1/trips"
}
```

**Ação**: Fazer refresh do token via Keycloak.

**404 Not Found** - Recurso não encontrado
```json
{
  "timestamp": "2026-01-14T12:30:45Z",
  "status": 404,
  "error": "Not Found",
  "message": "Trip not found with id: 850e8400-...",
  "path": "/api/v1/trips/850e8400-..."
}
```

**409 Conflict** - Conflito de estado
```json
{
  "timestamp": "2026-01-14T12:30:45Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot cancel a completed trip",
  "path": "/api/v1/trips/850e8400-.../cancel"
}
```

**500 Internal Server Error** - Erro no servidor
```json
{
  "timestamp": "2026-01-14T12:30:45Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later.",
  "path": "/api/v1/trips",
  "traceId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Ação**: Mostrar mensagem genérica ao usuário e reportar `traceId` para suporte.

---

## 19. Paginação

Endpoints de listagem que suportam paginação:

```http
GET /api/v1/passengers/{passengerId}/trips?page=0&size=20&sort=createdAt,desc
```

**Query Parameters**:
- `page` (default: 0): Número da página (zero-based)
- `size` (default: 20): Itens por página
- `sort` (optional): Campo de ordenação (ex: `createdAt,desc`)

**Response**:
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 145,
  "totalPages": 8,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

---

## 20. Rate Limiting

**Limites**:
- **Autenticado**: 1000 requests/hora por usuário
- **Não autenticado**: 100 requests/hora por IP
- **Location updates**: 720 requests/hora (1 a cada 5s)

**Headers de resposta**:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1642176000
```

**Quando exceder**:
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 45 seconds.",
  "retryAfter": 45
}
```

---

## 21. Versionamento

**URL versionada**: `/api/v1/...`

Quando houver breaking changes, será lançada `/api/v2/...`

Versões antigas suportadas por **6 meses** após lançamento da nova versão.

---

## 22. Ambiente de Desenvolvimento

### 22.1 Credenciais de Teste

**Staging API**: `https://staging-api.rappidrive.com`

**Motorista de Teste**:
```
Email: driver.test@rappidrive.com
Password: Test@123
TenantId: 550e8400-e29b-41d4-a716-446655440000
```

**Passageiro de Teste**:
```
Email: passenger.test@rappidrive.com
Password: Test@123
TenantId: 550e8400-e29b-41d4-a716-446655440000
```

**Cartão de Crédito de Teste (Stripe)**:
```
Número: 4242 4242 4242 4242
Validade: 12/34
CVV: 123
```

### 22.2 Ferramentas Recomendadas

- **Swagger UI**: https://staging-api.rappidrive.com/swagger-ui.html
- **Health Check**: https://staging-api.rappidrive.com/actuator/health

---

## 23. Boas Práticas

### 21.1 Gerenciamento de Token

```swift
// Swift - Exemplo
class AuthManager {
    var accessToken: String?
    var refreshToken: String?
    var tokenExpiry: Date?
    
    func isTokenExpired() -> Bool {
        guard let expiry = tokenExpiry else { return true }
        return Date() > expiry.addingTimeInterval(-300) // Refresh 5min antes
    }
    
    func refreshTokenIfNeeded() async {
        if isTokenExpired() {
            await refreshAccessToken()
        }
    }
}
```

### 21.2 Retry Logic

```swift
func performRequest(retryCount: Int = 0) async throws -> Response {
    do {
        return try await networkClient.execute(request)
    } catch {
        if retryCount < 3 && error.isRetryable {
            let delay = pow(2.0, Double(retryCount)) // Exponential backoff
            try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
            return try await performRequest(retryCount: retryCount + 1)
        }
        throw error
    }
}
```

### 21.3 Atualização de Localização Eficiente

```swift
// Enviar apenas quando houve mudança significativa
var lastSentLocation: CLLocation?

func shouldSendLocationUpdate(_ newLocation: CLLocation) -> Bool {
    guard let lastLocation = lastSentLocation else { return true }
    
    let distance = newLocation.distance(from: lastLocation)
    let timeDiff = newLocation.timestamp.timeIntervalSince(lastLocation.timestamp)
    
    // Enviar se: moveu > 50m OU passou > 10s
    return distance > 50 || timeDiff > 10
}
```

### 21.4 Polling Enquanto WebSocket Não Está Disponível

```swift
// Enquanto WebSocket (v2) não está disponível, use polling com backoff
class TripStatusPoller {
    var tripId: String
    var intervalSeconds: Double = 3.0
    
    func startPolling() {
        Task {
            while !Task.isCancelled {
                let trip = try await apiClient.getTrip(tripId)
                handleStatusUpdate(trip.status)
                
                if trip.status == "COMPLETED" || trip.status == "CANCELLED" {
                    break
                }
                
                try await Task.sleep(nanoseconds: UInt64(intervalSeconds * 1_000_000_000))
            }
        }
    }
}
```

### 21.5 Handling de WebSocket Reconnection (v2)

```swift
class WebSocketManager {
    var reconnectAttempts = 0
    let maxReconnectAttempts = 10
    
    func connect() {
        socket.onDisconnect { [weak self] reason in
            self?.handleDisconnect(reason)
        }
    }
    
    func handleDisconnect(_ reason: String) {
        guard reconnectAttempts < maxReconnectAttempts else {
            showError("Connection lost. Please restart the app.")
            return
        }
        
        reconnectAttempts += 1
        let delay = min(30, pow(2.0, Double(reconnectAttempts)))
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.connect()
        }
    }
}
```

---

## 24. SDKs e Bibliotecas Recomendadas

### 24.1 iOS (Swift)

```swift
// Networking
import Alamofire

// Maps + Geocoding (usar diretamente no cliente enquanto API de geocoding não existe)
import MapboxMaps

// Push Notifications
import FirebaseMessaging

// Analytics
import FirebaseAnalytics
```

### 24.2 Android (Kotlin)

```kotlin
// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'

// WebSocket (v2)
implementation 'com.squareup.okhttp3:okhttp:4.11.0'

// Maps + Geocoding (usar diretamente no cliente)
implementation 'com.mapbox.maps:android:10.16.0'

// Push Notifications
implementation 'com.google.firebase:firebase-messaging:23.3.1'

// Location
implementation 'com.google.android.gms:play-services-location:21.0.1'
```

### 24.3 Exemplo de Client HTTP (Swift)

```swift
class RappiDriveAPIClient {
    static let shared = RappiDriveAPIClient()
    
    private let baseURL = "https://api.rappidrive.com"
    private let session = URLSession.shared
    
    func createTrip(origin: Location, destination: Location) async throws -> Trip {
        let endpoint = "\(baseURL)/api/v1/trips"
        
        var request = URLRequest(url: URL(string: endpoint)!)
        request.httpMethod = "POST"
        request.setValue("Bearer \(AuthManager.shared.accessToken!)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(AuthManager.shared.tenantId, forHTTPHeaderField: "X-Tenant-ID")
        
        let body: [String: Any] = [
            "tenantId": AuthManager.shared.tenantId,
            "passengerId": AuthManager.shared.userId,
            "origin": [
                "latitude": origin.latitude,
                "longitude": origin.longitude
            ],
            "destination": [
                "latitude": destination.latitude,
                "longitude": destination.longitude
            ]
        ]
        
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await session.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              (200...299).contains(httpResponse.statusCode) else {
            throw APIError.invalidResponse
        }
        
        return try JSONDecoder().decode(Trip.self, from: data)
    }
}
```

---

## Changelog

| Versão | Data | Mudanças |
|--------|------|----------|
| 1.2 | 2026-05-16 | `GET /drivers/me` e `GET /passengers/me` agora implementados; `POST /ratings` com nova assinatura (tipo enum + `rateeId` como query param, sem tags no request); notificações: header `X-Tenant-Id`, filtro `status` em vez de `unreadOnly`, `POST` requer ADMIN; `POST /drivers` agora requer `documentUrls`; novos endpoints documentados: `PUT /vehicles/{id}/assign`, `PUT /trips/{id}/complete`, áreas de serviço (seção 13), fluxo de aprovação de motoristas (seção 14); numeração de seções atualizada |
| 1.1 | 2026-04-24 | Corrigir contratos de endpoints (métodos HTTP, paths, responses); marcar features não implementadas; adicionar endpoints não documentados; atualizar auth para Keycloak OIDC + PKCE |
| 1.0 | 2026-01-14 | Documentação inicial completa |

---

## Suporte

- **Email**: mobile-support@rappidrive.com
- **Slack**: #mobile-dev (workspace RappiDrive)
- **Status Page**: https://status.rappidrive.com

---

**Última atualização**: 16 de maio de 2026
