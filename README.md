# BienestarService

## 1. Descripción general

**BienestarService** es un microservicio Java/Spring Boot que forma parte de una arquitectura de microservicios más amplia (paquete raíz `eci.dosw.alpha`, junto a otros servicios como `EventService`, referenciado como colaborador externo). Su responsabilidad de negocio es exponer el **Centro de Bienestar Universitario**, es decir, la funcionalidad relacionada con el bienestar (físico, psicológico y emocional) de la comunidad universitaria. Concretamente expone:

- **Recursos de bienestar** (`Resource`): contenido informativo o de apoyo — tips, artículos, contactos — clasificado por tipo y categoría (por ejemplo `PSICOLOGÍA`, `NUTRICIÓN`, `BIENESTAR`).
- **Eventos de bienestar**: no se gestionan directamente en este microservicio, sino que se **consultan en tiempo real a otro microservicio externo** (`EventService`, vía HTTP) filtrando por categoría `BIENESTAR`, y se exponen bajo un DTO propio (`EventDTO`).
- **Contactos de emergencia** (`EmergencyContact`): líneas de atención en crisis, contactos de psicología, salud, etc., que se pueden crear y consultar.

El servicio se ejecuta por defecto en el **puerto 8083** y expone toda su API bajo el path base `/bienestar`.

Al ser un microservicio autocontenido con su propia base de datos (MongoDB), y comunicarse con otros servicios exclusivamente vía HTTP, sigue el principio de **descentralización de datos** propio de las arquitecturas de microservicios: cada servicio es dueño de sus propios datos y no accede directamente a la base de datos de otro servicio.

## 2. Arquitectura y patrón de capas

El proyecto sigue una **arquitectura en capas (layered architecture)** clásica de Spring Boot, con una separación de responsabilidades muy clara y un flujo de dependencias unidireccional:

```
Cliente HTTP
     │
     ▼
Controller  (eci.dosw.alpha.BienestarService.controller)
     │  delega en
     ▼
Service     (eci.dosw.alpha.BienestarService.service)
     │  usa
     ├──► Repository (eci.dosw.alpha.BienestarService.repository) ──► MongoDB
     └──► RestTemplate (config.RestTemplateConfig) ──► EventService (HTTP externo)
     │
     ▼
Model / DTO (eci.dosw.alpha.BienestarService.model / dto)
```

Capas identificadas:

| Capa | Paquete | Responsabilidad |
|---|---|---|
| **Presentación / API** | `controller` | Recibe peticiones HTTP REST, valida parámetros de entrada básicos, delega en la capa de servicio y documenta el contrato OpenAPI de cada endpoint. |
| **Lógica de negocio** | `service` | Orquesta las reglas de negocio: filtrado de recursos, tolerancia a fallos al consumir el servicio de eventos, obtención/creación de contactos de emergencia. |
| **Acceso a datos** | `repository` | Interfaces `MongoRepository` de Spring Data MongoDB que abstraen el acceso a las colecciones de MongoDB. |
| **Modelo de dominio** | `model` | Entidades de dominio persistidas como documentos MongoDB (`@Document`). |
| **Transferencia de datos** | `dto` | Objetos planos (`EventDTO`) usados para deserializar la respuesta del microservicio externo `EventService`, desacoplando el modelo interno del contrato externo. |
| **Configuración** | `config` | Beans de infraestructura: cliente MongoDB, `RestTemplate` para comunicación entre servicios, configuración de OpenAPI/Swagger. |
| **Manejo de errores** | `exception` | `@RestControllerAdvice` centralizado que traduce excepciones no controladas en respuestas HTTP consistentes. |

Esta separación permite que cada capa tenga una única razón de cambio (principio de responsabilidad única), facilita las pruebas unitarias por capa (los tests usan Mockito para aislar `Controller` de `Service`, y `Service` de sus repositorios/`RestTemplate`), y hace que el dominio (`model`) no dependa de detalles de framework web.

## 3. Tecnologías y stack completo

| Tecnología / Dependencia | Versión | Propósito en el proyecto | Por qué se eligió |
|---|---|---|---|
| **Java** | 21 (LTS) | Lenguaje de implementación (`java.version` en `pom.xml`). | Última versión LTS disponible al momento de creación del proyecto; ofrece mejoras de rendimiento de la JVM, *pattern matching*, `records`, y soporte a largo plazo, ideal para un servicio productivo de larga vida. |
| **Spring Boot** (`spring-boot-starter-parent`) | 4.1.0 | Framework base: autoconfiguración, inyección de dependencias, servidor embebido, gestión de dependencias transitivas (BOM). | Reduce drásticamente el *boilerplate* de configuración de una aplicación Spring, ofrece un servidor HTTP embebido (Tomcat) sin necesidad de desplegar un WAR en un servidor externo, y es el estándar de facto para microservicios Java en el ecosistema Spring/Netflix OSS. |
| **spring-boot-starter** | gestionada por el BOM del parent | Núcleo de autoconfiguración, logging (SLF4J + Logback), etc. | Dependencia base obligatoria de cualquier aplicación Spring Boot. |
| **spring-boot-starter-web** | gestionada por el BOM del parent | Publica endpoints REST (`@RestController`), incluye Tomcat embebido y Jackson para serialización JSON. | Es la forma estándar en Spring Boot de exponer una API REST sin configuración manual de servlets ni serializadores. |
| **spring-boot-starter-data-mongodb** | gestionada por el BOM del parent | Integración con MongoDB vía Spring Data: repositorios (`MongoRepository`), `MongoTemplate`, mapeo objeto-documento (`@Document`, `@Id`). | MongoDB es una base de datos documental que encaja de forma natural con entidades semiestructuradas como "recursos de bienestar" o "contactos de emergencia", cuyo esquema puede evolucionar con el tiempo sin necesidad de migraciones rígidas de esquema como en una base relacional. Spring Data MongoDB además evita escribir consultas boilerplate: basta con declarar interfaces (`findByCategory`) para obtener consultas derivadas automáticamente. |
| **MongoDB Java Driver** (`mongo-java-driver` / `mongodb-driver-sync`, transitivo) | gestionado por el BOM | Cliente de bajo nivel usado explícitamente en `MongoConfig` (`MongoClients.create(...)`) para construir el `MongoClient`. | Viene incluido transitivamente con el starter y es necesario para construir manualmente el `MongoClient`/`MongoDatabaseFactory` cuando se requiere personalizar el nombre de la base de datos, como se hace en este proyecto. |
| **Lombok** | gestionada por el BOM (`optional=true`) | Genera automáticamente getters, setters, `equals`, `hashCode` y `toString` en `model` y `dto` mediante la anotación `@Data`. | Elimina código repetitivo (boilerplate) en clases anémicas de modelo/DTO, mejorando la legibilidad del código sin sacrificar funcionalidad, muy común en proyectos Spring Boot orientados a POJOs. |
| **springdoc-openapi-starter-webmvc-ui** | 2.8.14 | Genera automáticamente la especificación OpenAPI 3 y expone la UI de Swagger para explorar/probar la API interactivamente. | Facilita la documentación viva de la API (siempre sincronizada con el código gracias a las anotaciones `@Operation`, `@ApiResponse`, `@Tag`), esencial en un ecosistema de microservicios donde otros equipos/servicios consumen esta API y necesitan un contrato claro y probable sin herramientas externas. |
| **spring-boot-starter-test** | gestionada por el BOM (scope `test`) | Trae JUnit 5 (Jupiter), Mockito, AssertJ, Spring Test y JSONPath para pruebas. | Es el paquete estándar de testing de Spring Boot; incluye todo lo necesario para pruebas unitarias y de integración sin tener que declarar cada librería por separado. |
| **JaCoCo Maven Plugin** | 0.8.12 | Instrumenta el código y genera reportes de cobertura de pruebas; falla el build si la cobertura de instrucciones cae debajo del 80 %. | Garantiza un umbral mínimo de calidad de pruebas de forma automática en cada build (`mvn verify`), integrado con el pipeline de CI. Excluye de la medición las clases de configuración, modelo, DTO y repositorio (por ser mayormente declarativas/generadas), enfocando la métrica en la lógica de negocio real (`service`, `controller`, `exception`). |
| **spring-boot-maven-plugin** | gestionada por el BOM | Empaqueta la aplicación como un JAR ejecutable ("fat jar") con todas sus dependencias. | Permite generar un artefacto autocontenido (`java -jar app.jar`) ideal para contenerizar con Docker, sin necesidad de un servidor de aplicaciones externo. |

