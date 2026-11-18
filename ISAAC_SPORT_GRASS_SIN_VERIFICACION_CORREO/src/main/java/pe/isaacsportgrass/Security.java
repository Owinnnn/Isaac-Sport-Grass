package pe.isaacsportgrass;
import jakarta.servlet.http.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
public final class Security {
 private Security(){}
 public static String randomToken(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return HexFormat.of().formatHex(b);}
 public static String sha256(String s){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(b);}catch(Exception e){throw new IllegalStateException(e);}}
 public static boolean allowed(HttpServletRequest r,String role){HttpSession s=r.getSession(false);return s!=null&&role.equals(s.getAttribute("role"));}
 public static long user(HttpServletRequest r){HttpSession s=r.getSession(false);return s!=null&&s.getAttribute("uid") instanceof Long?(Long)s.getAttribute("uid"):-1;}
 public static String name(HttpServletRequest r){HttpSession s=r.getSession(false);return s==null?"":String.valueOf(s.getAttribute("name"));}
}
