# 🗓️ Reserva de salas: API REST

Una API para reservar salas de reuniones sin pisarse. Si alguien ya tiene la sala de 10:00 a 11:00, nadie más puede quedársela en ese rato, ni siquiera si dos personas pulsan «Reservar» en el mismo milisegundo.

Hecha con **Java 17 y Spring Boot 4**.

**De un vistazo:**

- ✅ Crear y listar salas. Crear, listar y cancelar reservas.
- 🚫 **409** si la sala ya está ocupada, y **400** con mensajes que dicen exactamente qué campo está mal.
- 🔎 Listado de reservas con filtros por sala, fecha y estado, y paginación.
- 🗄️ H2 para arrancar sin instalar nada y PostgreSQL para lo serio, con el esquema gestionado por **Flyway**.
- 🔐 **JWT**: para crear o cancelar hace falta token; para consultar, no.
- 🧪 13 tests, incluido uno que lanza 10 reservas simultáneas al mismo hueco.

---

## 🚀 Ponerlo en marcha

Solo necesitas **Java 17 o superior**. Maven no hace falta, porque el proyecto trae su propio wrapper (`./mvnw`).

```bash
./mvnw spring-boot:run    # arranca en http://localhost:8080 (con H2 en memoria)
./mvnw test               # ejecuta los tests
```

### ¿Prefieres PostgreSQL?

```bash
docker compose up -d                                            # levanta PostgreSQL 17
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,postgres  # en local, con las credenciales de desarrollo
```

Para apuntar a otra base de datos, usa `DB_URL`, `DB_USER` y `DB_PASSWORD`.

### Credenciales

Si no indicas ningún perfil, se activa **`dev`**: credenciales de prueba y consola de H2 en `/h2-console`, para arrancar sin configurar nada.

Con cualquier otro perfil (`postgres`, `prod`…) **no hay valores por defecto**: si falta alguna variable, la aplicación se niega a arrancar y te dice cuál falta. Así es imposible desplegar por despiste con un secreto que está publicado en el repositorio.

| Variable | En `dev` | Para qué sirve |
|---|---|---|
| `APP_USUARIO` | `admin` | Usuario para pedir el token |
| `APP_CLAVE` | `admin123` | Su contraseña |
| `APP_JWT_SECRETO` | clave de desarrollo | Firma de los tokens (mínimo 32 caracteres) |

```bash
# Ejemplo de arranque "de verdad"
export APP_USUARIO=ops APP_CLAVE='...' APP_JWT_SECRETO="$(openssl rand -base64 48)"
java -jar target/reservas-salas-0.0.1-SNAPSHOT.jar --spring.profiles.active=postgres
```

---

## 🧭 Probarlo en 1 minuto

```bash
# 1. Pide un token (vale 1 hora)
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usuario":"admin","clave":"admin123"}' | jq -r .token)

# 2. Crea una sala
curl -X POST localhost:8080/api/salas -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"nombre":"Sala Azul","capacidad":8}'

# 3. Resérvala
curl -X POST localhost:8080/api/reservas -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"salaId":1,"responsable":"Ana","inicio":"2027-01-10T10:00:00","fin":"2027-01-10T11:00:00"}'

# 4. Intenta reservarla otra vez a las 10:30... y verás el 409 😉

# 5. Consulta las reservas (sin token)
curl 'localhost:8080/api/reservas?salaId=1&fecha=2027-01-10'

# 6. Cancela
curl -X POST localhost:8080/api/reservas/1/cancelacion -H "Authorization: Bearer $TOKEN"
```

### Endpoints

| Método | Ruta | ¿Token? | Qué puede devolver |
|---|---|:---:|---|
| `POST` | `/api/auth/login` | | 200 con el token · 400 · 401 |
| `POST` | `/api/salas` | 🔐 | 201 · 400 · 401 · 409 si ya hay una sala con ese nombre |
| `GET` | `/api/salas` · `/api/salas/{id}` | | 200 · 404 |
| `POST` | `/api/reservas` | 🔐 | 201 · 400 · 401 · 404 si la sala no existe · **409 si está ocupada** |
| `GET` | `/api/reservas` | | 200 (paginado) · 400 si un filtro no es válido |
| `GET` | `/api/reservas/{id}` | | 200 · 404 |
| `POST` | `/api/reservas/{id}/cancelacion` | 🔐 | 200 · 401 · 404 · 409 si ya estaba cancelada |

**Filtros del listado.** Todos son opcionales y se pueden combinar:
- `salaId`: reservas de una sala.
- `fecha` (`AAAA-MM-DD`): todo lo que ocurre ese día. También aparece una reserva que empezó la noche anterior y termina de madrugada.
- `estado`: `ACTIVA` o `CANCELADA`.
- `page`, `size` (20 por defecto, máximo 100) y `sort` (`id`, `inicio`, `fin`, `responsable` o `estado`).

