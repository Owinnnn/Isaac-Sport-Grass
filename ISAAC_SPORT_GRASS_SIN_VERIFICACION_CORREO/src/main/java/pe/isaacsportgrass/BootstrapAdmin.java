package pe.isaacsportgrass;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.*;
import java.util.logging.*;
import org.mindrot.jbcrypt.BCrypt;
@WebListener
public class BootstrapAdmin implements ServletContextListener {
 private static final Logger LOG=Logger.getLogger(BootstrapAdmin.class.getName());
 @Override public void contextInitialized(ServletContextEvent e){
  String email=Database.env("ISG_ADMIN_EMAIL","").trim().toLowerCase();
  String pass=Database.env("ISG_ADMIN_PASSWORD","");
  if(email.isBlank()||pass.isBlank()){LOG.warning("ADMIN: configura ISG_ADMIN_EMAIL e ISG_ADMIN_PASSWORD (no hay administrador predeterminado)");return;}
  if(pass.length()<12){LOG.warning("ADMIN: la contraseña debe tener al menos 12 caracteres");return;}
  try(Connection c=Database.open();PreparedStatement check=c.prepareStatement("SELECT id FROM users WHERE email=?")){
   check.setString(1,email);
   try(ResultSet rs=check.executeQuery()){if(rs.next()){LOG.info("Cuenta admin existente; no se modifica su contraseña");return;}}
   try(PreparedStatement p=c.prepareStatement("INSERT INTO users(name,email,password_hash,role,verified) VALUES('Administrador',?,?,'ADMIN',1)")){
    p.setString(1,email);p.setString(2,BCrypt.hashpw(pass,BCrypt.gensalt(12)));p.executeUpdate();LOG.info("Cuenta de administración inicial creada");
   }
  }catch(Exception ex){LOG.log(Level.SEVERE,"No se pudo crear el administrador; ¿importaste database/isg_mysql.sql?",ex);}
 }
}
