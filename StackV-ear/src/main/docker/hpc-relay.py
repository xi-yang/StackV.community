#!/usr/bin/python

import sys, socket, re
from shell_utils import InvalidEnvironmentStateError, CommandLineParamMap, shell

def main(argv):
    argumentMap = CommandLineParamMap(
        required=["sockpath", "cmd"],
        optional=["recv-delim"]
        ).parse(argv, prefix="--", validSymbols="+-./$_{}[]();`%")
    if len(argumentMap) == 0:
        raise ValueError("No arguments found")

    sockpath = argumentMap.getArgument("sockpath", "No --sockpath argument found; a shared UNIX socket path must be given")
    cmdArray = argumentMap.getArgument("cmd", "No --cmd argument found; missing command to relay to host", cardinality='*')
    recvDelimPattern = argumentMap.getOptionalArgument("recv-delim", r"{\[]}[\r]{0,1}\n")

    # Prepare command and append delimiter
    if isinstance(cmdArray, basestring):
        cmd = cmdArray
    else:
        cmd = ''
        for cmdFrag in cmdArray:
            cmd += cmdFrag + ' '
    cmd += '\n'

    try:
        sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        sock.connect(sockpath)
        sock.send(cmd)
        
        recvBuffer = ''
        tokenre = re.compile(recvDelimPattern)
        while True:
            data = sock.recv(1024)
            if data:
                match = re.search(tokenre, data)
                if match:
                    cutoff = match.span()[0] - 1
                    recvBuffer += data[:cutoff]
                    break
                else:
                    recvBuffer += data
            else:
                break
        
        print recvBuffer
    except Exception:
        raise
    finally:
        sock.close()

if __name__ == "__main__":
    exitCode = 0
    try:
        main(sys.argv)
    except OSError as e:
        exitCode = 10
        print "Fatal error: {}".format(e)
    except ValueError as e:
        exitCode = 1
        print "Error: {}".format(e)