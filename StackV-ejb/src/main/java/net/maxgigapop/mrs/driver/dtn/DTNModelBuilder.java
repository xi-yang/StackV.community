 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.maxgigapop.mrs.common.*;

//TODO add the public ip address that an instance might have that is not an
//elastic ip

/*TODO: Intead of having separate routeFrom statements for routes in a route table 
 associated with subnets. Include the routeFrom statement just once in the model, 
 meaning that look just once for the associations of the route table, 
 do not do a routeFrom statement for every route.*/

/*
 *
 * @author muzcategui
 */
public class DTNModelBuilder {

    private static final StackLogger logger = DTNDriver.logger;
        
    private final String user_account;
    private String transferMap;
    private String performanceMap;
    private int numofservers;
    private long sum_active_transfer;
    private String avgTraffic="";
    private String error;
    private String output;
    
    @SuppressWarnings("empty-statement")
    public DTNModelBuilder(String user_account, String mappingId, String perf){
        this.user_account = user_account;
        this.transferMap = mappingId;
        this.performanceMap = perf;
    }

    @SuppressWarnings("empty-statement")
    public OntModel createOntology(String user_account, String access_key, String proxy_server, String addresses, String topologyURI, 
            String endpoint) throws IOException {
        String method = "createOntology";
        boolean auth_done = false;
        String cred_file="";
        if(!access_key.isEmpty()){
            if(access_key.contains("/tmp/")){
                //if credential file is given, use it to access dtn
                cred_file = access_key;
                auth_done = checkCredValidation(cred_file);
            }
            else {
                //if password is given, autometically renew credential file to access dtn
                //check credential validation information
                cred_file = "/tmp/"+user_account+"_"+endpoint;
                boolean valid = checkCredValidation(cred_file);
                if(!valid){
                    int exitVal = dtn_authentication(user_account, access_key, proxy_server, cred_file);
                    if(exitVal==0)
                        auth_done = true;
                }
                else
                    auth_done = true;
            }
        }

        if(!auth_done){
            throw logger.error_throwing(method, String.format("%s authentication fail", endpoint));
        }
        
        //create model object
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        
        //set all the model prefixes
        model.setNsPrefix("rdfs", RdfOwl.getRdfsURI());
        model.setNsPrefix("rdf", RdfOwl.getRdfURI());
        model.setNsPrefix("xsd", RdfOwl.getXsdURI());
        model.setNsPrefix("owl", RdfOwl.getOwlURI());
        model.setNsPrefix("nml", Nml.getURI());
        model.setNsPrefix("mrs", Mrs.getURI());

        //set the global properties
        Property hasNode = Nml.hasNode;
        Property isAlias = Nml.isAlias;
        Property hasBidirectionalPort = Nml.hasBidirectionalPort;
        Property hasService = Nml.hasService;
        Property providesVolume = Mrs.providesVolume;
        Property providesBucket = Mrs.providesBucket;
        Property hasBucket = Mrs.hasBucket;
        Property hasVolume = Mrs.hasVolume;
        Property hasFileSystem = Mrs.hasFileSystem;
        Property hasTransfer = Mrs.hasTransfer;

        //set the global data properties
        Property cpu = Mrs.num_core;
        Property memory_mb = Mrs.memory_mb;
        Property type = Mrs.type;
        Property value = Nml.value;
        Property values = Nml.values;
        Property address = Nml.address;
        Property link_capacity_Mbps = Mrs.capacity;
        Property disk_cap = Mrs.disk_gb;
        Property parameters = Nml.parameter;
        Property disk_avail = model.createProperty(model.getNsPrefixURI("mrs") + "disk_free_gb");
        Property mount = model.createProperty(model.getNsPrefixURI("mrs") + "mount_point");
        Property active_transfers = Mrs.active_transfers;
        Property sys_info = model.createProperty(model.getNsPrefixURI("mrs") + "measurement");
        Property avg_traffic = Mrs.average_traffic;
        Property history = model.createProperty(model.getNsPrefixURI("mrs") + "transfer_history");
        Property duplex = model.createProperty(model.getNsPrefixURI("mrs") + "duplex");

        //set the global resources
        Resource storageService = Mrs.StorageService;
        Resource blockStorageService = Mrs.BlockStorageService;
        Resource objectStorageService = Mrs.ObjectStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology = Nml.Topology;
        Resource switchingService = Mrs.SwitchingService;
        Resource clusterService = Mrs.DataTransferClusterService;
        Resource dataTransferService = Mrs.DataTransferService;
        Resource dataTransfer = Mrs.DataTransfer;
        Resource fileSystem = Mrs.FileSystem;
        Resource node = Nml.Node;
        Resource biPort = Nml.BidirectionalPort;

        //create resource for tcpport labels
        Resource tcpport = model.createResource("http://schemas.ogf.org/mrs/2015/08/label/layer4#tcpport");

        Resource dtnTopology = RdfOwl.createResource(model, topologyURI, topology);

        Resource CLUSTERSERVICE = null;
        if (endpoint.length() > 0){
            Property endPoint  = model.createProperty(model.getNsPrefixURI("mrs") + "endpoint");
            CLUSTERSERVICE = RdfOwl.createResource(model, topologyURI+":clusterservice-"+endpoint, clusterService);
            model.add(model.createStatement(dtnTopology, hasService, CLUSTERSERVICE));
            model.addLiteral(CLUSTERSERVICE, endPoint, ResourceFactory.createTypedLiteral(endpoint,XSDDatatype.XSDstring));
        }
        Resource INTERCONNECTION = RdfOwl.createResource(model,topologyURI+":interconnection", switchingService);
        model.add(model.createStatement(dtnTopology, hasService, INTERCONNECTION));
        
        boolean fullduplex = true;
        double[][] averageTraffic = new double[4][4];
        String[] ips = addresses.split("[\\(\\)]");
        List<FileSystem> pfslist = new ArrayList<>();
        for (String ip : ips) {
            if (ip != null && ip.length() > 0) {
                DTNGet conf = new DTNGet(user_account, cred_file, proxy_server, ip);
                if (conf.getDTNNode() != null) {
                    this.numofservers ++;
                    //create the outer layer of the DTN model
                    Resource DTN_NODE = RdfOwl.createResource(model, topologyURI + ":" + conf.getDTNNode().getHostName(), node);
                    model.add(model.createStatement(dtnTopology, hasNode, DTN_NODE));
                    //create CPU and memory information
                    model.addLiteral(DTN_NODE, cpu, conf.getDTNNode().getNumCPU());                    
                    model.addLiteral(DTN_NODE, memory_mb, conf.getDTNNode().getMemorySize());
                    String sysInfo = "cpu_usage=" + conf.getCPUload() + ";mem_usage="+conf.getMemload();
                    model.addLiteral(DTN_NODE, sys_info, ResourceFactory.createTypedLiteral(sysInfo, XSDDatatype.XSDstring));

                    //create NIC information
                    if (!conf.getDTNNode().getNICs().isEmpty()) {
                        for (NIC i : conf.getDTNNode().getNICs()) {
                            String nic_id = i.getNICid();
                            Resource NIC = RdfOwl.createResource(model, DTN_NODE.getURI() + ":nic-" + nic_id, biPort);
                            model.add(model.createStatement(DTN_NODE, hasBidirectionalPort, NIC));
                            model.addLiteral(NIC, type, ResourceFactory.createTypedLiteral(i.getLinkType(), XSDDatatype.XSDstring));
                            model.addLiteral(NIC, address, ResourceFactory.createTypedLiteral(i.getIPAddress(), XSDDatatype.XSDstring));
                            if (i.getLinkCapacity() != 0L) {
                                model.addLiteral(NIC, link_capacity_Mbps, i.getLinkCapacity());
                                model.addLiteral(NIC, duplex, ResourceFactory.createTypedLiteral(i.getLinkDuplex(), XSDDatatype.XSDstring));
                            }
                            //create interconnections for local networks
                            if (!i.getIPAddress().equals(conf.getDTNNode().getIP())) {
                                Resource counterNIC = RdfOwl.createResource(model, INTERCONNECTION.getURI() + ":" + conf.getDTNNode().getHostName() + "-" + nic_id, biPort);
                                model.add(model.createStatement(INTERCONNECTION, hasBidirectionalPort, counterNIC));
                                model.add(model.createStatement(NIC, isAlias, counterNIC));
                                model.add(model.createStatement(counterNIC, isAlias, NIC));
                            } else {
                                if(i.getLinkDuplex().equals("half"))
                                    fullduplex = false;
                            }
                        }
                    }

                    //create storage informatioin
                    if (conf.getFileSystems() != null) {
                        if (!conf.getFileSystems().isEmpty()) {
                            for (FileSystem f : conf.getFileSystems()) {
                                if (!f.isParallel()) {
                                    String fs_id = f.getDeviceName();
                                    Resource LOCALFS = RdfOwl.createResource(model, DTN_NODE.getURI() + ":localfilesystem-" + fs_id, fileSystem);
                                    model.add(model.createStatement(DTN_NODE, hasFileSystem, LOCALFS));
                                    model.addLiteral(LOCALFS, type, ResourceFactory.createTypedLiteral(f.getType(), XSDDatatype.XSDstring));
                                    model.addLiteral(LOCALFS, mount, ResourceFactory.createTypedLiteral(f.getMountPoint(), XSDDatatype.XSDstring));
                                    if (f.isBlockStorage()) {
                                        Resource BLOCKSERVICE = RdfOwl.createResource(model, LOCALFS.getURI() + ":blockstorageservice", blockStorageService);
                                        model.add(model.createStatement(LOCALFS, hasService, BLOCKSERVICE));
                                        model.add(model.createStatement(DTN_NODE, hasService, BLOCKSERVICE));
                                        Resource LOCALVOLUME = RdfOwl.createResource(model, LOCALFS.getURI() + ":localvolume", volume);
                                        model.add(model.createStatement(BLOCKSERVICE, providesVolume, LOCALVOLUME));
                                        model.add(model.createStatement(DTN_NODE, hasVolume, LOCALVOLUME));
                                        model.addLiteral(LOCALVOLUME, disk_cap, f.getSize());
                                        model.addLiteral(LOCALVOLUME, disk_avail, f.getAvailableSize());
                                    } else {
                                        Resource OBJECTSERVICE = RdfOwl.createResource(model, LOCALFS.getURI() + ":objectstorageservice", objectStorageService);
                                        model.add(model.createStatement(LOCALFS, hasService, OBJECTSERVICE));
                                        model.add(model.createStatement(DTN_NODE, hasService, OBJECTSERVICE));
                                        Resource LOCALBUCKET = RdfOwl.createResource(model, LOCALFS.getURI() + ":localbucket", bucket);
                                        model.add(model.createStatement(OBJECTSERVICE, providesBucket, LOCALBUCKET));
                                        model.add(model.createStatement(DTN_NODE, hasBucket, LOCALBUCKET));
                                        model.addLiteral(LOCALBUCKET, disk_cap, f.getSize());
                                        model.addLiteral(LOCALBUCKET, disk_avail, f.getAvailableSize());
                                    }
                                } else {
                                    if (!pfslist.contains(f)) {
                                        pfslist.add(f);
                                    }
                                }
                            }
                        }
                    }

                    //create data transfer service informatioin
                    if (conf.getTransferServiceType() != null) {
                        Resource TRANSFERSERVICE = RdfOwl.createResource(model, DTN_NODE.getURI() + ":datatransferservice-" + conf.getTransferServiceType(), dataTransferService);
                        model.add(model.createStatement(DTN_NODE, hasService, TRANSFERSERVICE));
                        Map<String, String> entries = conf.getTransferConf();

                        if (entries.containsKey("Service_type")){
                            model.addLiteral(TRANSFERSERVICE, type, ResourceFactory.createTypedLiteral(entries.get("Service_type"),XSDDatatype.XSDstring));
                        }
                        if (entries.containsKey("Port")){
                            Resource TCPPORT = RdfOwl.createResource(model, TRANSFERSERVICE.getURI() + ":port", Nml.Label);
                            model.add(model.createStatement(TCPPORT, Nml.labeltype, tcpport));
                            model.add(model.createStatement(TCPPORT, value, entries.get("Port")));
                            model.add(model.createStatement(TRANSFERSERVICE, Nml.hasLabel, TCPPORT));
                        }
                        if (entries.containsKey("Port_range")){
                            Resource PORT_RANGE = RdfOwl.createResource(model, TRANSFERSERVICE.getURI() + ":portrange", Nml.LabelGroup);
                            model.add(model.createStatement(PORT_RANGE, Nml.labeltype, tcpport));
                            model.add(model.createStatement(PORT_RANGE, values, entries.get("Port_range")));
                            model.add(model.createStatement(TRANSFERSERVICE, Nml.hasLabelGroup, PORT_RANGE));
                        }
                        
                        //dynamic information: number of active transfer
                        long at = conf.getActiveTransfers();
                        model.addLiteral(TRANSFERSERVICE, active_transfers, at);
                        this.sum_active_transfer += at;
                        
                        //history information: internet traffic & IO traffic
                        String[] traffics = conf.getDTNNode().gettTraffic().split("--");
                        for(int i=0; i<traffics.length;i++){
                            String[] parts = traffics[i].split(",");
                            for(int j=0; j<parts.length;j++){
                                if(!parts[j].equalsIgnoreCase("NA"))
                                    averageTraffic[i][j] += Double.parseDouble(parts[j]);
                            }
                        }
                        
                        if (endpoint.length() > 0) {
                            model.add(model.createStatement(TRANSFERSERVICE, hasService, CLUSTERSERVICE));
                        }
                    }
                }
            }
        }

        //for parallel file systems
        if (!pfslist.isEmpty()) {
            Resource STORAGE_NODE = RdfOwl.createResource(model, topologyURI + ":storagenode", node);
            model.add(model.createStatement(dtnTopology, hasNode, STORAGE_NODE));
            for (FileSystem pf : pfslist) {
                String fs_id = pf.getMountPoint();
                Resource PARALLELEFS = RdfOwl.createResource(model, topologyURI+":parallelfilesystem-"+fs_id, fileSystem);

                model.add(model.createStatement(STORAGE_NODE, hasFileSystem, PARALLELEFS));
                model.addLiteral(PARALLELEFS, type, ResourceFactory.createTypedLiteral(pf.getType(), XSDDatatype.XSDstring));
                model.addLiteral(PARALLELEFS, mount, ResourceFactory.createTypedLiteral(pf.getMountPoint(), XSDDatatype.XSDstring));
                if (pf.isBlockStorage()) {
                    Resource BLOCKSERVICE = RdfOwl.createResource(model, PARALLELEFS.getURI() + ":blockstorageservice", blockStorageService);
                    model.add(model.createStatement(PARALLELEFS, hasService, BLOCKSERVICE));
                    model.add(model.createStatement(STORAGE_NODE, hasService, BLOCKSERVICE));
                    Resource VOLUME = RdfOwl.createResource(model, PARALLELEFS.getURI() + ":volume", volume);
                    model.add(model.createStatement(BLOCKSERVICE, providesVolume, VOLUME));
                    model.add(model.createStatement(STORAGE_NODE, hasVolume, VOLUME));
                    model.addLiteral(VOLUME, disk_cap, pf.getSize());
                    model.addLiteral(VOLUME, disk_avail, pf.getAvailableSize());
                } else {
                    Resource OBJECTSERVICE = RdfOwl.createResource(model, PARALLELEFS.getURI() + ":objectstorageservice", objectStorageService);
                    model.add(model.createStatement(PARALLELEFS, hasService, OBJECTSERVICE));
                    model.add(model.createStatement(STORAGE_NODE, hasService, OBJECTSERVICE));
                    Resource BUCKET = RdfOwl.createResource(model, PARALLELEFS.getURI() + ":bucket", bucket);
                    model.add(model.createStatement(OBJECTSERVICE, providesBucket, BUCKET));
                    model.add(model.createStatement(STORAGE_NODE, hasVolume, BUCKET));
                    model.addLiteral(BUCKET, disk_cap, pf.getSize());
                    model.addLiteral(BUCKET, disk_avail, pf.getAvailableSize());
                }
            }
            //create connections
            Resource fs_port = RdfOwl.createResource(model, STORAGE_NODE.getURI() + ":biport", biPort);
            model.add(model.createStatement(STORAGE_NODE, hasBidirectionalPort, fs_port));
            Resource counter_port = RdfOwl.createResource(model, INTERCONNECTION.getURI() + ":storage-biport", biPort);
            model.add(model.createStatement(INTERCONNECTION, hasBidirectionalPort, counter_port));
            model.add(model.createStatement(fs_port, isAlias, counter_port));
            model.add(model.createStatement(counter_port, isAlias, fs_port));
        }
        
        //create maximum allowed parallelism and concurrency 
        String max = getMaxParameters(endpoint);
         model.addLiteral(CLUSTERSERVICE, parameters, ResourceFactory.createTypedLiteral(max,XSDDatatype.XSDstring));
        
        //create average traffic        
        if(!fullduplex){
            double[][] old = averageTraffic;
            for(int i=0;i<2;i++){
                for(int j=0;j<averageTraffic[0].length;j++){
                    averageTraffic[i][j] += old[0][j] + old[1][j];
                }
            }
        }
        for(int i=0;i<averageTraffic.length;i++){
            for(int j=0;j<averageTraffic[i].length;j++){
                averageTraffic[i][j] = averageTraffic[i][j]/this.numofservers;
                this.avgTraffic += averageTraffic[i][j]+",";
            }
            this.avgTraffic = this.avgTraffic.substring(0, this.avgTraffic.length() - 1) + "--";
        }
        
        model.addLiteral(CLUSTERSERVICE, avg_traffic, ResourceFactory.createTypedLiteral(this.avgTraffic,XSDDatatype.XSDstring));        
        
        //create active data transfers
        model.addLiteral(CLUSTERSERVICE, active_transfers, this.sum_active_transfer);  
                
        
        if (endpoint.length() > 0){
            String active_string = parseTransferMap(endpoint);
            if(active_string.length()!=0){
                String[] activeTransfers = active_string.split("\n");
                for(String item:activeTransfers){
                    String parts[] = item.split(";");
                    String taskid = parts[3];
                    Resource TRANSFER = RdfOwl.createResource(model, CLUSTERSERVICE.getURI()+":transfer-"+taskid, dataTransfer);
                    model.add(model.createStatement(CLUSTERSERVICE, hasTransfer, TRANSFER));
                    Property tag = Mrs.tag;
                    Property src = Mrs.source;
                    Property dst = Mrs.destination;
                    model.addLiteral(TRANSFER, tag, ResourceFactory.createTypedLiteral(parts[0],XSDDatatype.XSDstring));
                    model.addLiteral(TRANSFER, src, ResourceFactory.createTypedLiteral(parts[1],XSDDatatype.XSDstring));
                    model.addLiteral(TRANSFER, dst, ResourceFactory.createTypedLiteral(parts[2],XSDDatatype.XSDstring));
                }
            }
        }
        
        //create past data transfer performance information
        model.addLiteral(CLUSTERSERVICE, history, ResourceFactory.createTypedLiteral(this.performanceMap,XSDDatatype.XSDstring));
        
        return model;
    }
    
