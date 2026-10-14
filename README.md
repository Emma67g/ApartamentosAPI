# API de Gestión de Renta de Habitaciones y Pisos

API REST para administrar propiedades, clientes, reservaciones y pagos de una empresa que renta habitaciones y casas o pisos completos, con reportes de ocupación, ingresos y morosidad. Incluye una interfaz web sencilla.

## Descripción de la API

El sistema sustituye los procesos manuales y las hojas de cálculo, evitando errores y duplicidad de información, y facilita el seguimiento de las rentas.

| Módulo | Qué permite |
|---|---|
| **Clientes** | Administrar arrendatarios y propietarios (tipos `PROPIETARIO`, `CLIENTE` y `EMPRESA`) y consultar el estado de cuenta de cada cliente. |
| **Propiedades** | Registrar pisos y casas completas con sus características, y sus imágenes (con una portada). |
| **Habitaciones** | Registrar las habitaciones de una propiedad que se rentan por separado. |
| **Disponibilidad** | Bloquear fechas, definir tarifas especiales y consultar si un rango de fechas está libre. |
| **Reservas** | Reservar una propiedad completa o una habitación, validando disponibilidad y calculando el monto. |
| **Pagos** | Programar cuotas de una reserva, registrar pagos y detectar los vencidos. |
| **Reportes** | Ocupación, ingresos y deudores (morosidad). |

**Reglas principales**

- No se permiten reservas con fechas cruzadas: reservar la propiedad completa choca con cualquiera de sus habitaciones, y viceversa. Los bloqueos del calendario también impiden reservar.
- El monto de la reserva lo calcula el sistema, noche por noche, con el precio base o la tarifa especial vigente.
- Los estados de una reserva siguen el flujo `PENDIENTE` → `CONFIRMADA` → `COMPLETADA`; puede pasar a `CANCELADA` desde `PENDIENTE` o `CONFIRMADA`. Al cancelar, sus pagos pendientes se cancelan.
- Los pagos son cuotas de una reserva y su suma no puede superar el total. Un pago `PENDIENTE` con fecha de vencimiento pasada se considera vencido.
- Solo un `PROPIETARIO` o una `EMPRESA` activos pueden ser dueños de una propiedad.
- El proyecto no implementa inicio de sesión.

**Arquitectura** (código Java en `src/main/java/com/blue/apartamentos`)

| Carpeta | Responsabilidad |
|---|---|
| `models` | Entidades JPA con sus validaciones. |
| `repositories` | Acceso a datos con Spring Data JPA. |
| `services` | Reglas de negocio y excepciones. |
| `controllers` | Endpoints REST y manejo global de errores. |

La interfaz web está en `src/main/resources/static` (archivos HTML).

## Tecnologías utilizadas

- **Java 21**
- **Spring Boot 4.1.1**: Spring Web, Spring Data JPA (Hibernate), Bean Validation y DevTools
- **MySQL 8** con MySQL Connector/J
- **Maven** (se incluye Maven Wrapper: `mvnw` y `mvnw.cmd`)
- **Interfaz web:** HTML5, JavaScript y Bootstrap 5.3 (cargado desde CDN)
- **Pruebas manuales:** Postman

## Pasos de instalación y ejecución

**Requisitos previos:** JDK 21, MySQL 8 en ejecución (puerto 3306) y Git.

1. **Clonar el repositorio**

   ```bash
   https://github.com/Emma67g/ApartamentosAPI.git
   cd apartamentos
   ```

2. **Crear la base de datos vacía.** Las tablas las crea Hibernate automáticamente al iniciar la aplicación.

   ```sql
   CREATE DATABASE apartamentos_blue CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Configurar la conexión** con tu usuario y contraseña de MySQL editando `src/main/resources/application.properties`.

4. **Ejecutar la aplicación**

   ```bash
   # Windows
   .\mvnw.cmd clean spring-boot:run

   # Linux / macOS
   ./mvnw clean spring-boot:run
   ```

   La aplicación está lista cuando la consola muestra `Started ApartamentosApplication`.

5. **Acceder**

   | Qué | Dirección |
   |---|---|
   | Interfaz web | http://localhost:8081/ |
   | API (ejemplo) | http://localhost:8081/api/clientes |

> Si la base de datos viene de una versión anterior del proyecto, elimínala y vuelve a crearla (paso 2): Hibernate agrega columnas nuevas, pero no modifica ni borra las que ya existen.

## Variables de entorno necesarias

La conexión a la base de datos y el puerto se pueden definir con variables de entorno, que tienen prioridad sobre los valores de `application.properties`. Así no es necesario guardar credenciales en el código.

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/apartamentos_blue` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de MySQL | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de MySQL | *(la definida en `application.properties`)* |
| `SERVER_PORT` | Puerto de la aplicación (opcional) | `8081` |

