# Guía de consumo de la API Telegram IA

Telegram IA es un backend Spring Boot que permite a una empresa conectar de forma segura uno de sus registros de cliente con la cuenta de Telegram de ese cliente mediante un enlace de invitación temporal. Existe para resolver un problema operativo frecuente: un bot de Telegram puede identificar a un usuario de Telegram, pero no puede saber qué cliente de la empresa representa ese usuario salvo que un backend confiable realice la vinculación, la autorización y el registro de auditoría.

El beneficio de negocio es la automatización controlada. Las empresas pueden invitar clientes a una experiencia con bot de Telegram sin exponer datos del cliente dentro del token, sin confiar en nombres de usuario de Telegram como identidad y sin perder trazabilidad sobre quién creó, validó, revocó o confirmó un enlace.

> Swagger está disponible como referencia contractual complementaria en `http://localhost:8080/swagger-ui.html`. Este README es intencionalmente más explicativo: describe cuándo llamar a cada servicio, qué significa la carga útil y cómo se comporta el flujo de negocio.

## Qué hace este backend

Este backend administra el ciclo de vida de vinculación con Telegram:

1. Un usuario interno de la empresa crea una invitación temporal de Telegram para un cliente.
2. El backend genera un token opaco sin procesar, almacena solo su hash y devuelve un enlace profundo de Telegram.
3. Un bot de Telegram o adaptador Hermes recibe `/start <token>` desde Telegram y solicita a este backend validar el token.
4. Después de que el cliente confirma dentro del bot, el bot vuelve a llamar a este backend para crear el enlace permanente con la cuenta de Telegram.
5. Los consumidores orientados a administración pueden revocar invitaciones pendientes e inspeccionar el estado actual de vinculación de un cliente.

El servicio **no** incluye el tiempo de ejecución del bot de Telegram, el listener de webhooks ni la integración con el SDK de Telegram. Un servicio bot/Hermes consume los endpoints orientados al bot documentados más abajo.

## Modelo mental para consumidores

| Concepto | Significado para consumidores de la API |
|---|---|
| Empresa / tenant | El límite de aislamiento. Usuarios, clientes, invitaciones, enlaces de Telegram y eventos de auditoría pertenecen a una empresa. Los datos de distintas empresas no deben mezclarse. |
| Usuario interno de empresa | El usuario actual del lado servidor que crea/revoca invitaciones o consulta estados. En este MVP se simula mediante configuración, no mediante encabezados de solicitud. |
| Cliente | El cliente/persona de la empresa que se vinculará con una cuenta de Telegram. Un cliente debe estar activo y pertenecer a la empresa del usuario actual. |
| Token de invitación temporal | Un token breve, aleatorio, opaco y de un solo uso incluido en `https://t.me/<bot>?start=<token>`. Expira después de 48 horas. La base de datos almacena solo un hash y un prefijo corto, nunca el token sin procesar. |
| Enlace permanente con cuenta de Telegram | La relación duradera entre un cliente y un ID de usuario de Telegram después de la confirmación del bot. Vive separada de la invitación temporal. |
| Eventos de auditoría | Registros internos escritos por creación de invitaciones, intentos de validación, revocación, detección de Telegram duplicado y vinculación exitosa. Son contexto para consumidores, no respuestas públicas de la API. |

## Flujo de negocio completo

```text
Interfaz admin / servicio interno
  -> POST /api/v1/admin/telegram-link/invitations
  <- 201 Created con https://t.me/<bot_username>?start=<raw_token>

El cliente abre el enlace de Telegram
  -> Telegram envía /start <raw_token> al bot

Bot / servicio Hermes
  -> POST /api/v1/bot/telegram-link/validate
  <- 200 OK con status VALID o INVALID

El bot pide confirmación al cliente
  -> POST /api/v1/bot/telegram-link/confirm
  <- 200 OK con status CONFIRMED o INVALID

Interfaz admin / servicio interno
  -> GET /api/v1/admin/telegram-link/clients/{clientId}/status
  <- estado actual: NOT_LINKED, INVITATION_PENDING o LINKED
```

### Reglas de negocio importantes para consumidores

- Un token es temporal, opaco, de un solo uso y expira después de 48 horas.
- Un token **no** es un JWT. No lo decodifiques; devuélvelo exactamente como fue recibido.
- Solo el token sin procesar incluido en el enlace de Telegram generado puede validarse. `tokenPrefix` es solo para soporte/depuración.
- El backend almacena `token_hash`, no el token sin procesar.
- Un cliente puede tener solo una cuenta de Telegram activa por empresa.
- Un usuario de Telegram puede vincularse solo una vez por empresa.
- El bot debe llamar primero a `validate` si quiere previsualizar el cliente y luego llamar a `confirm` solo después de que el usuario acepte.
- La confirmación revalida el token dentro de una transacción de escritura y bloquea la invitación para preservar el comportamiento de un solo uso.
- Los nombres de usuario de Telegram pueden cambiar. Los consumidores deben tratar `telegramUserId` como la identidad estable de Telegram.