    private String parseTransferMap(String endpoint){
        String activeTransfers="";
        if(this.transferMap.length()!=0){
            String[] transferMapMatrix=this.transferMap.split("\n");
            for(String aLine:transferMapMatrix){               
                String parts[] = aLine.split(";");
                String taskid = parts[2];
                String src = parts[0].substring(0, parts[0].indexOf("/"));
                String dst = parts[1].substring(0, parts[1].indexOf("/"));
                DataTransfer transfer = new DataTransfer(taskid, src, dst);
                long at = Long.parseLong(parts[4]);
                transfer.setBackgroud(at, parts[5]);                
                ArrayList<String> cmdarray = new ArrayList<>();                
                cmdarray.add("gsissh"); cmdarray.add("cli.globusonline.org"); cmdarray.add("details");
                cmdarray.add(taskid);cmdarray.add("-f"); cmdarray.add("status,mbits_sec,command,"
                        + "request_time,completion_time,bytes_transferred,files_transferred");
                cmdarray.add("-O");cmdarray.add("csv");
                String cmd[] = new String[cmdarray.size()];
                cmd = cmdarray.toArray(cmd);
                int exit = runcommand(cmd, null,null);
                if(exit==0){
                    String[] items = this.output.trim().split(",");
                    String status = items[0];                    
                    if(status.contains("ACTIVE")){
                        activeTransfers += aLine+"\n";
                    }
                    else if (status.contains("SUCCEEDED")) {//transfer donestahisstatus
                        this.transferMap = this.transferMap.replaceAll(aLine+"\n", "");
                        String info = translateInfo(items, transfer);
                        addToPerformanceMap(info);  //store performance information
                    }
                    else if (status.contains("FAILED")) { //transfer fail
                        this.transferMap = this.transferMap.replaceAll(aLine+"\n", "");
                    }
                    else {  //transfer inactive
                        this.transferMap = this.transferMap.replaceAll(aLine, aLine.replace(parts[3], status));
                    }
                }
            }
        }
        return activeTransfers;
    }
   
