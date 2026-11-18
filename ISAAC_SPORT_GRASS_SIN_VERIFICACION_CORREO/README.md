# ISAAC SPORT GRASS — Proyecto reconstruido

Proyecto **Maven WAR** para **NetBeans 25**, Java 17 o 21, **Tomcat 10.1** y **MySQL 8**.

## Qué incluye

Se conservan las pantallas JSP, login/registro, usuarios, canchas, buscador, horarios,
reservas, carrito, tienda deportiva, pedidos, inventario y soporte al cliente.

Flujo nuevo para TODAS las canchas:

1. Usuario selecciona cancha y horario.
2. Botón **Separar · Yape 50%** abre **pago.jsp** (sin crear aún la reserva).
3. Usuario consulta QR auténtico y número del propietario, total y adelanto del 50%.
4. Usuario paga con Yape, escribe su referencia y adjunta voucher JPG/PNG/WebP.
5. Servidor retiene el turno (20 minutos) y registra voucher; al recibirlo cambia
   a `EN_REVISION` y crea registro de auditoría.
6. Admin ve una campana y solicitudes pendientes que se actualizan cada 10 segundos,
   abre imagen del voucher y aprueba o rechaza después de verificar los fondos en Yape.
7. Reserva aprobada = `CONFIRMADA`; rechazada = `RECHAZADA`. El usuario puede ver el estado.

Un voucher no demuestra por sí solo que el dueño recibió el dinero. **No hay integración
bancaria ni notificación push fuera del navegador**. El pago es manual.

## Instalación

Leer **LEEME_PRIMERO.txt** antes de ejecutar SQL.

- Nueva instalación vacía: `database/01_INSTALACION_NUEVA.sql`.
- Instalación con usuarios/reservas previos: respaldar y ejecutar
  `database/02_ACTUALIZAR_BASE_EXISTENTE.sql` en MySQL Workbench.
- NO ejecutar ambos scripts. El segundo está preparado para columnas ya presentes.
- Conexión MySQL: variables opcionales `ISG_DB_URL`, `ISG_DB_USER`, `ISG_DB_PASSWORD`.
  Valores por defecto de Database.java: localhost:3306, BD `isaac_sport_grass`, root sin contraseña.
- Admin inicial: `ISG_ADMIN_EMAIL` y `ISG_ADMIN_PASSWORD` (12+ caracteres) en el entorno
  de Tomcat; cuentas administradoras existentes no cambian su contraseña.
- Admin -> sección vista general: guardar número Yape real de 9 dígitos y subir QR propio.
- Carpeta de fotos fuera del sitio: `${user.home}/IsaacSportGrassPrivado/pagos` por defecto,
  configurable con `ISG_UPLOAD_DIR`.

**Limitaciones:** la prueba de compilación y el funcionamiento extremo a extremo con
un servidor MySQL/Tomcat real dependen del equipo destino. La sintaxis JavaScript se
verificó localmente. No se debe recibir dinero real antes de validar el flujo en pruebas.