## Qué debe existir antes de consumir el servicio

Antes de consumir los endpoints, el entorno ya debe contener:

| Requisito | Por qué importa |
|---|---|
| Base de datos PostgreSQL | Los datos de tiempo de ejecución se almacenan en PostgreSQL. El perfil local `dev` espera `jdbc:postgresql://localhost:5432/telegram_ia` con usuario `root` y contraseña `admin`. |
| Empresa sembrada | La empresa es el límite de tenant para el usuario actual y los clientes. |
| Usuario interno de empresa sembrado | Los endpoints admin usan un usuario actual simulado del lado servidor en este MVP. Los valores por defecto de dev apuntan al usuario de empresa `20000000-0000-0000-0000-000000000003`. |
| Cliente sembrado | Las invitaciones se crean para clientes activos existentes. Los ejemplos de dev usan el cliente `20000000-0000-0000-0000-000000000004`. |
| Asignación opcional de cliente | Requerido solo cuando el rol del usuario actual simulado es `AGENT`. `OWNER`, `ADMIN` y `SUPERVISOR` pueden operar como roles de gestión. |
| Nombre de usuario del bot de Telegram | Se usa para construir enlaces como `https://t.me/JarviServeProBiker_bot?start=<token>`. Configúralo con `TELEGRAM_LINK_BOT_USERNAME`; el valor por defecto es `JarviServeProBiker_bot`. |
| Integración bot/Hermes | El backend expone endpoints HTTP orientados al bot, pero el bot debe recibir actualizaciones de Telegram y llamar a esta API. |

## Catálogo de servicios

Ruta base para ejemplos locales:

```bash
BASE_URL=http://localhost:8080
CLIENT_ID=20000000-0000-0000-0000-000000000004
```

| Endpoint | Quién lo llama | Por qué se llama | Comportamiento HTTP | Orientación |
|---|---|---|---|---|
| `POST /api/v1/admin/telegram-link/invitations` | Interfaz admin o servicio interno de backoffice | Crear una invitación pendiente y obtener un enlace profundo de Telegram para un cliente. | HTTP semántico: éxito `201`; fallas de negocio usan `ProblemDetail` con estado 4xx. | Orientado a admin |
| `DELETE /api/v1/admin/telegram-link/invitations/{invitationId}` | Interfaz admin o servicio interno de backoffice | Cancelar una invitación pendiente antes de que se use. | HTTP semántico: éxito `200`; fallas de negocio usan `ProblemDetail` con estado 4xx. | Orientado a admin |
| `GET /api/v1/admin/telegram-link/clients/{clientId}/status` | Interfaz admin o servicio interno de backoffice | Mostrar si un cliente no está vinculado, tiene una invitación pendiente o ya está vinculado. | HTTP semántico: éxito `200`; fallas de negocio usan `ProblemDetail` con estado 4xx. | Orientado a admin |
| `POST /api/v1/bot/telegram-link/validate` | Bot de Telegram o adaptador Hermes | Verificar si un token sin procesar es válido antes de pedir confirmación al cliente. | Semántica por carga útil: las fallas de negocio devuelven `200 OK` con `status: "INVALID"`; solicitudes mal formadas pueden devolver `400 ProblemDetail`. | Orientado a bot |
| `POST /api/v1/bot/telegram-link/confirm` | Bot de Telegram o adaptador Hermes | Crear el enlace permanente con la cuenta de Telegram después de que el cliente confirma. | Semántica por carga útil: las fallas de negocio devuelven `200 OK` con `status: "INVALID"`; solicitudes mal formadas pueden devolver `400 ProblemDetail`. | Orientado a bot |

## Comportamiento de endpoints admin

Los endpoints admin usan el usuario actual configurado en `telegram-link.current-user.*`. En el MVP actual, no se leen encabezados de identidad ni tokens bearer desde la solicitud. Los consumidores de producción deben ubicar estos endpoints detrás de autenticación real antes de exponerlos externamente.

Las fallas admin se devuelven como cargas útiles Spring `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Telegram linking request failed",
  "status": 409,
  "detail": "Client already has a pending invitation",
  "errorCode": "INVITATION_ALREADY_PENDING",
  "path": "/api/v1/admin/telegram-link/invitations"
}
```

## Comportamiento de endpoints bot

Los endpoints bot están diseñados para que el bot siempre pueda renderizar un mensaje controlado para el usuario ante fallas de negocio. Los casos inválidos, expirados, revocados, ya usados o de enlace duplicado devuelven `200 OK` con una carga útil `INVALID` en lugar de lanzar errores HTTP semánticos.

