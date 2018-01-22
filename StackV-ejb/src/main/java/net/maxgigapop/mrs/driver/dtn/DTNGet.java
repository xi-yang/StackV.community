/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBException;
//import javax.xml.bind.Marshaller;
//import javax.xml.bind.Unmarshaller;
//import org.globusonline.transfer;
//import java.io.InputStream;
//import com.jcraft.jsch.Channel;
//import com.jcraft.jsch.ChannelExec;
//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.Session;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import org.globus.myproxy.MyProxy;

/**
 *
 * @author xin
 */
public class DTNGet {

    private String timestamp;
    private DTNNode dtn;
    private List<FileSystem> fileSystems;
    private String transfer_service_type;
    private Map<String, String> transfer_service_conf = new HashMap<String, String>();
    private long active_transfers;
    private double cpu_usage;
    private String error;
    private String output;
    private Logger logger = Logger.getLogger(DTNGet.class.getName());
	private final static long cutoff = 20;
    
    public DTNGet(String pullInvokePattern, Map<String, String> pullCommandParamMap) {
        this.dtn = null;
        this.timestamp = null;
        this.transfer_service_type = null;
        this.fileSystems = new ArrayList<>();

        //todo:active proxy credential
        Node tmpNode;
        Element tmpEle;
        try {
			String cmdStr = (new PullCmdAssembler(pullInvokePattern, logger)).generateCmdStr(pullCommandParamMap);
			String cmd[] = cmdStr.split(" ");

			// Log command issued to shell
			logger.log(Level.INFO, String.format("Issuing metadata pull command to shell: %s", cmdStr));
			
			int exit = runcommand(cmd);
//			logger.log(Level.INFO, output);
//			logger.log(Level.INFO, error);
			
			if (exit != 0)
				throw new IllegalStateException("Error transferring file: Error=" + this.error + ", Output=" + this.output);
			else {
				// Check every 1 second for the file, abort if the cutoff time is exceeded
				File metadataFile = new File(pullCommandParamMap.get("dtn-metadata-paths"));
				long startTime = System.currentTimeMillis() / 1000L;
				while (!metadataFile.exists()) {
					Thread.sleep(1000);
					long currentTime = System.currentTimeMillis() / 1000L;
					if (currentTime - startTime > cutoff)
						throw new FileNotFoundException(String.format("DTN metadata not found at %s", metadataFile));
				}
				Document doc = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder()
						.parse(metadataFile);
                doc.getDocumentElement().normalize();
                
                //Timestamp
                if (doc.getElementsByTagName("Timestamp_UTC").getLength() != 0) {
                    this.timestamp = doc.getElementsByTagName("Timestamp_UTC").item(0).getTextContent();
                }
                
                //DTN node information
                if (doc.getElementsByTagName("dtnNode").getLength() != 0) {
                    tmpNode = doc.getElementsByTagName("dtnNode").item(0);
                    if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element dtnNode = (Element) tmpNode;
                        String ip = dtnNode.getElementsByTagName("IP").item(0).getTextContent();
                        String hostname = dtnNode.getElementsByTagName("Hostname").item(0).getTextContent();
                        this.dtn = new DTNNode(ip, hostname);

                        int cpu = Integer.parseInt(dtnNode.getElementsByTagName("CPU").item(0).getTextContent());
                        this.dtn.setCPU(cpu);
                        double memory = Double.parseDouble(dtnNode.getElementsByTagName("Memory_kB").item(0).getTextContent()) / 1024.0;
                        this.dtn.setMemory(memory);

                        //Get NICs
                        if (dtnNode.getElementsByTagName("NICs").getLength() != 0) {
                            tmpNode = dtnNode.getElementsByTagName("NICs").item(0);
                            if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
                                tmpEle = (Element) tmpNode;
                                if (tmpEle.getElementsByTagName("NIC").getLength() != 0) {
                                    NodeList nList = tmpEle.getElementsByTagName("NIC");
                                    List<NIC> nics = new ArrayList<>();
                                    for (int i = 0; i < nList.getLength(); i++) {
                                        if (nList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                            Element nic = (Element) nList.item(i);
                                            String nic_id = nic.getElementsByTagName("NIC_ID").item(0).getTextContent();
                                            String link_type = nic.getElementsByTagName("Link_type").item(0).getTextContent();
                                            String ip_addr = nic.getElementsByTagName("IP_address").item(0).getTextContent();
                                            NIC aNic = new NIC(nic_id, link_type, ip_addr);
                                            String tmpCap = nic.getElementsByTagName("Link_capacity_Mbps").item(0).getTextContent();
                                            if (tmpCap.length() != 0) {
                                                long link_cap = Long.parseLong(tmpCap);
                                                if (link_cap > 0) {
                                                    aNic.setLinkCapacity(link_cap);
                                                }
                                            }
                                            nics.add(aNic);
                                        }
                                    }
                                    this.dtn.setNICs(nics);
                                }
                            }
                        }

                        String rbuff = dtnNode.getElementsByTagName("TCP_read_buffer").item(0).getTextContent();
                        this.dtn.setTCPReadBuffer(rbuff);

                        String wbuff = dtnNode.getElementsByTagName("TCP_write_buffer").item(0).getTextContent();
                        this.dtn.setTCPWriteBuffer(wbuff);

                        //GridFTP configuration
                        if (dtnNode.getElementsByTagName("DataTransferService").getLength() != 0) {
                            tmpNode = dtnNode.getElementsByTagName("DataTransferService").item(0);
                            if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element transfer_conf = (Element) tmpNode;
                                this.transfer_service_type = transfer_conf.getElementsByTagName("Service_type").item(0).getTextContent();
                                NodeList childNodes = transfer_conf.getChildNodes();
                                for (int i = 0; i < childNodes.getLength(); i++) {
                                    Node childNode = childNodes.item(i);
                                    this.transfer_service_conf.put(childNode.getNodeName(), childNode.getTextContent());
                                }
                            }
                        }
                    }
                }

