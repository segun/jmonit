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
import org.json.JSONException;
import org.json.JSONObject;
import za.co.axon.monitor.Initializer;
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
                            long value = Long.parseLong(valuString);
                            if (command.equals("lt")) {
                                doMemoryLessThan(value, percent, "free");
                            } else if (command.equals("gt")) {
                                doMemoryGreaterThan(value, percent, "free");
                            } else {
                                doMemoryEqualTo(value, percent, "free");
                            }
                        }
                    }

                    if (system.monitor.memory.used.condition != null) {
                        String condition = system.monitor.memory.used.condition;
                        String command = condition.substring(0, 2);
                        if (condition.endsWith("%")) {
                            long percent = getUsedMemoryPercent(new StringTokenizer(freeOutput, ","));
                            String valuString = condition.substring(3).replace("%", "").trim();
                            long value = Long.parseLong(valuString);
                            if (command.equals("lt")) {
                                doMemoryLessThan(value, percent, "used");
                            } else if (command.equals("gt")) {
                                doMemoryGreaterThan(value, percent, "used");
                            } else {
                                doMemoryEqualTo(value, percent, "used");
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

    private void doMemoryGreaterThan(long value, long percent, String type) {
    }

    private void doMemoryLessThan(long value, long percent, String type) {
    }

    private void doMemoryEqualTo(long value, long percen, String typet) {
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