Ejemplos:

```bash
# Linux / macOS
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=mi_contraseña
./mvnw clean spring-boot:run
```

```powershell
# Windows (PowerShell)
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "mi_contraseña"
.\mvnw.cmd clean spring-boot:run
```

## Listado de endpoints

Todas las rutas parten de `http://localhost:8081`. Los cuerpos de `POST`, `PUT` y `PATCH` son JSON. Las relaciones se envían como objeto con su id, por ejemplo `"cliente": {"id": 2}`. Las fechas usan el formato `AAAA-MM-DD`.

### Clientes (`/api/clientes`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/clientes?tipo=&estatus=` | Lista clientes (filtros opcionales). |
| GET | `/api/clientes/{id}` | Consulta un cliente. |
| GET | `/api/clientes/{id}/estado-cuenta` | Estado de cuenta: facturado, pagado, saldo y vencido. |
| POST | `/api/clientes` | Registra un cliente (201). |
| PUT | `/api/clientes/{id}` | Actualiza un cliente. |
| DELETE | `/api/clientes/{id}` | Elimina un cliente sin propiedades ni reservas (204). |

### Propiedades (`/api/propiedades`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/propiedades?tipo=&propietarioId=&ciudad=&estado=` | Lista propiedades (filtros opcionales). |
| GET | `/api/propiedades/{id}` | Consulta una propiedad. |
| POST | `/api/propiedades` | Registra una propiedad (201). |
| PUT | `/api/propiedades/{id}` | Actualiza una propiedad. |
| DELETE | `/api/propiedades/{id}` | Elimina una propiedad sin habitaciones ni reservas (204). |

### Imágenes de propiedad (`/api/propiedades/{propiedadId}/imagenes`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/propiedades/{propiedadId}/imagenes` | Lista las imágenes. |
| POST | `/api/propiedades/{propiedadId}/imagenes` | Agrega una imagen (201). La primera queda como portada. |
| PATCH | `/api/propiedades/{propiedadId}/imagenes/{imagenId}/portada` | Marca la imagen como portada. |
| DELETE | `/api/propiedades/{propiedadId}/imagenes/{imagenId}` | Elimina una imagen (204). |

### Habitaciones (`/api/habitaciones`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/habitaciones?propiedadId=&estado=&tipo=` | Lista habitaciones (filtros opcionales). |
| GET | `/api/habitaciones/{id}` | Consulta una habitación. |
| POST | `/api/habitaciones` | Registra una habitación (201). |
| PUT | `/api/habitaciones/{id}` | Actualiza una habitación. |
| PATCH | `/api/habitaciones/{id}/estado` | Cambia solo el estado. |
| DELETE | `/api/habitaciones/{id}` | Elimina una habitación sin reservas (204). |

### Disponibilidad (`/api/disponibilidades`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/disponibilidades?propiedadId=&habitacionId=` | Lista las reglas del calendario (filtros opcionales). |
| GET | `/api/disponibilidades/consulta?propiedadId=&habitacionId=&inicio=&fin=` | Indica si el rango está libre y su monto estimado. |
| GET | `/api/disponibilidades/{id}` | Consulta una regla. |
| POST | `/api/disponibilidades` | Crea un bloqueo o una tarifa especial (201). |
| DELETE | `/api/disponibilidades/{id}` | Elimina una regla (204). |

### Reservas (`/api/reservas`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/reservas?clienteId=&propiedadId=&habitacionId=&estado=` | Lista reservas (filtros opcionales). |
| GET | `/api/reservas/{id}` | Consulta una reserva. |
| POST | `/api/reservas` | Crea una reserva `PENDIENTE` (201). |
| PUT | `/api/reservas/{id}` | Cambia fechas, huéspedes o notas. |
| PATCH | `/api/reservas/{id}/estado` | Confirma, completa o cancela. |
| DELETE | `/api/reservas/{id}` | Elimina una reserva sin pagos (204). |

