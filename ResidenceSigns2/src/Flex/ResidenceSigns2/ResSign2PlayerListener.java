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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

/**
 *
 * @author Flex
 */
public class ResSign2PlayerListener extends PlayerListener {
    private final ResidenceSigns2 plugin;

    public ResSign2PlayerListener(ResidenceSigns2 instance) {
        plugin = instance;
    }
    
    //Player interacts with a block
    @Override
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        //Check for right click
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
        {
            Block block = event.getClickedBlock();
            Player player = event.getPlayer();
            BlockState state = block.getState();
            //Make sure they clicked a sign
            if (state instanceof Sign)
            {
                Sign sign = (Sign)state;
                String line0 = sign.getLine(0);
                        
                //Check if it's a rent or a buy sign
                if (line0.toLowerCase().equals("§3" + plugin.RentSignFirstLine))
                {
                    //it's a rent sign
                    Location loc = block.getLocation();
                    ClaimedResidence res = Residence.getResidenceManger().getByLoc(loc);
                    RentManager rentman = Residence.getRentManager();
                    String landname = res.getName();
                    
                    //Check if the player is already renting the residence
                    if (player.getName().equals(rentman.getRentingPlayer(landname)))
                    {
                        //If he is stop having him rent it
                        rentman.removeFromForRent(player, landname, false);
                    }
                    else
                    {
                        //If he isn't make him rent it
                        rentman.rent(player, landname, true, false);
                    }
                }
                else if (line0.toLowerCase().equals("§3" + plugin.ForSaleSignFirstLine))
                {
                    //It's a buy sign, buy the residence
                    Location loc = block.getLocation();
                    ClaimedResidence res = Residence.getResidenceManger().getByLoc(loc);
                    TransactionManager transman = Residence.getTransactionManager();
                    transman.buyPlot(res.getName(), event.getPlayer(), false);
                }
            }
        }
    }

    //Insert Player related code here
}
