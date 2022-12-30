  LÉAME.md
litografíaGithubCI Bandeja de basura Únase al chat en https://gitter.im/facebook/litho Licencia


Litho es un marco declarativo para crear interfaces de usuario eficientes en Android.

Declarativo: Litho utiliza una API declarativa para definir los componentes de la interfaz de usuario. Simplemente describa el diseño de su interfaz de usuario en función de un conjunto de entradas inmutables y el marco se encarga del resto.
Diseño asíncrono: Litho puede medir y diseñar su interfaz de usuario con anticipación sin bloquear el hilo de la interfaz de usuario.
Vista plana: Litho usa Yoga para el diseño y reduce automáticamente la cantidad de ViewGroups que contiene su interfaz de usuario.
Reciclaje detallado: cualquier componente, como un texto o una imagen, se puede reciclar y reutilizar en cualquier parte de la interfaz de usuario.
Para empezar, echa un vistazo a estos enlaces:

Aprende a usar Litho en tu proyecto.
Comienza con nuestro tutorial.
Lea más sobre Litho en nuestros documentos.
Instalación
Litho se puede integrar en proyectos Gradle o Buck. Lea nuestra guía de inicio para obtener instrucciones de instalación.

Inicio rápido
1. Inicialice SoLoaderen su Applicationclase.
public class SampleApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, false);
  }
}
2. Crea y muestra un componente en tu Actividad
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext c = new ComponentContext(this);

    final Component component = Text.create(c)
        .text("Hello World")
        .textSizeDip(50)
        .build();

    setContentView(LithoView.create(c, component));
}
Ejecutar muestra
Puede encontrar más ejemplos en nuestra aplicación de muestra .

Para compilar y ejecutar (en un dispositivo/emulador adjunto) la aplicación de muestra, ejecute

$ buck fetch sample
$ buck install -r sample
o, si prefiere Gradle,

$ ./gradlew :sample:installDebug
contribuyendo
Antes de contribuir con Litho, primero lea el Código de conducta que esperamos que cumplan los participantes del proyecto.

Para solicitudes de incorporación de cambios, consulte nuestra guía CONTRIBUCIÓN .

Consulte nuestra página de problemas para obtener ideas sobre cómo contribuir o para informarnos sobre cualquier problema.

Lea también nuestro Estilo de codificación y Código de conducta antes de contribuir.

Obteniendo ayuda
Publique en StackOverflow usando la #lithoetiqueta.
Chatea con nosotros en Gitter .
Únase a nuestro grupo de Facebook para mantenerse al día con los anuncios.
Abra problemas de GitHub solo si sospecha un error en el marco o si tiene una solicitud de función y no para preguntas generales.
Licencia
Litho tiene la licencia Apache 2.0 .

Lanzamientos 51
v0.45.0
El último
last month
