# Java 21: IntelliJ/Maven y NetBeans/Ant

Ambos entornos editan `src/main/java` y utilizan `src/main/resources`.
No hay copias del código. La compilación Ant no ejecuta ni necesita Maven.

## IntelliJ

Abrir la raíz del repositorio como proyecto Maven y seleccionar JDK 21.
La configuración original de `pom.xml` se conserva.

## NetBeans

Abrir la carpeta `netbeans` mediante Archivo > Abrir proyecto.
Esa carpeta contiene un proyecto Java Application con Ant. No abrir la raíz
como proyecto NetBeans, porque contiene también el descriptor Maven.
Seleccionar JDK 21 como plataforma Java en las propiedades del proyecto.
Usar Limpiar y construir, o Ejecutar proyecto.

## Terminal

Con JDK 21 y Ant disponibles, desde la raíz:

```text
ant jar
ant run
```

El JAR Ant queda en `netbeans/dist/ALD_segundo_proyecto_EDII.jar`.
Maven conserva su salida en `target/`. Para compilar con Maven: `mvn package`.

Actualmente la clase principal es el ejemplo existente `org.example.Main`.
Esta preparación no implementa funciones del proyecto ni pruebas de algoritmos.
Cuando existan cliente y servidor, se acordarán sus entradas de ejecución.

## Colaboración

Versionar `pom.xml`, `build.xml`, esta guía y los archivos compartidos de
`netbeans/nbproject`. Excluir `netbeans/nbproject/private`, `netbeans/build`
y `netbeans/dist`. Compartir siempre el repositorio completo: la carpeta
`netbeans` utiliza rutas relativas a las fuentes de la carpeta superior.

Las dependencias futuras deben configurarse en ambos sistemas. Por ahora no
hay dependencias externas ni bibliotecas de pruebas. Si se agregan recursos,
se copian sin filtrado en ambas compilaciones. No introducir filtrado Maven
sin definir el comportamiento equivalente en Ant.
