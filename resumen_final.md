Se ha implementado el backend y el frontend para las funcionalidades de multi-usuario, seguridad y paginación.

**Backend:**
- **Autenticación:** Sistema de login con roles 'admin' y 'trabajador' usando JWT y bcrypt.
- **Autorización:** Endpoints protegidos y filtrado de datos según el rol.
- **Paginación:** El endpoint de recorridos ahora soporta paginación.
- **Corrección:** Se añadió una ruta temporal `/api/setup-first-admin` para crear el primer administrador.

**Aplicación Móvil:**
- **Login:** Nueva pantalla de inicio que gestiona la sesión del usuario.
- **Roles:** Navegación y vistas diferenciadas para administradores y trabajadores.
- **Paginación:** La vista de historial del administrador ahora carga los recorridos por páginas.
- **Seguridad:** Implementados interceptores para la autenticación y validación de roles en las vistas.
- **UI/UX:**
    - Restaurada y mejorada la `BottomNavigationView` con los iconos, el orden y el estilo solicitados.
    - Solucionado el problema de visibilidad y funcionalidad de los iconos y botones.
    - Ajustados los colores del tema y los layouts para mejorar el contraste y la consistencia de diseño, incluyendo la pantalla de login, el panel lateral del historial y los diálogos.
    - Añadida funcionalidad para el botón de "Información del Vehículo".
    - Corregido el problema de contraste de texto en las listas de vehículos mediante el uso de atributos de tema y overlays, asegurando la legibilidad en diferentes fondos.
    - Eliminado el hint "dejar en blanco para no cambiar" del campo de contraseña en el diálogo de edición de trabajador.

Se han corregido los errores de compilación y se han proporcionado instrucciones detalladas para las pruebas.