> Nota: casi todas las versiones de las dependencias (excepto `springdoc-openapi` y `jacoco-maven-plugin`, que se fijan explícitamente) son gestionadas de forma centralizada por el BOM heredado de `spring-boot-starter-parent:4.1.0`, lo cual asegura compatibilidad binaria entre todas las librerías del ecosistema Spring usadas.

## 4. Estructura de paquetes

```
src/main/java/eci/dosw/alpha/BienestarService/
├── BienestarServiceApplication.java     # Clase principal (@SpringBootApplication)
├── config/                              # Beans de infraestructura
│   ├── MongoConfig.java
│   ├── OpenApiConfig.java
│   └── RestTemplateConfig.java
├── controller/                          # Capa REST
│   └── BienestarController.java
├── dto/                                 # Objetos de transferencia (contratos externos)
│   └── EventDTO.java
├── exception/                           # Manejo centralizado de errores
│   └── GlobalExceptionHandler.java
├── model/                               # Entidades de dominio (documentos MongoDB)
│   ├── EmergencyContact.java
│   └── Resource.java
├── repository/                          # Acceso a datos (Spring Data MongoDB)
│   ├── EmergencyContactRepository.java
│   └── ResourceRepository.java
└── service/                             # Lógica de negocio
    └── BienestarService.java

src/main/resources/
└── application.properties               # Configuración de la aplicación

src/test/java/eci/dosw/alpha/BienestarService/
├── BienestarControllerTest.java         # Pruebas unitarias del controller
└── BienestarServiceApplicationTests.java # Pruebas unitarias del service
```

Explicación de cada paquete:

- **`(raíz) eci.dosw.alpha.BienestarService`**: contiene únicamente la clase de arranque de Spring Boot.
- **`config`**: centraliza toda la configuración de *beans* que no encajan naturalmente en otra capa (cliente MongoDB, `RestTemplate` para llamadas salientes, metadatos de OpenAPI). Aísla detalles de infraestructura del resto del código.
- **`controller`**: capa de entrada HTTP. Traduce peticiones REST en llamadas a la capa de servicio.
- **`dto`**: modelos de transporte usados específicamente para la integración con servicios externos (en este caso, deserializar la respuesta JSON de `EventService`). Al no reutilizar el modelo interno, el servicio queda desacoplado de cambios en el contrato de `EventService`.
- **`exception`**: manejador global de excepciones (`@RestControllerAdvice`) que evita duplicar bloques `try/catch` de conversión a respuestas HTTP en cada controlador.
- **`model`**: entidades de dominio persistentes, mapeadas directamente a colecciones de MongoDB mediante `@Document`.
- **`repository`**: interfaces que extienden `MongoRepository`, aprovechando la generación automática de consultas de Spring Data.
- **`service`**: única clase con la lógica de negocio real: reglas de resiliencia, transformación y orquestación entre repositorios y llamadas externas.

## 5. Catálogo detallado de clases

### 5.1 Clase principal

#### `BienestarServiceApplication` (paquete raíz)
- **Anotaciones**: `@SpringBootApplication` (combina `@Configuration`, `@EnableAutoConfiguration` y `@ComponentScan`).
- **Responsabilidad**: punto de entrada de la aplicación. Su método `main(String[] args)` invoca `SpringApplication.run(...)`, arrancando el contenedor de Spring, el servidor Tomcat embebido y todo el proceso de autoconfiguración (incluida la conexión a MongoDB y el escaneo de componentes bajo el paquete `eci.dosw.alpha.BienestarService`).
- **Relaciones**: implícitamente "conoce" (por *component scan*) a todas las clases anotadas con `@Component`, `@Service`, `@RestController`, `@Configuration`, `@Repository` del proyecto.

### 5.2 Paquete `config` — Configuración de infraestructura

