/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *
 * @author xin
 */
public class DTNGet {
    private String timestamp;
    private DTNNode dtn;
    private List<FileSystem> fileSystems;
    private String transfer_service_type;
    private String transfer_service_conf;
    
    public DTNGet(String user_account, String access_key, String address){
        this.dtn = null;
        this.timestamp = null;
        this.transfer_service_type = null;
        this.transfer_service_conf = null;
        this.fileSystems = new ArrayList<>();

        
        //todo:authentication and communication with script on dtns
        //Parse xml file
        Node tmpNode; Element tmpEle;
        try {
            File inputFile = new File("/home/xin/Dropbox/RAINS/DTN_Driver/conf_files/dtn-"+address+".xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            
            //Timestamp
            if ( doc.getElementsByTagName("Timestamp_UTC").getLength()!=0 ){
                this.timestamp = doc.getElementsByTagName("Timestamp_UTC").item(0).getTextContent();
            }    
            
            //DTN node information
            if ( doc.getElementsByTagName("dtnNode").getLength()!=0 ){
                tmpNode = doc.getElementsByTagName("dtnNode").item(0);
                if (tmpNode.getNodeType()==Node.ELEMENT_NODE){
                    Element dtnNode = (Element) tmpNode;
                    String ip = dtnNode.getElementsByTagName("IP").item(0).getTextContent();
                    String hostname = dtnNode.getElementsByTagName("Hostname").item(0).getTextContent();
                    this.dtn = new DTNNode(ip,hostname);
                    
                    int cpu = Integer.parseInt(dtnNode.getElementsByTagName("CPU").item(0).getTextContent());
                    this.dtn.setCPU(cpu);
                    long memory = Long.parseLong(dtnNode.getElementsByTagName("Memory_kB").item(0).getTextContent());
                    this.dtn.setMemory(memory);
                    
                    //Get NICs
                    if (dtnNode.getElementsByTagName("NICs").getLength() != 0){
                        tmpNode = dtnNode.getElementsByTagName("NICs").item(0);
                        if (tmpNode.getNodeType()==Node.ELEMENT_NODE){
                            tmpEle = (Element) tmpNode;
                            if (tmpEle.getElementsByTagName("NIC").getLength() != 0){
                                NodeList nList = tmpEle.getElementsByTagName("NIC");
                                List<NIC> nics = new ArrayList<>();
                                for (int i=0; i<nList.getLength(); i++){
                                    if (nList.item(i).getNodeType()==Node.ELEMENT_NODE){
                                        Element nic = (Element) nList.item(i);
                                        String nic_id = nic.getElementsByTagName("NIC_ID").item(0).getTextContent();
                                        String link_type = nic.getElementsByTagName("Link_type").item(0).getTextContent();
                                        String ip_addr = nic.getElementsByTagName("IP_address").item(0).getTextContent();
                                        NIC aNic = new NIC(nic_id,link_type,ip_addr);
                                        String tmpCap = nic.getElementsByTagName("Link_capacity_Mbps").item(0).getTextContent();
                                        if (tmpCap.length()!=0){
                                            long link_cap = Long.parseLong(tmpCap);
                                            if (link_cap > 0){
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
                    if (dtnNode.getElementsByTagName("DataTransferService").getLength() != 0){
                        tmpNode = dtnNode.getElementsByTagName("DataTransferService").item(0);
                        if (tmpNode.getNodeType()==Node.ELEMENT_NODE){
                            Element transfer_conf = (Element) tmpNode;
                            this.transfer_service_type = transfer_conf.getElementsByTagName("Service_type").item(0).getTextContent(); 
                            this.transfer_service_conf = transfer_conf.getElementsByTagName("Value").item(0).getTextContent();
                        }
                    }
                }
            }            
            
            //Storage information
            if ( doc.getElementsByTagName("Storage").getLength()!=0 ){
                tmpNode = doc.getElementsByTagName("Storage").item(0);
                if (tmpNode.getNodeType()==Node.ELEMENT_NODE){
                    Element storage = (Element) tmpNode;
                    //Get file systems
                    if (storage.getElementsByTagName("File_System").getLength() != 0){
                        NodeList nList = storage.getElementsByTagName("File_System");
                        for (int i=0; i<nList.getLength(); i++){
                            if (nList.item(i).getNodeType()==Node.ELEMENT_NODE){
                                Element fs = (Element) nList.item(i);
                                String device = fs.getElementsByTagName("Device").item(0).getTextContent();
                                String fs_type = fs.getElementsByTagName("Type").item(0).getTextContent();
                                FileSystem aFS = new FileSystem(device, fs_type);
                                
                                String mountPoint = fs.getElementsByTagName("Mount_point").item(0).getTextContent();
                                aFS.setMountPoint(mountPoint);
                                long size = Long.parseLong(fs.getElementsByTagName("Capacity_kB").item(0)
                                        .getTextContent());
                                aFS.setSize(size);
                                long avail = Long.parseLong(fs.getElementsByTagName("Available_kB").item(0)
                                        .getTextContent());
                                aFS.setAvailableSize(avail);
                                this.fileSystems.add(aFS);
                            }
                        }
                    }
                }
            }     
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public String getTimestamp(){
        return this.timestamp;
    }
    
    public DTNNode getDTNNode(){
        return this.dtn;
    }
    
    public List<FileSystem> getFileSystems(){
        return this.fileSystems;
    }
    
    public String getTransferServiceType(){
        return this.transfer_service_type;
    }
    
    public String getTransferConf(){
        return this.transfer_service_conf;
    }
}