Las solicitudes de transporte mal formadas siguen devolviendo `400 ProblemDetail`. Ejemplos: cuerpo JSON ausente, token en blanco o `telegramUserId` no positivo.

## Detalle de endpoints

### 1. Crear invitación de Telegram

```http
POST /api/v1/admin/telegram-link/invitations
```

Crea una invitación pendiente de 48 horas para un cliente activo en la empresa del usuario actual configurado.

| Ítem | Detalles |
|---|---|
| Quién debería llamarlo | Admin UI, integración CRM o servicio interno actuando como el usuario de empresa configurado. |
| Cuándo llamarlo | Cuando un usuario de la empresa quiere invitar a un cliente a iniciar o conectarse mediante el bot de Telegram. |
| Parámetros de ruta | Ninguno. |
| Parámetros de consulta | Ninguno. |
| Campos requeridos del cuerpo | `clientId` UUID. |
| Éxito | `201 Created` con metadatos de invitación y el enlace de Telegram. |

Cuerpo de la solicitud:

```json
{
  "clientId": "20000000-0000-0000-0000-000000000004"
}
```

Respuesta exitosa:

```json
{
  "invitationId": "3f70297f-d4e5-4264-9ec2-78421c46f5c2",
  "status": "PENDING",
  "link": "https://t.me/JarviServeProBiker_bot?start=ABCD2345EFGH",
  "expiresAt": "2026-06-17T12:00:00Z",
  "tokenPrefix": "ABCD"
}
```

Errores de negocio:

| HTTP | `errorCode` | Significado |
|---|---|---|
| `404` | `CLIENT_NOT_FOUND` | El cliente no existe o pertenece a otra empresa. |
| `409` | `CLIENT_ALREADY_LINKED` | El cliente ya tiene un enlace activo con una cuenta de Telegram. |
| `409` | `INVITATION_ALREADY_PENDING` | El cliente ya tiene una invitación pendiente no expirada. |
| `403` | `USER_NOT_ACTIVE` | El usuario actual configurado no está activo. |
| `403` | `CLIENT_NOT_ASSIGNED_TO_AGENT` | El usuario configurado es un agente y no está asignado a este cliente. |
| `403` | `CURRENT_USER_NOT_AVAILABLE` | Falta la configuración del usuario actual simulado. |
| `400` | `CLIENT_NOT_ACTIVE`, `CROSS_COMPANY_ACCESS`, `ROLE_NOT_ALLOWED`, `INVALID_REQUEST` | La regla de negocio actual o la forma de la solicitud es inválida. Algunos códigos de política de autorización actualmente caen en `400` salvo que estén mapeados explícitamente. |

Curl:

```bash
curl -i -X POST "$BASE_URL/api/v1/admin/telegram-link/invitations" \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"'"$CLIENT_ID"'"}'
```

Notas y consideraciones:

- Guarda el valor `link` o envíalo inmediatamente. El token sin procesar no se almacena en la base de datos.
- `tokenPrefix` no es una credencial y no puede usarse para validar la invitación.
- Una invitación pendiente duplicada se rechaza. Revoca la invitación pendiente anterior antes de crear una nueva.
- El encabezado de respuesta `Location` apunta a `/api/v1/admin/telegram-link/invitations/{invitationId}`, pero en este MVP no existe un endpoint GET separado por invitación.

### 2. Revocar invitación de Telegram

```http
DELETE /api/v1/admin/telegram-link/invitations/{invitationId}
```

Revoca una invitación pendiente antes de que se use.

| Ítem | Detalles |
|---|---|
| Quién debería llamarlo | Admin UI, integración CRM o servicio interno actuando como el usuario de empresa configurado. |
| Cuándo llamarlo | Cuando la invitación se envió por error, ya no debería ser válida o debe regenerarse. |
| Parámetros de ruta | `invitationId` UUID. |
| Parámetros de consulta | Ninguno. |
| Campos requeridos del cuerpo | Ninguno. |
| Éxito | `200 OK` con metadatos de la invitación revocada. |

Cuerpo de la solicitud: ninguno.

Respuesta exitosa:

```json
{
  "invitationId": "3f70297f-d4e5-4264-9ec2-78421c46f5c2",
  "status": "REVOKED",
  "revokedAt": "2026-06-15T12:30:00Z"
}
```

Errores de negocio:

