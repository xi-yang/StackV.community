/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.ccsn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * @author xin, jboley
 */
public class CCSNPull {

    private CCSNode ccsn = null;
    private String timestamp = null;
    private List<FileSystem> fileSystems = new ArrayList<>();
    private boolean hasSshd;
    private double cpu_usage;
    private String error;
    private String output;
    private static final long CUTOFF = 20;
    private static final Logger logger = Logger.getLogger(CCSNPull.class.getName());

    @SuppressWarnings("unchecked")
    public CCSNPull(String pullInvokePattern, HashMap<String, String> pullUtilParams) {
        //todo:active proxy credential
        Node tmpNode;

        try {
            String cmdStr = (new PullCmdAssembler(pullInvokePattern, logger)).generateCmdStr(pullUtilParams);
            String cmd[] = cmdStr.split(" ");

            // Log command issued to shell
            logger.log(Level.INFO, String.format("Issuing metadata pull command to shell: %s", cmdStr));

            int exit = runcommand(cmd);
//            logger.log(Level.INFO, output);
//            logger.log(Level.INFO, error);

            if (exit != 0) {
                throw new IllegalStateException("Error transferring file: Error=" + this.error + ", Output=" + this.output);
            }
            else {
                // Check every 1 second for the file, abort if the cutoff time is exceeded
                File metadataFile = new File(pullUtilParams.get("node-metadata-paths"));
                long startTime = System.currentTimeMillis() / 1000L;
                while (!metadataFile.exists()) {
                    Thread.sleep(1000);
                    long currentTime = System.currentTimeMillis() / 1000L;
                    if (currentTime - startTime > CUTOFF)
                        throw new FileNotFoundException(String.format("Login node metadata not found at %s", metadataFile.toString()));
                }

                /*
                 * Gather login node and compute cluster information
                 */

                // Login node information
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(metadataFile);
                doc.getDocumentElement().normalize();
                if (doc.getElementsByTagName("Timestamp_epoch").getLength() != 0)
                    this.timestamp = doc.getElementsByTagName("Timestamp_epoch").item(0).getTextContent();

                if (doc.getElementsByTagName("loginConfig").getLength() != 0) {
                    tmpNode = doc.getElementsByTagName("loginConfig").item(0);
                    if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element ccsNode = (Element) tmpNode;
                        String ip = ccsNode.getElementsByTagName("IP").item(0).getTextContent();
                        String hostname = ccsNode.getElementsByTagName("Hostname").item(0).getTextContent();
                        ccsn = new CCSNode(ip, hostname);

                        int cpu = Integer.parseInt(ccsNode.getElementsByTagName("CPU").item(0).getTextContent());
                        ccsn.setCPU(cpu);
                        long memory = Long.parseLong(ccsNode.getElementsByTagName("Memory_kB").item(0).getTextContent()) / 1024;
                        ccsn.setMemory(memory);

                        // NIC info
                        if (ccsNode.getElementsByTagName("NICs").getLength() != 0) {
                            List<NIC> listOfNics = new ArrayList<>();
                            Element NICListNode = (Element) ccsNode.getElementsByTagName("NICs").item(0);
                            NodeList NICs = NICListNode.getChildNodes();
                            for (int i = 0; i < NICs.getLength(); ++i) {
                                Element NICNode = (Element) NICs.item(i);
                                String  nic_id = ((Element) NICNode.getElementsByTagName("NIC_ID").item(0)).getTextContent(),
                                        nic_ip = ((Element) NICNode.getElementsByTagName("IP_address").item(0)).getTextContent(),
                                        link_type = ((Element) NICNode.getElementsByTagName("Link_type").item(0)).getTextContent(),
                                        link_capacity = ((Element) NICNode.getElementsByTagName("Link_capacity_Mbps").item(0)).getTextContent();
                                NIC nic;
                                if (NICNode.getElementsByTagName("rx-bytes").getLength() != 0) {
                                    Element rxBytesNode = (Element) NICNode.getElementsByTagName("rx-bytes").item(0);
                                    Element txBytesNode = (Element) NICNode.getElementsByTagName("tx-bytes").item(0);

                                    nic = new NIC(
                                            nic_id, nic_ip, link_type,
                                            !"".equals(link_capacity) ? Long.parseLong(link_capacity) : -1L,
                                            Long.parseLong(rxBytesNode.getTextContent()),
                                            Long.parseLong(txBytesNode.getTextContent()),
                                            this.timestamp
                                            );
                                }
                                else {
                                    nic = new NIC(
                                            nic_id, nic_ip, link_type,
                                            !"".equals(link_capacity) ? Long.parseLong(link_capacity) : -1L
                                            );
                                }
                                listOfNics.add(nic);
                            }
                            ccsn.setNICs(listOfNics);
                        }

                        // Traffic load
                        GlobalPersistentStore store = GlobalPersistentStore.get();
                        if (!store.exists("login-" + ip))
                            store.create("login-" + ip);
                        List<String> rxBytes;
                        if (store.get("login-" + ip, "TCP_read_buffer") == null) {
                            rxBytes = new ArrayList<>();
                            store.add("login-" + ip, "TCP_read_buffer", rxBytes);
                        }
                        else
                            rxBytes = (List<String>) store.get("login-" + ip, "TCP_read_buffer");

                        String rbuff = ccsNode.getElementsByTagName("TCP_read_buffer").item(0).getTextContent();
                        rxBytes.add(rbuff);
//                        this.ccsn.setTCPReadBuffer(rxBytes);

                        List<String> txBytes;
                        if (store.get("login-" + ip, "TCP_write_buffer") == null) {
                            txBytes = new ArrayList<>();
                            store.add("login-" + ip, "TCP_write_buffer", txBytes);
                        }
                        else
                            txBytes = (List<String>) store.get("login-" + ip,  "TCP_write_buffer");

                        String wbuff = ccsNode.getElementsByTagName("TCP_write_buffer").item(0).getTextContent();
                        txBytes.add(wbuff);
//                        this.ccsn.setTCPWriteBuffer(txBytes);
                    }
                }

                if (doc.getElementsByTagName("CPU_usage").getLength() != 0)
                    this.cpu_usage = Double.parseDouble(doc.getElementsByTagName("CPU_usage").item(0).getTextContent());

                // Compute cluster information
                if (doc.getElementsByTagName("compute").getLength() != 0) {
                    tmpNode = doc.getElementsByTagName("compute").item(0);
                    if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
                        // Get scheduler and compute type
                        Element compute = (Element) tmpNode;
                        ccsn.setScheduler( compute.getAttribute("scheduler") );
                        ccsn.setClusterType( compute.getAttribute("type") );

                        int totalNodes = 0, availNodes = 0,
                            coresPerNode = 0;
                        String memPerNode = null;
                        if (compute.getElementsByTagName("hardware").getLength() != 0) {
                            // Get login node basic configuration info
                            Element hardware = (Element) compute.getElementsByTagName("hardware").item(0);
                            if (hardware.getElementsByTagName("nodes").getLength() != 0) {
                                tmpNode = hardware.getElementsByTagName("nodes").item(0);
                                if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
                                    totalNodes = Integer.parseInt( ((Element) tmpNode).getAttribute("total") );
                                    availNodes = Integer.parseInt( ((Element) tmpNode).getAttribute("avail") );
                                }
                            }
                            if (hardware.getElementsByTagName("mem_pernode").getLength() != 0) {
                                tmpNode = compute.getElementsByTagName("mem_pernode").item(0);
                                if (tmpNode.getNodeType() == Node.ELEMENT_NODE)
                                    memPerNode = ((Element) tmpNode).getTextContent();
//                                  memPerNode = Integer.parseInt( ((Element) tmpNode).getTextContent() );
                            }
                            if (compute.getElementsByTagName("cores_pernode").getLength() != 0) {
                                tmpNode = compute.getElementsByTagName("cores_pernode").item(0);
                                if (tmpNode.getNodeType() == Node.ELEMENT_NODE)
                                    coresPerNode = Integer.parseInt( ((Element) tmpNode).getTextContent() );
                            }
                        }

                        ComputeResource arch;
                        String name = ((Element) compute.getElementsByTagName("name").item(0)).getTextContent();
                        switch(compute.getAttribute("type")) {
                        case "supercomputer":
                        case "partition":
                            arch = new Supercomputer(name, compute.getAttribute("scheduler"), totalNodes, availNodes, coresPerNode, memPerNode);
                            break;
                        case "cluster":
                        default:
                            arch = new ComputeCluster(name, compute.getAttribute("scheduler"), totalNodes, availNodes, coresPerNode, memPerNode);
                        }
                        arch.parseHardwareConfigsXml(compute);
                        arch.parseServiceConfigsXml(compute);
                        ccsn.setComputeResource(arch);
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
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, String.format("%s...Defaulting to last known configuration (if any)", e.getMessage()) );
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s\n", e.getMessage()) );
            for (StackTraceElement elem: e.getStackTrace())
                sb.append(String.format("%s\n", elem.toString()) );
            sb.append("Defaulting to last known configuration (if any)");
            logger.log(Level.SEVERE, sb.toString());
        }
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public List<FileSystem> getFileSystems() {
        return this.fileSystems;
    }

    public double getCPUload() {
        return this.cpu_usage;
    }

    public boolean providesScpTransfers() {
        return hasSshd;
    }

    public
    CCSNode getCCSNode() {
        return ccsn;
    }

//	@SuppressWarnings("unused")
    private int runcommand(String[] cmd){
        String s = null, output = "", error="";
        int exitVal = -1;
        try {
            // using the Runtime exec method:            
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()) );
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()) );

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
//			System.out.println("Exit: "+exitVal+"out: " + this.output+ "error: "+this.error);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ex) {
            Logger.getLogger(CCSNPull.class.getName()).log(Level.SEVERE, null, ex);
        }
        return exitVal;
    }
}
