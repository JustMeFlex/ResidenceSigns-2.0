/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Flex.ResidenceSigns2;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.economy.TransactionManager;
import com.bekvon.bukkit.residence.economy.rent.RentManager;
import com.bekvon.bukkit.residence.event.ResidenceOwnerChangeEvent;
import com.bekvon.bukkit.residence.event.ResidenceRentEvent;
import com.bekvon.bukkit.residence.event.ResidenceRentEvent.RentEventType;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

/**
 *
 * @author Flex
 */
public class ResidenceSigns2 extends JavaPlugin
{   
    private final ResSign2PlayerListener playerListener = new ResSign2PlayerListener(this);
    private final ResSign2BlockListener blockListener = new ResSign2BlockListener(this);
    private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
    
    //An instance of our database handler used for loading and saving signs
    public SignDatabaseHandler db;
    
    //The logger used to tell the player about stuff and some constants like name and version
    public static final Logger log = Logger.getLogger("Minecraft");
    public static final String name = "[ResidenceSigns 2.0] - ";
    public static final String version = "v1.03";
    
    //All the values from the config
    public Boolean updateSignsOnStartup;
    public String RentSignFirstLine;
    public String ForSaleSignFirstLine;
    public String RentFormatMessage;
    public String NullResidenceMessage;
    public String SellFormatMessage;
    public Boolean AllowNonOwnerSignBreak;
    public String NotOwnerMessage;
    
    //Apparently in bukkit events are called before they actually happend. So if someone rents a building it will send the event and it will then afterwards set the building to rented by that player
    //Then of course they pass the new data in the event, but I'm bad a OOP so I need to use these variables
    public Location storedSignLocation;
    public String storedSignLine0;
    public Integer signPrice;
    public Integer signRentDays;

    @Override
    public void onDisable()
    {
        //Nothing to save, the database saves in real time
        log.info("[ResidenceSigns 2.0] Disabled");
    }

    @Override
    public void onEnable()
    {
        //Simple check found on the residence wiki
        PluginManager pm = getServer().getPluginManager();
        Plugin p = pm.getPlugin("Residence");
        if(p!=null)
        {
             if(!p.isEnabled())
             {
                 //Residence was found, but not enabled (Likely because residence was loaded after this program
                 //Enable it
                 log.log(Level.WARNING, name + "Manually Enabling Residence!");
                 pm.enablePlugin(p);
             }
        }
        else
        {
            //Residence was not found
            //Disable this program to prevent long stacktraces
            log.log(Level.SEVERE, name + "Residence NOT Installed, DISABLED!");
            this.setEnabled(false);
            return;
        }
        
        //Initialize a residence listener
        //I should keep a reference to this but who cares. It has a reference to the main class and that's all it needs
        ResidenceListener resListener = new ResidenceListener(this);
        
        // Register our events
        pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.CUSTOM_EVENT, resListener, Priority.Normal, this);
        
        // Register our commands
        // I have no idea what pos and nothing does so I just commented them out
        //getCommand("pos").setExecutor(new SamplePosCommand(this));
        //getCommand("").setExecutor(new SampleDebugCommand(this));
        
        //Initialize our config file and ready it
        Config config = new Config(this);
        config.configCheck();
        
        //Check if the h2 library is available and if not get it
        //Server restart nescesary after download
        if (!new File("lib" + File.separator, "h2.jar").exists())
        {
            Downloader.install("http://dl.dropbox.com/u/13546396/h2.jar", "h2.jar");
        }
        
        //Initialize the databasehandler
        db = new SignDatabaseHandler(this);
        
        try {
            db.initialize();
        }
        catch (Exception ex)
        {
            log.log(Level.SEVERE, null, ex);
            this.setEnabled(false);
            return;
        }
        
        //If the node in the config file is true
        if (this.updateSignsOnStartup)
        {
            //Update all signs :)
            log.info(name + "Updating all signs");
            updateAllSigns(null);
        }
        