| HTTP | `errorCode` | Significado |
|---|---|---|
| `404` | `INVITATION_NOT_FOUND` | No existe una invitación pendiente no expirada con este ID en la empresa del usuario actual. Las invitaciones ya usadas, expiradas y ya revocadas no pueden revocarse mediante este endpoint. |
| `404` | `CLIENT_NOT_FOUND` | La invitación existe, pero su registro de cliente ya no puede encontrarse. |
| `403` | `USER_NOT_ACTIVE` | El usuario actual configurado no está activo. |
| `403` | `CLIENT_NOT_ASSIGNED_TO_AGENT` | El usuario configurado es un agente y no está asignado a este cliente. |
| `400` | `CLIENT_NOT_ACTIVE`, `CROSS_COMPANY_ACCESS`, `ROLE_NOT_ALLOWED`, `INVALID_REQUEST` | La regla de negocio actual o la forma de la solicitud es inválida. |

Curl:

```bash
INVITATION_ID=3f70297f-d4e5-4264-9ec2-78421c46f5c2

curl -i -X DELETE "$BASE_URL/api/v1/admin/telegram-link/invitations/$INVITATION_ID"
```

Notas y consideraciones:

- La revocación aplica solo a invitaciones pendientes. No desvincula una cuenta de Telegram ya vinculada.
- Un token revocado luego se validará como `INVALID` con `errorCode: "INVITATION_REVOKED"` en los endpoints orientados al bot.
- La revocación escribe un evento de auditoría.

### 3. Obtener estado de vinculación de Telegram del cliente

```http
GET /api/v1/admin/telegram-link/clients/{clientId}/status
```

Devuelve el estado actual de vinculación con Telegram para un cliente.

| Ítem | Detalles |
|---|---|
| Quién debería llamarlo | Admin UI, integración CRM o servicio interno actuando como el usuario de empresa configurado. |
| Cuándo llamarlo | Antes de mostrar una acción de invitación, después de crear/revocar una invitación o después de la confirmación del bot. |
| Parámetros de ruta | `clientId` UUID. |
| Parámetros de consulta | Ninguno. |
| Campos requeridos del cuerpo | Ninguno. |
| Éxito | `200 OK` con uno de `NOT_LINKED`, `INVITATION_PENDING` o `LINKED`. |

Cuerpo de la solicitud: ninguno.

Respuesta exitosa cuando el cliente no está vinculado:

```json
{
  "clientId": "20000000-0000-0000-0000-000000000004",
  "status": "NOT_LINKED",
  "invitationId": null,
  "invitationExpiresAt": null,
  "telegramUserId": null,
  "telegramUsername": null,
  "linkedAt": null
}
```

Respuesta exitosa cuando hay una invitación pendiente:

```json
{
  "clientId": "20000000-0000-0000-0000-000000000004",
  "status": "INVITATION_PENDING",
  "invitationId": "3f70297f-d4e5-4264-9ec2-78421c46f5c2",
  "invitationExpiresAt": "2026-06-17T12:00:00Z",
  "telegramUserId": null,
  "telegramUsername": null,
  "linkedAt": null
}
```

Respuesta exitosa cuando el cliente está vinculado:

```json
{
  "clientId": "20000000-0000-0000-0000-000000000004",
  "status": "LINKED",
  "invitationId": null,
  "invitationExpiresAt": null,
  "telegramUserId": 777001,
  "telegramUsername": "jane_doe",
  "linkedAt": "2026-06-15T12:35:00Z"
}
```

Errores de negocio:

| HTTP | `errorCode` | Significado |
|---|---|---|
| `404` | `CLIENT_NOT_FOUND` | El cliente no existe o pertenece a otra empresa. |
| `403` | `USER_NOT_ACTIVE` | El usuario actual configurado no está activo. |
| `403` | `CLIENT_NOT_ASSIGNED_TO_AGENT` | El usuario configurado es un agente y no está asignado a este cliente. |
| `400` | `CLIENT_NOT_ACTIVE`, `CROSS_COMPANY_ACCESS`, `ROLE_NOT_ALLOWED`, `INVALID_REQUEST` | La regla de negocio actual o la forma de la solicitud es inválida. |

Curl:

```bash
curl -i "$BASE_URL/api/v1/admin/telegram-link/clients/$CLIENT_ID/status"
```

Notas y consideraciones:

- Si existe una cuenta de Telegram activa, `status` es `LINKED` incluso si también existen registros de invitaciones antiguas.
- Las invitaciones pendientes se resuelven dinámicamente usando `expiresAt`; los registros pendientes expirados no se devuelven como `INVITATION_PENDING`.
- `telegramUsername` puede ser null y puede quedar desactualizado si el usuario cambia luego su nombre de usuario de Telegram.

### 4. Validar token de invitación desde bot/Hermes

```http
POST /api/v1/bot/telegram-link/validate
```

Verifica un token sin procesar de inicio de Telegram antes de crear el enlace permanente. Usa este endpoint para decidir si el bot debería pedir confirmación al usuario.

