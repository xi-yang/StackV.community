#!/usr/bin/env python

from datetime import datetime
import subprocess
# from optparse import OptionParser
import os
import re
import shlex
import xml.dom.minidom as dom
import xml.etree.ElementTree as ET

def prettify(elem):
    #Return a pretty-printed XML string for the Element
    rough_string = ET.tostring(elem, 'utf-8')
    reparsed = dom.parseString(rough_string)
    return reparsed.toprettyxml(indent="  ")

def is_ip_private(ip):
    priv_lo = re.compile("^127\.\d{1,3}\.\d{1,3}\.\d{1,3}$")
    priv_24 = re.compile("^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$")
    priv_20 = re.compile("^192\.168\.\d{1,3}.\d{1,3}$")
    priv_16 = re.compile("^172.(1[6-9]|2[0-9]|3[0-1]).[0-9]{1,3}.[0-9]{1,3}$")
    return priv_lo.match(ip) or priv_24.match(ip) or priv_20.match(ip) or priv_16.match(ip)


def run_command(command):
    # command = re.findall(r'(?:"[^"]*"|[^\s"])+',command)
    command = shlex.split(command)
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return p.communicate()

def run_pipe_cmd(cmd1, cmd2):
    # cmd1 = re.findall(r'(?:"[^"]*"|[^\s"])+',cmd1)
    # cmd2 = re.findall(r'(?:"[^"]*"|[^\s"])+',cmd2)
    cmd1 = shlex.split(cmd1)
    cmd2 = shlex.split(cmd2)
    p1 = subprocess.Popen(cmd1, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    p2 = subprocess.Popen(cmd2, stdin=p1.stdout, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    p1.stdout.close()  # Allow p1 to receive a SIGPIPE if p2 exits.
    p1.stderr.close()
    return p2.communicate()


# if __name__ == "__main__":
    # p = OptionParser()
    # p.add_option("-i", "--id", dest = "host_id", type = "string", 
    #                 help = "host_id for xml file")
    
    # (opts, args) = p.parse_args()
    
    # if not opts.host_id:
    #     print "please specify host id of xml file (-i)"
    #     p.print_help()
    #     exit()
    
#Initiate xml document
dtnConfig = ET.Element("dtnConfig")

#Create time stamp
timestamp = ET.SubElement(dtnConfig, "Timestamp_UTC")
timestamp.text = str(datetime.utcnow())

#Get Host Information
dtnNode = ET.SubElement(dtnConfig, "dtnNode")

cpu = ET.SubElement(dtnNode, "CPU")
output,err = run_command('nproc')
# cpu = output.replace('\n','')
# print 'cpu: %s' %(output.rstrip())
cpu.text = output.rstrip()

memory_kB = ET.SubElement(dtnNode, "Memory_kB")
output,err = run_pipe_cmd('cat /proc/meminfo','grep MemTotal')
if(len(output)!=0):
    # memory_kB = output.split()[1]
    # print 'mem: %s' %(output.split()[1])
    memory_kB.text = output.split()[1]

public_ip = ET.SubElement(dtnNode, "IP")
hostname = ET.SubElement(dtnNode, "Hostname")
nics = ET.SubElement(dtnNode, "NICs")

#Get NIC 
output,err = run_pipe_cmd('ifconfig -a','grep -B1 "inet addr"')
if(output!=""):
    multi_nics = output.split("--\n")
    for nic in multi_nics:
        nic = nic.replace('\n',' ')
        parts = re.split(r'\s{2,}', nic[nic.index('Link'):])
        if (parts[0].split(":")[1] == 'Local Loopback'):
            continue
        one_nic = ET.SubElement(nics, "NIC")

        ET.SubElement(one_nic, "NIC_ID").text = nic.split()[0]
        ET.SubElement(one_nic, "Link_type").text = parts[0].split(":")[1]
        ip = parts[2].split(":")[1]
        ET.SubElement(one_nic, "IP_address").text = ip

        if(is_ip_private(ip) == None):
            public_ip.text = ip
            #Get hostname
            output, err = run_command('host '+ ip)
            hostname.text = output.split()[-1][0:-1]
        output,err = run_command('cat /sys/class/net/'+nic.split()[0]+'/speed')
        ET.SubElement(one_nic, "Link_capacity_Mbps").text = output.rstrip()

#Get TCP buffer
rbuf = ET.SubElement(dtnNode, "TCP_read_buffer")
output, err = run_command('cat /proc/sys/net/ipv4/tcp_rmem')
tcp_rmem = output.split()
rbuf.text = "%s,%s,%s" % (tcp_rmem[0],tcp_rmem[1],tcp_rmem[2])

wbuf = ET.SubElement(dtnNode, "TCP_write_buffer")
output, err = run_command('cat /proc/sys/net/ipv4/tcp_wmem')
tcp_wmem = output.split()
wbuf.text = "%s,%s,%s" % (tcp_wmem[0],tcp_wmem[1],tcp_wmem[2])

#Get GridFTP configuration
paths = ('/etc/grid-security/gridftp.conf', '$GLOBUS_LOCATION/etc/gridftp.conf')

dt_service = ET.SubElement(dtnNode, "DataTransferService")
ET.SubElement(dt_service, "Service_type").text = "GridFTP"

for path in paths:
    output,err = run_command("cat "+path)
    if (len(output)==0):
        continue
    lines = output.split("\n")
    for line in lines:
        if(len(line) == 0):
            continue
        if (line[0] == "#"):
            continue
        parts = line.split()
        if (parts[0] == 'port'):
            ET.SubElement(dt_service, "Port").text = parts[1]
            continue
        if (parts[0] == '$GLOBUS_TCP_PORT_RAGE'):
            ET.SubElement(dt_service, "Port_range").text = parts[1]
            continue
        ET.SubElement(dt_service, parts[0]).text = parts[1]

#Get File System Information
storage = ET.SubElement(dtnConfig, "Storage")

discardfs = ('none','tmpfs','devtmpfs','udev')

output,err = run_command("df -kPT")
if(len(output)!=0):
    lines = output.split("\n")[1:]
    for line in lines:
        if (len(line) == 0):
            continue
        parts = line.split()
        if parts[0] in discardfs:
            continue
        fs = ET.SubElement(storage, "File_System")
        ET.SubElement(fs,"Device").text = parts[0]
        ET.SubElement(fs,"Type").text = parts[1]
        ET.SubElement(fs,"Capacity_kB").text = parts[2]
        ET.SubElement(fs,"Available_kB").text = parts[4]
        ET.SubElement(fs,"Mount_point").text = parts[6]

# Get active data transfers information
active_transfers = ET.SubElement(dtnConfig, "Active_transfers")
transfer_counter = 0

discardusers = ('root','gridftp')

output,err = run_pipe_cmd('ps aux','grep globus-gridftp-server')
if(len(output)!=0):
    lines = output.split("\n")
    for line in lines:
        if (len(line)==0):
            continue
        parts = line.split()
        if parts[0] in discardusers:
            continue
        if (parts[10] == 'grep'):
            continue
        transfer_counter = transfer_counter + 1

active_transfers.text = str(transfer_counter)

# Get cpu usage information
cpu_usage = ET.SubElement(dtnConfig, "CPU_usage")

output,err = run_pipe_cmd('top -b -n 1','grep Cpu')
if(len(output)!=0):
    parts = output.split()
    us = parts[1].split("%")[0]
    sy = parts[3].split("%")[0]
    usage = float(us)+float(sy)
    cpu_usage.text = str('%0.2f'%usage)

# print prettify(dtnConfig)
# wd = os.getcwd()
outfile = open("/tmp/dtn-"+str(public_ip.text)+".xml", "w")
# outfile.write(prettify(dtnConfig))   
outfile.write(ET.tostring(dtnConfig, 'utf-8'))
outfile.close()

exit()