#### `MongoConfig`
- **Anotaciones**: `@Configuration`.
- **Responsabilidad**: define manualmente los beans de bajo nivel para la conexión a MongoDB, en lugar de depender exclusivamente de la autoconfiguración por defecto de Spring Boot.
- **Campos**: `mongoUri` inyectado desde la propiedad `spring.data.mongodb.uri` vía `@Value`.
- **Métodos/Beans**:
  - `mongoClient(): MongoClient` — crea el cliente nativo de MongoDB (`MongoClients.create(mongoUri)`) a partir de la URI configurada.
  - `mongoDatabaseFactory(MongoClient): MongoDatabaseFactory` — construye una `SimpleMongoClientDatabaseFactory` apuntando explícitamente a la base de datos `"eventsdb"` (nombre fijado en código, aunque también viene incluido en la URI de conexión; ver sección de observaciones/config).
  - `mongoTemplate(MongoDatabaseFactory): MongoTemplate` — expone el `MongoTemplate`, la API de bajo nivel de Spring Data MongoDB para operaciones más avanzadas que las que ofrecen los repositorios derivados (aunque en este proyecto los repositorios `MongoRepository` son el mecanismo de acceso a datos usado activamente).
- **Relaciones**: es consumida indirectamente por Spring Data para construir los repositorios `EmergencyContactRepository` y `ResourceRepository`.

#### `OpenApiConfig`
- **Anotaciones**: `@Configuration`.
- **Responsabilidad**: personaliza los metadatos globales de la documentación OpenAPI/Swagger generada por `springdoc-openapi`.
- **Métodos/Beans**:
  - `bienestarServiceOpenAPI(): OpenAPI` — define título ("BienestarService API"), descripción ("Centro de Bienestar Universitario: recursos, eventos de bienestar y contactos de emergencia.") y versión ("1.0.0") del documento OpenAPI expuesto por defecto en `/v3/api-docs` y visualizable en `/swagger-ui.html`.

#### `RestTemplateConfig`
- **Anotaciones**: `@Configuration`.
- **Responsabilidad**: expone un bean singleton de `RestTemplate`, el cliente HTTP síncrono usado para comunicarse con el microservicio externo `EventService`.
- **Métodos/Beans**:
  - `restTemplate(): RestTemplate` — instancia simple sin personalización adicional (sin *timeouts* ni interceptores configurados explícitamente).
- **Relaciones**: inyectado directamente en `BienestarService` para realizar la llamada `GET` a `EventService`.

### 5.3 Paquete `controller` — API REST

#### `BienestarController`
- **Anotaciones**: `@RestController`, `@RequestMapping("/bienestar")`, `@Tag(name = "Bienestar", ...)` (agrupación en Swagger UI).
- **Responsabilidad**: expone la API pública del microservicio. Es un controlador "delgado": no contiene lógica de negocio, solo delega en `BienestarService`.
- **Dependencias**: `BienestarService` (inyectado por constructor).
- **Métodos públicos (endpoints)**:
  - `getResources(String category): List<Resource>` — `GET /bienestar/resources`. Acepta un parámetro de consulta opcional `category`. Documentado con `@Operation`/`@ApiResponse` indicando que una lista vacía es un caso válido (no un error) cuando no hay recursos.
  - `getEvents(): List<EventDTO>` — `GET /bienestar/events`. Devuelve los eventos de categoría `BIENESTAR` obtenidos desde `EventService`; documentado explícitamente que si `EventService` no está disponible, se devuelve una lista vacía en lugar de propagar un error.
  - `getContact(): EmergencyContact` — `GET /bienestar/contact`. Devuelve el primer contacto de emergencia registrado; documentado con dos posibles respuestas: `200` si existe, `500` si no hay ninguno configurado.
  - `createContact(EmergencyContact contact): EmergencyContact` — `POST /bienestar/contact`. Recibe un contacto de emergencia en el cuerpo de la petición y lo persiste.
  - `getAllContacts(): List<EmergencyContact>` — `GET /bienestar/contacts`. Devuelve todos los contactos de emergencia registrados.
- **Relaciones**: única dependencia directa es `BienestarService`; usa los modelos `Resource`, `EmergencyContact` y el DTO `EventDTO` como tipos de retorno.

### 5.4 Paquete `service` — Lógica de negocio

#### `BienestarService`
- **Anotaciones**: `@Service`.
- **Responsabilidad**: contiene toda la lógica de negocio del microservicio: filtrado de recursos, tolerancia a fallos al integrar con `EventService`, y gestión de contactos de emergencia.
- **Dependencias inyectadas por constructor**: `ResourceRepository`, `RestTemplate`, `EmergencyContactRepository`. Además, `eventsServiceUrl` inyectado vía `@Value("${events.service.url}")`.
- **Métodos públicos**:
  - `getResources(String category): List<Resource>` — si `category` no es nulo ni está en blanco, delega en `resourceRepository.findByCategory(category)`; en caso contrario retorna todos los recursos (`findAll()`). Regla de negocio documentada como **E1**: la ausencia de recursos se modela como una lista vacía, no como un error.
  - `getWellbeingEvents(): List<EventDTO>` — construye la URL `eventsServiceUrl + "?category=BIENESTAR"` y realiza una petición `GET` síncrona con `RestTemplate.getForObject(url, EventDTO[].class)`. Si la respuesta es `null`, retorna lista vacía. Si ocurre una `RestClientException` (por ejemplo, `EventService` caído o inalcanzable), **captura la excepción y retorna una lista vacía** en lugar de propagar el error — regla de negocio documentada como **E2**: la caída de un servicio externo no debe romper la experiencia del Centro de Bienestar, solo omitir la sección de eventos.
  - `getEmergencyContact(): EmergencyContact` — obtiene todos los contactos y retorna el primero (`findFirst()`); si no existe ninguno, lanza una `RuntimeException("No hay contacto de emergencia configurado")`, que es interceptada más tarde por `GlobalExceptionHandler`.
  - `createEmergencyContact(EmergencyContact contact): EmergencyContact` — persiste el contacto vía `emergencyContactRepository.save(contact)`.
  - `getAllEmergencyContacts(): List<EmergencyContact>` — retorna todos los contactos de emergencia registrados.
- **Relaciones**: es el orquestador central; depende de `ResourceRepository`, `EmergencyContactRepository` (persistencia) y de `RestTemplate` (integración HTTP saliente hacia `EventService`). Es consumido exclusivamente por `BienestarController`.
- **Patrón de resiliencia**: aunque no usa una librería dedicada de resiliencia (como Resilience4j), implementa manualmente un patrón de tipo *fail-safe / graceful degradation* mediante el bloque `try/catch` alrededor de la llamada HTTP externa, evitando que un fallo en `EventService` se propague como error 500 al cliente del Centro de Bienestar.