| Ítem | Detalles |
|---|---|
| Quién debería llamarlo | Servicio de bot de Telegram o adaptador Hermes. |
| Cuándo llamarlo | Después de que Telegram envía `/start <token>` al bot. |
| Parámetros de ruta | Ninguno. |
| Parámetros de consulta | Ninguno. |
| Campos requeridos del cuerpo | `token` string no vacío, `telegramUserId` entero positivo. |
| Éxito | `200 OK` con `status: "VALID"`. |
| Falla de negocio | `200 OK` con `status: "INVALID"` y `errorCode`. |

Cuerpo de la solicitud:

```json
{
  "token": "ABCD2345EFGH",
  "telegramUserId": 777001
}
```

Respuesta exitosa:

```json
{
  "status": "VALID",
  "confirmationRequired": true,
  "errorCode": null,
  "clientId": "20000000-0000-0000-0000-000000000004",
  "clientFullName": "Jane Doe",
  "expiresAt": "2026-06-17T12:00:00Z"
}
```

Respuesta de falla de negocio:

```json
{
  "status": "INVALID",
  "confirmationRequired": false,
  "errorCode": "INVITATION_EXPIRED",
  "clientId": null,
  "clientFullName": null,
  "expiresAt": null
}
```

Significado de errores de negocio:

| `errorCode` | Significado |
|---|---|
| `INVALID_TOKEN` | El token está en blanco, mal formado, es desconocido o no está en un estado utilizable. |
| `INVITATION_ALREADY_USED` | La invitación ya fue confirmada y no puede reutilizarse. |
| `INVITATION_REVOKED` | La invitación fue revocada por un usuario interno. |
| `INVITATION_EXPIRED` | La invitación superó su hora de expiración. |
| `CLIENT_NOT_AVAILABLE` | El cliente vinculado falta, pertenece a otra empresa o no está activo. |
| `CLIENT_ALREADY_LINKED` | El cliente ya tiene una cuenta de Telegram activa. |
| `TELEGRAM_ACCOUNT_ALREADY_LINKED` | Este ID de usuario de Telegram ya está vinculado a un cliente en la misma empresa. |

Curl:

```bash
TOKEN=ABCD2345EFGH
TELEGRAM_USER_ID=777001

curl -i -X POST "$BASE_URL/api/v1/bot/telegram-link/validate" \
  -H 'Content-Type: application/json' \
  -d '{"token":"'"$TOKEN"'","telegramUserId":'"$TELEGRAM_USER_ID"'}'
```

Notas y consideraciones:

- Este endpoint no crea `client_telegram_accounts` ni marca la invitación como `USED`.
- El bot no debería exponer texto de error interno detallado al usuario de Telegram. Usa `errorCode` para elegir un mensaje seguro y genérico orientado al usuario.
- La validación escribe eventos de auditoría tanto en éxito como en falla.
- `telegramChatId`, nombre de usuario, nombre y apellido no son aceptados por este endpoint en la implementación actual. Envía esos campos a `confirm`.

### 5. Confirmar enlace de Telegram desde bot/Hermes

```http
POST /api/v1/bot/telegram-link/confirm
```

Crea el enlace permanente con la cuenta de Telegram después de que el usuario confirma en el bot.

| Ítem | Detalles |
|---|---|
| Quién debería llamarlo | Servicio de bot de Telegram o adaptador Hermes. |
| Cuándo llamarlo | Después de que `validate` devuelve `VALID` y el cliente confirma el enlace en la UX del bot. |
| Parámetros de ruta | Ninguno. |
| Parámetros de consulta | Ninguno. |
| Campos requeridos del cuerpo | `token` string no vacío, `telegramUserId` entero positivo. |
| Campos opcionales del cuerpo | `telegramChatId`, `telegramUsername`, `telegramFirstName`, `telegramLastName`. |
| Éxito | `200 OK` con `status: "CONFIRMED"`. |
| Falla de negocio | `200 OK` con `status: "INVALID"` y `errorCode`. |

Cuerpo de la solicitud:

```json
{
  "token": "ABCD2345EFGH",
  "telegramUserId": 777001,
  "telegramChatId": 777001,
  "telegramUsername": "jane_doe",
  "telegramFirstName": "Jane",
  "telegramLastName": "Doe"
}
```

Respuesta exitosa:

```json
{
  "status": "CONFIRMED",
  "errorCode": null,
  "clientId": "20000000-0000-0000-0000-000000000004",
  "telegramAccountId": "6550b0e6-ff5a-4fae-8273-31e7b1f3bb19",
  "telegramUserId": 777001,
  "linkedAt": "2026-06-15T12:35:00Z"
}
```

Respuesta de falla de negocio:

```json
{
  "status": "INVALID",
  "errorCode": "TELEGRAM_ACCOUNT_ALREADY_LINKED",
  "clientId": null,
  "telegramAccountId": null,
  "telegramUserId": null,
  "linkedAt": null
}
```

