# MicroSpringBoot

Prototipo minimo de un servidor web en Java con un micro-framework IoC basado en reflexion. El proyecto descubre componentes anotados con `@RestController`, registra metodos anotados con `@GetMapping`, resuelve parametros con `@RequestParam` y sirve recursos estaticos desde el classpath.

## Estado actual

La implementacion actual ya cubre una version funcional del laboratorio:

- Define las anotaciones `@RestController` y `@GetMapping`.
- Define la anotacion `@RequestParam`.
- Descubre controladores del paquete base usando reflexion.
- Levanta un servidor HTTP basico con `ServerSocket`.
- Atiende multiples solicitudes no concurrentes.
- Sirve respuestas dinamicas de tipo `String`.
- Sirve archivos estaticos HTML y PNG desde `src/main/resources/static`.
- Incluye los controladores `HelloController` y `GreetingController`.

El proyecto ya puede ejecutarse desde navegador o por peticiones HTTP directas en `http://localhost:8080`.

## Requisitos previos

- Java 17
- Maven 3.9 o superior

## Estructura del proyecto

```text
microspringboot/
|-- pom.xml
|-- README.md
|-- src/
|   |-- main/java/com/arep/microspringboot/
|   |   |-- GetMapping.java
|   |   |-- HelloController.java
|   |   |-- GreetingController.java
|   |   |-- MicroSpringBoot.java
|   |   |-- RequestParam.java
|   |   `-- RestController.java
|   |-- main/resources/static/
|   |   |-- index.html
|   |   `-- images/pixel.png
|   `-- test/java/com/arep/microspringboot/
|       `-- AppTest.java
`-- target/
```

## Compilacion

```bash
mvn clean package
```

El proyecto ahora genera un jar ejecutable en:

```text
target/microspringboot-1.0-SNAPSHOT.jar
```

## Ejecucion

Levantar el servidor en el puerto por defecto `8080`:

```bash
mvn compile
java -cp target/classes com.arep.microspringboot.MicroSpringBoot
```

Tambien puedes ejecutarlo como jar:

```bash
java -jar target/microspringboot-1.0-SNAPSHOT.jar
```

Tambien puedes indicar un puerto distinto:

```bash
java -cp target/classes com.arep.microspringboot.MicroSpringBoot 35000
```

O cargar solo una clase controladora especifica:

```bash
java -cp target/classes com.arep.microspringboot.MicroSpringBoot com.arep.microspringboot.HelloController
```

## Endpoints disponibles

Dinamicos:

- `/`
- `/hello`
- `/pi`
- `/greeting?name=AREP`

Estaticos:

- `/index.html`
- `/images/pixel.png`

## Ejemplos de uso

```bash
curl http://localhost:8080/hello
curl "http://localhost:8080/greeting?name=AREP"
curl http://localhost:8080/index.html
```

Respuestas esperadas:

- `/hello` responde `Hello World!`
- `/greeting?name=AREP` responde `Hola AREP`
- `/index.html` entrega una pagina HTML de prueba
- `/images/pixel.png` entrega una imagen PNG de prueba

## Controladores de ejemplo

`HelloController` expone la pagina principal, `/hello` y `/pi`.

`GreetingController` demuestra el uso de `@RequestParam`:

```java
@RestController
public class GreetingController {

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hola " + name;
    }
}
```

## Pruebas

Para ejecutar las pruebas:

```bash
mvn test
```

Las pruebas actuales validan:

- Resolucion de rutas dinamicas.
- Soporte de `@RequestParam` con y sin valor.
- Entrega de HTML estatico.
- Entrega de PNG estatico.
- Respuesta `404` para recursos inexistentes.

## Alcance logrado frente al taller

Implementado:

- Proyecto estructurado con Maven.
- Servidor HTTP basico en Java.
- Descubrimiento automatico de controladores anotados con `@RestController`.
- Soporte para `@GetMapping`.
- Soporte para `@RequestParam`.
- Entrega de paginas HTML e imagenes PNG.
- Atencion de multiples solicitudes no concurrentes.
- Aplicacion de ejemplo derivada de POJOs.

Pendiente para la entrega completa:

- Desplegar la aplicacion en AWS y adjuntar evidencia de ejecucion.
- Agregar al repositorio las evidencias finales de despliegue solicitadas por el laboratorio.

## Siguiente iteracion recomendada

1. Desplegar el servidor en AWS.
2. Guardar capturas de compilacion, ejecucion y pruebas HTTP.
3. Subir el codigo final a GitHub.
4. Documentar la URL, IP publica o evidencia de acceso remoto.

## Despliegue en AWS

Deje una guia especifica para EC2 en [AWS-EC2-DEPLOY.md](AWS-EC2-DEPLOY.md).

Resumen rapido:

1. Ejecutar `mvn clean package`.
2. Subir `target/microspringboot-1.0-SNAPSHOT.jar` a una instancia EC2.
3. Instalar Java 17 en la instancia.
4. Abrir el puerto `8080` en el Security Group.
5. Ejecutar `java -jar microspringboot-1.0-SNAPSHOT.jar`.
6. Probar desde navegador o `curl` contra la IP publica.

## Autor

Proyecto academico para el curso de AREP.