                //Storage information
                if (doc.getElementsByTagName("Storage").getLength() != 0) {
                    tmpNode = doc.getElementsByTagName("Storage").item(0);
                    if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element storage = (Element) tmpNode;
                        //Get file systems
                        if (storage.getElementsByTagName("File_System").getLength() != 0) {
                            NodeList nList = storage.getElementsByTagName("File_System");
                            for (int i = 0; i < nList.getLength(); i++) {
                                if (nList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                                    Element fs = (Element) nList.item(i);
                                    String device = fs.getElementsByTagName("Device").item(0).getTextContent();
                                    String fs_type = fs.getElementsByTagName("Type").item(0).getTextContent();
                                    FileSystem aFS = new FileSystem(device, fs_type);

                                    String mountPoint = fs.getElementsByTagName("Mount_point").item(0).getTextContent();
                                    aFS.setMountPoint(mountPoint);
                                    double size = Double.parseDouble(fs.getElementsByTagName("Capacity_kB").item(0)
                                            .getTextContent()) / 1024.0 / 1024.0;
                                    aFS.setSize(size);
                                    double avail = Double.parseDouble(fs.getElementsByTagName("Available_kB").item(0)
                                            .getTextContent()) / 1024.0 / 1024.0;
                                    aFS.setAvailableSize(avail);
                                    this.fileSystems.add(aFS);
                                }
                            }
                        }
                    }
                }

                if (doc.getElementsByTagName("Active_transfers").getLength() != 0) {
                    this.active_transfers = Long.parseLong(doc.getElementsByTagName("Active_transfers").item(0).getTextContent());
                }

                if (doc.getElementsByTagName("CPU_usage").getLength() != 0) {
                    this.cpu_usage = Double.parseDouble(doc.getElementsByTagName("CPU_usage").item(0).getTextContent());
                }
            }

        } catch (FileNotFoundException e) {
			logger.log(Level.WARNING, String.format("%s...Defaulting to last known configuration (if any)", e.getMessage()) );
    	} catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public DTNNode getDTNNode() {
        return this.dtn;
    }

    public List<FileSystem> getFileSystems() {
        return this.fileSystems;
    }

    public String getTransferServiceType() {
        return this.transfer_service_type;
    }

    public Map<String, String> getTransferConf() {
        return this.transfer_service_conf;
    }

    public long getActiveTransfers() {
        return this.active_transfers;
    }

    public double getCPUload() {
        return this.cpu_usage;
    }
    
    private int runcommand(String[] cmd){
        String s = null, output = "", error="";
        int exitVal = -1;
        try {
            // using the Runtime exec method:            
            Process p = Runtime.getRuntime().exec(cmd);
             
            BufferedReader stdInput = new BufferedReader(new
                 InputStreamReader(p.getInputStream()));
 
            BufferedReader stdError = new BufferedReader(new
                 InputStreamReader(p.getErrorStream()));
 
            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
               output += s+"\n";
            }
          
            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                error += s+"\n";
            }
            exitVal = p.waitFor();
            this.error = error;
            this.output = output;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ex) {
            Logger.getLogger(DTNGet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return exitVal;
    }
}