    public String getTransferMap(){
        return this.transferMap;
    }
    
    public String getPerformanceMap(){
        return this.performanceMap;
    }
    
    private boolean checkCredValidation(String cred_file) {
        boolean valid = false;
        if(new File(cred_file).isFile()){
            String cmd = "grid-proxy-info -f "+cred_file+" -timeleft";
            int exitval = runcommand(cmd.split(" "), null,null);
            if(exitval==0){
                long timeleft = Long.parseLong(this.output.trim());
                if(timeleft >= 60)
                    valid = true;
            }            
        }
        return valid;
    }

    private int dtn_authentication(String user_account, String access_key, String proxy_server, String cred_fn) {
        
        String cmd = "myproxy-logon -b -T -s "+proxy_server+" -l "+user_account+" -o "+cred_fn+" -t 72 -S";
        int exitVal = runcommand(cmd.split(" "), null, access_key);
        if(exitVal != 0){
            //if error happens, possibly need to reset MYPROXY_SERVER_DN
            if(this.error.contains("does not match expected identities")) {
                String dn = this.error.substring(this.error.indexOf("(")+1, this.error.indexOf(")"));
                String[] envp = new String[1];
                envp[0] = "MYPROXY_SERVER_DN="+dn;                
                exitVal = runcommand(cmd.split(" "), envp, access_key);
            }            
        }
        return exitVal;
    }
    