### Pagos (`/api/pagos`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/pagos?reservaId=&clienteId=&estado=&vencidos=` | Lista pagos; `vencidos=true` devuelve solo los vencidos. |
| GET | `/api/pagos/{id}` | Consulta un pago. |
| POST | `/api/pagos` | Programa una cuota `PENDIENTE` de una reserva (201). |
| PUT | `/api/pagos/{id}` | Edita una cuota que sigue pendiente. |
| PATCH | `/api/pagos/{id}/estado` | Registra el pago (`PAGADO`) o lo cancela. |
| DELETE | `/api/pagos/{id}` | Elimina un pago que no esté pagado (204). |

### Reportes (`/api/reportes`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/reportes/ocupacion?desde=&hasta=&propiedadId=` | Ocupación por propiedad en el rango `[desde, hasta)`. |
| GET | `/api/reportes/ingresos?desde=&hasta=&propiedadId=` | Ingresos cobrados en el rango (extremos incluidos), con desglose por método de pago. |
| GET | `/api/reportes/morosidad` | Clientes con pagos vencidos y monto adeudado. |

### Códigos de respuesta

| Código | Significado |
|---|---|
| 200 / 201 / 204 | Operación correcta / recurso creado / eliminado sin contenido. |
| 400 | Datos inválidos (campos obligatorios, formatos, fechas, capacidad excedida o valor de enumeración inexistente). |
| 404 | El recurso solicitado, o uno al que hace referencia el cuerpo, no existe. |
| 409 | Conflicto con el estado actual (email duplicado, fechas ocupadas, transición no permitida, pago que excede la reserva, registros relacionados). |
| 500 | Error inesperado del servidor. |

## Ejemplos de peticiones y respuestas

Los ejemplos siguen un flujo completo sobre una base de datos vacía. En las respuestas, los objetos anidados (`cliente`, `propiedad`, `propietario`) se devuelven completos; aquí se muestran resumidos para facilitar la lectura.

### 1. Registrar un cliente propietario y un arrendatario

`POST /api/clientes`

```json
{
  "tipo": "PROPIETARIO",
  "nombres": "Alejandro",
  "apellidos": "Fuentes Marín",
  "email": "alejandro.fuentes@correo.com",
  "telefono": "2291478520",
  "fechaNacimiento": "1975-06-12",
  "ine": "FUMA750612HVZNRL09",
  "direccion": "Calle Canal 25, Centro, Veracruz",
  "estatus": "ACTIVO"
}
```

Respuesta `201 Created` (encabezado `Location: http://localhost:8081/api/clientes/1`):

```json
{
  "id": 1,
  "tipo": "PROPIETARIO",
  "nombres": "Alejandro",
  "apellidos": "Fuentes Marín",
  "email": "alejandro.fuentes@correo.com",
  "telefono": "2291478520",
  "fechaNacimiento": "1975-06-12",
  "ine": "FUMA750612HVZNRL09",
  "direccion": "Calle Canal 25, Centro, Veracruz",
  "fechaRegistro": "2027-02-01T10:15:30.482913",
  "estatus": "ACTIVO",
  "nombreCompleto": "Alejandro Fuentes Marín"
}
```

Con el mismo formato se registra el arrendatario (`"tipo": "CLIENTE"`, id 2: Valeria Ochoa Beltrán).

### 2. Registrar una propiedad

`POST /api/propiedades`

