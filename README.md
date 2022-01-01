# Controladores Funcionales
Programación reactiva y funcional
Spring WebFlux Router nos permitirá que nuestros flujos reactivos sean accesibles desde el api por medio de funciones, ya no se un controlador imperativo sino por medio de funciones puras.

En el ámbito funcional, un servicio web se denomina ruta y el concepto tradicional de @Controller y @RequestMapping se reemplaza por RouterFunction. Para crear nuestro primer servicio, tomemos un servicio basado en anotaciones y veamos cómo se puede traducir a su equivalente funcional. Usaremos el ejemplo de un servicio que devuelve todos los productos de un catálogo de productos:

```java
@RestController
public class ProductController {
    @RequestMapping("/product")
    public List<Product> productListing() {
        return ps.findAll();
    }
}
```
El equivalente funcional de este método seria el siguiente:

```java
@Bean
public RouterFunction<ServerResponse> productListing(ProductService ps) {
    return route().GET("/product", req -> ok().body(ps.findAll()))
      .build();
}
```
Otro cambio significativo a la hora de crear nuestra api es la inclusión de un componente de configuración, en este caso lo usaremos también para permitir el mapeo de entrada para los servicios desde cualquier fuente externa. 

## Config
```java
@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
    @Bean
    public WebFluxConfigurer corsConfigure() {
        return new WebFluxConfigurerComposite() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*")
                        .allowedMethods("*");
            }
        };
    }
}
```
Debemos tener en cuenta que en el enfoque funcional, el método productListing() devuelve una RouterFunction en lugar del cuerpo de respuesta tradicional. Es la definición de la ruta, no la ejecución de una solicitud.

RouterFunction incluye la ruta, los encabezados de solicitud, una función de controlador, que se utilizará para generar el cuerpo de respuesta y los encabezados de respuesta. Puede contener uno o un grupo de servicios web. 

En este ejemplo, usamos el método static route() en RouterFunctions para crear una RouterFunction. Todas las solicitudes y atributos de respuesta para una ruta se pueden proporcionar utilizando este método.

## Predicado de Petición
En nuestro ejemplo, usamos el método GET() en route() para especificar que se trata de una solicitud GET, con una ruta proporcionada como String. También podemos utilizar RequestPredicate cuando queramos especificar más detalles de la solicitud. 

Por ejemplo, la ruta del ejemplo anterior también se puede especificar mediante RequestPredicate como:

```java
RequestPredicates.path("/product")
```

## Respuesta
ServerResponse contiene métodos de estáticos que se utilizan para crear el objeto de respuesta. En nuestro ejemplo, usamos ok() para agregar un estado HTTP 200 a los encabezados de respuesta y luego usamos el body() para especificar el cuerpo de la respuesta. Además, ServerResponse admite la creación de respuestas a partir de tipos de datos personalizados mediante EntityResponse como se venia haciendo previamente. 



API Rest con Webflux y Funciones Router
Vamos a convertir el ejemplo anterior en una api que además de usar los flujos de datos reactivos Mono y Flux defina estos servicios como funciones usando Routers en lugar de Controladores. para ello vamos a cambiar un poco el proyecto base.

incluiremos una dependencia adicional, que nos permitirá y facilitara realizar validaciones
```xml
           <dependency>
                     <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-validation</artifactId>
            </dependency>
```
Incluiremos un DTO y un Mapper de la siguiente manera:

## datoDTO
```java
public class DatoDTO {

    private String id;
    @NotBlank
    private String informacion;

    public DatoDTO() {

    }

    public DatoDTO(String informacion) {
        this.informacion = informacion;
    }

    public DatoDTO(String id, String informacion) {
        this.id = id;
        this.informacion = informacion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInformacion() {
        return informacion;
    }

    public void setInformacion(String informacion) {
        this.informacion = informacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatoDTO datoDTO = (DatoDTO) o;
        return Objects.equals(id, datoDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DatoDTO{" +
                "id='" + id + '\'' +
                ", informacion='" + informacion + '\'' +
                '}';
    }
}
```
Definimos dos constructores, teniendo en cuenta que el dato sin crear no tendra el ID generado por Mongo, mientras que un dato que ya existe en la base de datos si tendra dicho campo.

## Mapper
```java
@Component
public class MapperUtils {
    public Function<DatoDTO, Dato> mapperToDato(String id) {
        return updateDato -> {
            var dato = new Dato();
            dato.setId(id);
            dato.setInformacion(updateDato.getInformacion());
            return dato;
        };
    }
    public Function<Dato, DatoDTO> mapDatoToDTO() {
        return entity -> new DatoDTO(entity.getId(), entity.getInformacion());
    }
}
```

En este maper podemos ver que ahora usamos funciones para convertir los datos.

Continuando con nuestro ejemplo vamos a prescindir de los paquetes de Controller y Service, y crearemos el Routers y Casos de Uso. Un Router permitirá reemplazar al controlador permitiendo la comunicación del api con el exterior, enviado y recibiendo datos. Un Caso de Uso formara una función que tomara la informacion del Router y la enviara al repositorio. Veamos un ejemplo con una petición GET.

## Router Creación
```java
@Configuration
public class CrearDatoRouter {
    @Bean
    public RouterFunction<ServerResponse> createQuestion(UseCaseCrear useCaseCrear) {
        return route(POST("/crear").and(accept(MediaType.APPLICATION_JSON)),
                request -> request.bodyToMono(DatoDTO.class)
                        .flatMap(questionDTO -> useCaseCrear.apply(questionDTO)
                                .flatMap(result -> ServerResponse.ok()
                                        .contentType(MediaType.TEXT_PLAIN)
                                        .bodyValue(result))
                        )
        );
    }
}
```

