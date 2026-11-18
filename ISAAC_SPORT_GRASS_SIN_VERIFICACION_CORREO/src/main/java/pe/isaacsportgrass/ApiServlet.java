package pe.isaacsportgrass;

import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/api/*")
public class ApiServlet extends HttpServlet {
 private static final Logger LOG=Logger.getLogger(ApiServlet.class.getName());
 private static final Gson G=new Gson();
 private static final String CAPTCHA="captcha-answer";
 private final java.security.SecureRandom random=new java.security.SecureRandom();
 private static class Err extends RuntimeException { final int code; final String msg; Err(int code,String msg){super(msg);this.code=code;this.msg=msg;} int code(){return code;} String msg(){return msg;} }
 private void fail(int status,String msg){throw new Err(status,msg);}
 private String val(HttpServletRequest r,String key){String x=r.getParameter(key);return x==null?"":x.trim();}
 private long lng(HttpServletRequest r,String key){try{return Long.parseLong(val(r,key));}catch(Exception e){fail(400,"Identificador incorrecto");return -1;}}
 private int num(HttpServletRequest r,String key){try{return Integer.parseInt(val(r,key));}catch(Exception e){fail(400,"Número incorrecto");return -1;}}
 private void require(HttpServletRequest r,String role){if(!Security.allowed(r,role))fail(401,"Inicia sesión con una cuenta "+role);}
 private void json(HttpServletResponse resp,int code,Object data)throws IOException{resp.setStatus(code);resp.setContentType("application/json;charset=UTF-8");resp.setHeader("Cache-Control","no-store");resp.getWriter().write(G.toJson(data));}
 private Map<String,Object> message(String m){return Map.of("message",m);}
 private List<Map<String,Object>> rows(ResultSet rs)throws SQLException {List<Map<String,Object>> out=new ArrayList<>();ResultSetMetaData md=rs.getMetaData();while(rs.next()){Map<String,Object> a=new LinkedHashMap<>();for(int i=1;i<=md.getColumnCount();i++){Object v=rs.getObject(i);if(v instanceof java.sql.Date||v instanceof Time||v instanceof Timestamp)v=v.toString();a.put(md.getColumnLabel(i),v);}out.add(a);}return out;}
 private List<Map<String,Object>> query(Connection c,String sql,Object... args)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);try(ResultSet rs=p.executeQuery()){return rows(rs);}}}
 private int update(Connection c,String sql,Object... args)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p.executeUpdate();}}
 private long create(Connection c,String sql,Object... args)throws SQLException {try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();try(ResultSet rs=p.getGeneratedKeys()){if(rs.next())return rs.getLong(1);}throw new SQLException("No se generó ID");}}
 @Override protected void service(HttpServletRequest req,HttpServletResponse resp)throws IOException,jakarta.servlet.ServletException {req.setCharacterEncoding("UTF-8");resp.setHeader("X-Content-Type-Options","nosniff");resp.setHeader("Cache-Control","no-store");if("POST".equals(req.getMethod())){String origin=req.getHeader("Origin");if(origin!=null&&!origin.equals(req.getScheme()+"://"+req.getServerName()+((req.getServerPort()==80||req.getServerPort()==443)?"":":"+req.getServerPort()))){json(resp,403,message("Origen no permitido"));return;}}super.service(req,resp);}
 @Override protected void doGet(HttpServletRequest r,HttpServletResponse w)throws IOException{try{String path=r.getPathInfo();if(path==null)path="";
  // DIAGNÓSTICO TEMPORAL solo para acceso desde la propia computadora.
  // Eliminar el bloque /db-check antes de publicar el sitio.
  if(path.equals("/db-check")){
   String ip=r.getRemoteAddr();
   if(!"127.0.0.1".equals(ip)&&!"0:0:0:0:0:0:0:1".equals(ip)&&!"::1".equals(ip)){
    json(w,403,message("Diagnóstico disponible solo desde localhost"));return;
   }
   try(Connection c=Database.open()){
    json(w,200,Map.of("conexion","OK","catalogo",c.getCatalog(),
      "mensaje","Tomcat puede conectarse a MySQL"));
   }catch(SQLException e){
    LOG.log(Level.SEVERE,"DIAGNOSTICO MYSQL DESDE TOMCAT",e);
    Throwable root=e;
    while(root.getCause()!=null&&root.getCause()!=root)root=root.getCause();
    Map<String,Object> result=new LinkedHashMap<>();
    result.put("conexion","ERROR");
    result.put("SQLState",String.valueOf(e.getSQLState()));
    result.put("codigo",e.getErrorCode());
    result.put("mensaje",String.valueOf(e.getMessage()));
    result.put("causa",root.getClass().getSimpleName()+": "+String.valueOf(root.getMessage()));
    json(w,503,result);
   }
   return;
  }
  if(path.equals("/captcha")){int a=random.nextInt(9)+1,b=random.nextInt(9)+1;r.getSession().setAttribute(CAPTCHA,a+b);json(w,200,Map.of("question",a+" + "+b+" = ?"));return;}
  if(path.equals("/session")){HttpSession s=r.getSession(false);json(w,200,s==null||s.getAttribute("uid")==null?Map.of("loggedIn",false):Map.of("loggedIn",true,"name",s.getAttribute("name"),"role",s.getAttribute("role")));return;}
  if(path.equals("/courts")||path.equals("/products")){require(r,"USER");try(Connection c=Database.open()){json(w,200,query(c,path.equals("/courts")?"SELECT id,name,sport,district,price,available,image FROM courts ORDER BY id DESC":"SELECT id,name,category,price,stock,image FROM products ORDER BY id"));}return;}
  if(path.equals("/slots")){require(r,"USER");long court=lng(r,"court");LocalDate date=LocalDate.parse(val(r,"date"));if(date.isBefore(LocalDate.now(ZoneId.of("America/Lima")))||date.isAfter(LocalDate.now(ZoneId.of("America/Lima")).plusMonths(3)))fail(400,"Fecha fuera del rango permitido");try(Connection c=Database.open()){update(c,"UPDATE reservations SET status='EXPIRADA' WHERE status='PENDIENTE_PAGO' AND hold_expires<NOW()");json(w,200,query(c,"SELECT TIME_FORMAT(booking_time,'%H:%i') hour FROM reservations WHERE court_id=? AND booking_date=? AND status IN ('CONFIRMADA','PENDIENTE_PAGO','EN_REVISION')",court,date));}return;}
  if(path.equals("/bookings")){require(r,"USER");try(Connection c=Database.open()){json(w,200,query(c,"SELECT r.id,c.name court_name,r.booking_date,r.booking_time,r.status,c.price FROM reservations r JOIN courts c ON c.id=r.court_id WHERE r.user_id=? ORDER BY r.booking_date DESC,r.booking_time DESC",Security.user(r)));}return;}
  if(path.equals("/support")){require(r,"USER");try(Connection c=Database.open()){json(w,200,query(c,"SELECT id,author,message,created_at FROM support_messages WHERE user_id=? ORDER BY id DESC LIMIT 100",Security.user(r)));}return;}
  if(path.equals("/admin/data")){require(r,"ADMIN");try(Connection c=Database.open()){json(w,200,Map.of("courts",query(c,"SELECT * FROM courts ORDER BY id DESC"),"products",query(c,"SELECT * FROM products ORDER BY id DESC"),"bookings",query(c,"SELECT r.id,r.user_id,u.name user_name,u.email,c.name court_name,r.booking_date,r.booking_time,r.status FROM reservations r JOIN users u ON u.id=r.user_id JOIN courts c ON c.id=r.court_id ORDER BY r.id DESC LIMIT 300"),"support",query(c,"SELECT m.id,m.user_id,u.name user_name,u.email,m.author,m.message,m.created_at FROM support_messages m JOIN users u ON m.user_id=u.id ORDER BY m.id DESC LIMIT 200")));}return;}
  fail(404,"Recurso inexistente");
 }catch(Err e){json(w,e.code(),message(e.msg()));}catch(Exception e){LOG.log(Level.SEVERE,"GET API",e);json(w,500,message("No se pudo completar la operación. Revisa MySQL y el servidor."));}}
 @Override protected void doPost(HttpServletRequest r,HttpServletResponse w)throws IOException{try{String path=r.getPathInfo();if(path==null)path="";
  if(path.equals("/register")){String name=val(r,"name"),email=val(r,"email").toLowerCase(Locale.ROOT),pass=r.getParameter("password"),answer=val(r,"captcha");HttpSession s=r.getSession();Object expected=s.getAttribute(CAPTCHA);s.removeAttribute(CAPTCHA);if(expected==null||!String.valueOf(expected).equals(answer))fail(400,"CAPTCHA incorrecto. Intenta con una nueva pregunta.");if(name.length()<3||name.length()>100||!email.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")||pass==null||pass.length()<8||pass.length()>72)fail(400,"Revisa nombre, correo y contraseña (8 a 72 caracteres).");long id;try(Connection c=Database.open()){
        try{
            id=create(c,"INSERT INTO users(name,email,password_hash,role,verified) VALUES(?,?,?,'USER',1)",
                name,email,BCrypt.hashpw(pass,BCrypt.gensalt(12)));
        }catch(SQLIntegrityConstraintViolationException e){
            LOG.log(Level.WARNING,"Registro: correo duplicado o restricción SQL",e);
            fail(409,"Ya existe una cuenta con ese correo, o existe una restricción en la tabla users.");
            return;
        }
    }catch(SQLException e){
        LOG.log(Level.SEVERE,"REGISTRO MYSQL - SQLState="+e.getSQLState()+" / código="+e.getErrorCode(),e);
        String aviso="No se pudo guardar la cuenta en MySQL (SQLState: "+e.getSQLState()+", código: "+e.getErrorCode()+").";
        String ip=r.getRemoteAddr();
        if("127.0.0.1".equals(ip)||"::1".equals(ip)||"0:0:0:0:0:0:0:1".equals(ip)){
            Throwable root=e;while(root.getCause()!=null&&root.getCause()!=root)root=root.getCause();
            aviso+=" Causa: "+root.getClass().getSimpleName()+": "+String.valueOf(root.getMessage());
        }
        fail(500,aviso);
        return;
    }
   json(w,200,message("Cuenta creada correctamente. Ya puedes iniciar sesión sin verificar tu correo."));return;}
  if(path.equals("/login")){String email=val(r,"email").toLowerCase(Locale.ROOT),password=r.getParameter("password"),role=val(r,"role").equals("admin")?"ADMIN":"USER";try(Connection c=Database.open()){List<Map<String,Object>> a=query(c,"SELECT id,name,password_hash,role FROM users WHERE email=?",email);if(a.isEmpty()||!BCrypt.checkpw(password==null?"":password,String.valueOf(a.get(0).get("password_hash")))||!role.equals(a.get(0).get("role")))fail(401,"Correo o contraseña incorrectos para este tipo de acceso.");Map<String,Object> u=a.get(0);HttpSession old=r.getSession(false);if(old!=null)old.invalidate();HttpSession s=r.getSession(true);s.setAttribute("uid",((Number)u.get("id")).longValue());s.setAttribute("role",role);s.setAttribute("name",u.get("name"));s.setMaxInactiveInterval(1800);json(w,200,Map.of("message","Acceso correcto","role",role));}return;}
  if(path.equals("/logout")){HttpSession s=r.getSession(false);if(s!=null)s.invalidate();json(w,200,message("Sesión cerrada"));return;}
  if(path.equals("/reserve")){fail(410,"Utiliza el nuevo proceso de separación con Yape y comprobante");return;}
  if(path.equals("/cancel")){require(r,"USER");try(Connection c=Database.open()){if(update(c,"UPDATE reservations SET status='CANCELADA' WHERE id=? AND user_id=? AND status='PENDIENTE_PAGO'",lng(r,"id"),Security.user(r))==0)fail(400,"No se puede cancelar");json(w,200,message("Separación sin pago cancelada. Para devoluciones, contacta con el administrador"));}return;}
  if(path.equals("/support/send")){require(r,"USER");String m=val(r,"message");if(m.isBlank()||m.length()>1000)fail(400,"Escribe un mensaje de hasta 1000 caracteres");try(Connection c=Database.open()){create(c,"INSERT INTO support_messages(user_id,author,message) VALUES(?,'USER',?)",Security.user(r),m);json(w,200,message("Mensaje recibido. Un administrador puede responder desde su panel."));}return;}
  if(path.equals("/admin/reply")){require(r,"ADMIN");String m=val(r,"message");if(m.isBlank()||m.length()>1000)fail(400,"Mensaje inválido");try(Connection c=Database.open()){create(c,"INSERT INTO support_messages(user_id,author,message) VALUES(?,'ADMIN',?)",lng(r,"user"),m);json(w,200,message("Respuesta enviada"));}return;}
  if(path.equals("/admin/court")){require(r,"ADMIN");String name=val(r,"name"),sport=val(r,"sport"),district=val(r,"district");int price=num(r,"price");if(name.length()<3||name.length()>150||sport.length()>50||district.length()>80||price<1||price>2000)fail(400,"Datos de cancha inválidos");try(Connection c=Database.open()){create(c,"INSERT INTO courts(name,sport,district,price,image) VALUES(?,?,?,?,?)",name,sport,district,price, switch(sport){case "Tenis"->"cancha-tenis.svg";case "Básquet"->"cancha-basquet.svg";case "Vóley"->"cancha-voley.svg";default->"cancha-futbol.svg";});json(w,200,message("Cancha creada"));}return;}
  if(path.equals("/admin/court/toggle")){require(r,"ADMIN");try(Connection c=Database.open()){update(c,"UPDATE courts SET available=NOT available WHERE id=?",lng(r,"id"));json(w,200,message("Disponibilidad actualizada"));}return;}
  if(path.equals("/admin/product/stock")){require(r,"ADMIN");try(Connection c=Database.open()){update(c,"UPDATE products SET stock=? WHERE id=?",Math.max(0,num(r,"stock")),lng(r,"id"));json(w,200,message("Inventario actualizado"));}return;}
  if(path.equals("/checkout")){require(r,"USER");String data=val(r,"items");JsonArray items=JsonParser.parseString(data).getAsJsonArray();if(items.size()<1||items.size()>30)fail(400,"Carrito no válido");Connection c=Database.open();try{c.setAutoCommit(false);double total=0;List<long[]> detail=new ArrayList<>();List<Double> prices=new ArrayList<>();for(JsonElement el:items){JsonObject item=el.getAsJsonObject();long pid=item.get("id").getAsLong();int q=item.get("quantity").getAsInt();if(q<1||q>100)fail(400,"Cantidad inválida");List<Map<String,Object>> p=query(c,"SELECT price,stock FROM products WHERE id=? FOR UPDATE",pid);if(p.isEmpty()||((Number)p.get(0).get("stock")).intValue()<q)fail(409,"No hay stock suficiente");double price=((Number)p.get(0).get("price")).doubleValue();total+=price*q;detail.add(new long[]{pid,q});prices.add(price);}long oid=create(c,"INSERT INTO orders(user_id,total) VALUES(?,?)",Security.user(r),total);for(int i=0;i<detail.size();i++){long[] d=detail.get(i);update(c,"UPDATE products SET stock=stock-? WHERE id=?",d[1],d[0]);create(c,"INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES(?,?,?,?)",oid,d[0],d[1],prices.get(i));}c.commit();json(w,200,message("Pedido #"+oid+" registrado. Pendiente de pago presencial: S/ "+String.format(Locale.US,"%.2f",total)));}catch(Exception e){c.rollback();throw e;}finally{c.close();}return;}
  fail(404,"Acción inexistente");
 }catch(Err e){
      json(w,e.code(),message(e.msg()));
  }catch(Exception e){
      LOG.log(Level.SEVERE,"POST API - ruta: "+r.getPathInfo(),e);
      e.printStackTrace();
      json(w,500,message("Error interno al procesar "+r.getPathInfo()+". Consulta la consola de Tomcat (POST API)."));
  }}
}