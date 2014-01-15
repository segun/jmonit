/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import za.co.axon.monitor.config.MonitorSystem;
import za.co.axon.monitor.monitors.CPUMonitor;
import za.co.axon.monitor.monitors.MemoryMonitor;
import za.co.axon.monitor.monitors.ProcessMonitor;
import za.co.axon.monitor.utils.Mailer;

/**
 *
 * @author aardvocate
 */
public class Initializer {

    public JSONObject config = null;
    public ExecutorService execService;
    public MonitorSystem system = new MonitorSystem();

    public void initConfig() throws JSONException, FileNotFoundException, IOException {
        execService = Executors.newCachedThreadPool();

        String configPath = "/data/etc/monitor/config.json";
        SystemMonitor.LOGGER.log(Level.INFO, "Reading Config File");
        File configFile = new File(configPath);
        String jsonContent = "";
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        while ((jsonContent = reader.readLine()) != null) {
            buffer.append(jsonContent);
        }
        SystemMonitor.LOGGER.log(Level.INFO, "Config File Read Successfully");
        JSONObject object = new JSONObject(buffer.toString());

        SystemMonitor.LOGGER.log(Level.INFO, "Filling Up System");
        config = object.getJSONObject("system");

        system.systemName = config.getString("system_name");
        system.ipAddress = config.getString("ip_address");

        SystemMonitor.LOGGER.log(Level.INFO, "Filling Up Alerts");
        JSONArray emails = config.getJSONObject("alerts").getJSONArray("mail");
        for (int i = 0; i < emails.length(); i++) {
            system.alerts.emails.add(emails.getString(i));
        }

        JSONArray phones = config.getJSONObject("alerts").getJSONArray("sms");
        for (int i = 0; i < phones.length(); i++) {
            system.alerts.phoneNumbers.add(phones.getString(i));
        }

        if (config.has("mail_server")) {
            SystemMonitor.LOGGER.log(Level.INFO, "Filling Up Mail Server");
            JSONObject mailServer = config.getJSONObject("mail_server");
            system.mailServer.host = mailServer.getString("host");
            system.mailServer.password = mailServer.getString("password");
            system.mailServer.port = mailServer.getInt("port");
            system.mailServer.startTLS = mailServer.getBoolean("starttls");
            system.mailServer.useAuth = mailServer.getBoolean("use_auth");
            system.mailServer.user = mailServer.getString("user");
        }

        if (config.has("smsc")) {
            SystemMonitor.LOGGER.log(Level.INFO, "Filling Up SMSC");
            JSONObject smsc = config.getJSONObject("smsc");
            system.smsc.ipAddress = smsc.getString("ip_address");
            system.smsc.port = smsc.getInt("port");
            system.smsc.systemID = smsc.getString("system_id");
            system.smsc.password = smsc.getString("password");
            system.smsc.systemType = smsc.getString("system_type");
            system.smsc.senderID = smsc.getString("sender_id");
        }

        if (config.getJSONObject("monitor").has("cpu")) {
            SystemMonitor.LOGGER.log(Level.INFO, "Filling Up CPU Monitor");
            if (config.getJSONObject("monitor").getJSONObject("cpu").has("load_avg")) {
                JSONObject loadavg = config.getJSONObject("monitor").getJSONObject("cpu").getJSONObject("load_avg");
                JSONArray loadAVGAction = config.getJSONObject("monitor").getJSONObject("cpu").getJSONObject("load_avg").getJSONArray("action");
                for (int i = 0; i < loadAVGAction.length(); i++) {
                    system.monitor.cpu.loadAVG.action.add(MonitorSystem.ACTION.valueOf(loadAVGAction.getString(i).toUpperCase()));
                }
                system.monitor.cpu.loadAVG.condition = loadavg.getString("condition");
                system.monitor.cpu.loadAVG.timeInterval = loadavg.getInt("time_interval");
                system.monitor.cpu.loadAVG.pingInterval = loadavg.getLong("ping_interval");
            }
        }

        if (config.getJSONObject("monitor").has("memory")) {
            SystemMonitor.LOGGER.log(Level.INFO, "Filling Up Memory Monitor");
            JSONObject memory = config.getJSONObject("monitor").getJSONObject("memory");
            if (memory.has("ping_interval")) {
                system.monitor.memory.pingInterval = memory.getLong("ping_interval");
            } else {
                system.monitor.memory.pingInterval = 60000;
            }

            if (memory.has("free")) {
                JSONObject free = memory.getJSONObject("free");
                JSONArray freeAction = free.getJSONArray("action");
                for (int i = 0; i < freeAction.length(); i++) {
                    system.monitor.memory.free.action.add(MonitorSystem.ACTION.valueOf(freeAction.getString(i).toUpperCase()));
                }

                system.monitor.memory.free.condition = free.getString("condition");
            }

            if (memory.has("used")) {
                JSONObject used = memory.getJSONObject("used");
                JSONArray usedAction = used.getJSONArray("action");
                for (int i = 0; i < usedAction.length(); i++) {
                    system.monitor.memory.used.action.add(MonitorSystem.ACTION.valueOf(usedAction.getString(i).toUpperCase()));
                }

                system.monitor.memory.used.condition = used.getString("condition");
            }
        }

        if (config.getJSONObject("monitor").has("send_output")) {
            SystemMonitor.LOGGER.log(Level.INFO, "Filling Up Send Output");
            JSONObject sendOutput = config.getJSONObject("monitor").getJSONObject("send_output");
            JSONArray sendOutputAction = sendOutput.getJSONArray("action");
            for (int i = 0; i < sendOutputAction.length(); i++) {
                system.monitor.sendOutput.action.add(MonitorSystem.ACTION.valueOf(sendOutputAction.getString(i).toUpperCase()));
            }

            system.monitor.sendOutput.command = sendOutput.getString("command");
            system.monitor.sendOutput.pingInterval = sendOutput.getLong("ping_interval");
        }

        if (config.getJSONObject("monitor").has("process")) {
            SystemMonitor.LOGGER.log(Level.INFO, "Filling Up Process Monitor");
            JSONObject process = config.getJSONObject("monitor").getJSONObject("process");
            JSONArray processAction = process.getJSONArray("action");
            for (int i = 0; i < processAction.length(); i++) {
                system.monitor.process.action.add(MonitorSystem.ACTION.valueOf(processAction.getString(i).toUpperCase()));
            }
            if(process.has("name")) {
                system.monitor.process.name = process.getString("name");
            }
            system.monitor.process.pid_file = process.getString("pid_file");
            system.monitor.process.startScript = process.getString("start_script");
            system.monitor.process.stopScript = process.getString("stop_script");
            system.monitor.process.pingInterval = process.getLong("ping_interval");
        }
        
        SystemMonitor.LOGGER.log(Level.INFO, "Filling Up Command Output Monitor");
        JSONObject output = config.getJSONObject("monitor").getJSONObject("output");
        JSONArray outputAction = config.getJSONObject("monitor").getJSONObject("output").getJSONArray("action");
        for (int i = 0; i < outputAction.length(); i++) {
            system.monitor.output.action.add(MonitorSystem.ACTION.valueOf(outputAction.getString(i).toUpperCase()));
        }

        system.monitor.output.command = output.getString("command");
        system.monitor.output.expected = output.getString("expected");
        system.monitor.output.pingInterval = output.getLong("ping_interval");

        SystemMonitor.LOGGER.log(Level.INFO, "Filling Up Port Monitor");
        JSONObject port = config.getJSONObject("monitor").getJSONObject("port");
        JSONArray portAction = config.getJSONObject("monitor").getJSONObject("port").getJSONArray("action");
        for (int i = 0; i < portAction.length(); i++) {
            system.monitor.port.action.add(MonitorSystem.ACTION.valueOf(portAction.getString(i).toUpperCase()));
        }

        system.monitor.port.number = port.getInt("number");
        system.monitor.port.pingInterval = port.getLong("ping_interval");

        SystemMonitor.LOGGER.log(Level.INFO, "Filling Up URL Monitor");
        JSONObject web = config.getJSONObject("monitor").getJSONObject("web");
        JSONArray webAction = config.getJSONObject("monitor").getJSONObject("web").getJSONArray("action");
        for (int i = 0; i < webAction.length(); i++) {
            system.monitor.web.action.add(MonitorSystem.ACTION.valueOf(webAction.getString(i).toUpperCase()));
        }

        system.monitor.web.url = web.getString("url");
        system.monitor.web.pingInterval = web.getLong("ping_interval");
    }