### 5.5 Paquete `model` — Entidades de dominio (documentos MongoDB)

#### `Resource`
- **Anotaciones**: `@Data` (Lombok — genera getters/setters/equals/hashCode/toString), `@Document(collection = "resources")`.
- **Campos**:
  - `id: String` — anotado con `@Id`, identificador del documento MongoDB.
  - `title: String` — título del recurso.
  - `description: String` — descripción del recurso.
  - `type: String` — tipo de recurso, con valores de convención documentados en comentario: `TIP`, `ARTICLE`, `CONTACT` (no está modelado como `enum`, es un `String` libre).
  - `category: String` — categoría temática del recurso (por ejemplo `PSICOLOGÍA`, `NUTRICIÓN`).
- **Persistencia**: colección MongoDB `resources`.
- **Relaciones**: consumido/producido por `ResourceRepository`, retornado directamente por `BienestarController.getResources`.

#### `EmergencyContact`
- **Anotaciones**: `@Data` (Lombok), `@Document(collection = "emergency_contacts")`.
- **Campos**:
  - `id: String` — anotado con `@Id`.
  - `name: String` — nombre del contacto o línea de atención.
  - `phone: String` — teléfono de contacto.
  - `email: String` — correo de contacto.
- **Persistencia**: colección MongoDB `emergency_contacts`.
- **Relaciones**: consumido/producido por `EmergencyContactRepository`, expuesto por los endpoints `/bienestar/contact`, `/bienestar/contacts` y `POST /bienestar/contact` en `BienestarController`.

### 5.6 Paquete `dto` — Objetos de transferencia

#### `EventDTO`
- **Anotaciones**: `@Data` (Lombok).
- **Responsabilidad**: representa el contrato JSON esperado desde el microservicio externo `EventService`, usado exclusivamente para deserializar su respuesta HTTP (`EventDTO[]`) y también como tipo de retorno hacia el cliente de `BienestarService`.
- **Campos**:
  - `id: String`
  - `name: String`
  - `description: String`
  - `category: String`
  - `date: String`
  - `availableCapacity: int`
- **Relaciones**: usado por `BienestarService.getWellbeingEvents()` (deserialización de la respuesta de `RestTemplate`) y por `BienestarController.getEvents()` (tipo de retorno del endpoint).

### 5.7 Paquete `repository` — Acceso a datos

#### `ResourceRepository` (interfaz)
- **Extiende**: `MongoRepository<Resource, String>`.
- **Responsabilidad**: abstrae el acceso CRUD a la colección `resources`.
- **Métodos declarados**:
  - `findByCategory(String category): List<Resource>` — *query method* derivado automáticamente por Spring Data a partir del nombre del método (sin necesidad de escribir la consulta manualmente).
  - Además hereda todos los métodos estándar de `MongoRepository` (`findAll`, `save`, `deleteById`, etc.), usados indirectamente por el servicio.

#### `EmergencyContactRepository` (interfaz)
- **Extiende**: `MongoRepository<EmergencyContact, String>`.
- **Responsabilidad**: abstrae el acceso CRUD a la colección `emergency_contacts`. No declara *query methods* adicionales; usa únicamente los heredados (`findAll`, `save`).

### 5.8 Paquete `exception` — Manejo de errores

#### `GlobalExceptionHandler`
- **Anotaciones**: `@RestControllerAdvice`.
- **Responsabilidad**: intercepta de forma centralizada las excepciones no controladas lanzadas por cualquier controlador de la aplicación, evitando que se filtren como *stack traces* crudos al cliente.
- **Métodos**:
  - `handleRuntime(RuntimeException ex): Map<String, String>` — anotado con `@ExceptionHandler(RuntimeException.class)` y `@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)`. Captura cualquier `RuntimeException` (incluida la lanzada por `BienestarService.getEmergencyContact()` cuando no hay contactos configurados) y la traduce en una respuesta HTTP `500` con cuerpo JSON `{"error": "<mensaje de la excepción>"}`.
- **Relaciones**: intercepta excepciones originadas en `BienestarController` / `BienestarService`, siendo el único punto de manejo de errores de todo el microservicio.

### 5.9 Clases de prueba (`src/test/java`)

