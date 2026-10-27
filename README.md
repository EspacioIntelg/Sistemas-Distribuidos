# Biblioteca Encantada

Aplicación para consultar un catálogo de libros y gestionar sus préstamos, con una temática visual especial de Halloween. La solución usa React + Vite en el frontend y una arquitectura de microservicios Spring Boot en el backend.

## Frontend y backend

- **Frontend:** `frontend/`, una aplicación React construida y servida con Vite. Muestra el catálogo, sus filtros y detalles, y permite gestionar préstamos. La interfaz usa una temática de Halloween y consume la API a través del Gateway.
- **Backend:** `eureka-server/`, `gateway/`, `ms-buscador/` y `ms-operador/`. Está construido con Spring Boot y Spring Cloud, y proporciona los servicios, el descubrimiento y el enrutamiento de la API.

## Arquitectura

```text
React + Vite (:5173)
       │ /api/...
       ▼
Gateway (:8080) ─────► Eureka (:8761) ◄──── registro de microservicios
       │
       ├── /api/libros/**    ──► ms-buscador (:8082)
       └── /api/prestamos/** ──► ms-operador (:8081)
                                      │          │
                                      └── Eureka ┘
                                      Circuit breaker y timeouts
```

- **eureka-server**: registro y descubrimiento de instancias de los servicios.
- **gateway**: punto de entrada para el frontend y enrutamiento mediante el balanceador de carga y Eureka.
- **ms-buscador**: catálogo, filtros opcionales, alta de libros y control atómico de disponibilidad.
- **ms-operador**: creación, consulta y devolución de préstamos; se comunica con el catálogo mediante su nombre de servicio registrado en Eureka.
- **frontend**: catálogo filtrable con detalle y alta de libros, además de registro, listado y devolución de préstamos.

### Estructuras y conceptos de sistemas distribuidos

- **Arquitectura de microservicios:** el catálogo (`ms-buscador`) y los préstamos (`ms-operador`) son servicios independientes, cada uno responsable de su dominio y persistencia.
- **Descubrimiento de servicios:** Eureka registra las instancias; los consumidores se dirigen a ellas por nombre lógico, no por una dirección fija.
- **API Gateway:** Spring Cloud Gateway centraliza la entrada HTTP y enruta las peticiones del frontend a cada servicio.
- **Balanceo de carga del cliente:** Gateway y `ms-operador` usan el balanceador de Spring Cloud para resolver servicios descubiertos por Eureka.
- **Comunicación síncrona:** `ms-operador` consulta y actualiza la disponibilidad de los libros mediante HTTP en `ms-buscador`.
- **Tolerancia a fallos:** las llamadas desde `ms-operador` tienen timeout de conexión (2 s) y de lectura (3 s), además de un circuit breaker Resilience4j. Tras fallos repetidos, el circuito se abre temporalmente para evitar acumular llamadas a un catálogo no disponible; la API responde `503 Service Unavailable` y vuelve a probar cuando pasa el periodo de espera. Errores funcionales HTTP 4xx del catálogo no cuentan como fallos del circuito.
- **Consistencia entre servicios:** al crear o devolver un préstamo se actualiza también la disponibilidad del libro. Si falla el guardado local, el servicio intenta compensar el cambio remoto y conserva los errores de compensación.
- **Persistencia por servicio:** cada microservicio usa su propia base H2 en memoria; los datos se reinician al detener el proceso. El catálogo carga libros de ejemplo desde `ms-buscador/src/main/resources/data.sql`.

El circuit breaker usa una ventana de 5 llamadas, requiere al menos 3 antes de evaluar fallos y se abre al alcanzar un 50 % de errores. Permanece abierto 10 segundos y luego permite llamadas de prueba para comprobar si el catálogo se recuperó.

## Requisitos

- Java 17 o superior.
- Maven.
- Node.js **20.19+** o **22.12+** (Vite 8).
- npm.

## Arranque local

Inicia cada componente en una terminal independiente y en este orden:

```powershell
# 1. Registro
cd eureka-server
mvn spring-boot:run
```

```powershell
# 2. Catálogo
cd ms-buscador
mvn spring-boot:run
```

```powershell
# 3. Préstamos
cd ms-operador
mvn spring-boot:run
```

```powershell
# 4. Gateway
cd gateway
mvn spring-boot:run
```

```powershell
# 5. Frontend
cd frontend
npm install
npm start
```

Abre <http://localhost:5173>. La aplicación llama a rutas relativas `/api/...`; el proxy de Vite las reenvía al Gateway en `http://localhost:8080`. El navegador no debe llamar directamente a los microservicios.

## Funcionalidades

### Catálogo

- Buscar por título, autor, año de publicación y disponibilidad; los filtros se pueden combinar.
- Consultar autor, año, ISBN y sinopsis del libro seleccionado.
- Añadir libros al catálogo.
- El servicio comprueba de forma atómica que el libro siga disponible al reservarlo, para evitar que dos peticiones creen préstamos simultáneos para el mismo ejemplar.

### Préstamos

- Crear un préstamo indicando un libro disponible y el nombre del usuario.
- Consultar el historial, con fechas y estado.
- Registrar la devolución de un préstamo activo.
- No permite devolver dos veces el mismo préstamo.
- Si falla el guardado local tras cambiar la disponibilidad, intenta compensar el cambio en el otro microservicio; los errores de compensación se conservan como causa suprimida.
- Si el servicio de catálogo no responde o el circuit breaker está abierto, la operación devuelve `503 Service Unavailable` para indicar que se intente de nuevo más tarde.

## API principal

Las rutas públicas son las del Gateway:

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/libros?titulo=...&autor=...&anio=...&disponible=...` | Buscar libros; todos los filtros son opcionales. |
| `GET` | `/api/libros/{id}` | Consultar un libro. |
| `POST` | `/api/libros` | Añadir un libro. |
| `PUT` | `/api/libros/{id}/disponibilidad` | Actualizar disponibilidad; un intento de reservar un libro ya prestado devuelve `409 Conflict`. |
| `GET` | `/api/prestamos` | Listar préstamos. |
| `GET` | `/api/prestamos/{id}` | Consultar un préstamo. |
| `POST` | `/api/prestamos` | Crear un préstamo con `{ "libroId": 1, "usuario": "Nombre" }`. |
| `PUT` | `/api/prestamos/{id}` | Devolver un préstamo activo. |

El panel de Eureka está disponible en <http://localhost:8761>. La consola H2 del catálogo, mientras el servicio está activo, está en <http://localhost:8082/h2-console> (JDBC URL: `jdbc:h2:mem:buscador`, usuario `sa`, contraseña vacía).

## Validación

Desde cada directorio Java:

```powershell
mvn test
```

En `frontend`:

```powershell
npm run lint
npm run build
```
