# HIST-2026-012: Fluxo de Aprovação de Motoristas - Resumo Executivo

## 🎯 Objetivo

Implementar uma **camada administrativa** onde usuários com perfil ADMIN analisam documentação de motoristas recém cadastrados e decidem sobre aprovação ou rejeição, ativando ou bloqueando o acesso à plataforma.

## 📋 Contexto

Atualmente:
- ✗ Motoristas se registram com status `PENDING_APPROVAL`
- ✗ Não existe fluxo de análise e aprovação
- ✗ Motoristas ficam inativos indefinidamente

Necessário:
- ✅ Fluxo administrativo de aprovação/rejeição
- ✅ Análise de documentação (CNH, comprovante, etc.)
- ✅ Ativação ou bloqueio do motorista
- ✅ Auditoria completa (quem, quando, motivo)
- ✅ Notificação ao motorista

## 🏗️ Arquitetura

### Fluxo Happy Path (Aprovação)

```
1. Motorista se registra
   Status: PENDING_APPROVAL
   ↓
2. Sistema cria DriverApprovalRequest
   Status: PENDING (aguardando análise)
   ↓
3. Admin acessa dashboard
   Vê lista de aprovações pendentes
   ↓
4. Admin abre detalhes do motorista
   Vê documentação anexada
   ↓
5. Admin aprova
   → DriverApprovalRequest status: APPROVED
   → Driver status: ACTIVE
   → Motorista recebe email de confirmação
   ↓
6. Motorista pode aceitar viagens
```

### Fluxo Alternativo (Rejeição)

```
1-4. Mesmos passos acima
   ↓
5. Admin rejeita com motivo
   EX: "Documentação de CNH inválida"
   
   Se permanentBan = false:
   → Driver status: INACTIVE
   → Motorista pode resubmeter depois
   
   Se permanentBan = true:
   → Driver status: BLOCKED
   → Motorista banido permanentemente
   
   → Email enviado com motivo da rejeição
```

## 🗄️ Agregados de Domínio

### 1. DriverApprovalRequest (Raiz de Agregado)

**Responsabilidade**: Representar e gerenciar o ciclo de vida da aprovação

**Campos**:
```
- id: UUID
- driverId: UUID (referência ao motorista)
- tenantId: UUID (multi-tenancy)
- status: ApprovalStatus (PENDING, APPROVED, REJECTED)
- submittedDocuments: String (JSON com URLs dos docs)
- submittedAt: LocalDateTime
- reviewedAt: LocalDateTime (null se pendente)
- reviewedByAdminId: UUID (null se pendente)
- rejectionReason: String (null se aprovado)
```

**Comportamentos**:
- `approve(adminId)` - Aprova e muda status
- `reject(adminId, reason)` - Rejeita com motivo
- `isPending()`, `isApproved()`, `isRejected()` - Query methods

**Invariantes** (regras de negócio):
- ✗ Não pode aprovar se não está PENDING
- ✗ Não pode rejeitar se não está PENDING
- ✗ Rejection reason é obrigatório se REJECTED
- ✗ reviewedByAdminId é obrigatório se não PENDING

### 2. AdminUser (Value Object)

**Responsabilidade**: Representar um usuário administrativo

**Campos**:
```
- id: UUID
- email: Email (value object)
- role: AdminRole (SUPER_ADMIN, COMPLIANCE_OFFICER, SUPPORT_ADMIN)
- fullName: String
- createdAt: LocalDateTime
```

**Imutável**: Não pode ser modificado após criação

### 3. ApprovalStatus (Enum)

```java
PENDING      // Aguardando análise
APPROVED     // Aprovado, motorista ativo
REJECTED     // Rejeitado
```

### 4. AdminRole (Enum)

```java
SUPER_ADMIN         // Acesso total
COMPLIANCE_OFFICER  // Pode aprovar/rejeitar
SUPPORT_ADMIN       // Apenas visualização
```

## 📦 Casos de Uso (Use Cases)

### 1. SubmitDriverApprovalUseCase

**Entrada**: 
```
driverId: UUID
documentUrls: List<String>
```

**Processamento**:
1. Verifica se driver existe e está PENDING_APPROVAL
2. Valida documentos mínimos (pelo menos 2)
3. Cria DriverApprovalRequest com status PENDING
4. Publica evento: DriverApprovalSubmittedEvent

