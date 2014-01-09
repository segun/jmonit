/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONException;

/**
 *
 * @author aardvocate
 */
public class SystemMonitor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, JSONException {
        BasicConfigurator.configure();
        Initializer initializer = new Initializer();
        initializer.initConfig();
        initializer.initMailServer();
        //initializer.initSMSC();
        initializer.initMonitors();
    }
}
