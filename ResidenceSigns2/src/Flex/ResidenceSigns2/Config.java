package Flex.ResidenceSigns2;

import java.io.File;
import java.util.List;

import java.util.logging.Level;
import org.bukkit.util.config.Configuration;

public class Config {
    private static ResidenceSigns2 plugin;
    public Config(ResidenceSigns2 instance) {
        //Get ourself a reference to the main class and figure out where to save the config file
        //This class is based on a tutorial found here: 
        //Don't blame I have only been using java for two months or something. I wouldn't know how to write YAML files
        plugin = instance;
        directory = "plugins" + File.separator + plugin.getDescription().getName();
        file = new File(directory + File.separator + "config.yml");
    }

   public String directory;
   File file;


    public void configCheck()
    {
        //Make the directory the file is saved in to prevent errors
        new File(directory).mkdir();
        
        //Check if there already is a config file
        if(!file.exists())
        {
            plugin.log.log(Level.INFO, plugin.name + "Creating config file...");
            try
            {
                //If there isn't make one and write the default values
                file.createNewFile();
                addDefaults();

            } catch (Exception ex) {
                //On error inform the user, so they can inform me :)
                ex.printStackTrace();
            }
        }
        else
        {
            //If it does exist just load the data in it
            loadkeys();
        }
    }
    
    //All of these just uses the YAML parser from the load method to read and write the config
    private void write(String root, Object x){
        Configuration config = load();
        config.setProperty(root, x);
        config.save();
    }
    private Boolean readBoolean(String root){
        Configuration config = load();
        return config.getBoolean(root, true);
    }

    private Double readDouble(String root){
        Configuration config = load();
        return config.getDouble(root, 0);
    }
    private List<String> readStringList(String root){
        Configuration config = load();
        return config.getKeys(root);
    }
    private String readString(String root){
        Configuration config = load();
        return config.getString(root);
    }
    private Configuration load()
    {
        //Get's a YAML parser associated with the file
        try {
            Configuration config = new Configuration(file);
            config.load();
            return config;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private void addDefaults(){
        //Set all the default nodes in the file that my program uses
        plugin.log.info(plugin.name + "Generating Config File...");
        write("ForSaleSignFirstLine", "[forsale]");
        write("RentSignFirstLine", "[rent]");
        write("RentFormatMessage", "Please write the second line of the sign in this format \"<Price>/<Days>\". For example \"100/10\"");
        write("SellFormatMessage", "Please enter the price of the residence in the second line of the sign");
        write("NullResidenceMessage", "Residence not found");
        write("NotOwnerMessage", "You do not own the residence");
        write("AllowNonOwnerSignBreak", false);
        write("updateSignsOnStartup", true);
        
     loadkeys();
    }
    private void loadkeys(){
        //Load all the data from the nodes my program uses and assign it to variables in my main class
        plugin.log.info(plugin.name + "Loading Config File...");
        plugin.ForSaleSignFirstLine = readString("ForSaleSignFirstLine").toLowerCase();
        plugin.RentSignFirstLine = readString("RentSignFirstLine").toLowerCase();
        plugin.RentFormatMessage = readString("RentFormatMessage");
        plugin.SellFormatMessage = readString("SellFormatMessage");
        plugin.NullResidenceMessage = readString("NullResidenceMessage");
        plugin.updateSignsOnStartup = readBoolean("updateSignsOnStartup");
        plugin.AllowNonOwnerSignBreak = readBoolean("AllowNonOwnerSignBreak");
        plugin.NotOwnerMessage = readString("NotOwnerMessage");
    }
}