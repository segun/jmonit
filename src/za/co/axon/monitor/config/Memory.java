/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor.config;


/**
 *
 * @author aardvocate
 */
public class Memory extends MonitorBase {    
    public Free free = new Free();
    public Used used = new Used();    
}