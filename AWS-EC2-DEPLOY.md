# Despliegue En AWS EC2

Esta guia deja el proyecto listo para una evidencia simple y valida del laboratorio usando una instancia EC2 Linux.

## 1. Crear la instancia

- Crear una instancia EC2 con Amazon Linux 2023 o Ubuntu 22.04.
- Tipo sugerido: `t2.micro` o `t3.micro`.
- Crear o reutilizar un par de llaves `.pem`.
- En el Security Group abrir:
  - `22` para SSH desde tu IP.
  - `8080` para HTTP desde `0.0.0.0/0` o desde tu rango permitido.

## 2. Empaquetar la aplicacion

Desde tu maquina local:

```bash
mvn clean package
```

El artefacto generado queda en:

```text
target/microspringboot-1.0-SNAPSHOT.jar
```

## 3. Subir el jar al servidor

Ejemplo con PowerShell o terminal con `scp`:

```bash
scp -i "ruta/a/tu-llave.pem" target/microspringboot-1.0-SNAPSHOT.jar ec2-user@TU_IP_PUBLICA:/home/ec2-user/
```

Si usas Ubuntu cambia `ec2-user` por `ubuntu`.

## 4. Entrar por SSH

```bash
ssh -i "ruta/a/tu-llave.pem" ec2-user@TU_IP_PUBLICA
```

## 5. Instalar Java 17

Amazon Linux 2023:

```bash
sudo dnf update -y
sudo dnf install -y java-17-amazon-corretto
java -version
```

Ubuntu 22.04:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version
```

## 6. Ejecutar la aplicacion

Opcion directa:

```bash
java -jar microspringboot-1.0-SNAPSHOT.jar
```

Opcion en background:

```bash
nohup java -jar microspringboot-1.0-SNAPSHOT.jar 8080 > server.log 2>&1 &
```

Verificar proceso:

```bash
ps -ef | grep microspringboot
tail -f server.log
```

## 7. Probar desde navegador o curl

Desde tu navegador:

- `http://TU_IP_PUBLICA:8080/hello`
- `http://TU_IP_PUBLICA:8080/greeting?name=AREP`
- `http://TU_IP_PUBLICA:8080/index.html`
- `http://TU_IP_PUBLICA:8080/images/pixel.png`

Desde terminal:

```bash
curl http://TU_IP_PUBLICA:8080/hello
curl "http://TU_IP_PUBLICA:8080/greeting?name=AREP"
curl http://TU_IP_PUBLICA:8080/index.html
```

## 8. Evidencia que deberias guardar

- Captura de `mvn clean package` exitoso.
- Captura de `java -version` en EC2.
- Captura de `java -jar microspringboot-1.0-SNAPSHOT.jar` o `tail -f server.log`.
- Captura del navegador abriendo `/hello`.
- Captura del navegador abriendo `/greeting?name=AREP`.
- Captura del navegador abriendo `/index.html`.
- Captura del navegador o respuesta mostrando `/images/pixel.png`.
- Captura de la IP publica o DNS de la instancia.

## 9. Texto corto para el README

Cuando ya tengas la evidencia real puedes agregar algo como:

```text
La aplicacion fue desplegada en AWS EC2 con Java 17 y expuesta por el puerto 8080.
Se verificaron correctamente los endpoints /hello, /greeting?name=AREP, /index.html y /images/pixel.png.
```

## 10. Nota importante

Este repositorio puede quedar completamente listo para despliegue, pero la evidencia final de AWS solo se puede producir una vez ejecutes estos pasos con tu propia cuenta y tu propia instancia.