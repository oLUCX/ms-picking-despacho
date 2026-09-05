# ms-picking-despacho

Microservicio de **Logística de Bodega y Salidas** del caso BodegaNube (asignatura JVY0101).

## Responsabilidad (bounded context)
Listar órdenes listas para picking, procesar confirmaciones de empaque, asignar número de
tracking y registrar el despacho. Ver sección 4.1 del informe de arquitectura.

## Requerimientos que cubre
- **RF-03**: consulta de órdenes disponibles para preparación (picking).
- **RF-05**: registro obligatorio de un número de seguimiento (tracking).

## Stack
Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL.

## Endpoints
| Método | Endpoint                                | Descripción                                         |
|--------|-------------------------------------------|-------------------------------------------------------|
| POST   | `/api/picking/{ordenId}/confirmar-empaque` | Marca la orden como empacada.                        |
| POST   | `/api/picking/{ordenId}/despachar`        | Asigna número de tracking y marca la orden despachada. |

## Cómo correrlo localmente
1. Crear una base PostgreSQL llamada `bodeganube_picking`.
2. `mvn spring-boot:run`

## Próximos pasos (fuera del alcance de este esqueleto)
- Consultar a `ms-ordenes` (vía HTTP) para validar que la orden está `LISTA_PARA_PICKING`
  antes de aceptar el empaque.
- Notificar a `ms-ordenes` el cambio de estado tras el despacho.
