/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.axon.monitor.config;

/**
 *
 * @author aardvocate
 */
public class Monitor {
    public Memory memory = new Memory();
    public CPU cpu = new CPU();
    public Process process = new Process();
    public Output output = new Output();
    public Port port = new Port();
    public Web web = new Web();
    public SendOutput sendOutput = new SendOutput();
}
