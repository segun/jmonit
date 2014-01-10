/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor.monitors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import za.co.axon.monitor.SystemMonitor;
import za.co.axon.monitor.config.MonitorSystem;

/**
 *
 * @author aardvocate
 */
public class MemoryMonitor {

    ExecutorService execService;

    public MemoryMonitor(ExecutorService execService) {
        this.execService = execService;
    }

    public void createMemoryThread(final MonitorSystem system) throws IOException {
        //"total,used,free"     
        SystemMonitor.LOGGER.log(Level.INFO, "Creating Memory Monitor Thread");
        Process memoryProcess = Runtime.getRuntime().exec("/data/etc/monitor/scripts/free");
        BufferedReader freeProcessReader = new BufferedReader(new InputStreamReader(memoryProcess.getInputStream()));
        final String freeOutput = freeProcessReader.readLine();
        execService.execute(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        if (system.monitor.memory.free.condition != null) {
                            String condition = system.monitor.memory.free.condition;
                            String command = condition.substring(0, 2);
                            if (condition.endsWith("%")) {
                                long percent = getFreeMemoryPercent(new StringTokenizer(freeOutput, ","));
                                String valuString = condition.substring(3).replace("%", "").trim();
                                long conditionValue = Long.parseLong(valuString);
                                if (command.equals("lt")) {
                                    if (conditionValue > percent) {
                                        sendMailAlert(system, conditionValue, percent, "free", "below");
                                        sendSMSAlert(system, conditionValue, percent, "free", "below");
                                    }
                                } else if (command.equals("gt")) {
                                    if (conditionValue < percent) {
                                        sendMailAlert(system, conditionValue, percent, "free", "above");
                                        sendSMSAlert(system, conditionValue, percent, "free", "above");
                                    }
                                } else if (command.equals("eq")) {
                                    if (conditionValue == percent) {
                                        sendMailAlert(system, conditionValue, percent, "free", "equal");
                                        sendSMSAlert(system, conditionValue, percent, "free", "equal");
                                    }
                                }
                            }
                        }

                        if (system.monitor.memory.used.condition != null) {
                            String condition = system.monitor.memory.used.condition;
                            String command = condition.substring(0, 2);
                            if (condition.endsWith("%")) {
                                long percent = getUsedMemoryPercent(new StringTokenizer(freeOutput, ","));
                                String valuString = condition.substring(3).replace("%", "").trim();
                                long conditonValue = Long.parseLong(valuString);
                                if (command.equals("lt")) {
                                    if (conditonValue > percent) {
                                        sendMailAlert(system, conditonValue, percent, "used", "below");
                                        sendSMSAlert(system, conditonValue, percent, "used", "below");
                                    }
                                } else if (command.equals("gt")) {
                                    if (conditonValue < percent) {
                                        sendMailAlert(system, conditonValue, percent, "used", "above");
                                        sendSMSAlert(system, conditonValue, percent, "used", "above");
                                    }
                                } else {
                                    if (conditonValue == percent) {
                                        sendMailAlert(system, conditonValue, percent, "used", "equal");
                                        sendSMSAlert(system, conditonValue, percent, "used", "equal");
                                    }
                                }
                            }
                        }

                        try {
                            Thread.sleep(system.monitor.memory.pingInterval);
                        } catch (InterruptedException ex) {
                            SystemMonitor.LOGGER.log(Level.SEVERE, null, ex);
                        }
                    } catch (Exception e) {
                        SystemMonitor.LOGGER.log(Level.SEVERE, null, e);
                    }
                }
            }
        });
    }

    private void sendSMSAlert(MonitorSystem system, long conditionValue, long percent, String type, String conditionalMessage) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
        SystemMonitor.LOGGER.log(Level.INFO, "Sending SMS Alert");
        if (!system.alerts.phoneNumbers.isEmpty()) {
            if (system.smsc.ipAddress != null) {
                String message = type + " memory is " + conditionalMessage + " " + conditionValue + "%. <br />"
                        + "Memory is now at " + percent + "%";
                TimeFormatter timeFormatter = new AbsoluteTimeFormatter();;

                for (String pn : system.alerts.phoneNumbers) {
                    
                    String messageId = system.session.submitShortMessage("CMT",
                            TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, "Axon-Alert",
                            TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, pn,
                            new ESMClass(), (byte) 0, (byte) 1,
                            timeFormatter.format(new Date()), null,
                            new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                            (byte) 0, new GeneralDataCoding(false, true, MessageClass.CLASS1, Alphabet.ALPHA_DEFAULT), (byte) 0,
                            message.getBytes());
                    SystemMonitor.LOGGER.log(Level.INFO, "SMS Sent with ID {0}", messageId);
                }
            }
        }
    }

    private void sendMailAlert(MonitorSystem system, long conditionValue, long percent, String type, String conditionalMessage) {
        SystemMonitor.LOGGER.log(Level.INFO, "Sending Email Alert");
        if (!system.alerts.emails.isEmpty()) {
            if (system.mailServer.host != null) {
                String header = "Alert On " + system.systemName + " - " + system.ipAddress;
                String message = type + " memory is " + conditionalMessage + " " + conditionValue + "%. <br />"
                        + "Memory is now at " + percent + "%";
                String htmlMessage = system.mailer.contructHTMLMessage(message, header);
                String rcpts = "";
                for (String email : system.alerts.emails) {
                    rcpts += email + ",";
                }

                rcpts = rcpts.substring(0, rcpts.lastIndexOf(","));

                SystemMonitor.LOGGER.log(Level.INFO, "Recipients: {0}", new Object[]{rcpts});
                system.mailer.sendMail("Axon Alerts", system.mailServer.user, rcpts, htmlMessage, "Alert On " + system.systemName);
            }
        } else {
            SystemMonitor.LOGGER.log(Level.INFO, "Email Alert not sent. No recipients found");
        }
    }

    private long getFreeMemoryPercent(StringTokenizer freeTokens) {
        long total = Long.parseLong(freeTokens.nextToken());
        long used = Long.parseLong(freeTokens.nextToken());
        long free = Long.parseLong(freeTokens.nextToken());

        long perc = Math.round(((double) free / (double) total) * 100);
        SystemMonitor.LOGGER.log(Level.INFO, "Free Memory Percentage: {0}", perc);
        return perc;
    }

    private long getUsedMemoryPercent(StringTokenizer freeTokens) {
        long total = Long.parseLong(freeTokens.nextToken());
        long used = Long.parseLong(freeTokens.nextToken());
        long free = Long.parseLong(freeTokens.nextToken());

        long perc = Math.round(((double) used / (double) total) * 100);
        SystemMonitor.LOGGER.log(Level.INFO, "Used Memory Percentage: {0}", perc);
        return perc;
    }
}
