package Flex.ResidenceSigns2;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.h2.jdbcx.JdbcConnectionPool;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Flex
 */
public class SignDatabaseHandler
{
    private static ResidenceSigns2 plugin;
    private JdbcConnectionPool h2pool;
    private String driver;
    private String dsn;
    private String username;
    private String password;
    
    public List<Location> signs = new ArrayList();
    
    public SignDatabaseHandler(ResidenceSigns2 instance) {
        plugin = instance;
        directory = "plugins" + File.separator + plugin.getDescription().getName();
    }
    
    public String directory;
   
    
    public void initialize() throws Exception
    {
            Class.forName("org.h2.Driver"); 
            this.driver = "org.h2.Driver";
            this.dsn = ("jdbc:h2:" + directory + File.separator + "DataBase;AUTO_RECONNECT=TRUE");
            this.username = "sa";
            this.password = "sa";
            this.h2pool = JdbcConnectionPool.create(this.dsn, this.username, this.password);
            
            setupSignsTable();
            getAllSigns();
    }
    
    public void setupSignsTable() throws Exception
    {
        Connection conn = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = conn.prepareStatement("CREATE TABLE SIGNS(id INT auto_increment, x INT, y INT, z INT, world varchar(100));");
            ps.executeUpdate();
        }
        catch (SQLException E){}
        
        this.close(conn);
    }
    
    public List<Location> getAllSigns ()
    {
        plugin.log.info(plugin.name + "Loading sign locations");
        Connection conn = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try
        {
            ps = conn.prepareStatement("SELECT * FROM SIGNS;");
            rs = ps.executeQuery();
            rs.next();
            signs = new ArrayList<Location>();
            for (int i = 0; rs.getRow() != 0; i++)
            {
                signs.add(new Location(plugin.getWorldByName(rs.getString("world")), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
                rs.next();
            }
            rs.close();
            this.close(conn);
            
            return signs;
        }
        catch (SQLException E){E.printStackTrace();}
        this.close(conn);
        
        return null;
    }
    
    public Connection getConnection()
    {
        try
        {
            return this.h2pool.getConnection();
        }
        catch (SQLException e)
        {
            System.out.println("[ResidenceSigns 2.0] Could not create connection: " + e);
        }return null;
    }
    
    public void close(Connection connection)
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException E){E.printStackTrace();}
        }
    }

    public void addSign(Location location)
    {
        Connection conn = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try
        {
            signs.add(location);
            ps = conn.prepareStatement("SELECT * FROM signs WHERE x=" + location.getBlockX() + " AND y=" + location.getBlockY() + " AND z=" + location.getBlockZ() + " AND world='" + location.getWorld().getName() + "';");
            rs = ps.executeQuery();
            
            rs.next();
            if (rs.getRow() == 0)
            {
                ps = conn.prepareStatement("INSERT INTO signs (X, Y, Z, WORLD) VALUES (" + location.getBlockX() + ", " + location.getBlockY() + "," + location.getBlockZ() + ",'" + location.getWorld().getName() + "');");
                ps.executeUpdate();
            }
            rs.close();
        }
        catch (SQLException E){E.printStackTrace();}
        
        this.close(conn);
    }
    
    public void removeSign(Location loc)
    {
        if (loc != null)
        {
            for (int i = 0; i < signs.size(); i++)
            {
                Location cSign = signs.get(i);
                if (cSign.getBlockX() == loc.getBlockX() && cSign.getBlockY() == loc.getBlockY() && cSign.getBlockZ() == loc.getBlockZ() && cSign.getWorld().getName().equals(loc.getWorld().getName()))
                {
                    signs.remove(i);
                    break;
                }
            }
            
            Connection conn = getConnection();
            PreparedStatement ps = null;
            ResultSet rs = null;

            try
            {
                ps = conn.prepareStatement("DELETE FROM signs WHERE x=" + loc.getBlockX() + " AND y=" + loc.getBlockY() + " AND z=" + loc.getBlockZ() + " AND world='" + loc.getWorld().getName() + "';");
                ps.executeUpdate();
            }
            catch (SQLException E){E.printStackTrace();}
            
            this.close(conn);
        } 
    }
}
