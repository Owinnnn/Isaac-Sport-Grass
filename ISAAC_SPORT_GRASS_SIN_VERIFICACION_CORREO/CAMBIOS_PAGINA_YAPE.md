# Mejoras: separación del 50% por Yape para TODAS las canchas

1. En usuario.jsp, selecciona cualquiera de las canchas (también las futuras), fecha y hora; pulsa «Separar cancha · pagar 50%».
2. El servidor valida disponibilidad y retiene el turno durante 20 minutos. El navegador redirige a **pago.jsp?id=...** (página independiente).
3. La página obtiene desde MySQL la reserva del usuario autenticado y el precio REAL de la cancha; muestra precio total, mitad para Yape, mitad pendiente y QR auténtico subido por el administrador.
4. Cliente envía imagen JPG/PNG/WebP (hasta 4 MB) y número de operación. El estado pasa a EN_REVISION.
5. En admin.jsp una campana 🔔 muestra el número de comprobantes EN_REVISION y actualiza automáticamente cada 10 segundos; también muestra un aviso cuando aparece una solicitud nueva mientras el administrador tiene la página abierta. En Vista general puede aprobar o rechazar comprobantes. El contador persiste mientras la solicitud siga pendiente.
6. El administrador DEBE comprobar el ingreso efectivo en su propia cuenta Yape antes de aprobar. No hay verificación automática del pago, transferencia ni abono bancario.

## Instalación
- El proyecto Maven conserva la base SQL original y la migración `database/02_mejoras_pagos.sql`. Si ya instalaste esa migración, **NO la repitas**.
- Configura variables `ISG_YAPE_NUMBER` (número real) e `ISG_UPLOAD_DIR` (carpeta privada fuera de webapps). Ingresa como administrador y sube el QR real desde la sección Validación de separaciones Yape.
- En NetBeans abre la carpeta con `pom.xml`, ejecuta Clean and Build y Run en Tomcat 10.1 con JDK 17.
- Los estados de comprobantes se consultan a MySQL, no se generan pagos por API de Yape. Las notificaciones del admin son **en el navegador mientras admin.jsp está abierta**, no push ni correo.
- Si el cliente envía Yape pero no consigue adjuntar comprobante antes de expirar, debe contactar administración. No se considera automáticamente devuelto.
