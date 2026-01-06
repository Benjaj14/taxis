### Parte 1: Probar el Backend por Separado

Puedes usar una herramienta como `curl` desde tu terminal o un cliente de API como Postman para verificar cada endpoint. Primero, asegúrate de que tu servidor esté corriendo en el directorio `backend`:

```bash
npm start
```

**Importante:** Según `backend/src/server.js`, el servidor se ejecuta por defecto en el **puerto 3000**. Así que todas las peticiones irán a `http://localhost:3000`.

**1. Crear un Nuevo Recorrido**

Simula el inicio de un viaje para el vehículo con ID 1.

```bash
curl -X POST -H "Content-Type: application/json" -d '{"idvehiculo": 1}' http://localhost:3000/recorridos
```

*   **Resultado esperado:** Recibirás un JSON con los datos del nuevo recorrido, incluyendo su `idrecorrido`. Anota este ID.
*   **Verificación:** Opcionalmente, revisa tu base de datos en la tabla `recorridos` para confirmar la creación.

**2. Registrar una Posición para ese Recorrido**

Usa el `idrecorrido` que obtuviste en el paso anterior (reemplaza `EL_ID_DEL_RECORRIDO`).

```bash
curl -X POST -H "Content-Type: application/json" -d '{"idrecorrido": EL_ID_DEL_RECORRIDO, "latitud": -33.456, "longitud": -70.654, "fechahora": "2025-12-04T10:00:00.000Z"}' http://localhost:3000/posiciones
```

*   **Resultado esperado:** Recibirás un JSON con los datos de la posición guardada.
*   **Verificación:** Revisa tu base de datos en la tabla `posicionesgps` para confirmar que se ha creado un registro.

**3. Cerrar el Recorrido**

Usa el mismo `idrecorrido` (reemplaza `EL_ID_DEL_RECORRIDO`).

```bash
curl -X PUT -H "Content-Type: application/json" -d '{"km": 15.5}' http://localhost:3000/recorridos/EL_ID_DEL_RECORRIDO
```

*   **Resultado esperado:** Recibirás un JSON del recorrido actualizado, con el estado "finalizado", una hora de fin y la duración calculada.
*   **Verificación:** Revisa tu base de datos en la tabla `recorridos` para confirmar que el registro se ha actualizado correctamente.

---

### Parte 2: Probar la Integración con la App Android

Aquí es donde verás todo funcionando en conjunto utilizando las herramientas de Android Studio.

**Paso Previo: Configurar tiempo de inactividad para pruebas**

Para que no tengas que esperar mucho tiempo durante las pruebas, te sugiero reducir temporalmente el `STOP_DELAY_MS` en tu `LocationService.kt`.

*   Abre el archivo: `mobile/app/src/main/java/com/taxisrodoviario/app/LocationService.kt`
*   Busca esta línea: `private val STOP_DELAY_MS = 10 * 60 * 1000L // 10 minutos`
*   Cámbiala a un valor menor, por ejemplo, 1 minuto: `private val STOP_DELAY_MS = 1 * 60 * 1000L // 1 minuto`
    *   **¡Importante!** Recuerda revertir este cambio a 10 minutos o el valor deseado para producción.

**Guía de Prueba en el Emulador de Android Studio:**

1.  **Inicia todo:**
    *   Asegúrate de que tu servidor backend esté corriendo en el puerto 3000.
    *   Abre el proyecto `mobile` en Android Studio y ejecuta la aplicación en un emulador.

2.  **Simula la primera ubicación para iniciar un recorrido:**
    *   Una vez que el emulador esté ejecutándose, haz clic en el botón de los tres puntos (`...`) en la barra de herramientas lateral del emulador (Extended Controls).
    *   Selecciona la pestaña **Location**.
    *   En el mapa, elige una ubicación (puedes buscar una dirección) y haz clic en **"SET LOCATION"**. Esto enviará la primera coordenada a la app.

3.  **Verifica la creación del recorrido (en Logcat):**
    *   En Android Studio, abre la ventana de **Logcat** (generalmente en la parte inferior).
    *   Filtra por el TAG `LocationService`.
    *   Deberías ver mensajes como:
        *   `LocationService: No hay recorrido activo. Creando uno nuevo...`
        *   `LocationService: Nuevo recorrido creado con ID: X` (donde X es el ID del nuevo recorrido)
        *   `LocationService: Ubicación enviada para el recorrido ID: X`
    *   **Verificación en DB:** Consulta tu base de datos en la tabla `recorridos` para confirmar que se ha creado un nuevo registro con estado "iniciado".

4.  **Simula movimiento y envío continuo de ubicaciones:**
    *   En los controles de **Location** del emulador, mueve el punto en el mapa a una nueva posición (simulando que el vehículo se mueve) y vuelve a hacer clic en **"SET LOCATION"**.
    *   En **Logcat**, deberías ver un nuevo mensaje de `LocationService: Ubicación enviada para el recorrido ID: X`, usando el **mismo ID** del recorrido que se inició antes.
    *   **Verificación en DB:** Confirma que se han añadido nuevas entradas a la tabla `posicionesgps` con el `idrecorrido` correcto.

5.  **Prueba la finalización por detención prolongada:**
    *   En los controles de **Location** del emulador, **deja de enviar ubicaciones**. No hagas clic en "SET LOCATION" por el tiempo configurado en `STOP_DELAY_MS` (por ejemplo, 1 minuto).
    *   Observa **Logcat**. Después del tiempo de inactividad, deberías ver mensajes como:
        *   `LocationService: Detención prolongada detectada. Finalizando recorrido.`
        *   `LocationService: Recorrido ID: X finalizado correctamente.`
    *   **Verificación en DB:** Consulta tu base de datos en la tabla `recorridos`. La fila del recorrido que tenías activo ahora debe tener el estado "finalizado", con una fecha y hora en la columna `fin` y un valor calculado para `duracion`.

6.  **Prueba el reinicio de un nuevo recorrido:**
    *   Una vez que el recorrido anterior se ha finalizado, vuelve a enviar una nueva ubicación desde el emulador (paso 2).
    *   En **Logcat**, deberías ver el ciclo empezar de nuevo: se detectará que no hay un recorrido activo y se creará un **nuevo recorrido** con un **nuevo ID**.

Con estos pasos, podrás verificar exhaustivamente el funcionamiento de la nueva lógica tanto en el backend como en la aplicación móvil. ¡Mucha suerte con las pruebas!