#### `BienestarControllerTest`
- **Frameworks**: JUnit 5 (`@Test`, `@BeforeEach`), Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`), AssertJ (`assertThat`).
- **Responsabilidad**: prueba unitaria de `BienestarController` con `BienestarService` mockeado (sin levantar el contexto de Spring ni `MockMvc`; se invocan los métodos del controlador directamente como objetos Java).
- **Casos cubiertos**: `getResources` sin categoría y con categoría, `getEvents`, `getContact`, `createContact`, `getAllContacts` — verificando en cada caso que el controlador delega correctamente en el servicio y retorna lo que este produce.

#### `BienestarServiceApplicationTests`
- **Frameworks**: JUnit 5, Mockito, AssertJ, `ReflectionTestUtils` (de `spring-test`) para inyectar el valor de `eventsServiceUrl` (campo privado anotado con `@Value`) sin levantar el contexto de Spring.
- **Responsabilidad**: prueba unitaria de `BienestarService` con sus tres dependencias (`ResourceRepository`, `RestTemplate`, `EmergencyContactRepository`) mockeadas.
- **Casos cubiertos** (organizados por método bajo prueba):
  - `getResources`: sin categoría (retorna todos), con categoría (filtra), categoría en blanco (tratada como "todas"), caso **E1** de lista vacía.
  - `getWellbeingEvents`: caso exitoso, caso **E2** de `EventService` caído (`RestClientException` → lista vacía), caso de respuesta `null` → lista vacía.
  - `getEmergencyContact`: caso con contacto existente, caso sin contactos (verifica que lanza `RuntimeException` con el mensaje esperado).
  - `createEmergencyContact`: verifica que se guarda y retorna el contacto.
  - `getAllEmergencyContacts`: verifica que retorna todos los contactos.
- Nota: a pesar del nombre de la clase (heredado de la plantilla por defecto de Spring Initializr, `*ApplicationTests`), **no** es una prueba de contexto de Spring Boot (no usa `@SpringBootTest`); es en realidad la suite de pruebas unitarias de la clase `BienestarService`.

## 6. Endpoints REST expuestos

Todos los endpoints están bajo el prefijo base **`/bienestar`**.

| Método | Ruta | Query/Body | Respuesta (200) | Otros códigos | Descripción |
|---|---|---|---|---|---|
| `GET` | `/bienestar/resources` | Query param opcional `category` (String) | `List<Resource>` (JSON array; puede ser `[]`) | — | Lista recursos de bienestar, opcionalmente filtrados por categoría. Una lista vacía es una respuesta válida, no un error. |
| `GET` | `/bienestar/events` | — | `List<EventDTO>` (JSON array; puede ser `[]`) | — | Consulta eventos de categoría `BIENESTAR` a `EventService`. Si el servicio externo no responde, retorna `[]` en lugar de fallar. |
| `GET` | `/bienestar/contact` | — | `EmergencyContact` (JSON object) | `500` si no hay ningún contacto configurado (cuerpo `{"error": "No hay contacto de emergencia configurado"}`) | Devuelve el primer contacto de emergencia registrado. |
| `POST` | `/bienestar/contact` | Body: `EmergencyContact` (`name`, `phone`, `email`) | `EmergencyContact` creado (incluye `id` generado) | — | Registra un nuevo contacto de emergencia. |
| `GET` | `/bienestar/contacts` | — | `List<EmergencyContact>` (JSON array) | — | Lista todos los contactos de emergencia registrados. |

**Documentación interactiva**: gracias a `springdoc-openapi-starter-webmvc-ui`, la especificación OpenAPI 3 está disponible en `/v3/api-docs` y la interfaz Swagger UI en `/swagger-ui.html` (o `/swagger-ui/index.html`), generadas automáticamente a partir de las anotaciones `@Operation`, `@ApiResponse(s)`, `@Parameter` y `@Tag` del controlador, junto con los metadatos definidos en `OpenApiConfig`.

## 7. Modelo de datos / entidades y sus relaciones

BienestarService usa **MongoDB** (base de datos NoSQL orientada a documentos), por lo que no existen relaciones formales tipo llave foránea como en un modelo relacional. Las dos colecciones son independientes entre sí:

```
┌───────────────────────────┐        ┌────────────────────────────────┐
│      resources             │        │      emergency_contacts          │
│  (colección MongoDB)       │        │      (colección MongoDB)         │
├───────────────────────────┤        ├────────────────────────────────┤
│ _id: ObjectId (String)     │        │ _id: ObjectId (String)           │
│ title: String              │        │ name: String                     │
│ description: String        │        │ phone: String                    │
│ type: String (TIP|ARTICLE| │        │ email: String                    │
│        CONTACT)            │        └────────────────────────────────┘
│ category: String           │
└───────────────────────────┘
```

Adicionalmente, existe un "modelo virtual" que **no se persiste en este microservicio**: `EventDTO`, que representa eventos gestionados por el microservicio externo `EventService` y que BienestarService solo consume en tiempo real vía HTTP (no los almacena localmente, no hay colección `events` en la base de datos de BienestarService).

Base de datos: por configuración (`SimpleMongoClientDatabaseFactory` en `MongoConfig`), el nombre de la base de datos usada explícitamente en el bean `mongoDatabaseFactory` es **`eventsdb`**, mientras que la URI por defecto en `application.properties` apunta a **`bienestardb`** (`mongodb://localhost:27017/bienestardb`). Esto es una inconsistencia a tener en cuenta/documentar: el nombre de base de datos indicado explícitamente en el código de `MongoConfig` (`"eventsdb"`) no coincide con el nombre de base de datos incluido en la URI por defecto (`bienestardb`). En la práctica, el nombre de base de datos que Spring Data MongoDB usará depende de cuál mecanismo prevalezca (la URI de conexión suele tener prioridad si incluye el nombre de base de datos), por lo que se recomienda revisar y unificar este punto en un futuro ajuste de configuración.

## 8. Configuración (application.properties, perfiles, variables de entorno)

Archivo `src/main/resources/application.properties` (único archivo de configuración; **no existen perfiles adicionales** `application-dev.yml`, `application-prod.yml`, etc. en el proyecto actual):

```properties
spring.application.name=BienestarService

server.port=8083

spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/bienestardb}

# URL del events-service
events.service.url=${EVENTS_SERVICE_URL:http://localhost:8081/events}
```

| Propiedad | Valor por defecto | Variable de entorno que la sobrescribe | Descripción |
|---|---|---|---|
| `spring.application.name` | `BienestarService` | — (fijo) | Nombre lógico de la aplicación, usado en logs y, si en el futuro se integra Eureka/Config Server, como identificador de registro. |
| `server.port` | `8083` | — (fijo) | Puerto HTTP en el que Tomcat embebido expone la API REST. |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/bienestardb` | `SPRING_DATA_MONGODB_URI` | Cadena de conexión completa a MongoDB (host, puerto, base de datos). En despliegue (ver workflow de CI/CD) se sobrescribe con la variable `SPRING_MONGODB_URI` inyectada como `-e` en el contenedor Docker — **nota**: existe una discrepancia de nombre entre la variable definida en `application.properties` (`SPRING_DATA_MONGODB_URI`) y la variable inyectada en el `docker run` del workflow (`SPRING_MONGODB_URI`); deben unificarse para que la sobrescritura en producción funcione correctamente. |
| `events.service.url` | `http://localhost:8081/events` | `EVENTS_SERVICE_URL` | URL base del microservicio externo `EventService`, al cual `BienestarService` le agrega el query param `?category=BIENESTAR` al consultar eventos. |

No se observa uso de Spring Cloud Config, Eureka Client, ni Feign en el proyecto: la comunicación con `EventService` es una llamada HTTP directa vía `RestTemplate` a una URL fija configurable por variable de entorno (no hay *service discovery* dinámico).

## 9. Persistencia

