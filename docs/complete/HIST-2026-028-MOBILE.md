# HIST-2026-028: Mobile — Integração Stripe para Captura de Cartão e Criação de PaymentMethod

**Data**: 30/04/2026
**Status**: 🔲 Pendente
**Tipo**: Feature Mobile
**Plataformas**: iOS (Swift) + Android (Kotlin)
**Tamanho**: Pequeno (1–2 dias)
**Prioridade**: Alta
**Depende de**: HIST-2026-027 (backend deve estar deployado antes de testar em staging)

---

## 1. Contexto

O backend (HIST-2026-027) passa a aceitar um campo `stripePaymentMethodId` nos endpoints de pagamento. Esse campo é um token opaco gerado **exclusivamente pelo SDK mobile do Stripe** — nunca pelo servidor. O Stripe exige essa arquitetura por razões de segurança: dados de cartão (número, CVV, validade) jamais devem trafegar pelo servidor do produto.

O fluxo atual no app passa um `paymentMethod: "CREDIT_CARD"` sem token. Com a integração Stripe, o fluxo muda:

```
ANTES (mock):
  App → POST /complete-with-payment { paymentMethod: "CREDIT_CARD" }

DEPOIS (Stripe):
  App → Stripe SDK (coleta cartão em componente nativo)
      → stripe.createPaymentMethod(cardDetails) → pm_xxxxx
  App → POST /complete-with-payment { paymentMethod: "CREDIT_CARD", stripePaymentMethodId: "pm_xxxxx" }
```

O Stripe SDK nunca envia dados de cartão para o backend — apenas o token `pm_xxxxx`. O backend recebe o token e o encaminha diretamente para a API do Stripe no servidor.

---

## 2. Escopo

### 2.1 O que está incluído

| Área | Entregável |
|---|---|
| SDK | Instalação do Stripe iOS SDK e Stripe Android SDK |
| Inicialização | Configurar `STPAPIClient` (iOS) / `PaymentConfiguration` (Android) com a chave publicável |
| UI de cartão | Tela/componente de entrada de dados de cartão usando componentes nativos do Stripe |
| Token | Criação do `PaymentMethod` (`pm_xxxxx`) via `stripe.createPaymentMethod()` |
| Integração API | Envio do `stripePaymentMethodId` nos endpoints afetados |
| Erros | Tratamento de erros do SDK Stripe e mapeamento para feedback ao usuário |
| Testes | Validação com cartões de teste do Stripe (staging) |

### 2.2 O que está fora do escopo

- Apple Pay / Google Pay (história separada)
- PIX via Stripe (história separada)
- Vault de cartões — salvar cartão para reuso (`setup_intent`)
- 3D Secure (3DS) — autenticação adicional para emissores europeus; cartões de teste `4242...` não requerem 3DS
- Stripe Connect — repasse automático ao motorista

---

## 3. Endpoints afetados

Dois endpoints do backend passam a aceitar `stripePaymentMethodId`:

### 3.1 Completar corrida com pagamento

```http
POST /api/v1/trips/{id}/complete-with-payment
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body — antes**:
```json
{
  "actualDistanceKm": 5.8,
  "actualDurationMinutes": 15,
  "paymentMethod": "CREDIT_CARD"
}
```

**Request Body — depois**:
```json
{
  "actualDistanceKm": 5.8,
  "actualDurationMinutes": 15,
  "paymentMethod": "CREDIT_CARD",
  "stripePaymentMethodId": "pm_1OqDZBLkdIwHu7ixZeAHnQ2d"
}
```

### 3.2 Processar pagamento avulso

```http
POST /api/v1/payments
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

**Request Body — antes**:
```json
{
  "tripId": "850e8400-...",
  "method": "CREDIT_CARD"
}
```

**Request Body — depois**:
```json
{
  "tripId": "850e8400-...",
  "method": "CREDIT_CARD",
  "stripePaymentMethodId": "pm_1OqDZBLkdIwHu7ixZeAHnQ2d"
}
```