**Saída**: approval ID

### 2. ListPendingApprovalsUseCase

**Entrada**:
```
adminId: UUID
pageNumber: int
pageSize: int
```

**Processamento**:
1. Valida se admin existe e tem permissão
2. Busca aprovações PENDING da mesma tenant
3. Retorna página com dados do motorista

**Saída**: Lista paginada de aprovações pendentes

### 3. ApproveDriverUseCase

**Entrada**:
```
approvalRequestId: UUID
adminId: UUID
notes: String (opcional)
```

**Processamento**:
1. Valida se admin é SUPER_ADMIN ou COMPLIANCE_OFFICER
2. Carrega DriverApprovalRequest e verifica se PENDING
3. Chama `approvalRequest.approve(adminId)`
4. Carrega Driver e chama `driver.activate()`
5. Salva ambos na **mesma transação**
6. Publica evento: DriverApprovedEvent
7. Notifica motorista por email

**Transação**: TUDO ou NADA (atomicidade)

### 4. RejectDriverUseCase

**Entrada**:
```
approvalRequestId: UUID
adminId: UUID
rejectionReason: String
permanentBan: boolean
```

**Processamento**:
1. Valida se admin é SUPER_ADMIN ou COMPLIANCE_OFFICER
2. Carrega DriverApprovalRequest e verifica se PENDING
3. Chama `approvalRequest.reject(adminId, reason)`
4. Se permanentBan=true: `driver.block()` (status BLOCKED)
5. Se permanentBan=false: `driver.deactivate()` (status INACTIVE)
6. Salva ambos na **mesma transação**
7. Publica evento: DriverRejectedEvent
8. Notifica motorista com motivo

## 🔌 Portas (Interfaces)

### Input Ports (Use Cases)
```
SubmitDriverApprovalInputPort
ListPendingApprovalsInputPort
ApproveDriverInputPort
RejectDriverInputPort
```

### Output Ports (Adaptadores)
```
DriverApprovalRepositoryPort     // Persistência
AdminUserRepositoryPort           // Lookup de admin
DriverRepositoryPort              // (já existe)
NotificationPort                  // (já existe) - enviar emails
EventDispatcherPort               // (já existe) - publicar eventos
```

## 📡 Eventos de Domínio

### 1. DriverApprovalSubmittedEvent
```
- driverId
- approvalRequestId
- documentCount
```

### 2. DriverApprovedEvent
```
- driverId
- approvalRequestId
- approvedByAdminId
- approverName
```

### 3. DriverRejectedEvent
```
- driverId
- approvalRequestId
- rejectedByAdminId
- rejectionReason
- permanentBan
```

**Padrão**: Usar Outbox Pattern (HIST-2026-011) para garantir entrega confiável

## 🗄️ Banco de Dados

### Tabela: driver_approval_requests

```sql
CREATE TABLE driver_approval_requests (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,  -- PENDING, APPROVED, REJECTED
    submitted_documents JSONB NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    reviewed_by_admin_id UUID,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (driver_id) REFERENCES drivers(id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id)
);

-- Índices para performance
CREATE INDEX idx_status_tenant ON driver_approval_requests(status, tenant_id);
CREATE INDEX idx_driver_id ON driver_approval_requests(driver_id);
CREATE INDEX idx_tenant_created ON driver_approval_requests(tenant_id, created_at DESC);
```

### Tabela: admin_users

```sql
CREATE TABLE admin_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,  -- SUPER_ADMIN, COMPLIANCE_OFFICER, SUPPORT_ADMIN
    full_name VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Índices
CREATE INDEX idx_email ON admin_users(email);
CREATE INDEX idx_role_tenant ON admin_users(role, tenant_id);
```

## 🌐 REST API

### Listar Aprovações Pendentes
```
GET /api/approvals/pending?page=0&size=50
Authorization: Bearer <token>

Resposta (200):
{
  "approvals": [
    {
      "approvalId": "uuid",
      "driverId": "uuid",
      "driverName": "João Silva",
      "driverEmail": "joao@example.com",
      "submittedAt": "2026-01-10T10:00:00",
      "documents": ["cnh_url", "comprovante_url"]
    }
  ],
  "totalCount": 120,
  "pageNumber": 0,
  "pageSize": 50
}
```