- **Motor de base de datos**: MongoDB (NoSQL, orientada a documentos).
- **Driver/integración**: Spring Data MongoDB (`spring-boot-starter-data-mongodb`), que internamente usa el driver oficial de MongoDB para Java.
- **Configuración del cliente**: personalizada manualmente en `MongoConfig` en lugar de dejar toda la configuración a la autoconfiguración por defecto de Spring Boot, lo que permite fijar explícitamente el nombre de la base de datos en el `MongoDatabaseFactory`.
- **Mapeo objeto-documento**: mediante anotaciones `@Document(collection = "...")` en las entidades (`Resource`, `EmergencyContact`) y `@Id` en el campo identificador (tipo `String`, correspondiente al `ObjectId` de MongoDB serializado como cadena hexadecimal).
- **Migraciones**: no existen scripts de migración (`schema.sql`/`data.sql`) ni herramientas tipo Mongock/Flyway — es esperable, dado que MongoDB no exige un esquema rígido; las colecciones se crean de forma implícita al primer `save()`.
- **Repositorios**: `ResourceRepository` y `EmergencyContactRepository`, ambos interfaces que extienden `MongoRepository<T, String>`, aprovechando la generación automática de implementaciones en tiempo de ejecución por parte de Spring Data (no hay implementaciones manuales de estas interfaces).
- **Por qué MongoDB y no una base relacional**: el dominio de "recursos de bienestar" (tips, artículos, contactos) es naturalmente semiestructurado y puede evolucionar sin necesidad de migraciones de esquema; MongoDB permite iterar rápido sobre el modelo de datos durante el desarrollo del microservicio, y su modelo de documentos encaja bien con entidades que no requieren relaciones complejas ni transacciones multi-tabla (como sí sería el caso, por ejemplo, de un sistema de facturación).

## 10. Seguridad

El proyecto **no incluye `spring-boot-starter-security`** ni ninguna dependencia de seguridad (no hay JWT, OAuth2, filtros de autenticación, ni control de roles). Todos los endpoints bajo `/bienestar/**` están **abiertos sin autenticación ni autorización** a nivel de este microservicio. Tampoco se configura CORS explícitamente. Esto sugiere que, en la arquitectura general del sistema, la autenticación/autorización se delega a otra capa (por ejemplo, un API Gateway o un servicio de autenticación centralizado que no forma parte de este repositorio), o que la seguridad aún no ha sido implementada en este microservicio y sería un punto a reforzar en una futura iteración.

## 11. Manejo de errores / excepciones

Centralizado en `GlobalExceptionHandler` (`@RestControllerAdvice`):

- Captura genéricamente cualquier `RuntimeException` no manejada en los controladores.
- La traduce a una respuesta HTTP **`500 Internal Server Error`** con cuerpo JSON de la forma `{"error": "<mensaje>"}`.
- Caso de uso concreto en este proyecto: `BienestarService.getEmergencyContact()` lanza `new RuntimeException("No hay contacto de emergencia configurado")` cuando la colección de contactos está vacía; este handler es quien finalmente traduce esa excepción a una respuesta HTTP consistente para el cliente.
- **Limitación identificada**: al capturar `RuntimeException` de forma genérica y devolver siempre `500`, no se distingue entre errores de "recurso no encontrado" (que debería ser semánticamente `404`) y errores de "fallo interno real" (`500`). Una mejora recomendable sería introducir excepciones de dominio específicas (por ejemplo, `EmergencyContactNotFoundException`) con su propio `@ExceptionHandler` y código HTTP más preciso (`404`).
- La resiliencia ante fallos de servicios externos (`EventService` caído) **no se maneja como excepción HTTP**, sino como una regla de negocio dentro de `BienestarService.getWellbeingEvents()` (captura interna de `RestClientException` y retorno de lista vacía), por lo que nunca llega a `GlobalExceptionHandler`.

## 12. Comunicación con otros microservicios

- **Servicio externo consumido**: `EventService`, vía HTTP síncrono usando `RestTemplate` (bean definido en `RestTemplateConfig`).
- **Endpoint consumido**: `GET {events.service.url}?category=BIENESTAR`, donde `events.service.url` por defecto apunta a `http://localhost:8081/events` (o a la URL configurada por la variable de entorno `EVENTS_SERVICE_URL` en despliegue).
- **Formato esperado de respuesta**: arreglo JSON deserializado como `EventDTO[]`.
- **Mecanismo de resiliencia**: manual, mediante `try/catch` de `RestClientException` en `BienestarService.getWellbeingEvents()`; si la llamada falla (timeout, conexión rechazada, error HTTP, etc.) o la respuesta es `null`, se retorna una lista vacía en lugar de propagar el error al cliente de `BienestarService`.
- **Ausencia de *service discovery***: no se usa Eureka, Consul ni Spring Cloud Gateway; la URL de `EventService` es fija y configurable únicamente por variable de entorno, lo cual es una integración simple adecuada para un número reducido de microservicios pero que podría migrarse a *service discovery* dinámico si el sistema crece.
- No se identifican otros clientes salientes (no hay Feign Clients, WebClient reactivo, ni colas de mensajería como Kafka/RabbitMQ) en el proyecto.

## 13. Contenerización (Dockerfile)

El `Dockerfile` en la raíz del proyecto usa una estrategia de **build multi-stage** para producir una imagen final ligera:

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Explicación paso a paso:

1. **Etapa `builder`** (`eclipse-temurin:21-jdk-alpine`): imagen con el **JDK** completo de Java 21 sobre Alpine Linux (distribución mínima), necesaria para compilar el proyecto.
   - `WORKDIR /app`: define el directorio de trabajo dentro del contenedor.
   - `COPY . .`: copia todo el código fuente del repositorio al contenedor.
   - `RUN chmod +x mvnw && ./mvnw -B package -DskipTests`: da permisos de ejecución al Maven Wrapper y ejecuta el empaquetado (`package`) en modo *batch* (`-B`, sin interacción), **omitiendo las pruebas** (`-DskipTests`) para acelerar el build de la imagen — las pruebas ya se ejecutan por separado en el pipeline de CI antes de llegar a esta etapa.
2. **Etapa final** (`eclipse-temurin:21-jre-alpine`): imagen mucho más liviana que solo contiene el **JRE** (Java Runtime Environment, sin herramientas de compilación), suficiente para *ejecutar* la aplicación ya compilada.
   - `COPY --from=builder /app/target/*.jar app.jar`: copia únicamente el JAR ejecutable generado en la etapa anterior, descartando el código fuente, Maven y el JDK completo de la imagen final.
   - `EXPOSE 8083`: documenta que el contenedor escucha en el puerto 8083 (coincide con `server.port` de `application.properties`).
   - `ENTRYPOINT ["java", "-jar", "app.jar"]`: comando de arranque del contenedor.