Significado de errores de negocio:

| `errorCode` | Significado |
|---|---|
| `INVALID_TOKEN` | El token está en blanco, es desconocido o no puede usarse. |
| `INVITATION_ALREADY_USED` | Otra confirmación ya consumió esta invitación. |
| `INVITATION_REVOKED` | La invitación fue revocada antes de la confirmación. |
| `INVITATION_EXPIRED` | La invitación expiró antes de la confirmación. |
| `CLIENT_NOT_AVAILABLE` | El cliente vinculado falta, pertenece a otra empresa o no está activo. |
| `CLIENT_ALREADY_LINKED` | El cliente ya tiene una cuenta de Telegram activa. |
| `TELEGRAM_ACCOUNT_ALREADY_LINKED` | Este ID de usuario de Telegram ya está vinculado a un cliente en la misma empresa. |

Curl:

```bash
curl -i -X POST "$BASE_URL/api/v1/bot/telegram-link/confirm" \
  -H 'Content-Type: application/json' \
  -d '{
    "token":"'"$TOKEN"'",
    "telegramUserId":'"$TELEGRAM_USER_ID"',
    "telegramChatId":777001,
    "telegramUsername":"jane_doe",
    "telegramFirstName":"Jane",
    "telegramLastName":"Doe"
  }'
```

Notas y consideraciones:

- La confirmación es el paso irreversible que crea el enlace activo con la cuenta de Telegram y marca la invitación como `USED`.
- El caso de uso revalida el token dentro de la transacción de escritura. Un token que era válido segundos antes todavía puede fallar si expira, es revocado o se consume antes de la confirmación.
- Si dos confirmaciones compiten, solo una debería tener éxito porque la invitación se carga con un bloqueo de escritura.
- `telegramUserId` es la identidad estable. Los nombres de usuario y nombres se almacenan solo como datos de instantánea de perfil.

## Referencia de estados y códigos de error

### Estados HTTP semánticos de admin

| Estado HTTP | Valores típicos de `errorCode` | Acción del consumidor |
|---|---|---|
| `200 OK` | Ninguno | La solicitud tuvo éxito para `GET` y `DELETE`. |
| `201 Created` | Ninguno | La invitación fue creada. Usa el `link` devuelto. |
| `400 Bad Request` | `INVALID_REQUEST`, `CLIENT_NOT_ACTIVE`, `CROSS_COMPANY_ACCESS`, `ROLE_NOT_ALLOWED` | Corrige input mal formado o precondiciones de negocio actuales. |
| `403 Forbidden` | `CURRENT_USER_NOT_AVAILABLE`, `USER_NOT_ACTIVE`, `CLIENT_NOT_ASSIGNED_TO_AGENT` | Corrige la configuración del usuario del lado servidor o la autorización. |
| `404 Not Found` | `CLIENT_NOT_FOUND`, `INVITATION_NOT_FOUND` | Verifica IDs y pertenencia al tenant. |
| `409 Conflict` | `CLIENT_ALREADY_LINKED`, `INVITATION_ALREADY_PENDING` | Muestra el estado existente, revoca la invitación pendiente o detén la vinculación duplicada. |

### Cargas útiles de negocio siempre-200 del bot

| Estado HTTP | Estado de la carga útil | Significado |
|---|---|---|
| `200 OK` | `VALID` | El token puede confirmarse. |
| `200 OK` | `CONFIRMED` | El cliente ahora está vinculado permanentemente al usuario de Telegram. |
| `200 OK` | `INVALID` | La solicitud era sintácticamente válida, pero el flujo de negocio no puede continuar. Lee `errorCode`. |
| `400 Bad Request` | `ProblemDetail` | El bot envió JSON mal formado o falló la validación de la solicitud. Corrige el código de solicitud del bot. |

## Referencia de datos para consumidores

Estas tablas explican los datos que observarás o necesitarás sembrar. No son una guía de arquitectura interna.

| Tabla | Contexto para consumidores |
|---|---|
| `companies` | Registros de tenant/empresa. Cada usuario, cliente, invitación, cuenta de Telegram y la mayoría de eventos de auditoría están acotados a una empresa. |
| `company_users` | Usuarios internos autorizados a crear/revocar invitaciones y consultar estados. La configuración del usuario actual de dev apunta conceptualmente a esta tabla. |
| `clients` | Clientes de la empresa que pueden recibir invitaciones de Telegram. |
| `company_user_client_assignments` | Requerida para que usuarios `AGENT` operen sobre clientes específicos. Los roles de gestión no necesitan asignación. |
| `telegram_invitation_tokens` | Registros temporales de invitación. Almacena `token_hash`, `token_prefix`, estado, expiración y timestamps de uso/revocación. |
| `client_telegram_accounts` | Enlaces permanentes de Telegram. Almacena `telegram_user_id` estable, campos opcionales de chat/instantánea de perfil y estado `ACTIVE`. |
| `telegram_link_events` | Registro de auditoría para creación, validación, revocación, detección de duplicados y resultados de confirmación. |

