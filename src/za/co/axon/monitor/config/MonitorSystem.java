/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor.config;

import org.jsmpp.session.SMPPSession;
import za.co.axon.monitor.utils.Mailer;

/**
 *
 * @author aardvocate
 */
public class MonitorSystem {
    public String systemName;
    public String ipAddress;
    public Alerts alerts = new Alerts();
    public MailServer mailServer = new MailServer();
    public SMSC smsc = new SMSC();
    public Monitor monitor = new Monitor();
    
    
    public Mailer mailer;
    public SMPPSession session;
    
    public enum ACTION {
        ALERT_EMAIL, ALERT_SMS, RESTART, ALERT_ALL
    };
}