    private String translateInfo(String[] items, DataTransfer transfer){
        String info="";
        long bt = Long.parseLong(items[5]);    //bytes transferred
        long ft = Long.parseLong(items[6]);    //files transferred
        int cc=0, p=0;
        for(String part:items[2].split(" ")){
            if(part.contains("--perf-cc"))
                cc = Integer.parseInt(part.split("=")[1]);  //concurrency
            if(part.contains("--perf-p"))
                p = Integer.parseInt(part.split("=")[1]);   //parallelism
        }
        
        //convert UTC time to epoch time
        long rt = 0, ct = 0;
        if(!items[3].equalsIgnoreCase("n/a")){
            rt = convertToEpoch(items[3]);  //request time
        }
        if(!items[4].equalsIgnoreCase("n/a")){
            ct = convertToEpoch(items[4]);  //completion time
        }
        double mbits = Double.parseDouble(items[1]);    //Mbits/sec
        
        transfer.setDetails(bt, ft, p, cc, rt, ct, mbits);
        info += transfer.toString();          
        return info;
    }
    
    private long convertToEpoch(String str){
        long epoch=0;
        String utc = str.replaceAll("Z", "UTC");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:sszzz");
        try {
            Date date = df.parse(utc);
            epoch = date.getTime();
        } catch (ParseException ex) {
            logger.catching("convertToEpoch", ex);
        }
        return epoch;
    }

