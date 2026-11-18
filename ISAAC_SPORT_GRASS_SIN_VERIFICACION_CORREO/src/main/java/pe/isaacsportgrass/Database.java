package pe.isaacsportgrass;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {

    private Database() {
    }

    public static String env(String key, String fallback) {
        String value = System.getenv(key);

        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }

        return value == null || value.isBlank()
                ? fallback : value;
    }

    // La misma configuración funciona tanto en NetBeans como dentro de Tomcat.
    // Se pueden sobrescribir los valores con variables de entorno ISG_DB_*.
    public static Connection open() throws SQLException {
        String url = env("ISG_DB_URL",
                "jdbc:mysql://127.0.0.1:3306/isaac_sport_grass"
                + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=America/Lima&connectTimeout=10000");
        String usuario = env("ISG_DB_USER", "root");
        String clave = env("ISG_DB_PASSWORD", "");
        try {
            // Carga explícita: proporciona un fallo comprensible si falta el conector.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Falta el driver MySQL Connector/J en WEB-INF/lib", "08001", e);
        }
        return DriverManager.getConnection(url, usuario, clave);
    }

    public static void main(String[] args) {

        try (Connection conexion = open()) {

            System.out.println("MYSQL CONECTADO CORRECTAMENTE");

        } catch (SQLException e) {

            System.out.println("ERROR MYSQL:");
            e.printStackTrace();
        }
    }
}