# ISAAC SPORT GRASS — EDICIÓN MEJORADA CON YAPE

Esta versión mantiene el proyecto Maven, Java 17, Tomcat 10.1, JSP y MySQL 8.

## Instalación sin borrar datos existentes
1. IMPORTANTE: haz una copia de seguridad de `isaac_sport_grass` en MySQL Workbench antes de modificarla.
2. Para base nueva, ejecuta `database/isg_mysql.sql` y después `database/02_mejoras_pagos.sql`.
3. Para una base que ya existe, ejecuta **solo** `database/02_mejoras_pagos.sql` (una sola vez).
4. Configura servidor Tomcat 10.1 en NetBeans. Conserva las variables de entorno existentes del README original.
5. Configura `ISG_UPLOAD_DIR` con una ruta absoluta **fuera de Tomcat/webapps** (ej.: `C:\ISG_privado\pagos`). El proceso Tomcat debe tener permisos de lectura y escritura.
6. Configura `ISG_YAPE_NUMBER` con el número auténtico del propietario, no con un correo. Yape con QR / número: el usuario transfiere en su propia app; NO se integra con la API oficial de Yape.
7. Reinicia NetBeans/Tomcat para que herede las variables; ingresa como ADMIN, ve al Dashboard y sube el QR Yape del propietario.
8. Desde usuario: elige cancha y hora, se retiene por 20 minutos; paga el 50% al QR/número, sube JPG/PNG/WebP (máximo 4 MB) y código de operación. Admin abre la evidencia, verifica en su Yape real y aprueba/rechaza. La reserva solo se CONFIRMA al aprobar.

## Seguridad y notas
- El QR y los comprobantes se guardan fuera del directorio web; las capturas solo las ve ADMIN.
- Comprobar un comprobante NO equivale a verificar el dinero. El administrador debe verificar la transferencia real en Yape.
- Si no se adjunta comprobante en 20 min, la separación expira al consultar la disponibilidad/listado del módulo de pagos; las solicitudes bajo revisión mantienen el horario bloqueado.
- La subida de imágenes valida firma básica y tamaño, pero para un servicio público hace falta escaneo/normalización, protección CSRF robusta y mayor endurecimiento.
- El pago del 50% es un adelanto, no un cobro procesado por el sitio. El saldo se paga en persona. Cancelaciones posteriores a comprobantes/reembolsos se gestionan manualmente.
- No pongas números de Yape, credenciales, QR ni evidencias reales en el repositorio ni compartas capturas con datos confidenciales.
- La versión original usa el endpoint `/api/reserve`: ahora está desactivado para evitar reservas sin adelanto.
- En la pestaña `Mis reservas` está el historial de separaciones, sus estados y el formulario de comprobante.

## Ideas para siguiente etapa
Calendario gráfico, control de aforo, fotos reales de canchas, comprobantes de cancelación/reembolso, filtros por franja, notificaciones de cambios de estado, recibos descargables y auditoría de inventario.

## Estado de pruebas
El código y el SQL han sido editados, pero **no se ha ejecutado una instancia real de Tomcat y MySQL en este entorno**. Debes validar la migración y probar los casos críticos en tu instalación local antes de usar pagos reales.