**Por qué multi-stage build**: reduce significativamente el tamaño final de la imagen (no incluye JDK, Maven, ni código fuente/`.m2`), lo cual acelera el despliegue, reduce la superficie de ataque de seguridad de la imagen en producción, y sigue una práctica estándar recomendada por Docker para aplicaciones compiladas (Java, Go, etc.). El uso de imágenes **Alpine** minimiza aún más el tamaño base al usar `musl libc` en lugar de una distribución Linux completa.

**Uso en el pipeline de CI/CD** (`.github/workflows/ci-cd.yml`): la imagen se construye y publica en dos registries distintos según el flujo: **GitHub Container Registry (GHCR)** (`ghcr.io/<owner>/bienestar-service`) para despliegue vía SSH a un servidor propio, y **Azure Container Registry (ACR)** (`alpharegistry2026.azurecr.io/bienestarservice`) para despliegue directo a una **Azure Web App** (`bienestarservice-alpha-2026`) autenticado vía OIDC (sin secretos de administrador, usando *federated credentials* de Azure AD). En el despliegue por SSH, el contenedor se ejecuta mapeando el puerto host `8083` al puerto de contenedor `8082` (discrepancia frente al `EXPOSE 8083` del Dockerfile y al `server.port=8083` de la aplicación, posible inconsistencia a revisar en la configuración de despliegue) e inyectando las variables de entorno `SPRING_MONGODB_URI`, `EVENTS_SERVICE_URL` y `SPRING_APPLICATION_NAME`.

## 14. Testing

- **Frameworks usados**: JUnit 5 (Jupiter), Mockito (con la extensión `MockitoExtension`), AssertJ (aserciones fluidas tipo `assertThat(...)`), y `ReflectionTestUtils` de `spring-test` para inyectar valores en campos privados anotados con `@Value` sin levantar el contexto completo de Spring.
- **Tipo de pruebas presentes**: exclusivamente **pruebas unitarias** basadas en mocks (`@Mock`/`@InjectMocks`). **No hay** pruebas de integración con `@SpringBootTest`, ni `MockMvc` para pruebas de la capa web real (serialización JSON, códigos de estado HTTP reales, etc.), ni **Testcontainers** para levantar una instancia real de MongoDB en pruebas.
- **Cobertura por clase**:
  - `BienestarController` → cubierto por `BienestarControllerTest` (todos los endpoints, verificando delegación correcta al servicio mockeado).
  - `BienestarService` → cubierto exhaustivamente por `BienestarServiceApplicationTests`, incluyendo casos de éxito, casos borde (categoría en blanco, lista vacía) y casos de fallo/resiliencia (E1, E2, respuesta `null`, ausencia de contacto).
  - `MongoConfig`, `OpenApiConfig`, `RestTemplateConfig` (paquete `config`), `Resource`, `EmergencyContact` (paquete `model`), `EventDTO` (paquete `dto`), `ResourceRepository`, `EmergencyContactRepository` (paquete `repository`) → **sin pruebas dedicadas**, y explícitamente **excluidos de la medición de cobertura de JaCoCo** (ver exclusiones en `pom.xml`: `**/config/**`, `**/model/**`, `**/dto/**`, `**/repository/**`, `**/BienestarServiceApplication.class`), ya que son en su mayoría clases declarativas, de configuración o generadas dinámicamente por el framework.
  - `GlobalExceptionHandler` → no tiene una prueba unitaria directa y explícita, aunque su comportamiento (traducción de `RuntimeException` a error 500) queda implícitamente probado a través del caso `getEmergencyContact_none_throws` en `BienestarServiceApplicationTests` (que solo verifica que se lanza la excepción, no que el handler la traduzca correctamente a HTTP).
- **Umbral de calidad exigido**: el `jacoco-maven-plugin` está configurado para **fallar el build** (`mvn verify`) si la cobertura de instrucciones del *bundle* (excluyendo los paquetes mencionados) cae por debajo del **80 %**, aplicado automáticamente en el job `ci` del pipeline de GitHub Actions en cada `push`/`pull request`.

## 15. Cómo ejecutar el proyecto localmente

### Prerrequisitos

- Java 21 (JDK) instalado, o usar el Maven Wrapper incluido (no requiere Maven instalado globalmente).
- Una instancia de MongoDB accesible (local en `localhost:27017`, o remota vía la variable de entorno `SPRING_DATA_MONGODB_URI`).
- (Opcional) El microservicio `EventService` corriendo en `http://localhost:8081` si se desea probar el endpoint `/bienestar/events` con datos reales; si no está disponible, el endpoint seguirá funcionando y devolverá una lista vacía.

### Compilar y ejecutar pruebas

En Windows (PowerShell), desde la raíz del proyecto (`C:\Users\CAndr\Downloads\Alpha\BienestarService`):

```powershell
.\mvnw.cmd clean verify
```

Este comando compila el proyecto, ejecuta todas las pruebas unitarias (JUnit 5 + Mockito) y genera el reporte de cobertura JaCoCo en `target/site/jacoco/index.html`, fallando el build si la cobertura es inferior al 80 %.

### Ejecutar la aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

O, tras empaquetar:

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\BienestarService-0.0.1-SNAPSHOT.jar
```

La API quedará disponible en `http://localhost:8083/bienestar/...` y la documentación interactiva Swagger en `http://localhost:8083/swagger-ui.html`.

### Variables de entorno relevantes para ejecución local personalizada

```powershell
$env:SPRING_DATA_MONGODB_URI = "mongodb://localhost:27017/bienestardb"
$env:EVENTS_SERVICE_URL = "http://localhost:8081/events"
.\mvnw.cmd spring-boot:run
```

### Ejecutar con Docker

```powershell
docker build -t bienestar-service .
docker run -d -p 8083:8083 `
  -e SPRING_DATA_MONGODB_URI="mongodb://host.docker.internal:27017/bienestardb" `
  -e EVENTS_SERVICE_URL="http://host.docker.internal:8081/events" `
  bienestar-service
