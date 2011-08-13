/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Flex.ResidenceSigns2;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.economy.TransactionManager;
import com.bekvon.bukkit.residence.economy.rent.RentManager;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

/**
 *
 * @author Flex
 */
public class ResSign2BlockListener extends BlockListener{
    private final ResidenceSigns2 plugin;
    
    public ResSign2BlockListener(final ResidenceSigns2 instance) {
        this.plugin = instance;
    }

    //Block broken
    @Override
    public void onBlockBreak(BlockBreakEvent event)
    {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        BlockState state = block.getState();
        //Check if it's a sign
        if (state instanceof Sign)
        {
            Sign sign = (Sign)state;
            //Check if it's a rent or sell sign
            if (sign.getLine(0).toLowerCase().equals("§3" + plugin.RentSignFirstLine))
            {
                Location loc = block.getLocation();
                ClaimedResidence res = Residence.getResidenceManger().getByLoc(loc);
                RentManager rentman = Residence.getRentManager();
                
                //Check if the player breaking the sign is the player who put the home for rent
                //Also check if there is a player renting the home, to prevent players from getting the
                //"You don't have permission to do this" message
                if (res.getOwner().equals(player.getName()) && rentman.isRented(res.getName()) == false)
                {
                    rentman.unrent(player, res.getName(), false);
                    plugin.db.removeSign(loc);
                }
                else if (plugin.AllowNonOwnerSignBreak == false)
                {
                    //If config file doesn't allow people to break signs when they aren't allowed to change the rent status we won't euther
                    event.setCancelled(true);
                }
                else
                {
                    //If the sign is destroyed anyway remove it from the database to prevent unnecessary memory use
                    //When updating signs
                    plugin.db.removeSign(loc);
                }
            }
            else if (sign.getLine(0).toLowerCase().equals("§3" + plugin.ForSaleSignFirstLine))
            {
                Location loc = block.getLocation();
                ClaimedResidence res = Residence.getResidenceManger().getByLoc(loc);
                
                //Make sure the player destroing is the player selling
                if (res.getOwner().equals(player.getName()))
                {
                    TransactionManager transman = Residence.getTransactionManager();
                    transman.removeFromSale(player, res.getName(), false);
                    plugin.db.removeSign(loc);
                }
                else if (plugin.AllowNonOwnerSignBreak == false)
                {
                    //Cancel the event to prevent sign break
                    event.setCancelled(true);
                }
                else
                {
                    plugin.db.removeSign(loc);
                }
            }
        }
    }
    
    
    @Override
    public void onSignChange (SignChangeEvent event)
    {
        //Someone changed the text of a sign
        //Check if the sign was made into a rent or sell sign
        Player player = event.getPlayer();
        String line0 = event.getLine(0);
        
        if (plugin.RentSignFirstLine.equals(line0.toLowerCase()))
        {
            //It's a rent sign
            event.setLine(0, "§3" + line0);
            Block block = event.getBlock();
            Location loc = block.getLocation();
            ClaimedResidence res = Residence.getResidenceManger().getByLoc(loc);
            RentManager rentman = Residence.getRentManager();
            
            //Make sure the residence exist
            if (res != null)
            {
                //Make sure the player trying to rent a residence is the one that owns it
                if (res.getOwner().equals(player.getName()))
                {
                    String landname = res.getName();
                    
                    //If the residence is for rent already use the data from it
                    if (rentman.isRented(landname) || rentman.isForRent(landname))
                    {
                        plugin.db.addSign(loc);
                        event.setLine(1, rentman.getCostOfRent(landname) + "/" + rentman.getRentDays(landname) + "d");
                        event.setLine(2, landname);

                        String rentingPlayer = rentman.getRentingPlayer(landname);
                        if (rentingPlayer == null)
                        {
                            event.setLine(3, "§2Available");
                        }
                        else
                        {
                            event.setLine(3, "§4" + rentingPlayer);
                        }
                    }
                    else
                    {
                        //Else try and see if it can parse the second line of the sign
                        String[] line1Split = event.getLine(1).split("/");

                        if (line1Split.length == 2)
                        {
                            try
                            {
                                Integer price = Integer.parseInt(line1Split[0]);
                                Integer days = Integer.parseInt(line1Split[1]);

                                plugin.db.addSign(loc);
                                plugin.storedSignLine0 = event.getLine(0);
                                plugin.storedSignLocation = event.getBlock().getLocation();
                                //Cancel the event so the method in the main class can change it witouth having it changed back right after
                                event.setCancelled(true);

                                plugin.signPrice = price;
                                plugin.signRentDays = days;
                                rentman.setForRent(player, landname, price, days, true, false);
                            }
                            catch (Exception error)
                            {
                                //Couldn't parse the data of the second line
                                player.sendMessage("[ResidenceSigns 2.0] " + plugin.RentFormatMessage);
                                event.setLine(0, "§4" + line0);
                            }
                        }
                        else
                        {
                            //Wrong number of slashes / on the second line
                            player.sendMessage("[ResidenceSigns 2.0] " + plugin.RentFormatMessage);
                            event.setLine(0, "§4" + line0);
                        }
                    }
                }
                else
                {
                    //If the player doesn't own the residence give him the message from the config
                    event.setLine(0, "§4" + line0);
                    player.sendMessage("[ResidenceSigns 2.0] " + plugin.NotOwnerMessage);
                }
            }
            else
            {
                //If the residence doesn't exist show the message from the config
                event.setLine(0, "§4" + line0);
                player.sendMessage("[ResidenceSigns 2.0] " + plugin.NullResidenceMessage);
            }
        }
        else if (plugin.ForSaleSignFirstLine.equals(line0.toLowerCase()))
        {
            //It's a forsale sign
            event.setLine(0, "§3" + line0);
            Location loc = event.getBlock().getLocation();
            ClaimedResidence res = Residence.getResidenceManger().getByLoc(loc);
            TransactionManager transman = Residence.getTransactionManager();
            
            //Make sure the residence exists
            if (res != null)
            {
                //Make sure the player owns the residence
                if (res.getOwner().equals(player.getName()))
                {
                    String landname = res.getName();

                    //If it' for sale use the preexisting data
                    if (transman.isForSale(landname))
                    {
                        Integer price = transman.getSaleAmount(landname);
                        event.setLine(1, price.toString());
                        event.setLine(2, res.getName());
                        event.setLine(3, "§2Available");
                        plugin.db.addSign(loc);
                    }
                    else
                    {
                        //If not put it for sale
                        String line1 = event.getLine(1);

                        try
                        {
                            Integer price = Integer.parseInt(line1);
                            plugin.db.addSign(loc);
                            event.setLine(1, price.toString());
                            event.setLine(2, res.getName());
                            event.setLine(3, "§2Available");
                            transman.putForSale(landname, player, price, false);
                        }
                        catch (Exception error)
                        {
                            //Couldn't parse sign line2
                            player.sendMessage("[ResidenceSigns 2.0] " + plugin.SellFormatMessage);
                            event.setLine(0, "§4" + line0);
                        }
                    }
                }
                else
                {
                    //Player doesn't own residence
                    event.setLine(0, "§4" + line0);
                    player.sendMessage("[ResidenceSigns 2.0] " + plugin.NotOwnerMessage);
                }
            }
            else
            {
                //Residence doesn't exist
                event.setLine(0, "§4" + line0);
                player.sendMessage("[ResidenceSigns 2.0] " + plugin.NullResidenceMessage);
            }
        }
    }
}

