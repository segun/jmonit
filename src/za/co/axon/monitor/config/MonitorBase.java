/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor.config;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author aardvocate
 */
public class MonitorBase {
    public long pingInterval;
    public List<MonitorSystem.ACTION> action = new ArrayList<MonitorSystem.ACTION>();
}
