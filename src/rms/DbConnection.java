/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rms;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 *
 * @author hm
 */
public class DbConnection {

   public static Connection getConnection() {
        Connection con = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            String db = "jdbc:mysql://localhost:3306/rms";
            String username = "root";
            String password = "f12345";

            con = DriverManager.getConnection(db, username, password);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return con;
    }

}
