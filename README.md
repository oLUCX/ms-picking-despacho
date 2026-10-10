# ms-picking-despacho

Microservicio de **logística de bodega** del caso BodegaNube (JVY0101). Registra el picking de cada orden,
confirma el empaque, asigna el transportista y despacha con un número de seguimiento.

| | |
|---|---|
| Puerto | `8084` |
| Base de datos | PostgreSQL `bodeganube_picking` (una base por microservicio) |
| Stack | Java 17, Spring Boot 3.2, Spring Data JPA, Bean Validation, Maven Wrapper |

## Requerimientos que cubre
- **RF-03**: consulta y preparación de las órdenes disponibles para picking.
- **RF-05**: registro obligatorio de un número de seguimiento (tracking) al despachar.

## Ciclo de vida de un despacho
```
EN_PREPARACION ──confirmar-empaque──► EMPACADO ──despachar──► DESPACHADO
```
- Solo se despacha una orden empacada y con transportista asignado; al despachar se genera el tracking
  `TRK-XXXXXXXX` y se guarda la fecha.
- Un despacho que ya salió de bodega (`DESPACHADO`) no se puede modificar ni eliminar.

## Arquitectura en capas
```
src/main/java/com/bodeganube/picking
├── controller/   PickingController        recibe HTTP, valida con @Valid y delega al service
├── service/      DespachoService          ciclo de vida del despacho y transacciones (@Transactional)
├── repository/   DespachoRepository       acceso a datos con Spring Data JPA
├── model/        Despacho, EstadoDespacho entidad JPA mapeada a la tabla despachos
├── dto/          *Request / *Response     contrato JSON de la API, separado de la entidad
└── exception/    GlobalExceptionHandler   todos los errores con el mismo formato JSON
```

## Modelo de datos
Tabla `despachos` (entidad `Despacho`). Hibernate la crea al iniciar (`ddl-auto: update`).

| Columna | Tipo | Restricciones |
|---|---|---|
| id | bigint | PK, autoincremental |
| orden_id | bigint | NOT NULL, UNIQUE. Id de la orden en ms-ordenes (no es FK: cada servicio tiene su base) |
| transportista | varchar(60) | obligatorio para despachar |
| tracking_number | varchar | se genera al despachar |
| estado | varchar | NOT NULL: `EN_PREPARACION`, `EMPACADO` o `DESPACHADO` |
| fecha_creacion | timestamp | se completa al crear (`@PrePersist`) |
| fecha_despacho | timestamp | se completa al despachar |

## Endpoints
Base: `http://localhost:8084` (o `http://localhost:8080` a través de ms-gateway). Los despachos se
identifican por el id de la orden.

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| GET | `/api/picking` | Lista despachos; filtro opcional `?estado=EMPACADO` | 200, 400 si el estado no existe |
| GET | `/api/picking/{ordenId}` | Obtiene el despacho de una orden | 200, 404 |
| POST | `/api/picking` | Abre el picking: `{"ordenId": 10, "transportista": "Starken"}` | 201 con `Location`, 400, 409 si ya existe |
| PUT | `/api/picking/{ordenId}` | Asigna o cambia el transportista | 200, 400, 404, 409 si ya se despachó |
| DELETE | `/api/picking/{ordenId}` | Elimina el registro de picking | 204, 404, 409 si ya se despachó |
| POST | `/api/picking/{ordenId}/confirmar-empaque` | Pasa a `EMPACADO` | 200, 404, 409 |
| POST | `/api/picking/{ordenId}/despachar` | Genera el tracking y pasa a `DESPACHADO` | 200, 404, 409 |

Todas las respuestas de error tienen el mismo formato:
```json
{
  "timestamp": "2026-10-08T18:30:12.448",
  "status": 409,
  "error": "Conflict",
  "mensaje": "Solo se puede despachar una orden empacada; estado actual: EN_PREPARACION",
  "ruta": "/api/picking/10/despachar"
}
```
En los errores de validación (400) se agrega `detalles`, con el mensaje de cada campo inválido.

## Levantar el servicio desde cero

### 1. Requisitos
- JDK 17 o superior (`java -version`).
- PostgreSQL 14 o superior en `localhost:5432` (usuario y contraseña `postgres` por defecto).
- Git. **No hace falta instalar Maven**: el repositorio trae el Maven Wrapper (`mvnw` y `mvnw.cmd`).

### 2. Clonar el repositorio
```bash
git clone https://github.com/oLUCX/ms-picking-despacho.git
cd ms-picking-despacho
```

### 3. Crear la base de datos
Con psql:
```bash
psql -U postgres -c "CREATE DATABASE bodeganube_picking;"
```
O en pgAdmin: clic derecho en *Databases* → *Create* → *Database...* → `bodeganube_picking`.
Las tablas las crea la aplicación al iniciar.

Si no tienes PostgreSQL instalado, puedes usar Docker:
```bash
docker run -d --name bodeganube-db -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16
docker exec bodeganube-db psql -U postgres -c "CREATE DATABASE bodeganube_picking;"
```

### 4. Compilar, probar y empaquetar
```bash
.\mvnw.cmd clean package    # Windows (PowerShell)
./mvnw clean package        # Linux, macOS o Git Bash
```
Descarga las dependencias, compila, corre las pruebas automatizadas y genera `target/ms-picking-despacho.jar`.

### 5. Ejecutar
```bash
java -jar target/ms-picking-despacho.jar
```
También se puede levantar sin empaquetar con `.\mvnw.cmd spring-boot:run`. Queda escuchando en
`http://localhost:8084`.

### 6. Probar con Postman
Importa `postman/ms-picking-despacho.postman_collection.json` (*Import* → archivo) y usa *Run collection*.
Ejecuta en orden 15 peticiones (crear, consultar, empacar, asignar transportista, despachar y eliminar, más
los casos de error 400, 404 y 409) y cada una verifica su código HTTP. Se puede repetir: el ordenId cambia
en cada ejecución.

## Configuración
Todo tiene un valor por defecto para desarrollo local y se puede cambiar con variables de entorno:

| Variable | Valor por defecto |
|---|---|
| `PORT` | `8084` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/bodeganube_picking` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |

## Pruebas automatizadas
`.\mvnw.cmd test` corre 13 pruebas que no necesitan base de datos:
- `DespachoServiceTest` (JUnit 5 + Mockito): duplicados, orden del ciclo de vida, transportista obligatorio
  y formato del tracking.
- `PickingControllerTest` (MockMvc): códigos HTTP, validaciones y formato de error.

## Ramas
`main` tiene la versión entregada, `develop` integra el trabajo en curso y cada cambio entra desde una
rama `feature/...` con un Pull Request hacia `develop`.

## Próximos pasos
- Recibir el evento `OrdenListaParaPicking` desde una cola (SQS) en vez de crear el picking a mano, para
  que ms-ordenes no dependa de que este servicio esté arriba (feedback de la Evaluación 1).
- Avisar a ms-ordenes y a ms-inventario cuando la orden se despacha.
