/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Flex.ResidenceSigns2;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.economy.rent.RentManager;
import com.bekvon.bukkit.residence.event.ResidenceOwnerChangeEvent;
import com.bekvon.bukkit.residence.event.ResidenceRentEvent;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;

/**
 *
 * @author Flex
 */
public class ResidenceListener extends CustomEventListener {
    public ResidenceSigns2 plugin;
    
    public ResidenceListener(ResidenceSigns2 instance)
    {
        plugin = instance;
    }
    
    @Override
    public void onCustomEvent(Event event)
    {
        //This just listens for residence events and passes them on to the main class
        if(event instanceof ResidenceOwnerChangeEvent)
        {
             ResidenceOwnerChangeEvent e = (ResidenceOwnerChangeEvent) event;
             plugin.updateAllSigns(e);
        }
        else if(event instanceof ResidenceRentEvent)
        {
             ResidenceRentEvent e = (ResidenceRentEvent) event;
             plugin.updateAllSigns(e);
        }
    }
}