> `stripePaymentMethodId` é **obrigatório** para `CREDIT_CARD` e `DEBIT_CARD`. Para `CASH`, deve ser omitido ou `null`.

---

## 4. Implementação iOS (Swift)

### 4.1 Instalação do SDK

**Swift Package Manager** (recomendado):
```
https://github.com/stripe/stripe-ios
Versão mínima: 23.x
```

**CocoaPods** (alternativa):
```ruby
pod 'Stripe', '~> 23.0'
```

### 4.2 Inicialização

Configurar a chave publicável no `AppDelegate` ou no ponto de inicialização do app. A chave publicável é segura para incluir no código-fonte — ela não concede acesso à conta Stripe.

```swift
import StripeCore

func application(_ application: UIApplication, 
                 didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    StripeAPI.defaultPublishableKey = Configuration.stripePublishableKey
    return true
}
```

A chave publicável deve ser configurada por ambiente:
- **Staging**: chave `pk_test_...` (Stripe test mode)
- **Produção**: chave `pk_live_...` (Stripe live mode)

Nunca hardcode a chave — usar um arquivo `Config.plist` ou variável de build excluída do controle de versão.

### 4.3 Componente de captura de cartão

O Stripe fornece `STPPaymentCardTextField`, um campo de entrada que coleta número do cartão, validade e CVV **sem que esses dados toquem o código do app**.

```swift
import UIKit
import Stripe

class PaymentCardViewController: UIViewController {

    private let cardField = STPPaymentCardTextField()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.addSubview(cardField)
        // layout constraints aqui
    }

    func confirmPayment() {
        guard cardField.isValid else {
            showError("Por favor, verifique os dados do cartão.")
            return
        }
        createPaymentMethod()
    }

    private func createPaymentMethod() {
        let cardParams = cardField.cardParams
        let paymentMethodParams = STPPaymentMethodParams(
            card: STPPaymentMethodCardParams(paymentMethodParams: cardParams),
            billingDetails: nil,
            metadata: nil
        )

        STPAPIClient.shared.createPaymentMethod(with: paymentMethodParams) { [weak self] paymentMethod, error in
            guard let self else { return }

            if let error {
                self.showError(error.localizedDescription)
                return
            }

            guard let paymentMethodId = paymentMethod?.stripeId else {
                self.showError("Erro ao processar cartão. Tente novamente.")
                return
            }

            self.completeTrip(stripePaymentMethodId: paymentMethodId)
        }
    }

    private func completeTrip(stripePaymentMethodId: String) {
        // chamar o serviço de API com o token
    }
}
```

### 4.4 Chamada ao backend

```swift
struct CompleteTripRequest: Encodable {
    let actualDistanceKm: Double
    let actualDurationMinutes: Int
    let paymentMethod: String
    let stripePaymentMethodId: String?
}

func completeTrip(tripId: String, stripePaymentMethodId: String) async throws -> TripCompletionResponse {
    let body = CompleteTripRequest(
        actualDistanceKm: trip.actualDistanceKm,
        actualDurationMinutes: trip.actualDurationMinutes,
        paymentMethod: "CREDIT_CARD",
        stripePaymentMethodId: stripePaymentMethodId
    )

    return try await apiClient.post(
        path: "/api/v1/trips/\(tripId)/complete-with-payment",
        body: body
    )
}
```

---

## 5. Implementação Android (Kotlin)

### 5.1 Instalação do SDK

```kotlin
// build.gradle (app)
dependencies {
    implementation("com.stripe:stripe-android:20.x.x")
}
```

Requer `minSdk = 21` (Android 5.0+).

### 5.2 Inicialização

```kotlin
import com.stripe.android.PaymentConfiguration

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            BuildConfig.STRIPE_PUBLISHABLE_KEY // definido no build.gradle via manifestPlaceholders
        )
    }
}
```

A chave publicável deve ser injetada via `BuildConfig`:
```groovy
// build.gradle
android {
    buildTypes {
        debug {
            buildConfigField "String", "STRIPE_PUBLISHABLE_KEY", "\"pk_test_...\""
        }
        release {
            buildConfigField "String", "STRIPE_PUBLISHABLE_KEY", "\"pk_live_...\""
        }
    }
}
```

