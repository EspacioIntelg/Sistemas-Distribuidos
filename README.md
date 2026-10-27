# Biblioteca Online

Aplicación para consultar un catálogo de libros y gestionar sus préstamos. La solución usa React + Vite en el frontend y una arquitectura de microservicios Spring Boot con Gateway, Eureka y dos servicios.

## Arquitectura

```text
React + Vite (:5173)
       │ /api/...
       ▼
Gateway (:8080) ─────► Eureka (:8761)
       │
       ├── /api/libros/**    ──► ms-buscador (:8082)
       └── /api/prestamos/** ──► ms-operador (:8081)
                                      │
                                      └──► ms-buscador por nombre Eureka
```

- **eureka-server**: registro de servicios.
- **gateway**: punto de entrada para el frontend y enrutamiento por Eureka.
- **ms-buscador**: catálogo, filtros opcionales, alta de libros y control atómico de disponibilidad.
- **ms-operador**: creación, consulta y devolución de préstamos; se comunica con el catálogo mediante `ms-buscador` en Eureka.
- **frontend**: catálogo filtrable con detalle y alta de libros, además de registro, listado y devolución de préstamos.

Cada microservicio utiliza una base H2 en memoria. Los datos se reinician al detener el proceso. El catálogo carga libros de ejemplo desde `ms-buscador/src/main/resources/data.sql`.

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
