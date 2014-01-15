/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor.monitors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
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
import za.co.axon.monitor.config.MonitorSystem;

/**
 *
 * @author aardvocate
 */
public class ProcessMonitor {

    ExecutorService execService;

    public ProcessMonitor(ExecutorService execService) {
        this.execService = execService;
    }

    public void createProcessMonitorThread(final MonitorSystem system) throws IOException {
        SystemMonitor.LOGGER.log(Level.INFO, "Creating Process Monitor Thread");
        final String name = system.monitor.process.name;
        final String pidFileName = system.monitor.process.pid_file;
        String pidString = "0";

        File pidFile = new File(pidFileName);
        SystemMonitor.LOGGER.log(Level.INFO, "PIDS IS FILE: {0}", pidFile.isFile());
        if (pidFile.isFile()) {
            pidString = new BufferedReader(new InputStreamReader(new FileInputStream(pidFile))).readLine();
            SystemMonitor.LOGGER.log(Level.INFO, "PIDS: {0}", pidString);
        }

        final String pid = pidString;

        final String stopScript = system.monitor.process.stopScript;
        final String startScript = system.monitor.process.startScript;
        String params[] = {
            "/data/etc/monitor/scripts/process",
            name,
            pidString
        };
        Process monitorProcess = Runtime.getRuntime().exec(params);
        BufferedReader processReader = new BufferedReader(new InputStreamReader(monitorProcess.getInputStream()));
        final String processOutput = processReader.readLine();

        SystemMonitor.LOGGER.log(Level.INFO, "OUTPUT OF PROCESS: {0}", processOutput);
        execService.execute(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        if (!processOutput.equals(pid)) {
                            if (system.monitor.process.action.contains(MonitorSystem.ACTION.ALERT_EMAIL)) {
                                sendMailAlert(system, name);
                            } else if (system.monitor.process.action.contains(MonitorSystem.ACTION.ALERT_SMS)) {
                                sendSMSAlert(system, name);
                            } else if (system.monitor.process.action.contains(MonitorSystem.ACTION.ALERT_ALL)) {
                                sendMailAlert(system, name);
                                sendSMSAlert(system, name);
                            }

                            if (system.monitor.process.action.contains(MonitorSystem.ACTION.RESTART)) {
                                SystemMonitor.LOGGER.log(Level.SEVERE, "Process {0} stopped. Restarting It.", name);                                
                                Process restartProcess = Runtime.getRuntime().exec(startScript.split(" "));
                                BufferedReader restartProcessReader = new BufferedReader(new InputStreamReader(restartProcess.getInputStream()));
                                final String restartProcessOutput = restartProcessReader.readLine();
                                SystemMonitor.LOGGER.log(Level.INFO, "Process {0} restarted with output {1}", new Object[]{name, restartProcessOutput});
                            }
                        }
                    } catch (Exception e) {
                        SystemMonitor.LOGGER.log(Level.SEVERE, null, e);
                    }
                    try {
                        Thread.sleep(system.monitor.process.pingInterval);
                    } catch (InterruptedException ex) {
                        SystemMonitor.LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
            }

            private void sendMailAlert(MonitorSystem system, String name) {
                SystemMonitor.LOGGER.log(Level.INFO, "Sending Email Alert for Process Monitor");
                if (!system.alerts.emails.isEmpty()) {
                    if (system.mailServer.host != null) {
                        String header = "Alert On " + system.systemName + " - " + system.ipAddress;
                        String message = "Process " + name + " is not running. <br />";
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

            private void sendSMSAlert(MonitorSystem system, String name) throws PDUException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException, IOException {
                SystemMonitor.LOGGER.log(Level.INFO, "Sending SMS Alert for CPU Monitor");
                if (!system.alerts.phoneNumbers.isEmpty()) {
                    if (system.smsc.ipAddress != null) {
                        String message = "Process " + name + " is not running.";
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
        });
    }
}