### 5.3 Componente de captura de cartão

O Stripe fornece `CardInputWidget` (view XML) e `CardFormView` (formulário completo).

**Layout XML**:
```xml
<com.stripe.android.view.CardInputWidget
    android:id="@+id/cardInputWidget"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

**ViewModel / Fragment**:
```kotlin
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputWidget

class PaymentFragment : Fragment() {

    private lateinit var stripe: Stripe
    private lateinit var cardInputWidget: CardInputWidget

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stripe = Stripe(requireContext(), PaymentConfiguration.getInstance(requireContext()).publishableKey)
        cardInputWidget = view.findViewById(R.id.cardInputWidget)

        view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            createPaymentMethod()
        }
    }

    private fun createPaymentMethod() {
        val params = cardInputWidget.paymentMethodCreateParams ?: run {
            showError("Por favor, verifique os dados do cartão.")
            return
        }

        stripe.createPaymentMethod(
            paymentMethodCreateParams = params,
            callback = object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    completeTrip(stripePaymentMethodId = result.id!!)
                }

                override fun onError(e: Exception) {
                    showError(e.localizedMessage ?: "Erro ao processar cartão.")
                }
            }
        )
    }

    private fun completeTrip(stripePaymentMethodId: String) {
        viewModel.completeTrip(tripId, stripePaymentMethodId)
    }
}
```

### 5.4 Chamada ao backend

```kotlin
data class CompleteTripRequest(
    val actualDistanceKm: Double,
    val actualDurationMinutes: Int,
    val paymentMethod: String,
    val stripePaymentMethodId: String?
)

suspend fun completeTrip(tripId: String, stripePaymentMethodId: String): TripCompletionResponse {
    return apiService.completeTrip(
        tripId = tripId,
        body = CompleteTripRequest(
            actualDistanceKm = trip.actualDistanceKm,
            actualDurationMinutes = trip.actualDurationMinutes,
            paymentMethod = "CREDIT_CARD",
            stripePaymentMethodId = stripePaymentMethodId
        )
    )
}
```

---

## 6. Fluxo de tela no app do passageiro

O `stripePaymentMethodId` precisa ser criado **antes** de chamar o backend. O fluxo recomendado no app do passageiro:

```
Tela de confirmação de pagamento
  │
  ├─ Método = CASH → botão "Confirmar corrida" → POST /complete-with-payment (sem token)
  │
  └─ Método = CREDIT_CARD ou DEBIT_CARD
       │
       ├─ [já tem cartão salvo no session] → reusar pm_xxxxx da sessão
       │
       └─ [sem cartão] → exibir componente Stripe de captura
            │
            ├─ Dados válidos → stripe.createPaymentMethod() → pm_xxxxx
            │    → POST /complete-with-payment { stripePaymentMethodId: "pm_xxxxx" }
            │
            └─ Erro de rede/cartão inválido → exibir mensagem de erro → usuário corrige
