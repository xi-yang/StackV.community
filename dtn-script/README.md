Data Transfer Node Script:

(1) The script collects information from system including: 

    Hardware(CPU, memory, disk, NIC) information
    GridFTP service configuration
    Status of memory usage and active transfers

(2) To Run the script every minute on each node, enter "$crontab -e" to edit cron job as:

    * * * * * . $HOME/.bash_profile;/path/to/get.py

The script have been tested on Linux 2.62 and 3.13.
