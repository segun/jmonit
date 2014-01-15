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
import za.co.axon.monitor.config.Alerts;
import za.co.axon.monitor.config.MonitorSystem;

/**
 *
 * @author aardvocate
 */
public class CPUMonitor {

    ExecutorService execService;

    public CPUMonitor(ExecutorService execService) {
        this.execService = execService;
    }

    public void createCPUThread(final MonitorSystem system) throws IOException {
        SystemMonitor.LOGGER.log(Level.INFO, "Creating CPU Monitor Thread");
        Process cpuProcess = Runtime.getRuntime().exec("/data/etc/monitor/scripts/cpu");
        BufferedReader cpuProcessReader = new BufferedReader(new InputStreamReader(cpuProcess.getInputStream()));
        final String cpuOutput = cpuProcessReader.readLine();
        execService.execute(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        String condition = system.monitor.cpu.loadAVG.condition;
                        String command = condition.substring(0, 2);
                        double load = getCPULoad(system.monitor.cpu.loadAVG.timeInterval, new StringTokenizer(cpuOutput, ","));
                        double value = Double.parseDouble(condition.substring(3));
                        SystemMonitor.LOGGER.log(Level.INFO, "Load: {0}, Config Value: {1}", new Object[]{load, value});
                        if (command.equals("lt")) {
                            if (load < value) {
                                if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_EMAIL)) {
                                    sendMailAlert(system, value, load, "below");
                                } else if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_SMS)) {
                                    sendSMSAlert(system, value, load, "below");
                                } else if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_ALL)) {
                                    sendSMSAlert(system, value, load, "below");
                                    sendMailAlert(system, value, load, "below");
                                }
                            }
                        }
                        if (command.equals("gt")) {
                            if (load > value) {
                                if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_EMAIL)) {
                                    sendMailAlert(system, value, load, "above");
                                } else if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_SMS)) {
                                    sendSMSAlert(system, value, load, "above");
                                } else if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_ALL)) {
                                    sendSMSAlert(system, value, load, "above");
                                    sendMailAlert(system, value, load, "above");
                                }
                            }
                        }

                        if (command.equals("eq")) {
                            if (load == value) {
                                if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_EMAIL)) {
                                    sendMailAlert(system, value, load, "equal");
                                } else if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_SMS)) {
                                    sendSMSAlert(system, value, load, "equal");
                                } else if (system.monitor.cpu.loadAVG.action.contains(MonitorSystem.ACTION.ALERT_ALL)) {
                                    sendSMSAlert(system, value, load, "equal");
                                    sendMailAlert(system, value, load, "equal");
                                }
                            }
                        }
                    } catch (Exception e) {
                        SystemMonitor.LOGGER.log(Level.SEVERE, null, e);
                    }
                    try {
                        Thread.sleep(system.monitor.cpu.loadAVG.pingInterval);
                    } catch (InterruptedException ex) {
                        SystemMonitor.LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    private void sendMailAlert(MonitorSystem system, double value, double load, String conditionalMessage) {
        SystemMonitor.LOGGER.log(Level.INFO, "Sending Email Alert for CPU Monitor");
        if (!system.alerts.emails.isEmpty()) {
            if (system.mailServer.host != null) {
                String header = "Alert On " + system.systemName + " - " + system.ipAddress;
                String message = "cpu load avg is " + conditionalMessage + " " + value + ". <br />"
                        + "Load Avg is now at " + load + "%";
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

    private void sendSMSAlert(MonitorSystem system, double value, double load, String conditionalMessage) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
        SystemMonitor.LOGGER.log(Level.INFO, "Sending SMS Alert for CPU Monitor");
        if (!system.alerts.phoneNumbers.isEmpty()) {
            if (system.smsc.ipAddress != null) {
                String message = "cpu load avg is " + conditionalMessage + " " + value + "%."
                        + "CPU Load Avg is now at " + load + "%";
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

    private double getCPULoad(int timeInterval, StringTokenizer cpuTokens) {
        double retVal = 0;
        switch (timeInterval) {
            case 1:
                retVal = Double.parseDouble(cpuTokens.nextToken());
                break;
            case 5:
                cpuTokens.nextToken();
                retVal = Double.parseDouble(cpuTokens.nextToken());
                break;
            case 15:
                cpuTokens.nextToken();
                cpuTokens.nextToken();
                retVal = Double.parseDouble(cpuTokens.nextToken());
                break;
            default:
                cpuTokens.nextToken();
                retVal = Double.parseDouble(cpuTokens.nextToken());
                break;
        }

        return retVal;
    }
}