## Router Consulta
```java
@Configuration
public class ConsultarDatoRouter {
    @Bean
    public RouterFunction<ServerResponse> getAll(UseCaseListar useCaseListar) {
        return route(
                GET("/consultar").and(accept(MediaType.APPLICATION_JSON)),
                request -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromPublisher(useCaseListar.get(), DatoDTO.class))
        );
    }
}
```

Como puedes ver tenemos una función router que recibe el caso de uso, es decir, cuando la función router se notifique como iniciada usara lo que definamos en el caso de uso para funcionar. La función router define un GET en la ruta (/consultar), permitiendo a un mensaje en formato JSON convertirse en un objeto de datos que será enviado evaluado por la función caso de uso.

## Use Case
```java
@FunctionalInterface
public interface GuardarDato {
    public Mono<String> apply(DatoDTO datoDTO);
}
```

Debemos definir una interfaz que nos permita definir los parámetros de la función, en este caso recibirá un objeto DTO y devolverá un String Mono correspondiente al ID del dato creado.

## Use Case Creación
```java
@Service
@Validated
public class UseCaseCrear implements GuardarDato {
    private final Repositorio repositorio;
    private final MapperUtils mapperUtils;
    @Autowired
    public UseCaseCrear(MapperUtils mapperUtils, Repositorio repositorio) {
        this.repositorio = repositorio;
        this.mapperUtils = mapperUtils;
    }

    @Override
    public Mono<String> apply(DatoDTO datoDTO) {
        return repositorio.save(mapperUtils.mapperToDato(null).apply(datoDTO)).map(Dato::getId);
    }
}
```

## Use Case Consulta
```java
@Service
@Validated
public class UseCaseListar implements Supplier<Flux<DatoDTO>> {
    private final Repositorio repositorio;
    private final MapperUtils mapperUtils;
    public UseCaseListar(MapperUtils mapperUtils, Repositorio repositorio) {
        this.repositorio = repositorio;
        this.mapperUtils = mapperUtils;
    }

    @Override
    public Flux<DatoDTO> get() {
        return repositorio.findAll().map(mapperUtils.mapDatoToDTO());
    }
}
```

El caso de uso es muy sencillo, utiliza el repositorio para guardar el objeto que previamente se mapeo de DTO a objeto de colección. Al final usamos un mapa para obtener el ID del dato guardado.

Con estos elementos hemos cubierto la creación de una aplicación Spring Boot completamente funcional y reactiva para construir apis de RESTful services. Puedes probar con Postman el funcionamiento de estos servicios. 


# Test en Controladores Funcionales

Pruebas a API reactiva y funcional

Tomando de base el api de servicios que creamos anteriormente vamos a crear algunos test para probar nuestros servicios y controladores funcionales y reactivos.



## Pruebas Unitarias a Routers Reactivos.

Veamos ahora el caso de nuestros Routers, debido a que este es accesible mediante protocolo HTTP no podemos instanciarlo como hicimos con el casos de uso, debemos usar WebTestClient el cual nos permite simular una interfaz web para hacer los llamados GET, POST, PUT o DELETE de manera reactiva y no que harían quien consumiera dichos servicios.


### Prueba Funcional GET
```java
@WebFluxTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ConsultarDatoRouter.class, UseCaseListar.class, MapperUtils.class})
class ConsultarDatoRouterTest {

  @MockBean
    private Repositorio repositorio;

    @Autowired
    private WebTestClient webTestClient;


    @Test
    public void testGetDatos() {
     Dato dato1 = new Dato();
        dato1.setInformacion("Informacion 1");
        Dato dato2 = new Dato();
        dato2.setInformacion("Informacion 2");

        when(repositorio.findAll()).thenReturn(Flux.just(dato1, dato2));

        webTestClient.get()
                .uri("/consultar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DatoDTO.class)
                .value(userResponse -> {
                            Assertions.assertThat(userResponse.get(0).getInformacion()).isEqualTo(dato1.getInformacion());
                            Assertions.assertThat(userResponse.get(1).getInformacion()).isEqualTo(dato2.getInformacion());
                        }
                );
    }

}
```


### Prueba Funcional POST
```java
@WebFluxTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CrearDatoRouter.class, UseCaseCrear.class, MapperUtils.class})
class CrearDatoRouterTest {

    @MockBean
    private Repositorio repositorio;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testCreateUser() {

        Dato dato = new Dato();
        dato.setId("xxxxxxx");
        dato.setInformacion("Informacion 1");
        DatoDTO datoDTO = new DatoDTO(dato.getId(), dato.getInformacion());
        Mono<Dato> datoMono = Mono.just(dato);
        when(repositorio.save(any())).thenReturn(datoMono);

        webTestClient.post()
                .uri("/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(datoDTO), DatoDTO.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(userResponse -> {
                            Assertions.assertThat(userResponse).isEqualTo(dato.getId());
                        }
                );
    }

}
```

Las pruebas descritas no solo nos servirán para advertir si las funcionalidades esperadas cambian con el tiempo, sino que serán guia para que el desarrollo se mantenga estable, cuando creamos APIs de servicios debemos garantizar que la comunicación sea la misma siempre de manera que quien consuma dicha API no tenga que realizar cambios inesperados a raíz de modificaciones en el back-end.