    public void initMailServer() throws JSONException {
        SystemMonitor.LOGGER.log(Level.INFO, "Initializing Mailer");
        system.mailer = new Mailer(system);
    }

    public void initSMSC() throws JSONException, IOException {
        SystemMonitor.LOGGER.log(Level.INFO, "Initializing SMS Sender");
        system.session = new SMPPSession();
        String host = system.smsc.ipAddress;
        int port = system.smsc.port;
        String systemID = system.smsc.systemID;
        String password = system.smsc.password;
        String systemType = system.smsc.systemType;
        int timeout = 60000;
        system.session.connectAndBind(host, port, new BindParameter(BindType.BIND_TX, systemID, password, systemType, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null), timeout);
        SystemMonitor.LOGGER.log(Level.INFO, "Connected and Bound to SMSC");
        //String messageId = session.submitShortMessage("CMT", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, "1616", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, "628176504657", new ESMClass(), (byte) 0, (byte) 1, timeFormatter.format(new Date()), null, registeredDelivery, (byte) 0, DataCodings.ZERO, (byte) 0, "jSMPP simplify SMPP on Java platform".getBytes());
    }

    public void initMonitors() throws JSONException, IOException {
        SystemMonitor.LOGGER.log(Level.INFO, "Initializing Memory Monitor");
        if (system.monitor.memory.free.condition != null || system.monitor.memory.used.condition != null) {
            MemoryMonitor memoryMonitor = new MemoryMonitor(execService);
            memoryMonitor.createMemoryThread(system);
        }

        if (system.monitor.cpu.loadAVG.condition != null) {
            CPUMonitor cpuMonitor = new CPUMonitor(execService);
            cpuMonitor.createCPUThread(system);
        }
        
        if(system.monitor.process.name != null) {
            ProcessMonitor processMonitor = new ProcessMonitor(execService);
            processMonitor.createProcessMonitorThread(system);
        }
    }
}