```

## 16. Justificación de decisiones tecnológicas

- **¿Por qué Spring Boot?** Es el framework más consolidado del ecosistema Java para construir microservicios: ofrece autoconfiguración basada en las dependencias presentes en el classpath (por ejemplo, al incluir `spring-boot-starter-web` se configura automáticamente Tomcat, Jackson y el `DispatcherServlet`), un servidor embebido que simplifica el despliegue (no requiere instalar un servidor de aplicaciones aparte), y una enorme cantidad de *starters* que reducen drásticamente el tiempo de configuración inicial de un servicio nuevo. Para una arquitectura de microservicios en Java, es prácticamente el estándar de facto.

- **¿Por qué MongoDB en lugar de una base de datos relacional?** El dominio manejado por este microservicio (recursos de bienestar, contactos de emergencia) es sencillo, con entidades que no requieren relaciones complejas ni transacciones multi-entidad, y cuyo esquema puede evolucionar con frecuencia durante el desarrollo (agregar nuevos campos a un recurso, por ejemplo) sin necesidad de escribir y versionar migraciones de esquema SQL. El modelo de documentos de MongoDB encaja naturalmente con este tipo de datos semiestructurados, y Spring Data MongoDB permite implementar el acceso a datos con muy poco código (interfaces declarativas).

- **¿Por qué Spring Data MongoDB (repositorios) en lugar de acceso manual al driver?** Los repositorios de Spring Data eliminan código repetitivo de acceso a datos (no hay que escribir manualmente consultas `find`/`insert`/`update`), aportan *query methods* derivados del nombre del método (`findByCategory`) y se integran de forma transparente con el resto del ecosistema Spring (inyección de dependencias, manejo de excepciones de infraestructura traducidas a excepciones de Spring). Aun así, el proyecto expone también un `MongoTemplate` (en `MongoConfig`) para casos en los que se necesite mayor control sobre las consultas, aunque actualmente no se usa activamente fuera de los repositorios derivados.

- **¿Por qué Lombok?** Las clases de modelo y DTO de este proyecto son esencialmente POJOs con getters/setters. Lombok (`@Data`) elimina ese código repetitivo, mejorando la legibilidad y reduciendo el riesgo de errores manuales al escribir `equals`/`hashCode`/`toString`, sin cambiar el comportamiento en tiempo de ejecución (el bytecode generado es equivalente al que se escribiría a mano).

- **¿Por qué springdoc-openapi (Swagger)?** En un ecosistema de microservicios, cada servicio necesita un contrato de API claro y explorable por otros equipos o servicios consumidores. Generar la documentación directamente desde las anotaciones del código (en lugar de mantener un documento separado y propenso a desactualizarse) garantiza que la documentación esté siempre sincronizada con la implementación real, y la UI de Swagger permite probar los endpoints manualmente sin herramientas externas como Postman.

- **¿Por qué la arquitectura en capas (Controller → Service → Repository)?** Es el patrón más simple y ampliamente entendido para estructurar un microservicio de complejidad moderada: separa claramente la responsabilidad de "recibir y traducir peticiones HTTP" (`controller`), "aplicar reglas de negocio" (`service`) y "persistir/consultar datos" (`repository`), lo que facilita las pruebas unitarias aisladas por capa (como se observa en los tests del proyecto, que mockean la capa inmediatamente inferior) y hace que el código sea más fácil de mantener y extender a medida que crece el dominio de bienestar.

- **¿Por qué `RestTemplate` y no `WebClient` u OpenFeign para comunicarse con `EventService`?** `RestTemplate`, aunque está en modo de mantenimiento dentro del ecosistema Spring (Spring recomienda `WebClient` para nuevos desarrollos, especialmente en contextos reactivos), sigue siendo una opción válida, simple y de bajo *overhead* para una integración HTTP síncrona puntual como la que requiere este microservicio (una sola llamada `GET` a un solo servicio externo), sin necesitar la complejidad adicional de programación reactiva (`WebClient`) ni la configuración de un cliente declarativo (Feign), que aportarían más valor en integraciones con múltiples endpoints o servicios.

- **¿Por qué Docker con build multi-stage?** Permite generar una imagen final mínima que solo contiene el JRE y el JAR compilado, sin arrastrar herramientas de build (Maven, JDK completo) ni código fuente a producción, lo cual reduce el tamaño de la imagen, acelera los despliegues y disminuye la superficie de ataque. Es el enfoque estándar recomendado para contenerizar aplicaciones JVM.

- **¿Por qué JaCoCo con umbral mínimo de cobertura?** Automatizar la verificación de un umbral mínimo de cobertura de pruebas (80 % en este proyecto) dentro del propio build de Maven, integrado al pipeline de CI, evita que código sin pruebas suficientes llegue a producción, actuando como una red de seguridad de calidad continua sin depender de revisión manual exclusivamente.

## 17. Observaciones y posibles mejoras identificadas durante el análisis

Estas observaciones no forman parte del comportamiento documentado como "correcto", sino hallazgos útiles para quien profundice en el microservicio:

1. **Inconsistencia de nombre de base de datos**: `MongoConfig` fija explícitamente el nombre de base de datos `"eventsdb"` en `SimpleMongoClientDatabaseFactory`, mientras que la URI por defecto en `application.properties` apunta a `bienestardb`.
2. **Inconsistencia de variable de entorno de MongoDB**: `application.properties` espera `SPRING_DATA_MONGODB_URI`, pero el workflow de despliegue por SSH inyecta `SPRING_MONGODB_URI` (nombre distinto), lo que podría hacer que en ese flujo de despliegue la URI de producción no sobrescriba correctamente el valor por defecto.
3. **Inconsistencia de puerto en despliegue SSH**: el `Dockerfile` expone el puerto `8083` (igual que `server.port`), pero el `docker run` del workflow de despliegue SSH mapea `-p 8083:8082`, asumiendo que el contenedor escucha en `8082` en vez de `8083`.
4. **Sin seguridad**: no hay autenticación/autorización a nivel de este microservicio; se asume que se maneja en otra capa de la arquitectura general.
5. **Manejo de errores genérico**: `GlobalExceptionHandler` traduce toda `RuntimeException` a `500`, sin diferenciar semánticamente casos como "no encontrado" (`404`).
6. **Sin pruebas de integración**: no se usan `@SpringBootTest`, `MockMvc` ni Testcontainers, por lo que no hay verificación automatizada de la integración real con MongoDB ni de la capa HTTP completa (serialización JSON real, códigos de estado reales end-to-end).
