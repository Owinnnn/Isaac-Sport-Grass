package pe.isaacsportgrass;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import com.google.gson.Gson;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;

/** Reservas con anticipo Yape, revisión humana y evidencias fuera del directorio público. */
@WebServlet("/payments/*")
@MultipartConfig(maxFileSize=4194304,maxRequestSize=5242880,fileSizeThreshold=65536)
public class PaymentServlet extends HttpServlet {
 private static final Logger LOG=Logger.getLogger(PaymentServlet.class.getName());
private static final Gson G = new GsonBuilder()
    .registerTypeAdapter(
        LocalDateTime.class,
        (com.google.gson.JsonSerializer<LocalDateTime>)
            (src, typeOfSrc, context) ->
                new JsonPrimitive(src.toString())
    )
    .registerTypeAdapter(
        LocalDate.class,
        (com.google.gson.JsonSerializer<LocalDate>)
            (src, typeOfSrc, context) ->
                new JsonPrimitive(src.toString())
    )
    .registerTypeAdapter(
        LocalTime.class,
        (com.google.gson.JsonSerializer<LocalTime>)
            (src, typeOfSrc, context) ->
                new JsonPrimitive(src.toString())
    )
    .create();
 
 private static final ZoneId LIMA=ZoneId.of("America/Lima");
 private static final int HOLD_MINUTES=20;
 private static final class Problem extends RuntimeException {final int status;Problem(int s,String m){super(m);status=s;}}
 private void fail(int code,String msg){throw new Problem(code,msg);}
 private String v(HttpServletRequest r,String key){return Optional.ofNullable(r.getParameter(key)).orElse("").trim();}
 private long id(HttpServletRequest r,String key){try{return Long.parseLong(v(r,key));}catch(Exception ex){fail(400,"ID inválido");return -1;}}
 private void auth(HttpServletRequest r,String role){if(!Security.allowed(r,role))fail(403,"Acceso restringido");}
 private void output(HttpServletResponse w,int status,Object obj)throws IOException {w.setStatus(status);w.setHeader("Cache-Control","no-store");w.setContentType("application/json;charset=UTF-8");w.getWriter().write(G.toJson(obj));}
 private void msg(HttpServletResponse w,String m)throws IOException{output(w,200,Map.of("message",m));}
 private Path folder() throws IOException {String raw=Database.env("ISG_UPLOAD_DIR","");if(raw.isBlank())raw=Path.of(System.getProperty("user.home"),"IsaacSportGrassPrivado","pagos").toString();Path p=Path.of(raw).toAbsolutePath().normalize();Files.createDirectories(p);return p;}
 private String yapeNumber(Connection c)throws SQLException {
  List<Map<String,Object>> rows=read(c,"SELECT config_value FROM payment_settings WHERE config_key='yape_number'");
  String value=rows.isEmpty()?"":String.valueOf(rows.get(0).get("config_value"));
  return value.isBlank()?Database.env("ISG_YAPE_NUMBER",""):value;
 }
 private void bind(PreparedStatement p,Object... args)throws SQLException {for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);}
 private int update(Connection c,String sql,Object... a)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql)){bind(p,a);return p.executeUpdate();}}
 private List<Map<String,Object>> read(Connection c,String sql,Object... args)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql)){bind(p,args);try(ResultSet rs=p.executeQuery()){List<Map<String,Object>> list=new ArrayList<>();ResultSetMetaData md=rs.getMetaData();while(rs.next()){Map<String,Object> x=new LinkedHashMap<>();for(int i=1;i<=md.getColumnCount();i++){Object val=rs.getObject(i);if(val instanceof java.sql.Date||val instanceof Time||val instanceof Timestamp)val=val.toString();x.put(md.getColumnLabel(i),val);}list.add(x);}return list;}}}
 private void expire(Connection c)throws SQLException {update(c,"UPDATE reservations SET status='EXPIRADA' WHERE status='PENDIENTE_PAGO' AND hold_expires<NOW()");}
 private void audit(Connection c,long booking,Long actor,String action,String notes)throws SQLException{update(c,"INSERT INTO booking_audit(reservation_id,actor_user_id,action,notes) VALUES(?,?,?,?)",booking,actor,action,notes);}
 private byte[] image(Part p)throws IOException{if(p==null||p.getSize()<100||p.getSize()>4194304)throw new Problem(400,"Sube una imagen JPG, PNG o WebP de hasta 4 MB");byte[] bytes;try(InputStream in=p.getInputStream()){bytes=in.readAllBytes();}boolean jpg=bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255;boolean png=bytes.length>8&&(bytes[0]&255)==137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71;boolean webp=bytes.length>12&&new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");if(!jpg&&!png&&!webp)throw new Problem(400,"Formato incorrecto: únicamente JPG, PNG o WebP");return bytes;}
 private String ext(byte[] b){return (b[0]&255)==137?".png":(b[0]&255)==255?".jpg":".webp";}
 private void checkPost(HttpServletRequest r){String marker=r.getHeader("X-ISG-Request");if(!"1".equals(marker))fail(403,"Solicitud no autorizada");String origin=r.getHeader("Origin");if(origin!=null&&!origin.equals(r.getScheme()+"://"+r.getServerName()+((r.getServerPort()==80||r.getServerPort()==443)?"":":"+r.getServerPort())))fail(403,"Origen inválido");}
 @Override protected void doGet(HttpServletRequest r,HttpServletResponse w)throws IOException{try{String path=Optional.ofNullable(r.getPathInfo()).orElse("");
  if(path.equals("/config")){auth(r,"USER");try(Connection c=Database.open()){
  output(w,200,Map.of("number",yapeNumber(c),"qrReady",Files.isRegularFile(folder().resolve("yape-owner-qr")),"holdMinutes",HOLD_MINUTES));
 }return;}
  if(path.equals("/admin/config")){auth(r,"ADMIN");try(Connection c=Database.open()){output(w,200,Map.of("number",yapeNumber(c),"qrReady",Files.isRegularFile(folder().resolve("yape-owner-qr"))));}return;}
  if(path.equals("/admin/qr-view")){auth(r,"ADMIN");sendFile(w,folder().resolve("yape-owner-qr"));return;}
  if(path.equals("/qr")){auth(r,"USER");Path f=folder().resolve("yape-owner-qr");if(!Files.isRegularFile(f))fail(404,"El administrador todavía no ha configurado el QR Yape");sendFile(w,f);return;}
  if(path.equals("/detail")){auth(r,"USER");try(Connection c=Database.open()){expire(c);List<Map<String,Object>> rows=read(c,"SELECT r.id,r.booking_date,r.booking_time,r.status,r.amount_total,r.deposit_amount,r.hold_expires,GREATEST(0,TIMESTAMPDIFF(SECOND,NOW(),r.hold_expires)) seconds_remaining,r.payment_reference,r.rejection_reason,c.name court_name,c.sport court_sport,c.district court_district FROM reservations r JOIN courts c ON c.id=r.court_id WHERE r.id=? AND r.user_id=?",id(r,"id"),Security.user(r));if(rows.isEmpty())fail(404,"Separación no encontrada");output(w,200,rows.get(0));}return;}
  if(path.equals("/mine")){auth(r,"USER");try(Connection c=Database.open()){expire(c);output(w,200,read(c,"SELECT r.id,r.booking_date,r.booking_time,r.status,r.amount_total,r.deposit_amount,r.hold_expires,GREATEST(0,TIMESTAMPDIFF(SECOND,NOW(),r.hold_expires)) seconds_remaining,r.payment_reference,r.rejection_reason,c.name court_name FROM reservations r JOIN courts c ON c.id=r.court_id WHERE r.user_id=? ORDER BY r.id DESC LIMIT 200",Security.user(r)));}return;}
  if(path.equals("/admin/list")){auth(r,"ADMIN");try(Connection c=Database.open()){expire(c);output(w,200,read(c,"SELECT r.id,r.status,r.booking_date,r.booking_time,r.amount_total,r.deposit_amount,r.hold_expires,r.payment_reference,r.proof_filename,r.rejection_reason,c.name court_name,u.name user_name,u.email user_email FROM reservations r JOIN users u ON u.id=r.user_id JOIN courts c ON c.id=r.court_id ORDER BY r.id DESC LIMIT 300"));}return;}
  if(path.equals("/proof")){auth(r,"ADMIN");try(Connection c=Database.open()){List<Map<String,Object>> a=read(c,"SELECT proof_filename FROM reservations WHERE id=?",id(r,"id"));if(a.isEmpty()||a.get(0).get("proof_filename")==null)fail(404,"Sin comprobante");sendFile(w,folder().resolve((String)a.get(0).get("proof_filename")));}return;}
  fail(404,"Ruta desconocida");
 }catch(Problem e){output(w,e.status,Map.of("message",e.getMessage()));}catch(Exception e){LOG.log(Level.SEVERE,"Payments GET",e);output(w,500,Map.of("message","Error de servidor o configuración. Revisa los logs."));}}
 private void sendFile(HttpServletResponse w,Path file)throws IOException{if(!Files.isRegularFile(file))fail(404,"Archivo no disponible");byte[] b=Files.readAllBytes(file);w.setContentType(b.length>1&&(b[0]&255)==137?"image/png":b.length>1&&(b[0]&255)==255?"image/jpeg":"image/webp");w.setHeader("Cache-Control","no-store");w.setHeader("X-Content-Type-Options","nosniff");w.setHeader("Content-Disposition","inline; filename=imagen-pago");w.getOutputStream().write(b);}
 @Override protected void doPost(HttpServletRequest r,HttpServletResponse w)throws IOException{try{checkPost(r);String path=Optional.ofNullable(r.getPathInfo()).orElse("");
  if(path.equals("/reserve")){auth(r,"USER");long court=id(r,"court");LocalDate date;LocalTime at;try{date=LocalDate.parse(v(r,"date"));at=LocalTime.parse(v(r,"time"));}catch(Exception ex){fail(400,"Fecha u hora incorrecta");return;}LocalDateTime start=LocalDateTime.of(date,at);if(!v(r,"time").matches("(0[7-9]|1[0-9]|2[01]):00")||!start.isAfter(LocalDateTime.now(LIMA))||date.isAfter(LocalDate.now(LIMA).plusMonths(3)))fail(400,"El horario debe estar en el futuro, máximo tres meses");try(Connection c=Database.open()){
    if(!Files.isRegularFile(folder().resolve("yape-owner-qr"))||yapeNumber(c).isBlank())fail(503,"Yape todavía no está configurado. Pide al administrador que cargue su QR y número en el panel.");
    expire(c);List<Map<String,Object>> row=read(c,"SELECT price FROM courts WHERE id=? AND available=1",court);if(row.isEmpty())fail(404,"Cancha inactiva");BigDecimal price=(BigDecimal)row.get(0).get("price");BigDecimal deposit=price.multiply(new BigDecimal("0.50")).setScale(2,RoundingMode.HALF_UP);try(PreparedStatement p=c.prepareStatement("INSERT INTO reservations(user_id,court_id,booking_date,booking_time,status,amount_total,deposit_amount,hold_expires) VALUES(?,?,?,?,'PENDIENTE_PAGO',?,?,DATE_ADD(NOW(),INTERVAL 20 MINUTE))",Statement.RETURN_GENERATED_KEYS)){bind(p,Security.user(r),court,java.sql.Date.valueOf(date),Time.valueOf(at),price,deposit);p.executeUpdate();try(ResultSet keys=p.getGeneratedKeys()){keys.next();long newId=keys.getLong(1);audit(c,newId,Security.user(r),"CREADA","Espera comprobante Yape");output(w,200,Map.of("id",newId,"total",price,"deposit",deposit,"message","Horario retenido durante 20 minutos. Envía el comprobante de Yape."));}}catch(SQLIntegrityConstraintViolationException ex){fail(409,"Ese horario ya está ocupado o esperando validación");}}return;}
  if(path.equals("/proof")){auth(r,"USER");long booking=id(r,"id");String reference=v(r,"reference");if(reference.length()<4||reference.length()>100)fail(400,"Ingresa el número de operación Yape (4 a 100 caracteres)");byte[] bytes=image(r.getPart("proof"));Path f=folder();String name=UUID.randomUUID()+ext(bytes);Path target=f.resolve(name);try(Connection c=Database.open()){c.setAutoCommit(false);try{expire(c);List<Map<String,Object>> check=read(c,"SELECT id FROM reservations WHERE id=? AND user_id=? AND status='PENDIENTE_PAGO' AND hold_expires>NOW() FOR UPDATE",booking,Security.user(r));if(check.isEmpty())fail(409,"La separación venció, cambió de estado o no te pertenece");Files.write(target,bytes,StandardOpenOption.CREATE_NEW);int changed=update(c,"UPDATE reservations SET status='EN_REVISION',payment_reference=?,proof_filename=?,hold_expires=NULL WHERE id=? AND status='PENDIENTE_PAGO'",reference,name,booking);if(changed!=1)fail(409,"Reserva no disponible");audit(c,booking,Security.user(r),"COMPROBANTE","Referencia enviada para revisión");c.commit();msg(w,"Comprobante recibido. El administrador verificará el depósito antes de confirmar tu cancha.");}catch(Exception ex){c.rollback();Files.deleteIfExists(target);throw ex;}finally{c.setAutoCommit(true);}}return;}
  if(path.equals("/admin/number")){auth(r,"ADMIN");String number=v(r,"number").replace(" ","");
   if(!number.matches("[0-9]{9}"))fail(400,"Ingresa los 9 dígitos del teléfono Yape del propietario");
   try(Connection c=Database.open()){update(c,"INSERT INTO payment_settings(config_key,config_value) VALUES('yape_number',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)",number);}msg(w,"Número Yape guardado correctamente");return;}
  if(path.equals("/admin/qr")){auth(r,"ADMIN");byte[] qr=image(r.getPart("qr"));Path f=folder();Path tmp=f.resolve("yape-owner-qr.tmp");Files.write(tmp,qr);Files.move(tmp,f.resolve("yape-owner-qr"),StandardCopyOption.REPLACE_EXISTING);msg(w,"QR Yape actualizado");return;}
  if(path.equals("/admin/review")) {
    auth(r,"ADMIN");
    long booking=id(r,"id");
    String action=v(r,"action");
    String reason=v(r,"reason");
    if(!action.equals("approve")&&!action.equals("reject")) fail(400,"Acción inválida");
    if(action.equals("reject")&&(reason.length()<5||reason.length()>250))
     fail(400,"Especifica un motivo de rechazo (5 a 250 caracteres)");

    try(Connection c=Database.open()) {
     c.setAutoCommit(false);
     try {
      List<Map<String,Object>> rows=read(c,
       "SELECT r.id,r.user_id,r.booking_date,r.booking_time,r.amount_total,r.deposit_amount,ct.name court_name " +
       "FROM reservations r JOIN courts ct ON ct.id=r.court_id " +
       "WHERE r.id=? AND r.status='EN_REVISION' FOR UPDATE",booking);
      if(rows.isEmpty()) fail(409,"Esta reserva ya fue revisada o no tiene un comprobante pendiente");

      Map<String,Object> info=rows.get(0);
      int changed=update(c,
       "UPDATE reservations SET status=?,reviewed_at=NOW(),reviewed_by=?,rejection_reason=? " +
       "WHERE id=? AND status='EN_REVISION'",
       action.equals("approve")?"CONFIRMADA":"RECHAZADA",
       Security.user(r),action.equals("reject")?reason:null,booking);
      if(changed!=1) fail(409,"La reserva ya no está pendiente de revisión");
      audit(c,booking,Security.user(r),action.equals("approve")?"APROBADA":"RECHAZADA",reason);

      BigDecimal total=new BigDecimal(info.get("amount_total").toString());
      BigDecimal deposit=new BigDecimal(info.get("deposit_amount").toString());
      String code="ISG-"+String.format(Locale.ROOT,"%06d",booking);
      String court=String.valueOf(info.get("court_name"));
      String date=String.valueOf(info.get("booking_date"));
      String time=String.valueOf(info.get("booking_time"));
      if(time.length()>=5) time=time.substring(0,5);
      String notification;
      if(action.equals("approve")) {
       notification="[Aviso automático] ¡Reserva "+code+" CONFIRMADA!\n"+
        "Cancha: "+court+"\nFecha: "+date+"\nHora: "+time+"\n"+
        "Adelanto Yape aprobado: S/ "+deposit.setScale(2,RoundingMode.HALF_UP)+"\n"+
        "Saldo pendiente: S/ "+total.subtract(deposit).setScale(2,RoundingMode.HALF_UP)+"\n"+
        "El administrador verificó tu comprobante. ¡Te esperamos!";
      } else {
       notification="[Aviso automático] Reserva "+code+" RECHAZADA.\n"+
        "Cancha: "+court+"\nFecha: "+date+"\nHora: "+time+"\n"+
        "Motivo: "+reason+"\nEl horario quedó liberado. Puedes escribirnos por este soporte.";
      }
      update(c,"INSERT INTO support_messages(user_id,author,message) VALUES(?,'ADMIN',?)",
       ((Number)info.get("user_id")).longValue(),notification);
      c.commit();
      msg(w,action.equals("approve")?"Reserva confirmada: pago verificado manualmente":"Comprobante rechazado: horario liberado");
     } catch(Exception ex) {
      c.rollback();
      throw ex;
     } finally {
      c.setAutoCommit(true);
     }
    }
    return;
   }
  fail(404,"Acción desconocida");
 }catch(Problem e){output(w,e.status,Map.of("message",e.getMessage()));}catch(Exception e){LOG.log(Level.SEVERE,"Payments POST",e);output(w,500,Map.of("message", "No se pudo procesar la operación. Comprueba las tablas de pagos, MySQL y los registros de Tomcat."));}}
}
