/*
 * Copyright (c) 2013-2016 University of Maryland
 * Modified by: Antonio Heard 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

"use strict";
define([], function () {
    /**These are the strings used in the model**/
    return{
        type: "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
        hasBidirectionalPort: "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort",
        isAlias: "http://schemas.ogf.org/nml/2013/03/base#isAlias",
        namedIndividual: "http://www.w3.org/2002/07/owl#NamedIndividual",
        topology: "http://schemas.ogf.org/nml/2013/03/base#Topology",
        FileSystem: "http://schemas.ogf.org/mrs/2013/12/topology#FileSystem",
        node: "http://schemas.ogf.org/nml/2013/03/base#Node",
        bidirectionalPort: "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort",
        labelGroup: "http://schemas.ogf.org/nml/2013/03/base#LabelGroup",
        label: "http://schemas.ogf.org/nml/2013/03/base#Label",
        hasLabelGroup: "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup",
        hasLabel: "http://schemas.ogf.org/nml/2013/03/base#hasLabel",
        hasNode: "http://schemas.ogf.org/nml/2013/03/base#hasNode",
        hasService: "http://schemas.ogf.org/nml/2013/03/base#hasService",
        hasTopology: "http://schemas.ogf.org/nml/2013/03/base#hasTopology",
        hasFileSystem: "http://schemas.ogf.org/mrs/2013/12/topology#hasFileSystem",
        networkAdress: "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress",
        bucket: "http://schemas.ogf.org/mrs/2013/12/topology#Bucket",
        switchingSubnet: "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingSubnet",
        hasNetworkAddress: "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress",
        provideByService: "http://schemas.ogf.org/mrs/2013/12/topology#providedByService",
        hasBucket: "http://schemas.ogf.org/mrs/2013/12/topology#hasBucket",
        belongsTo: "http://schemas.ogf.org/nml/2013/03/base#belongsTo",
        name: "http://schemas.ogf.org/nml/2013/03/base#name",
        tag: "http://schemas.ogf.org/mrs/2013/12/topology#Tag",
        route: "http://schemas.ogf.org/mrs/2013/12/topology#Route",
        volume: "http://schemas.ogf.org/mrs/2013/12/topology#Volume",
        routingTable: "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable",
        hasVolume: "http://schemas.ogf.org/mrs/2013/12/topology#hasVolume",
        ontology: "http://www.w3.org/2002/07/owl#Ontology",
        POSIX_IOBenchmark: "http://schemas.ogf.org/mrs/2013/12/topology#POSIX_IOBenchmark",
        address: "http://schemas.ogf.org/mrs/2013/12/topology#Address",
        hasTag: "http://schemas.ogf.org/mrs/2013/12/topology#hasTag",
        topoType: "http://schemas.ogf.org/mrs/2013/12/topology#type",
        measurement: "http://schemas.ogf.org/mrs/2013/12/topology#measurement",
        mount_point: "http://schemas.ogf.org/mrs/2013/12/topology#mount_point",
        memory_mb: "http://schemas.ogf.org/mrs/2013/12/topology#memory_mb",
        num_core: "http://schemas.ogf.org/mrs/2013/12/topology#num_core",
        NetworkObject: "http://schemas.ogf.org/nml/2013/03/base#NetworkObject",
        value: "http://schemas.ogf.org/mrs/2013/12/topology#value",
        active_transfers: "http://schemas.ogf.org/mrs/2013/12/topology#active_transfers",
        hypervisorService: "http://schemas.ogf.org/mrs/2013/12/topology#HypervisorService",
        routingService: "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService",
        storageService: "http://schemas.ogf.org/mrs/2013/12/topology#StorageService",
        objectStorageService: "http://schemas.ogf.org/mrs/2013/12/topology#ObjectStorageService",
        blockStorageService: "http://schemas.ogf.org/mrs/2013/12/topology#BlockStorageService",
        IOPerformanceMeasurementService: "http://schemas.ogf.org/mrs/2013/12/topology#IOPerformanceMeasurementService",
        hypervisorBypassInterfaceService: "http://schemas.ogf.org/mrs/2013/12/topology#HypervisorBypassInterfaceService",
        virtualSwitchingService: "http://schemas.ogf.org/mrs/2013/12/topology#VirtualSwitchService",
        switchingService: "http://schemas.ogf.org/nml/2013/03/base#SwitchingService",
        topopolgySwitchingService: "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService",
        virtualCloudService: "http://schemas.ogf.org/mrs/2013/12/topology#VirtualCloudService",
        DataTransferService: "http://schemas.ogf.org/mrs/2013/12/topology#DataTransferService",
        DataTransferClusterService: "http://schemas.ogf.org/mrs/2013/12/topology#DataTransferClusterService",
        providesSubnet: "http://schemas.ogf.org/mrs/2013/12/topology#providesSubnet",
        encoding: "http://schemas.ogf.org/nml/2013/03/base#encoding",
        labelSwapping: "http://schemas.ogf.org/nml/2013/03/base#labelSwapping",
        providesVolume: "http://schemas.ogf.org/mrs/2013/12/topology#providesVolume",
        providesRoutingTable: "http://schemas.ogf.org/mrs/2013/12/topology#providesRoutingTable",
        providesRoute: "http://schemas.ogf.org/mrs/2013/12/topology#providesRoute",
        providesVM: "http://schemas.ogf.org/mrs/2013/12/topology#providesVM",
        providesVPC: "http://schemas.ogf.org/mrs/2013/12/topology#providesVPC",
        providesBucket: "http://schemas.ogf.org/mrs/2013/12/topology#providesBucket",
        spaPolicyData: "http://schemas.ogf.org/mrs/2015/02/spa#PolicyData",
        spaPolicyAction: "http://schemas.ogf.org/mrs/2015/02/spa#PolicyAction",
        spaType: "http://schemas.ogf.org/mrs/2015/02/spa#type",
        spaImportFrom: "http://schemas.ogf.org/mrs/2015/02/spa#importFrom",
        spaExportTo: "http://schemas.ogf.org/mrs/2015/02/spa#exportTo",
        spaDependOn: "http://schemas.ogf.org/mrs/2015/02/spa#dependOn",
        spaValue: "http://schemas.ogf.org/mrs/2015/02/spa#value",
        spaFormat: "http://schemas.ogf.org/mrs/2015/02/spa#format"
    };
});
