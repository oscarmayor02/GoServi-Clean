# GoServi Backend

Backend Spring Boot 17 para la plataforma GoServi — marketplace de servicios profesionales.

## Módulos

| Módulo | Endpoints | Descripción |
|--------|-----------|-------------|
| `auth` | `/auth/**` | Registro, login, cambio de rol |
| `user` | `/users/**` | Perfiles, búsqueda por GPS, favoritos |
| `serviceoffer` | `/service-offers/**` | Anuncios de servicios, categorías |
| `booking` | `/bookings/**` | Reservas, código de seguridad, estados |
| `chat` | `/chat/**` + WS | Mensajería en tiempo real |
| `tracking` | `/tracking/**` + WS | GPS del profesional en tiempo real |
| `notification` | interno | Email (Gmail) + SMS (Twilio) |
| `payment` | `/payments/**` | Pagos con Wompi, webhook |
| `review` | `/reviews/**` | Reseñas y calificaciones |

## Flujo principal

```
Cliente busca → /service-offers/nearby?lat=X&lng=Y
Cliente crea reserva → POST /bookings
Profesional acepta → POST /bookings/{id}/accept
  → Ambos reciben código de seguridad por email
Profesional va al lugar → POST /tracking/update (cada X segundos)
Cliente ve mapa → GET /tracking/{bookingId} + WS /topic/tracking/{bookingId}
Profesional llega → Chat: POST /chat/threads (bookingId)
Profesional verifica código → POST /bookings/{id}/verify
Trabajo finaliza → POST /bookings/{id}/complete
Cliente paga → POST /payments → link de Wompi
Wompi notifica → POST /payments/webhook
Cliente deja reseña → POST /reviews
```

## Inicio rápido (Local)

### 1. Base de datos
```bash
docker-compose up -d
```

### 2. Variables de entorno
```bash
cp .env.example .env
# Edita .env con tus credenciales
```

### 3. Correr el backend
```bash
./mvnw spring-boot:run
```

El servidor inicia en `http://localhost:8080`

### 4. Swagger
```
http://localhost:8080/swagger-ui.html
```

## Despliegue en Railway

1. Crear proyecto en Railway
2. Conectar repositorio GitHub
3. Agregar servicio PostgreSQL en Railway
4. En Variables, agregar todas las de `.env.example`
5. Railway detecta el `pom.xml` y despliega automáticamente

## WebSocket (Ionic)

```javascript
// Chat
const socket = new SockJS('https://tu-url/ws');
const client = Stomp.over(socket);
client.connect({}, () => {
  client.subscribe('/topic/chat/{threadId}', (msg) => {
    console.log(JSON.parse(msg.body));
  });
});

// Tracking
client.subscribe('/topic/tracking/{bookingId}', (msg) => {
  const location = JSON.parse(msg.body); // { latitude, longitude, etaMinutes }
});
```

## Roles

- `CLIENT`: buscar, reservar, pagar, reseñar
- `PROFESSIONAL`: crear anuncios, aceptar reservas, actualizar ubicación
- Un usuario puede tener ambos roles. Cambiar con `POST /auth/switch-role`

## Seguridad

- JWT Bearer token en header: `Authorization: Bearer <token>`
- Código de seguridad de 6 dígitos enviado por email al confirmar reserva
- Credenciales NUNCA en el código — todo por variables de entorno
