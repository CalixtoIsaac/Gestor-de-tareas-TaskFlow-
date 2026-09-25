# Sistema Empresarial Avanzado de Gestión de Tareas

Aplicación de escritorio en **Java Swing** que simula un sistema de gestión de tareas para una empresa, integrando en una sola interfaz gráfica las principales estructuras de datos y algoritmos de un curso de Estructuras de Datos: colas de prioridad, árboles binarios de búsqueda, tablas hash, algoritmos de ordenamiento y búsqueda, recursividad, divide y vencerás, y grafos dirigidos.

Proyecto Final — materia de Estructuras de Datos.

<img width="1163" height="781" alt="image" src="https://github.com/user-attachments/assets/a42a5ccb-e171-4ab5-a418-81be80ca9179" />
<img width="1162" height="780" alt="image" src="https://github.com/user-attachments/assets/0282b712-acfe-41ea-9a22-a76c6659b69c" />
<img width="1164" height="781" alt="image" src="https://github.com/user-attachments/assets/fbe2b574-fb10-4195-9b45-e726dfa5c870" />

---

## Tabla de contenido

- [Descripción general](#descripción-general)
- [Estructuras de datos y algoritmos implementados](#estructuras-de-datos-y-algoritmos-implementados)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos](#requisitos)
- [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
- [Guía rápida de uso](#guía-rápida-de-uso)
- [Limitaciones conocidas](#limitaciones-conocidas)
- [Roadmap / mejoras futuras](#roadmap--mejoras-futuras)
- [Documentación adicional](#documentación-adicional)
- [Autoría](#autoría)

---

## Descripción general

El sistema permite registrar tareas empresariales y asignarlas a distintas estructuras de datos según su naturaleza (urgentes, programadas, generales o priorizadas), administrar empleados, calcular estadísticas mediante recursividad, distribuir la carga de trabajo entre empleados, realizar búsquedas eficientes, ordenar tareas por urgencia y modelar dependencias entre tareas.

La interfaz está redactada en lenguaje orientado al usuario final (por ejemplo, "Cálculos y Distribución" o "Buscar Tarea por ID" en lugar de nombrar directamente los algoritmos); la terminología técnica de cada estructura se documenta en los comentarios del código y en la documentación del proyecto.

La aplicación utiliza una base de datos SQLite para conservar las tareas activas y los empleados. Al iniciar, carga esos datos desde la base; al cerrar la ventana, guarda los cambios. Las dependencias del grafo y las tareas ya procesadas o eliminadas no se conservan.

## Estructuras de datos y algoritmos implementados

| Estructura / algoritmo | Clase | Uso en el sistema |
|---|---|---|
| Pila (LIFO) | `PilaTareas` | Tareas urgentes |
| Cola (FIFO) | `ColaTareas` | Tareas programadas |
| Lista dinámica | `ListaTareas` | Tareas generales por departamento |
| Cola de Prioridad (Heap) | `ColaPrioridadTareas` | Tareas ordenadas por urgencia y fecha de entrega |
| Árbol Binario de Búsqueda | `ArbolEmpleados` | Registro y consulta de empleados por ID y departamento |
| Tabla Hash (`HashMap`) | `GestorTablasHashYAlgoritmos` | Búsqueda de tareas/empleados en tiempo O(1) |
| QuickSort | `GestorTablasHashYAlgoritmos` | Ordenamiento de tareas por urgencia |
| Búsqueda binaria | `GestorTablasHashYAlgoritmos` | Búsqueda de tareas en O(log n) |
| Recursividad | `ProcesadorRecursivo` | Cálculo del tiempo total estimado |
| Divide y Vencerás | `ProcesadorRecursivo` | Distribución equilibrada de tareas entre empleados |
| Grafo dirigido + orden topológico (Kahn) | `GrafoDependencias` | Dependencias entre tareas y secuencia de ejecución |

## Arquitectura

El proyecto sigue el patrón Modelo–Vista–Controlador (MVC) extendido con una capa de Persistencia / DAO:

Modelo (com.example.Modelo): Las estructuras de datos y la lógica de negocio pura, sin dependencia de la interfaz gráfica.

Persistencia / DAO (com.example.Persistencia): Manejo de conexiones JDBC y operaciones CRUD para almacenar y recuperar información desde la Base de Datos.

Vista (com.example.Vista): La interfaz gráfica (Swing).

Controlador (com.example.Controlador): Conecta los eventos de la Vista con el Modelo y la Base de Datos, manteniendo sincronizadas las tablas y métricas en pantalla.

## Estructura del proyecto

```
com.example
├── GestionTareasApp.java                     (Lanzador principal)
├── Controlador/
│   └── GestionTareasController.java         (Lógica de control y listeners)
├── Modelo/
│   ├── Tarea.java
│   ├── Empleado.java
│   ├── PilaTareas.java
│   ├── ColaTareas.java
│   ├── ListaTareas.java
│   ├── ColaPrioridadTareas.java
│   ├── ArbolEmpleados.java
│   ├── ProcesadorRecursivo.java
│   ├── GestorTablasHashYAlgoritmos.java
│   └── GrafoDependencias.java
├── Vista/
│   ├── GestionTareasView.java               (Interfaz gráfica Swing)
│   └── SelectorFechaPanel.java              (Selector de fecha: calendario + texto validado)
└── persistencia/
    ├── ConexionBD.java
    ├── EmpleadoRepositorio.java
    └── TareaRepositorio.java
```

## Requisitos

- **JDK 17** o superior.
- **Apache Maven** (o un IDE con soporte integrado: IntelliJ IDEA, Eclipse, NetBeans o VS Code con la extensión de Java).

## Cómo ejecutar el proyecto

### Opción A — Desde un IDE (recomendado)

1. Importar el proyecto como **Maven existente**, apuntando a la carpeta que contiene `pom.xml`.
2. Esperar a que el IDE indexe las dependencias.
3. Ejecutar `src/main/java/com/example/GestionTareasApp.java` (contiene el método `main`).

### Opción B — Desde la terminal con Maven

```bash
git clone <url-del-repositorio>
cd <carpeta-del-repositorio>
mvn compile
java -cp target/classes com.example.GestionTareasApp
```

## Guía rápida de uso

1. Toda tarea se crea desde **Registrar Tarea**, eligiendo a qué estructura se asigna (Pila, Cola, Lista o Cola de Prioridad) y, opcionalmente, su fecha de entrega (texto en formato `yyyy-MM-dd` o mediante el selector de calendario). Si se deja vacía, se usa la fecha del día. Los cambios se escriben en la base de datos al cerrar la ventana; cerrar el proceso desde el IDE o terminarlo a la fuerza puede impedir ese guardado.
2. Cada estructura tiene su propia pestaña (**Pilas**, **Colas**, **Listas**, **Cola Prioridad**) con las operaciones correspondientes (procesar, consultar, eliminar, buscar).
3. Todo empleado debe registrarse desde **Empleados** antes de usarse en búsquedas o en la distribución de tareas.
4. **Cálculos y Distribución** calcula el tiempo total estimado y reparte las tareas entre los empleados registrados.
5. **Búsquedas** permite localizar una tarea por ID (cubriendo todas las tareas registradas, o solo las activas en Pila/Cola/Lista) y ordenar las tareas de la Lista General por urgencia.
6. **Grafo Dependencias** permite declarar dependencias entre tareas y calcular su secuencia lógica de ejecución (o detectar dependencias circulares).
7. **Ver Todas / Consola** muestra el consolidado general de tareas y un registro de eventos en tiempo real.

## Limitaciones conocidas

- Los IDs de empleado (`String`) se comparan de forma **lexicográfica** en el árbol binario, no numérica (p. ej. "10" se ordena antes que "9").
- Las dependencias entre tareas no se guardan en la base de datos y deben registrarse de nuevo al iniciar.
- Solo se guardan las tareas que siguen activas en las estructuras; las tareas procesadas o eliminadas no se conservan.
- La base SQLite se encuentra en `data/gestion_tareas.db`, relativa a la carpeta de trabajo desde la que se ejecuta la aplicación.
- El árbol binario de empleados no se auto-balancea.

## Roadmap / mejoras futuras

- Dividir `GestionTareasView` en componentes más pequeños por sección (un archivo por pestaña) para mejorar la mantenibilidad.
- Agregar un resumen de tiempo estimado por departamento en el panel principal.
- Adoptar una estructura auto-balanceada (AVL/Red-Black) para el árbol de empleados.
- Implementar migraciones automáticas de base de datos (Flyway / Liquibase).

## Documentación adicional

La documentación técnica detallada del código (descripción de cada clase, flujo de ejecución, decisiones de diseño y casos de prueba) se encuentra en el documento de entrega del proyecto.

## Autoría

- **Estudiante:** Calixto Isaac Galeana Medrano
- **Materia:** Estructuras de Datos
- **Docente:** Joel Balderrama
