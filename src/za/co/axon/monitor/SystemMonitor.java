/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONException;

/**
 *
 * @author aardvocate
 */
public class SystemMonitor {

    public static final Logger LOGGER = Logger.getLogger("SystemMonitorLogger");
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, JSONException {        
        BasicConfigurator.configure();
        LOGGER.log(Level.INFO, "Starting Monitor");
        Initializer initializer = new Initializer();
        LOGGER.log(Level.INFO, "Initializing Config");
        initializer.initConfig();
        LOGGER.log(Level.INFO, "Initializing MailServer");
        //initializer.initMailServer();
        LOGGER.log(Level.INFO, "Initializing SMSC");
        initializer.initSMSC();
        LOGGER.log(Level.INFO, "Initializing Monitors");
        initializer.initMonitors();
    }
}