```json
{
  "propietario": { "id": 1 },
  "tipo": "CASA",
  "titulo": "Casa Vista Jardín en Boca del Río",
  "descripcion": "Casa familiar con patio y cochera",
  "direccion": "Calle Las Flores 58, Fracc. Costa Verde",
  "ciudad": "Boca del Río",
  "estadoFederativa": "Veracruz",
  "codigoPostal": "94294",
  "pais": "México",
  "precioNoche": 1800,
  "capacidad": 6,
  "numHabitaciones": 3,
  "numBanos": 2,
  "metrosCuadrados": 120,
  "comodidades": "WiFi, aire acondicionado, cochera, patio",
  "reglas": "No fiestas. Se aceptan mascotas pequeñas.",
  "estado": "DISPONIBLE"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "propietario": { "id": 1, "tipo": "PROPIETARIO", "nombreCompleto": "Alejandro Fuentes Marín" },
  "tipo": "CASA",
  "titulo": "Casa Vista Jardín en Boca del Río",
  "ciudad": "Boca del Río",
  "estadoFederativa": "Veracruz",
  "codigoPostal": "94294",
  "pais": "México",
  "precioNoche": 1800,
  "capacidad": 6,
  "numHabitaciones": 3,
  "numBanos": 2,
  "metrosCuadrados": 120,
  "estado": "DISPONIBLE",
  "fechaRegistro": "2027-02-01T10:20:11.105227",
  "fechaActualizacion": "2027-02-01T10:20:11.105227"
}
```

### 3. Crear una tarifa especial

`POST /api/disponibilidades` (con `"disponible": false` y sin `precioEspecial` se crea un bloqueo)

```json
{
  "propiedad": { "id": 1 },
  "fechaInicio": "2027-02-12",
  "fechaFin": "2027-02-16",
  "disponible": true,
  "precioEspecial": 2300,
  "motivo": "Puente de febrero"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "propiedad": { "id": 1, "titulo": "Casa Vista Jardín en Boca del Río" },
  "habitacion": null,
  "fechaInicio": "2027-02-12",
  "fechaFin": "2027-02-16",
  "disponible": true,
  "precioEspecial": 2300,
  "motivo": "Puente de febrero",
  "fechaRegistro": "2027-02-01T10:25:40.771902"
}
```

### 4. Consultar disponibilidad

`GET /api/disponibilidades/consulta?propiedadId=1&inicio=2027-02-10&fin=2027-02-14`

Respuesta `200 OK` (2 noches a $1,800 y 2 noches a $2,300):

```json
{
  "propiedadId": 1,
  "habitacionId": null,
  "fechaInicio": "2027-02-10",
  "fechaFin": "2027-02-14",
  "noches": 4,
  "disponible": true,
  "motivo": null,
  "montoEstimado": 8200.00
}
```

Si el rango no está libre, `disponible` es `false` y `motivo` explica la causa.

### 5. Crear una reserva

`POST /api/reservas` (sin `habitacion` se reserva la propiedad completa)

```json
{
  "cliente": { "id": 2 },
  "propiedad": { "id": 1 },
  "fechaInicio": "2027-02-10",
  "fechaFin": "2027-02-14",
  "numHuespedes": 4,
  "notas": "Llegan por la tarde"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "cliente": { "id": 2, "nombreCompleto": "Valeria Ochoa Beltrán" },
  "propiedad": { "id": 1, "titulo": "Casa Vista Jardín en Boca del Río" },
  "habitacion": null,
  "fechaInicio": "2027-02-10",
  "fechaFin": "2027-02-14",
  "numHuespedes": 4,
  "estado": "PENDIENTE",
  "montoTotal": 8200.00,
  "notas": "Llegan por la tarde",
  "fechaReserva": "2027-02-01T10:30:02.318744",
  "fechaActualizacion": "2027-02-01T10:30:02.318744",
  "noches": 4
}
```

### 6. Confirmar la reserva

`PATCH /api/reservas/1/estado`

```json
{ "estado": "CONFIRMADA" }
```

Respuesta `200 OK`: la reserva con `"estado": "CONFIRMADA"`.

### 7. Programar y registrar pagos

`POST /api/pagos`

```json
{
  "reserva": { "id": 1 },
  "monto": 4100,
  "fechaVencimiento": "2027-02-09",
  "referencia": "Anticipo 50%"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "reserva": { "id": 1, "estado": "CONFIRMADA", "montoTotal": 8200.00 },
  "monto": 4100,
  "metodoPago": null,
  "estado": "PENDIENTE",
  "fechaVencimiento": "2027-02-09",
  "fechaPago": null,
  "referencia": "Anticipo 50%",
  "fechaRegistro": "2027-02-01T10:35:19.064511",
  "vencido": false
}
```

Para registrar el pago, `PATCH /api/pagos/1/estado`:

```json
{
  "estado": "PAGADO",
  "metodoPago": "TRANSFERENCIA",
  "referencia": "SPEI 4821937"
}
```