Estados útiles:

| Área | Valores |
|---|---|
| Rol de company user | `OWNER`, `ADMIN`, `SUPERVISOR`, `AGENT` |
| Estado de company user | `ACTIVE`, `INVITED`, `SUSPENDED`, `DELETED` |
| Estado de client | `ACTIVE`, `INACTIVE`, `BLOCKED`, `DELETED` |
| Estado de invitation | `PENDING`, `USED`, `EXPIRED`, `REVOKED` |
| Respuesta de estado de enlace admin | `NOT_LINKED`, `INVITATION_PENDING`, `LINKED` |
| Estado de validación del bot | `VALID`, `INVALID` |
| Estado de confirmación del bot | `CONFIRMED`, `INVALID` |

## Swagger y OpenAPI

| Recurso | URL local |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

Usa Swagger para inspeccionar el contrato legible por máquina. Usa este README para entender la secuencia de negocio, la semántica de errores y las responsabilidades de los consumidores.

## Sandbox local

### Requisitos

| Requisito | Versión / nota |
|---|---|
| Java | 17 |
| Herramienta de build | Maven wrapper: `./mvnw` |
| Base de datos | PostgreSQL para tiempo de ejecución |
| Base de datos de test | H2 es usado por la suite automatizada de tests |

### Iniciar localmente

```bash
# 1) Crea la base de datos PostgreSQL local esperada por el perfil dev.
createdb -h localhost -p 5432 -U root telegram_ia

# 2) Inicia la app. El perfil activo por defecto es dev.
./mvnw spring-boot:run

# 3) Abre Swagger si quieres ver el contrato generado.
xdg-open http://localhost:8080/swagger-ui.html
```

### Sembrar datos mínimos de dev

Ejecuta esto después de que la aplicación haya creado/actualizado el esquema de dev:

```sql
insert into companies (id, name, legal_name, status, created_at, updated_at)
values ('20000000-0000-0000-0000-000000000001', 'Main Company', null, 'ACTIVE', now(), now())
on conflict (id) do nothing;

insert into company_users (id, company_id, email, full_name, role, status, created_at, updated_at)
values ('20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', 'admin@example.com', 'Admin', 'ADMIN', 'ACTIVE', now(), now())
on conflict (id) do nothing;

insert into clients (id, company_id, external_reference, document_type, document_number, full_name, phone_number, email, status, created_at, updated_at)
values ('20000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001', null, null, null, 'Jane Doe', null, null, 'ACTIVE', now(), now())
on conflict (id) do nothing;
```

### Configuración de dev

El perfil `dev` por defecto usa:

| Configuración | Valor |
|---|---|
| JDBC URL | `jdbc:postgresql://localhost:5432/telegram_ia` |
| Usuario | `root` |
| Contraseña | `admin` |
| Modo DDL de Hibernate | `update` |
| Pepper del token | `dev-only-telegram-link-token-pepper` |
| Usuario de empresa simulado | `20000000-0000-0000-0000-000000000003` |
| Empresa simulada | `20000000-0000-0000-0000-000000000001` |
| Rol/estado simulado | `ADMIN` / `ACTIVE` |
| Nombre de usuario de bot por defecto | `JarviServeProBiker_bot` |

### Configuración estilo producción

El perfil `prod` espera variables de entorno:

| Variable de entorno | Propósito |
|---|---|
| `TELEGRAM_IA_DATASOURCE_URL` | PostgreSQL JDBC URL. |
| `TELEGRAM_IA_DATASOURCE_USERNAME` | Usuario de la base de datos. |
| `TELEGRAM_IA_DATASOURCE_PASSWORD` | Contraseña de la base de datos. |
| `TELEGRAM_LINK_TOKEN_PEPPER` | Pepper del lado servidor usado para hashear tokens de invitación sin procesar. Cambiarlo invalida los tokens pendientes sin procesar existentes. |
| `TELEGRAM_LINK_BOT_USERNAME` | Nombre de usuario del bot de Telegram usado en los enlaces generados. |
| `TELEGRAM_LINK_CURRENT_USER_COMPANY_USER_ID` | ID del usuario actual simulado del MVP. |
| `TELEGRAM_LINK_CURRENT_USER_COMPANY_ID` | ID de empresa simulada del MVP. |
| `TELEGRAM_LINK_CURRENT_USER_ROLE` | Rol simulado del MVP, por ejemplo `ADMIN`. |
| `TELEGRAM_LINK_CURRENT_USER_STATUS` | Estado simulado del MVP, por ejemplo `ACTIVE`. |