```

> O `pm_xxxxx` é de uso único no contexto desta história — o token é consumido pelo `PaymentIntent` no backend. Não reusar entre corridas.

---

## 7. Tratamento de erros

| Cenário | Origem | Mensagem ao usuário | Ação |
|---|---|---|---|
| Dados de cartão inválidos | SDK Stripe (client-side) | "Número de cartão inválido" / "Data de validade incorreta" | Impedir submit; exibir inline |
| Falha de rede ao criar PaymentMethod | SDK Stripe | "Sem conexão. Verifique sua internet." | Permitir nova tentativa |
| Backend retorna `400` com `stripePaymentMethodId` ausente | API RappiDrive | (nunca deve ocorrer em produção) | Log + retry |
| Backend retorna `402` / pagamento recusado | API RappiDrive | "Pagamento recusado. Tente outro cartão." | Voltar à tela de captura |
| Backend retorna `503` (circuit breaker aberto) | API RappiDrive | "Serviço de pagamento temporariamente indisponível. Tente em alguns instantes." | Exibir botão "Tentar novamente" |

---

## 8. Cartões de teste (staging)

| Cenário | Número | Resultado esperado |
|---|---|---|
| Pagamento aprovado | `4242 4242 4242 4242` | `COMPLETED` |
| Cartão recusado | `4000 0000 0000 0002` | Backend retorna `402` / `failureReason: "Card declined"` |
| Fundos insuficientes | `4000 0000 0000 9995` | Backend retorna `402` / `failureReason: "Insufficient funds"` |
| Cartão expirado | `4000 0000 0000 0069` | SDK rejeita antes de enviar (`STPCardValidationStateInvalid`) |

Para todos os cartões de teste: validade `12/34`, CVV `123`, CEP `12345`.

---

## 9. Segurança — o que não fazer

| Proibido | Motivo |
|---|---|
| Enviar número do cartão ao backend próprio | PCI DSS nível 1 — expõe a empresa a multas e revogação de certificação |
| Armazenar `pm_xxxxx` em `SharedPreferences` / `UserDefaults` / banco local | Token pode ser reutilizado para charges se vazar |
| Logar `stripePaymentMethodId` em analytics | IDs de PaymentMethod são dados financeiros sensíveis |
| Usar a chave **secreta** do Stripe (`sk_...`) no app | Concede acesso total à conta Stripe — jamais sair do servidor |

---

## 10. Critérios de Aceite

- [ ] App iOS coleta dados de cartão com `STPPaymentCardTextField` — número, validade e CVV não aparecem no tráfego de rede para o backend.
- [ ] App Android coleta dados de cartão com `CardInputWidget` — mesma restrição.
- [ ] `stripe.createPaymentMethod()` retorna `pm_xxxxx` para o cartão `4242 4242 4242 4242`.
- [ ] `POST /api/v1/trips/{id}/complete-with-payment` enviado com `stripePaymentMethodId` resulta em `status: COMPLETED` no staging (com HIST-2026-027 deployado).
- [ ] Cartão `4000 0000 0000 0002` (recusado) exibe mensagem de erro amigável sem crash.
- [ ] Método `CASH` continua funcionando sem `stripePaymentMethodId`.
- [ ] Chave publicável Stripe não está hardcoded — vem de `Config.plist` (iOS) ou `BuildConfig` (Android).

---

## 11. Definição de Pronto (DoD)

- [ ] SDK Stripe adicionado e inicializado em ambas as plataformas.
- [ ] Tela de captura de cartão com componente nativo do Stripe integrada ao fluxo de encerramento de corrida.
- [ ] Todos os cenários de cartão de teste validados em staging (aprovado, recusado, fundos insuficientes).
- [ ] Revisão de segurança: confirmar que nenhum dado de cartão aparece em logs ou analytics.
- [ ] `MOBILE_API_DOCUMENTATION.md` atualizado com o campo `stripePaymentMethodId` nas seções 8.5 e 10.1.

---

## 12. Notas de Implementação

### Chave publicável por ambiente

Manter duas chaves: `pk_test_...` para staging/dev e `pk_live_...` para produção. A troca deve ser automática via flavor de build — nunca manual.

### Reutilização de `pm_xxxxx` dentro da sessão

O token `pm_xxxxx` pode ser criado uma vez e reutilizado **na mesma sessão**, caso o usuário tente encerrar a corrida novamente após um erro de rede (o token só é "consumido" quando o `PaymentIntent` é confirmado com sucesso no Stripe). Não persistir além da sessão.

### 3DS — limitação desta história

Cartões que requerem autenticação adicional (3D Secure) retornarão `FAILED` com `requires_action` nesta versão. O fluxo de 3DS exige que o mobile receba o `client_secret` do `PaymentIntent` e chame `stripe.handleNextAction(clientSecret)`. Isso é uma história separada. O cartão de teste `4242...` não requer 3DS e cobre o caso de uso do MVP.

### Stripe SDK version pinning

Usar versão minor fixa no SPM/Gradle (ex: `23.2.0`, não `~> 23`). O Stripe atualiza frequentemente e mudanças de breaking change dentro de minor ocorrem ocasionalmente.
