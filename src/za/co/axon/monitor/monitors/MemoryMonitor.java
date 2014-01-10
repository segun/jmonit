/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor.monitors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        Process memoryProcess = Runtime.getRuntime().exec("/data/etc/monitor/scripts/free");
        BufferedReader freeProcessReader = new BufferedReader(new InputStreamReader(memoryProcess.getInputStream()));
        final String freeOutput = freeProcessReader.readLine();
        execService.execute(new Runnable() {
            public void run() {
                while (true) {
                    if (system.monitor.memory.free.condition != null) {
                        String condition = system.monitor.memory.free.condition;
                        String command = condition.substring(0, 2);
                        if (condition.endsWith("%")) {
                            long percent = getFreeMemoryPercent(new StringTokenizer(freeOutput, ","));
                            String valuString = condition.substring(3).replace("%", "").trim();
                            long conditionValue = Long.parseLong(valuString);
                            System.out.println("FREE: VALUEC: " + conditionValue + ", PERC: " + percent + ", COMM: " + command);
                            if (command.equals("lt")) {
                                if (conditionValue > percent) {
                                    sendMailAlert(system, conditionValue, percent, "free", "below");
                                }
                            } else if (command.equals("gt")) {
                                if (conditionValue < percent) {
                                    sendMailAlert(system, conditionValue, percent, "free", "above");
                                }
                            } else if (command.equals("eq")) {
                                if (conditionValue == percent) {
                                    sendMailAlert(system, conditionValue, percent, "free", "equal");
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
                                }
                            } else if (command.equals("gt")) {
                                if (conditonValue < percent) {
                                    sendMailAlert(system, conditonValue, percent, "used", "above");
                                }
                            } else {
                                if (conditonValue == percent) {
                                    sendMailAlert(system, conditonValue, percent, "used", "equal");
                                }
                            }
                        }
                    }

                    try {
                        Thread.sleep(system.monitor.memory.pingInterval);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MemoryMonitor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    private void sendMailAlert(MonitorSystem system, long conditionValue, long percent, String type, String conditionalMessage) {
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
                system.mailer.sendMail("Axon Alerts", system.mailServer.user, rcpts, htmlMessage, "Alert On " + system.systemName);
            }
        }

        if (!system.alerts.phoneNumbers.isEmpty()) {
        }
    }

    private long getFreeMemoryPercent(StringTokenizer freeTokens) {
        long total = Long.parseLong(freeTokens.nextToken());
        long used = Long.parseLong(freeTokens.nextToken());
        long free = Long.parseLong(freeTokens.nextToken());

        long perc = Math.round(((double) free / (double) total) * 100);
        System.out.println("FREE PERC: " + perc);
        return perc;
    }

    private long getUsedMemoryPercent(StringTokenizer freeTokens) {
        long total = Long.parseLong(freeTokens.nextToken());
        long used = Long.parseLong(freeTokens.nextToken());
        long free = Long.parseLong(freeTokens.nextToken());

        long perc = Math.round(((double) used / (double) total) * 100);
        System.out.println("USED PERC: " + perc);
        return perc;
    }
}