Ejemplo:

```bash
SPRING_PROFILES_ACTIVE=prod \
TELEGRAM_IA_DATASOURCE_URL='jdbc:postgresql://db.example.com:5432/telegram_ia' \
TELEGRAM_IA_DATASOURCE_USERNAME='telegram_ia_app' \
TELEGRAM_IA_DATASOURCE_PASSWORD='change-me' \
TELEGRAM_LINK_TOKEN_PEPPER='change-me-to-a-long-secret' \
TELEGRAM_LINK_BOT_USERNAME='your_bot_username' \
TELEGRAM_LINK_CURRENT_USER_COMPANY_USER_ID='00000000-0000-0000-0000-000000000000' \
TELEGRAM_LINK_CURRENT_USER_COMPANY_ID='00000000-0000-0000-0000-000000000000' \
TELEGRAM_LINK_CURRENT_USER_ROLE='ADMIN' \
TELEGRAM_LINK_CURRENT_USER_STATUS='ACTIVE' \
java -jar target/Telegram-IA-0.0.1-SNAPSHOT.jar
```

### Comandos de build y test

| Comando | Propósito |
|---|---|
| `./mvnw spring-boot:run` | Iniciar con el perfil `dev` por defecto. |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` | Iniciar con el perfil `prod`. |
| `./mvnw test` | Ejecutar la suite automatizada de tests. |
| `./mvnw package` | Compilar, ejecutar tests y construir `target/Telegram-IA-0.0.1-SNAPSHOT.jar`. |
| `./mvnw -DskipTests package` | Construir el jar sin ejecutar tests. Úsalo solo cuando los tests ya fueron ejecutados o se omitieron intencionalmente. |

## Nota mínima de implementación interna

El servicio sigue una separación Domain / Application / Infrastructure. Esto importa a los consumidores de la API solo porque los controladores se mantienen delgados: las cargas útiles HTTP se mapean a casos de uso de aplicación, las decisiones de negocio se toman en la aplicación/modelo de dominio, y la infraestructura maneja REST, JPA, hashing, tiempo, transacciones y OpenAPI.

Referencias detalladas de diseño interno:

- `docs/telegram-backend-data-model.md`
- `docs/telegram-backend-technical-design.md`

## Precauciones operativas

- Reemplaza el proveedor de usuario actual simulado por autenticación real antes de exponer endpoints admin.
- Protege los endpoints orientados al bot en la capa de red, gateway o service-to-service antes de usarlos en producción.
- Agrega migraciones gestionadas con Flyway o Liquibase antes de producción. El perfil dev usa Hibernate `ddl-auto: update`; el perfil prod usa `validate`.
- Protege `TELEGRAM_LINK_TOKEN_PEPPER` como secreto. Cambiarlo hace que los tokens pendientes sin procesar existentes fallen la validación.
- Nunca almacenes, registres en logs ni expongas tokens de invitación sin procesar fuera del camino de entrega del enlace de Telegram generado.
- Restringe Swagger en producción si el servicio se expone fuera de redes confiables.

## Solución de problemas

| Síntoma | Causa probable | Corrección |
|---|---|---|
| La app no puede conectarse a PostgreSQL | La base de datos local, el usuario o la contraseña no coinciden con `application-dev.yaml`. | Crea `telegram_ia` y verifica las credenciales `root` / `admin`. |
| El endpoint admin devuelve `CURRENT_USER_NOT_AVAILABLE` | `telegram-link.current-user.*` falta o está incompleto. | Usa los valores por defecto del perfil dev o proporciona todas las variables de entorno de usuario actual. |
| Crear invitación devuelve `CLIENT_NOT_FOUND` | El cliente no existe, pertenece a otra empresa o faltan datos semilla. | Siembra un cliente activo bajo la empresa configurada. |
| Crear invitación devuelve `INVITATION_ALREADY_PENDING` | El cliente ya tiene una invitación pendiente no expirada. | Usa el enlace existente, revoca la invitación, espera la expiración o limpia los datos locales. |
| La validación del bot devuelve `INVALID_TOKEN` | El token sin procesar se escribió mal, fue generado con otro pepper, ya fue consumido o nunca existió. | Copia el token desde el parámetro de consulta `link` y conserva el mismo token pepper. |
| La validación del bot devuelve `INVITATION_EXPIRED` | El token supera su TTL de 48 horas. | Crea una nueva invitación. |
| La confirmación del bot devuelve `TELEGRAM_ACCOUNT_ALREADY_LINKED` | El mismo ID de usuario de Telegram ya está activo para otro cliente de la empresa. | No reutilices esa cuenta de Telegram o resuelve manualmente el enlace existente. |
