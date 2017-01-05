### Overview
NG-PCE (Next Generation Path Computation Element) is a branch of the StackV software. This is dedicated as a solution for the Survivability of Cyber-Infrastructure Backbone Networks Against WMD Attacks (HDTRA1-13-C-0027) project. It works as an intra- and/or inter-domain PCE that computes survivable paths based on both proactive and reactive schemes. 

It prototypes a testbed network controller for provisioning and protecting network services (connections) to survive massive diasasters such as WMD attacks. This software facilitates a testbed for experimenting vairous disaster scenarios and countering solutions.

### Key Components
The NG-PCE leverages the core functions of StackV for semantic model based operations, multi-domain integration and Model Computation Element (MCE) workflow. It added the following components for the specific missions of the above project.

    1. ONOS System Driver with Shared Resource Risk Group (SRRG) annotation

    2. Survivable OpenFlow Model Computation Element (MCE)

    3. Shared Resource Risk Group (SRRG) modeling logic

    4. SSRG based resource disjoint protection (proactive) and fast restoration (reactive) path compuation algorithms

### A Step-by-Step Guide to Installation and Deployment
https://github.com/MAX-UMD/StackV.community/wiki#deployment-guide-for-centos-7
