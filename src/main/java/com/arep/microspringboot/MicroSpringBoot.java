package com.arep.microspringboot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MicroSpringBoot {

    static final int DEFAULT_PORT = 8080;
    private static final String STATIC_ROOT = "static";
    static final Map<String, RouteDefinition> controllerMethods = new LinkedHashMap<>();

    public static void main(String[] args) throws Exception {
        Configuration configuration = resolveConfiguration(args);

        if (configuration.controllerClassName != null) {
            loadSingleController(configuration.controllerClassName);
        } else {
            loadControllers(configuration.basePackage);
        }

        startServer(configuration.port);
    }

    static void loadControllers(String basePackage) throws Exception {
        controllerMethods.clear();
        for (Class<?> candidate : findClasses(basePackage)) {
            registerController(candidate);
        }
    }

    static void loadSingleController(String controllerClassName) throws Exception {
        controllerMethods.clear();
        registerController(Class.forName(controllerClassName));
    }

    static HttpResponse handleRequest(String httpMethod, String target)
            throws InvocationTargetException, IllegalAccessException {
        if (!"GET".equalsIgnoreCase(httpMethod)) {
            return textResponse(405, "Method Not Allowed", "Only GET is supported");
        }

        URI uri;
        try {
            uri = new URI(target);
        } catch (URISyntaxException e) {
            return textResponse(400, "Bad Request", "Malformed request target");
        }

        String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
        Map<String, String> queryParams = parseQuery(uri.getRawQuery());

        RouteDefinition route = controllerMethods.get(path);
        if (route != null) {
            return invokeRoute(route, queryParams);
        }

        return loadStaticResource(path);
    }

    private static HttpResponse invokeRoute(RouteDefinition route, Map<String, String> queryParams)
            throws InvocationTargetException, IllegalAccessException {
        Object[] arguments = new Object[route.parameters.size()];
        for (int index = 0; index < route.parameters.size(); index++) {
            ParameterDefinition parameter = route.parameters.get(index);
            arguments[index] = queryParams.getOrDefault(parameter.name, parameter.defaultValue);
        }

        String responseBody = (String) route.method.invoke(route.controllerInstance, arguments);
        String contentType = looksLikeHtml(responseBody) ? "text/html; charset=UTF-8" : "text/plain; charset=UTF-8";
        return new HttpResponse(200, "OK", contentType, responseBody.getBytes(StandardCharsets.UTF_8));
    }

    private static HttpResponse loadStaticResource(String path) {
        String normalizedPath = normalizeStaticPath(path);
        if (normalizedPath == null) {
            return textResponse(403, "Forbidden", "Invalid path");
        }

        byte[] content = readClasspathResource(normalizedPath);
        if (content == null && "/".equals(path)) {
            content = readClasspathResource("index.html");
            normalizedPath = "index.html";
        }

        if (content == null) {
            return textResponse(404, "Not Found", "Resource not found");
        }

        return new HttpResponse(200, "OK", detectContentType(normalizedPath), content);
    }

    private static String normalizeStaticPath(String path) {
        String resourcePath = "/".equals(path) ? "index.html" : path.startsWith("/") ? path.substring(1) : path;
        if (resourcePath.contains("..")) {
            return null;
        }
        return resourcePath;
    }

    private static byte[] readClasspathResource(String resourcePath) {
        ClassLoader classLoader = MicroSpringBoot.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(STATIC_ROOT + "/" + resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            return inputStream.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> queryParams = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = keyValue.length > 1
                    ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
                    : "";
            queryParams.put(key, value);
        }
        return queryParams;
    }

    private static void startServer(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("MicroSpringBoot listening on http://localhost:" + port);
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleClient(clientSocket);
                }
            }
        }
    }

    private static void handleClient(Socket clientSocket) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isBlank()) {
                return;
            }

            String line;
            while ((line = reader.readLine()) != null && !line.isBlank()) {
                // Minimal HTTP parser: only request line and header termination are required.
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                writeResponse(clientSocket.getOutputStream(), textResponse(400, "Bad Request", "Malformed request line"));
                return;
            }

            HttpResponse response;
            try {
                response = handleRequest(requestParts[0], requestParts[1]);
            } catch (InvocationTargetException | IllegalAccessException e) {
                response = textResponse(500, "Internal Server Error", "Error invoking controller");
            }

            writeResponse(clientSocket.getOutputStream(), response);
        } finally {
            clientSocket.close();
        }
    }

    private static void writeResponse(OutputStream outputStream, HttpResponse response) throws IOException {
        String headers = "HTTP/1.1 " + response.statusCode + ' ' + response.statusText + "\r\n"
                + "Content-Type: " + response.contentType + "\r\n"
                + "Content-Length: " + response.body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        outputStream.write(headers.getBytes(StandardCharsets.UTF_8));
        outputStream.write(response.body);
        outputStream.flush();
    }

    private static Configuration resolveConfiguration(String[] args) {
        String basePackage = MicroSpringBoot.class.getPackageName();
        String controllerClassName = null;
        int port = DEFAULT_PORT;

        if (args.length == 0) {
            return new Configuration(basePackage, controllerClassName, port);
        }

        if (isInteger(args[0])) {
            port = Integer.parseInt(args[0]);
            return new Configuration(basePackage, controllerClassName, port);
        }

        try {
            Class<?> controllerClass = Class.forName(args[0]);
            controllerClassName = controllerClass.getName();
        } catch (ClassNotFoundException e) {
            basePackage = args[0];
        }

        if (args.length > 1 && isInteger(args[1])) {
            port = Integer.parseInt(args[1]);
        }

        return new Configuration(basePackage, controllerClassName, port);
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void registerController(Class<?> candidate) throws Exception {
        if (!candidate.isAnnotationPresent(RestController.class)) {
            return;
        }

        Object controllerInstance = candidate.getDeclaredConstructor().newInstance();

        for (Method method : candidate.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(GetMapping.class) || method.getReturnType() != String.class) {
                continue;
            }

            List<ParameterDefinition> parameters = analyzeParameters(method);
            String path = method.getAnnotation(GetMapping.class).value();
            controllerMethods.put(path, new RouteDefinition(controllerInstance, method, parameters));
        }
    }

    private static List<ParameterDefinition> analyzeParameters(Method method) {
        List<ParameterDefinition> parameters = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam == null || parameter.getType() != String.class) {
                throw new IllegalArgumentException("Only String parameters annotated with @RequestParam are supported");
            }

            parameters.add(new ParameterDefinition(requestParam.value(), requestParam.defaultValue()));
        }
        return parameters;
    }

    private static List<Class<?>> findClasses(String basePackage) throws Exception {
        String path = basePackage.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
        List<Class<?>> classes = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                File directory = new File(resource.toURI());
                classes.addAll(findClassesInDirectory(directory, basePackage));
                continue;
            }

            if ("jar".equals(resource.getProtocol())) {
                classes.addAll(findClassesInJar(resource, path));
            }
        }

        return classes;
    }

    private static List<Class<?>> findClassesInJar(URL resource, String packagePath) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        JarURLConnection connection = (JarURLConnection) resource.openConnection();

        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entryName.startsWith(packagePath)
                        || !entryName.endsWith(".class")
                        || entryName.contains("$")) {
                    continue;
                }

                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }

    private static List<Class<?>> findClassesInDirectory(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClassesInDirectory(file, packageName + '.' + file.getName()));
                continue;
            }

            if (!file.getName().endsWith(".class") || file.getName().contains("$")) {
                continue;
            }

            String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
            classes.add(Class.forName(className));
        }

        return classes;
    }

    private static boolean looksLikeHtml(String responseBody) {
        String trimmed = responseBody == null ? "" : responseBody.stripLeading().toLowerCase();
        return trimmed.startsWith("<!doctype html") || trimmed.startsWith("<html");
    }

    private static String detectContentType(String resourcePath) {
        String normalized = resourcePath.toLowerCase();
        if (normalized.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (normalized.endsWith(".png")) {
            return "image/png";
        }
        if (normalized.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (normalized.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        return "application/octet-stream";
    }

    private static HttpResponse textResponse(int statusCode, String statusText, String body) {
        return new HttpResponse(statusCode, statusText, "text/plain; charset=UTF-8",
                body.getBytes(StandardCharsets.UTF_8));
    }

    static final class Configuration {
        private final String basePackage;
        private final String controllerClassName;
        private final int port;

        private Configuration(String basePackage, String controllerClassName, int port) {
            this.basePackage = basePackage;
            this.controllerClassName = controllerClassName;
            this.port = port;
        }
    }

    static final class RouteDefinition {
        private final Object controllerInstance;
        private final Method method;
        private final List<ParameterDefinition> parameters;

        private RouteDefinition(Object controllerInstance, Method method, List<ParameterDefinition> parameters) {
            this.controllerInstance = controllerInstance;
            this.method = method;
            this.parameters = parameters;
        }
    }

    static final class ParameterDefinition {
        private final String name;
        private final String defaultValue;

        private ParameterDefinition(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }

    static final class HttpResponse {
        private final int statusCode;
        private final String statusText;
        private final String contentType;
        private final byte[] body;

        private HttpResponse(int statusCode, String statusText, String contentType, byte[] body) {
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.contentType = contentType;
            this.body = body;
        }

        int statusCode() {
            return statusCode;
        }

        String contentType() {
            return contentType;
        }

        byte[] body() {
            return body;
        }

        String bodyAsString() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }
}