### Obter Detalhes de uma Aprovação
```
GET /api/approvals/{id}
Authorization: Bearer <token>

Resposta (200):
{
  "id": "uuid",
  "driverId": "uuid",
  "driverName": "João Silva",
  "status": "PENDING",
  "submittedAt": "2026-01-10T10:00:00",
  "documents": ["cnh_url", "comprovante_url"]
}
```

### Aprovar Motorista
```
POST /api/approvals/{id}/approve
Authorization: Bearer <token>
Content-Type: application/json

{
  "notes": "Documentação está ok" (opcional)
}

Resposta (204 No Content)
```

### Rejeitar Motorista
```
POST /api/approvals/{id}/reject
Authorization: Bearer <token>
Content-Type: application/json

{
  "rejectionReason": "Documentação de CNH inválida",
  "permanentBan": false
}

Resposta (204 No Content)
```

### Status de Aprovação de um Motorista
```
GET /api/drivers/{driverId}/approval-status
Authorization: Bearer <token>

Resposta (200):
{
  "status": "PENDING",  // ou APPROVED, REJECTED
  "submittedAt": "2026-01-10T10:00:00",
  "rejectionReason": null,
  "canResubmit": false
}
```

## 🔐 Segurança & Permissões

### RBAC (Role-Based Access Control)

| Operação | SUPER_ADMIN | COMPLIANCE_OFFICER | SUPPORT_ADMIN |
|----------|:---:|:---:|:---:|
| Listar pendentes | ✅ | ✅ | ❌ |
| Ver detalhes | ✅ | ✅ | ✅ |
| Aprovar | ✅ | ✅ | ❌ |
| Rejeitar | ✅ | ✅ | ❌ |

### Multi-Tenancy

- Admin A (Tenant X) **não pode** ver aprovações de Tenant Y
- Queries sempre incluem: `WHERE tenant_id = current_user.tenant_id`
- Isolamento garantido em nível de banco de dados

## 📊 Critérios de Aceite

### Fase 1: Domínio ✅
- [ ] Agregados criados com invariantes
- [ ] Value objects imutáveis
- [ ] Enums definidos
- [ ] Eventos de domínio
- [ ] 100% sem Spring/JPA

### Fase 2: Aplicação ✅
- [ ] 4 casos de uso implementados
- [ ] Portas entrada/saída definidas
- [ ] Testes unitários com mocks
- [ ] Transações garantidas

### Fase 3: Infraestrutura ✅
- [ ] JPA entities + mappers
- [ ] Adapters implementados
- [ ] Database migration
- [ ] Índices otimizados
- [ ] Testes de integração

### Fase 4: Apresentação ✅
- [ ] REST controller com 5 endpoints
- [ ] DTOs request/response
- [ ] Validação de permissões
- [ ] Exception handlers
- [ ] Testes E2E

### Fase 5: Completude ✅
- [ ] Logs estruturados
- [ ] Eventos publicados via Outbox
- [ ] Notificações por email
- [ ] Auditoria completa
- [ ] Performance (<100ms queries)

## ⏱️ Estimativa

| Fase | Duração |
|------|---------|
| Domínio | 1.5 dias |
| Aplicação | 1 dia |
| Infraestrutura | 1 dia |
| Apresentação | 1 dia |
| Refinamentos | 0.5 dias |
| **Total** | **4-5 dias** |

## 📚 Referências Internas

- HIST-2026-001: Optimistic Locking + Domain Events
- HIST-2026-009: Transactional Outbox Pattern (proposta)
- HIST-2026-010: Arquitetura Hexagonal (validação)
- HIST-2026-011: Outbox Event Processor (implementação)

## 🚀 Próximos Passos

1. **Implementar**: Seguir passo-a-passo do documento HIST-2026-012-IMPLEMENTATION.md
2. **Testar**: Rodar testes unitários, integração e E2E
3. **Revisar**: Code review com ênfase em arquitetura hexagonal
4. **Documentar**: Atualizar README com novos endpoints
5. **Fazer merge**: Para main branch
6. **Monitorar**: Rastrear métricas de aprovação
7. **Futuro**: Implementar integrações (background checks, webhooks)

---

**Status**: PROPOSTO (pronto para implementação)  
**Data**: 10/01/2026  
**Prioridade**: 🔴 ALTA  
**Complexidade**: 🟡 MÉDIA  