        //Wohoo everything worked. Plugin is enabled
        log.log(Level.INFO, name + "Version " + version + " is enabled!");
    }
    
    //Just a simple function to get a world through it's name
    //(When storing signs i store x, y, z and worldname. So I use this function to get the world from the name)
    //My plugin should actually have multiworld support
    public World getWorldByName(String name)
    {
        List<World> worlds = this.getServer().getWorlds();
        
        for (int i = 0; i < worlds.size(); i++)
        {
            World currentWorld = worlds.get(i);
            if (currentWorld.getName().equals(name))
            {
                return currentWorld;
            }
        }
        
        return null;
    }
    
    //Update all signs (I call this method far too much)
    //TODO: Save signs with their residence so we don't have to go through all signs when updating but only those in that residence
    //TODO: And I should add a command to manually call this if some signs should break for an unknown reason
    public void updateAllSigns(Event e)
    {
        //Get the signs from the database handler
        //They are cached so we won't have to reload the entire database each time
        List<Location> signLocations = db.signs;
        
        for (int i = 0; i < signLocations.size(); i++)
        {
            //For every sign in the database check if it still is a sign
            Location loc = signLocations.get(i);
            Block block = loc.getBlock();
            BlockState state = block.getState();
            if (state instanceof Sign)
            {
                Sign sign = (Sign)state;
                String line0 = sign.getLine(0);
                
                //If this is called in a sign change event line0 will be = ""
                //So when that happends I get the sign location and world and line0 in the event and pass it here
                if (this.storedSignLocation != null && this.storedSignLocation.getBlockX() == loc.getBlockX() && this.storedSignLocation.getBlockY() == loc.getBlockY() && this.storedSignLocation.getBlockZ() == loc.getBlockZ() && this.storedSignLocation.getWorld().getName().equals(loc.getWorld().getName()) && storedSignLine0.equals("") == false)
                {
                    //If the sign is the sign actually is the one from the event set line0 to the one from the event and update it
                    sign.setLine(0, storedSignLine0);
                    sign.update();
                    line0 = storedSignLine0;
                    
                    storedSignLocation = null;
                    storedSignLine0 = "";
                }
                
                //Check if the sign is a rent sign or a buy sign or something else
                if (line0.toLowerCase().equals("§3" + RentSignFirstLine))
                {
                    //If the event is null (Called on startup) or if the event is a rent event update it as a rent sign
                    if (e == null || e instanceof ResidenceRentEvent)
                    {
                        updateRentSign(block, (ResidenceRentEvent)e);
                    }
                }
                else if (line0.toLowerCase().equals("§3" + ForSaleSignFirstLine))
                {
                    //If the event null or a sell sign update it now
                    if (e == null || e instanceof ResidenceOwnerChangeEvent)
                    {
                        updateBuySign(block, (ResidenceOwnerChangeEvent)e);
                    }
                }
                else
                {
                    //If it doesn't follow any of above formats remove it from the database
                    db.removeSign(signLocations.get(i));
                }
            }
            else
            {
                //If it isn't a sign remove it from the sign database
                db.removeSign(signLocations.get(i));
            }
        }
    }
    
    public void updateRentSign(Block block, ResidenceRentEvent e)
    {
        //Updating a rent sign
        BlockState state = block.getState();
        //Even though we made sure it was a sign before I run this check again just to make sure it wasn't caled in another way
        if (state instanceof Sign)
        {
            Sign sign = (Sign)state;

            Location loc = block.getLocation();
            ClaimedResidence res = Residence.getResidenceManger().getByLoc(loc);
            RentManager rentman = Residence.getRentManager();

            //Make sure the residence associated with the sign isn't null
            if (res != null)
            {
                String landname = res.getName();
                //If the event isn't null and the sign is the one affected by the event use the data from the event
                if (e != null && landname.equals(e.getResidence().getName()))
                {
                    RentEventType cause = e.getCause();
                    
                    if (cause.toString().equals(RentEventType.RENT.toString()))
                    {
                        sign.setLine(1, rentman.getCostOfRent(landname) + "/" + rentman.getRentDays(landname) + "d");
                        sign.setLine(2, res.getName());
                        sign.setLine(3, "§4" + e.getPlayer().getName());
                        sign.update(true);
                    }
                    else if (cause.toString().equals(RentEventType.RENTABLE.toString()))
                    {
                        sign.setLine(1, signPrice + "/" + signRentDays + "d");
                        sign.setLine(2, res.getName());
                        sign.setLine(3, "§2Available");
                        sign.update(true);
                    }
                    else if (cause.toString().equals(RentEventType.RENT_EXPIRE.toString()))
                    {
                        sign.setLine(1, rentman.getCostOfRent(landname) + "/" + rentman.getRentDays(landname) + "d");
                        sign.setLine(2, res.getName());
                        sign.setLine(3, "§2Available");
                        sign.update(true);
                    }
                    else if (cause.toString().equals(RentEventType.UNRENT.toString()))
                    {
                        block.setType(Material.AIR);
                    }
                    else if (cause.toString().equals(RentEventType.UNRENTABLE.toString()))
                    {
                        sign.setLine(1, rentman.getCostOfRent(landname) + "/" + rentman.getRentDays(landname) + "d");
                        sign.setLine(2, res.getName());
                        sign.setLine(3, "§2Available");
                        sign.update(true);
                    }
                    
                    signPrice = 0;
                    signRentDays = 0;
                }
                else if (rentman.isRented(landname))
                {
                    //Else then just use the data from the residence
                    sign.setLine(1, rentman.getCostOfRent(landname) + "/" + rentman.getRentDays(landname) + "d");
                    sign.setLine(2, res.getName());
                    sign.setLine(3, "§4" + rentman.getRentingPlayer(landname));
                    sign.update(true);
                }
                else if (rentman.isForRent(landname))
                {
                    sign.setLine(1, rentman.getCostOfRent(landname) + "/" + rentman.getRentDays(landname) + "d");
                    sign.setLine(2, res.getName());
                    sign.setLine(3, "§2Available");
                    sign.update(true);
                }
                else
                {
                    //If the residence isn't for rent and there isn't any event make the sign invalid
                    sign.setLine(0, "§4" + sign.getLine(0).substring(2));
                    sign.setLine(1, "");
                    sign.setLine(2, "");
                    sign.setLine(3, "");
                }
            }
            else
            {
                //If there is not residence associated with the sign make it red to indicate that it is invalid
                sign.setLine(0, "§4" + sign.getLine(0).substring(2));
                sign.setLine(1, "");
                sign.setLine(2, "");
                sign.setLine(3, "");
            }
        }
    }
    
    public void updateBuySign(Block block, ResidenceOwnerChangeEvent e)
    {
        //Updating a buy sign
        BlockState state = block.getState();
        if (state instanceof Sign) {
            Sign sign = (Sign)state;

            Location loc = block.getLocation();
            ClaimedResidence res = Residence.getResidenceManger().getByLoc(loc);
            TransactionManager transman = Residence.getTransactionManager();

            //Make sure the residence exists or make the sign invalid
            if (res != null)
            {
                Integer price = transman.getSaleAmount(res.getName());
                //Check if the sign is the one in the event
                if (e != null && res.getName() == e.getResidence().getName())
                {
                    sign.setLine(0, "§4[SOLD]");
                    sign.setLine(1, price.toString());
                    sign.setLine(2, res.getName());
                    sign.setLine(3, "§4" + e.getNewOwner());
                    sign.update(true);
                }
                else
                {
                    //If not just update the price
                    sign.setLine(1, price.toString());
                }
            }
            else
            {
                //No residence found invalid sign
                sign.setLine(0, "§4" + sign.getLine(0).substring(2));
                sign.setLine(1, "");
                sign.setLine(2, "");
                sign.setLine(3, "");
            }
        }
    }
        
    //I have no idea what these guys do so I'll just leave them
    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }
}
