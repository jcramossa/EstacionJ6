# Nodo 8: Evaluación Crítica, Seguridad y Plan de Mejoras

Este documento presenta una auditoría técnica exhaustiva de la base de código actual de **EstacionJ6**, identificando vulnerabilidades de seguridad, riesgos de estabilidad y una hoja de ruta con mejoras arquitectónicas recomendadas para producción.

---

## 1. Auditoría de Seguridad y Credenciales Expuestas

> [!CAUTION]
> Se han detectado múltiples credenciales críticas, claves de API privadas y tokens de autenticación incrustados directamente (*hardcoded*) en el código fuente de control de versiones.

| Vulnerabilidad | Archivo y Ubicación | Riesgo | Mitigación Recomendada |
| :--- | :--- | :--- | :--- |
| **API Key de Google Gemini** | [`VoiceAssistantManager.kt`](file:///home/jcramossa/AndroidStudioProjects/EstacionJ6/app/src/main/java/com/example/estacionj6/util/VoiceAssistantManager.kt#L35) (`AIzaSyB3...`) | Exfiltración de cuota y acceso no autorizado a modelos de IA facturables. | Trasladar a `local.properties` y generar a través de `BuildConfig` o *Secrets Gradle Plugin*. |
| **Token de Acceso Plex** | [`MainActivity.kt`](file:///home/jcramossa/AndroidStudioProjects/EstacionJ6/app/src/main/java/com/example/estacionj6/MainActivity.kt#L52) (`Sz1L76...`) | Acceso completo de administrador al servidor multimedia y biblioteca personal. | Extraer a configuración protegida o pantalla de ajustes iniciales con `EncryptedSharedPreferences`. |
| **Credenciales Navidrome** | [`MainActivity.kt`](file:///home/jcramossa/AndroidStudioProjects/EstacionJ6/app/src/main/java/com/example/estacionj6/MainActivity.kt#L41) (`admin` / `admin`) | Compromiso de credenciales por defecto del servidor Subsonic. | Parametrizar en pantalla de ajustes y exigir cambio de contraseña por defecto. |
| **IPs Locales Rígidas** | [`NetworkClient.kt`](file:///home/jcramossa/AndroidStudioProjects/EstacionJ6/app/src/main/java/com/example/estacionj6/data/remote/NetworkClient.kt#L13) (`.13`, `.30`) | Si el router reinicia la asignación DHCP, la aplicación pierde comunicación completa. | Implementar resolución mDNS / Zeroconf (ej. `esphome.local`, `navidrome.local`) o permitir configurar IPs en la interfaz. |

---

## 2. Prevención de Quemado de Pantalla AMOLED (*Burn-In*)

> [!WARNING]
> El Samsung Galaxy J6 cuenta con un panel **Super AMOLED**. Los elementos estáticos con tipografía de alto brillo (como el reloj blanco a `90.sp`) visualizados de manera continua las 24 horas del día durante semanas provocarán **quemado irreversible de los píxeles (*burn-in*)**.

### Solución Técnica: Pixel Shifting
Implementar un mecanismo de desplazamiento sutil de píxeles cada cierto intervalo de minutos:
```kotlin
// Desplazamiento imperceptible de ±2 a 5 píxeles cada 5 minutos
val pixelOffsetX = remember { mutableStateOf(0.dp) }
val pixelOffsetY = remember { mutableStateOf(0.dp) }

LaunchedEffect(Unit) {
    while (true) {
        delay(300_000) // Cada 5 minutos
        pixelOffsetX.value = (-3..3).random().dp
        pixelOffsetY.value = (-3..3).random().dp
    }
}
```
Adicionalmente, activar un modo de salvapantallas de baja emisión (*true black*) cuando la habitación permanezca vacía por más de 10 minutos.

---

## 3. Deuda Técnica y Mejoras Arquitectónicas

### 3.1. Inyección de Dependencias (Migración a Hilt)
- **Estado Actual**: `MainActivity` instancia manualmente todos los repositorios y APIs dentro de un `ViewModelProvider.Factory` de 40 líneas.
- **Mejora**: Integrar **Dagger Hilt** (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`). Esto permitirá inyectar clientes HTTP desacoplados, mockear repositorios en tests unitarios y evitar código espagueti de inicialización en la Activity.

### 3.2. Persistencia de Alarmas (Migración de SharedPreferences a Room)
- **Estado Actual**: `HubRepositoryImpl` guarda la lista de alarmas como un string JSON en `SharedPreferences`.
- **Riesgos**: Operaciones de lectura/escritura de todo el archivo en cada actualización, sin transaccionalidad, ni soporte nativo de consultas reactivas con `Flow`.
- **Mejora**: Implementar **Room Database**:
  ```kotlin
  @Entity(tableName = "alarms")
  data class AlarmEntity(...)

  @Dao
  interface AlarmDao {
      @Query("SELECT * FROM alarms")
      fun observeAlarms(): Flow<List<AlarmEntity>>
  }
  ```

### 3.3. Gestión de Permisos en Tiempo de Ejecución
- **Estado Actual**:
  - `RECORD_AUDIO` se solicita ciegamente con `requestPermissions` en `onCreate` sin verificar si ya fue concedido.
  - Los permisos de ubicación (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) se ignoran mediante `@SuppressLint("MissingPermission")` en `WeatherRepository.kt`. Si el usuario instala la app sin concederlos manualmente en los ajustes de Android, la app siempre caerá en las coordenadas por defecto de Madrid.
- **Mejora**: Utilizar el contrato oficial de Compose `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` y solicitar los permisos de forma guiada al primer arranque.

### 3.4. Resiliencia de Red y Polling Adaptativo
- **Estado Actual**: El sondeo de telemetría a `192.168.1.13` se realiza cada 5 segundos de forma fija. Si el ESP32 se apaga o pierde conexión Wi-Fi, se registran excepciones constantes en el logcat y el estado de error parpadea.
- **Mejora**:
  - Implementar retroceso exponencial (*exponential backoff*): si fallan 3 intentos seguidos, espaciar el sondeo a 15s o 30s hasta que el host vuelva a responder.
  - Alternativa ideal: Cambiar el sondeo HTTP por un canal persistente de **Server-Sent Events (SSE)** o **WebSockets**, ambos soportados nativamente por ESPHome (`/events`). Esto reduce el tráfico de red local a cero cuando los sensores no cambian de valor.

---

## 4. Tabla Comparativa: Estado Actual vs Estado Propuesto

| Área | Implementación Actual | Arquitectura Recomendada | Beneficio |
| :--- | :--- | :--- | :--- |
| **Inyección de Dependencias** | Manual (Factory en Activity) | Hilt / Koin | Modularidad, desacoplamiento y testabilidad. |
| **Persistencia Local** | SharedPreferences + JSON Gson | Room Database (SQLite) | Integridad transaccional, tipos estrictos y queries reactivas. |
| **Secretos & Seguridad** | Claves hardcodeadas en código | `local.properties` + EncryptedSharedPreferences | Protección contra filtración de claves y robo de credenciales. |
| **Protocolo IoT** | HTTP Polling (cada 5s) | Server-Sent Events (SSE) o WebSockets | Respuestas inmediatas a presencia y reducción de carga en CPU/red. |
| **Protección Panel** | Atenuación básica al 95% | Pixel Shifting + True Black Saver | Evita el quemado (*burn-in*) del panel Super AMOLED. |
| **Permisos Android** | Invocación básica sin callbacks | ActivityResultContracts con Compose | Experiencia de usuario limpia y sin fallos silenciosos de GPS. |