**Cuando algo va mal**, la API te dice qué y dónde, con el formato estándar [ProblemDetail (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457):

```json
{
  "status": 400,
  "title": "Datos inválidos",
  "detail": "La petición tiene campos con errores",
  "errores": [{ "campo": "fin", "mensaje": "debe ser posterior a inicio" }]
}
```

---

## 🧠 Decisiones (y por qué)

### El punto delicado: que dos personas no reserven lo mismo

**¿Cuándo se pisan dos reservas?** Cuando cada una empieza antes de que termine la otra. Así, 10:00–11:00 y 11:00–12:00 **no** chocan: una acaba justo cuando empieza la otra, como en la vida real.

**El problema escondido.** Lo intuitivo es «compruebo si está libre y, si lo está, guardo». Pero si dos peticiones llegan a la vez, las dos ven la sala libre y las dos guardan. Resultado: sala reservada dos veces.

**La solución.** Antes de comprobar, la reserva **bloquea la sala** en la base de datos (`SELECT … FOR UPDATE`). Si llega otra petición para la misma sala, espera su turno y, cuando le toca, ya ve la reserva anterior y recibe un 409. Las salas distintas no se esperan entre sí, así que no se pierde rendimiento.

**¿Funciona de verdad?** Hay un test que lanza 10 reservas simultáneas al mismo hueco y comprueba que solo una lo consigue. Para asegurarme de que el test sirve para algo, **quité el bloqueo a propósito: se colaron las 10**. Con el bloqueo, entra una y las otras 9 reciben un 409.

**Cinturón y tirantes en PostgreSQL.** Además, una **restricción de exclusión** (`EXCLUDE USING gist`) hace que sea la propia base de datos la que rechace solapamientos, aunque alguien escriba sin pasar por la API. H2 no la soporta, así que esa migración solo se aplica en PostgreSQL (`db/vendor/postgresql`).

### Otras decisiones

- **Cancelar no borra.** La reserva queda como `CANCELADA`: el histórico se conserva y el hueco se libera. Por eso cancelar es un `POST …/cancelacion` y no un `DELETE`; la reserva sigue existiendo.
- **Validación en dos capas.** Las anotaciones comprueban cada campo por separado (obligatorios, longitudes, que `inicio` sea futuro). El servicio comprueba lo que relaciona varios campos: que `fin` vaya después de `inicio` y que la reserva no dure más de 12 horas. Las dos capas responden con el mismo formato, y la base de datos repite lo básico con `CHECK` por si acaso.
- **El esquema lo manda Flyway.** Hibernate no crea tablas: solo comprueba al arrancar que todo encaja (`ddl-auto=validate`).
- **JWT en lugar de Basic.** Es lo habitual cuando una API la consumen frontends u otros servicios.
  - Usa el soporte nativo de Spring Security, sin librerías extra.
  - Las contraseñas se guardan con bcrypt.
  - El login responde igual si el usuario no existe que si la contraseña está mal, para no dar pistas a nadie.
- **Listado rápido.** La sala de cada reserva se carga en la misma consulta (`@EntityGraph`), así que listar 6 reservas cuesta **una** consulta, no siete. Además, ordenar por un campo inventado da un 400 amable en vez de un 500, y nadie puede pedir más de 100 resultados de golpe.
- **Respuesta paginada propia.** No serializo directamente el `Page` de Spring, porque su JSON es un detalle interno que puede cambiar entre versiones.
- **Hora local.** Las salas son físicas y se reservan en la hora de la oficina (`LocalDateTime`).
- **Código organizado por temas** (`sala/`, `reserva/`, `seguridad/`, `comun/`) y no por capas: todo lo de las reservas está en un mismo sitio.

---

## 🔭 Si tuviera más tiempo

- Fechas con zona horaria, para oficinas en varios países.
- Usuarios reales con roles: que solo quien reservó (o un administrador) pueda cancelar, y que el responsable salga del token. *Refresh tokens*.
- Tests contra un PostgreSQL de verdad con **Testcontainers**, para cubrir también la restricción de exclusión.
- Documentación interactiva con **OpenAPI/Swagger**.
- Editar salas y reservas, buscar huecos libres y reservas recurrentes.
- Lo necesario para producción: Actuator, métricas, logs estructurados, Dockerfile y CI con GitHub Actions.
- Limitar los intentos de login para frenar ataques de fuerza bruta.

---

## 🤖 Cómo usé la IA

Trabajé con **Claude Code** (Claude Opus 5.5, de Anthropic), un asistente que trabaja en la terminal directamente sobre el repositorio.

### Cómo trabajamos

- Le pasé el enunciado completo y le pedí hacer la prueba **paso a paso**, con un commit por paso, para que se viera cómo evolucionaba el proyecto.
- Propuso 8 fases y las seguimos en ese orden: proyecto base → entidades y migraciones → endpoints y validación → control de solapamiento → seguridad → filtros y paginación → tests → README.
- En cada fase escribía el código y lo **probaba arrancando la aplicación con peticiones reales**, no solo compilando.
- Yo revisaba los cambios en VS Code y **hacía el commit** antes de pasar a la siguiente fase.
- Las decisiones técnicas (JWT, el bloqueo por sala, cancelar sin borrar, ProblemDetail, la paginación propia…) las propuso la IA explicando el porqué. Las revisé y las acepté porque me convencieron.

No reescribí el código a mano. Mi papel fue dirigir el trabajo, revisar cada paso y, sobre todo, cuestionar el resultado al final.

### Lo que se corrigió por el camino

- 🔧 **El test de concurrencia no compilaba en Java 17.** Usaba una característica de Java 19 (`ExecutorService` con `try-with-resources`); se cambió por un `try/finally` clásico.
- 🔧 **El token llevaba algo de más.** Spring Security 7 añadía una autoridad interna (`FACTOR_PASSWORD`) junto a los roles; ahora el token lleva solo los roles.
- 🔧 **El listado hacía una consulta por cada reserva** (el clásico N+1). Se detectó en la fase 3 y se arregló en la 6. Lo comprobé en el log de SQL: ahora es una sola consulta.
- 🔍 **No me fié del test de concurrencia.** Lo hice fallar quitando el bloqueo, para demostrar que de verdad detecta el problema.

### Revisión crítica al terminar

Con todo funcionando y los tests en verde, no lo di por bueno. Le pedí tres revisiones, sin tocar código hasta que yo lo aprobara:

1. **Concurrencia:** «Comprueba que una sala nunca pueda tener dos reservas activas solapadas: peticiones simultáneas, solapes parciales, reservas consecutivas, cancelación mientras otro reserva y rollback ante errores. Revisa el `SELECT FOR UPDATE`, el aislamiento y la restricción `EXCLUDE` en PostgreSQL frente a H2. Separa defectos reales de mejoras opcionales.»
2. **Seguridad:** «Audita el JWT (firma, validación, caducidad), el secreto, bcrypt, qué endpoints están protegidos y si los errores o las consultas públicas enseñan algo que no deberían. Propón solo lo necesario para producción, sin romper nada.»
3. **Tests:** «Busca falsos positivos y reglas de negocio sin cubrir. No infles la cobertura: prioriza tests que detecten regresiones. Ejecútalos y enséñame la evidencia.»

**Lo bueno:** el mecanismo principal aguanta.
- La segunda petición espera al bloqueo y, cuando entra, ya ve la reserva de la primera.
- Una cancelación simultánea, como mucho, provoca un 409 prudente, nunca un solapamiento.
- Si algo falla, la transacción se deshace entera.
- Los errores no enseñan trazas ni detalles internos.

**Lo que hay que mejorar** (separando defectos reales de mejoras):

| Gravedad | Qué pasa | Qué haría |
|---|---|---|
| 🔴 Alta | El secreto JWT y las credenciales tenían valores por defecto. Si alguien desplegaba sin configurar nada, cualquiera podría fabricar tokens válidos. | ✅ **Corregido.** Los valores por defecto viven solo en el perfil `dev`; con cualquier otro perfil la aplicación no arranca sin las variables. Lo comprueba `ConfiguracionSeguraTest`. |
| 🔴 Alta | La restricción `EXCLUDE` de PostgreSQL no la prueba ningún test, porque todos se ejecutan en H2. | Test con Testcontainers sobre PostgreSQL real. |
| 🟠 Media | El login no limita los intentos fallidos. | *Rate limiting* por IP y usuario. |
| 🟠 Media | La consola de H2 quedaba abierta siempre que se usara H2. | ✅ **Corregido** con el mismo cambio: solo se activa en `dev`. |
| 🟡 Baja | Los tests usan fechas de 2030 y exigen fechas futuras: en 2030 empezarán a fallar. | Calcular las fechas a partir de hoy. |
| 🟡 Baja | «Azul» y «azul» enviadas a la vez podrían colarse como dos salas distintas: el código compara sin mayúsculas y la base de datos no. | Índice único sobre `lower(nombre)`. |
| 🟡 Baja | El token se valida por firma y caducidad, pero no por emisor. | Validar también el emisor (`iss`). |

### Mi conclusión

La IA me ahorró la parte mecánica, pero el criterio no lo delegué. Revisé cada decisión, pedí que se probara contra la aplicación real y busqué a propósito los puntos débiles. Todo lo que hay en este repositorio lo entiendo y lo puedo defender.

---

## ⏱️ Tiempo empleado

**Unas 3 h 30 min** (16/11/2026, de 04:00 a 07:30 aprox.):

| Tramo | Tiempo | Qué hice |
|---|---|---|
| 04:00 – 06:00 | ~2 h | Leer el enunciado, preparar el entorno y planificar las fases y el diseño |
| 06:00 – 06:50 | ~50 min | Implementación por fases, probando cada una (commits de 06:21 a 06:50) |
| 06:50 – 07:30 | ~40 min | README, revisión crítica y documentación de los hallazgos |