    private String getMaxParameters(String endpoint) {
        int max_p = 64, max_cc = 64;
        String cmd = "gsissh cli.globusonline.org endpoint-details "+endpoint+" -f max_parallelism,max_concurrency -O csv";
        int exitVal = runcommand(cmd.split(" "), null, null);
        if(exitVal == 0){
            String[] temp = this.output.trim().split(",");
            max_p = Math.min(max_p, Integer.parseInt(temp[0]));
            max_cc = Math.min(max_cc, Integer.parseInt(temp[1]));            
        }
        String max_parameters = "max_p="+max_p+",max_cc="+max_cc;
        return max_parameters;
    }
    
    private int runcommand(String[] cmd, String[] env, String input){
        String s = null, output = "", error="";
        int exitVal = -1;
        try {
            Process p;
            if(env!=null)
                p = Runtime.getRuntime().exec(cmd, env);
            else    
                p = Runtime.getRuntime().exec(cmd);
             
            BufferedReader stdInput = new BufferedReader(new
                 InputStreamReader(p.getInputStream()));
 
            BufferedReader stdError = new BufferedReader(new
                 InputStreamReader(p.getErrorStream()));

            BufferedWriter stdOutput = new BufferedWriter(new
                     OutputStreamWriter(p.getOutputStream()));
            
            if(input!= null){
                stdOutput.write(input + "\n");
                stdOutput.flush();            
            } 
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
            stdInput.close();
            stdError.close();
            stdOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ex) {
            logger.catching("runcommand", ex);
        }
        return exitVal;
    }

    private void addToPerformanceMap(String info) {
        this.performanceMap += info+"\n";
        String[] history = this.performanceMap.split("\n");
        if(history.length <= 200)
            return;
        long now = Long.parseLong(history[history.length-1].split(";")[8]);
        for(String line:history){
            long past = Long.parseLong(line.split(";")[7]);
            if(now-past > 30*24*3600){
                this.performanceMap = this.performanceMap.replaceAll(line+"\n", "");
            }
        }
    }


    
}