Respuesta `200 OK`:

```json
{
  "id": 1,
  "reserva": { "id": 1, "estado": "CONFIRMADA", "montoTotal": 8200.00 },
  "monto": 4100.00,
  "metodoPago": "TRANSFERENCIA",
  "estado": "PAGADO",
  "fechaVencimiento": "2027-02-09",
  "fechaPago": "2027-02-03",
  "referencia": "SPEI 4821937",
  "fechaRegistro": "2027-02-01T10:35:19.064511",
  "vencido": false
}
```

### 8. Estado de cuenta de un cliente

`GET /api/clientes/2/estado-cuenta`

Respuesta `200 OK`:

```json
{
  "clienteId": 2,
  "clienteNombre": "Valeria Ochoa Beltrán",
  "fechaCorte": "2027-02-03",
  "totalFacturado": 8200.00,
  "totalPagado": 4100.00,
  "saldoPendiente": 4100.00,
  "montoVencido": 0,
  "reservas": [
    {
      "reservaId": 1,
      "propiedadTitulo": "Casa Vista Jardín en Boca del Río",
      "habitacionNumero": null,
      "fechaInicio": "2027-02-10",
      "fechaFin": "2027-02-14",
      "estado": "CONFIRMADA",
      "montoTotal": 8200.00,
      "montoPagado": 4100.00,
      "saldoPendiente": 4100.00,
      "montoVencido": 0,
      "pagos": [
        { "id": 1, "monto": 4100.00, "estado": "PAGADO", "fechaVencimiento": "2027-02-09", "fechaPago": "2027-02-03", "vencido": false }
      ]
    }
  ]
}
```

### 9. Reportes

`GET /api/reportes/ocupacion?desde=2027-02-01&hasta=2027-03-01&propiedadId=1`

```json
[
  {
    "idPropiedad": 1,
    "tituloPropiedad": "Casa Vista Jardín en Boca del Río",
    "desde": "2027-02-01",
    "hasta": "2027-03-01",
    "unidadesAlquilables": 1,
    "nochesReservadas": 4,
    "nochesTotales": 28,
    "porcentajeOcupacion": 14.29
  }
]
```

`GET /api/reportes/ingresos?desde=2027-02-01&hasta=2027-02-28`

```json
{
  "desde": "2027-02-01",
  "hasta": "2027-02-28",
  "idPropiedad": null,
  "totalIngresos": 4100.00,
  "cantidadPagos": 1,
  "ingresosPorMetodoPago": { "TRANSFERENCIA": 4100.00 }
}
```

`GET /api/reportes/morosidad` (consultado el 2027-02-15, con una segunda cuota de $4,100 vencida el 2027-02-09 y sin pagar)

```json
[
  {
    "idCliente": 2,
    "nombreCliente": "Valeria Ochoa Beltrán",
    "email": "valeria.ochoa@correo.com",
    "telefono": "2297412589",
    "montoAdeudado": 4100.00,
    "cantidadPagosVencidos": 1,
    "vencimientoMasAntiguo": "2027-02-09",
    "diasMaximoVencido": 6
  }
]
```

### 10. Respuestas de error

Todos los errores usan el mismo formato.

`POST /api/clientes` con datos inválidos → `400 Bad Request`

```json
{
  "timestamp": "2027-02-01T10:40:12.553108",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Los datos enviados no son válidos.",
  "ruta": "/api/clientes",
  "detalles": [
    "email: El email no tiene un formato válido",
    "nombres: Los nombres son obligatorios"
  ]
}
```

`GET /api/clientes/999` → `404 Not Found`

```json
{
  "timestamp": "2027-02-01T10:41:03.217690",
  "status": 404,
  "error": "Not Found",
  "mensaje": "Cliente con id 999 no existe.",
  "ruta": "/api/clientes/999",
  "detalles": []
}
```

`POST /api/reservas` en fechas ya ocupadas → `409 Conflict`

```json
{
  "timestamp": "2027-02-01T10:42:27.904415",
  "status": 409,
  "error": "Conflict",
  "mensaje": "Ya existe una reserva activa que se cruza con las fechas solicitadas.",
  "ruta": "/api/reservas",
  "detalles": []
}
```
