export default [
    {
        "urn:ogf:network:sdn.maxgigapop.net:network": {
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "1761-1769,1925-1929"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=MCLN:port=1-3-1:link=al2s": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "max-dragon-6"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MCLN:port=1-3-1:link=al2s:vlan-range"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-9:link=*": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "max-dragon-2"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-9:link=*:vlan-range"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-9:link=*:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "2-4094"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-8:link=*:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "2-4094"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=13/1:link=al2s:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "3700-3749"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-8:link=*": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "max-dragon-1"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-8:link=*:vlan-range"
                    }
                ]
            },
            "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxdragon:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "1700-1719"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching": {
                "http://schemas.ogf.org/nml/2013/03/base#encoding": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#SwitchingService"
                    },
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MCLN:port=1-3-1:link=al2s"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-9:link=*"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK-DYNES:port=1-1-12:link=*"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-8:link=*"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#labelSwapping": [
                    {
                        "type": "literal",
                        "value": "false"
                    }
                ]
            },
            "urn:ogf:network:domain=wix.internet2.edu:node=sdx": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "WIX SDX OSCARS"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:sdn.maxgigapop.net:network"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Node"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sdx:l2switching"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=13/1:link=al2s"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxdragon"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxaws"
                    }
                ]
            },
            "urn:ogf:network:domain=wix.internet2.edu:node=sdx:l2switching": {
                "http://schemas.ogf.org/nml/2013/03/base#encoding": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#SwitchingService"
                    },
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#labelSwapping": [
                    {
                        "type": "literal",
                        "value": "false"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=13/1:link=al2s"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxdragon"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxaws"
                    }
                ]
            },
            "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxaws:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "1700-1719,3700-3749"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxaws": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "wix-to-aws-dc"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sdx"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sdx:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxaws:vlan-range"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:directconnect+dxcon-fgfzhk4z"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "max-dragon-4"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*:vlan-range"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=MCLN:port=1-3-1:link=al2s:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "3020-3029"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK-DYNES:port=1-1-12:link=*": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "max-dragon-3"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK-DYNES:port=1-1-12:link=*:vlan-range"
                    }
                ]
            },
            "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON": {
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:sdn.maxgigapop.net:network"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "DRAGON OSCARS DCN"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Node"
                    },
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MCLN:port=1-3-1:link=al2s"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-9:link=*"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK-DYNES:port=1-1-12:link=*"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MAX:port=1-0-8:link=*"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK-DYNES:port=1-1-12:link=*:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "2-4094"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "max-dragon-5"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*:vlan-range"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:openstack.com:openstack-cloud:fake-l2switch:port-ext"
                    }
                ]
            },
            "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=13/1:link=al2s": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "wix-to-i2-al2s"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sdx"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sdx:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=13/1:link=al2s:vlan-range"
                    }
                ]
            },
            "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxdragon": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "wix-to-max-dragon"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#belongsTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sdx"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sdx:l2switching"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sw.net.wix.internet2.edu:port=15/2:link=maxdragon:vlan-range"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=dragon.maxgigapop.net:node=MCLN:port=1-3-1:link=wix"
                    }
                ]
            },
            "urn:ogf:network:sdn.maxgigapop.net:network": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Topology"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasNode": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:domain=wix.internet2.edu:node=sdx"
                    }
                ]
            },
            "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*:vlan-range": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "1700-1719,1761-1769,1925-1929,3020-3049"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                    },
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            }
        },
        "urn:ogf:network:aws.amazon.com:aws-cloud": {
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:tunnel1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-tunnel"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:tunnel1-ip"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:eth0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:eth0:ip+10.0.0.91"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-6b5ab202": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-gateway"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:volume+root": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Volume"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "standard"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#target_device": [
                    {
                        "type": "literal",
                        "value": "/dev/xvda"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#disk_gb": [
                    {
                        "type": "literal",
                        "value": "8"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingtable+rtb-576c582f:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:eth0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:eth0:ip+10.0.0.148"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:eth0:ip+10.0.0.49"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:tunnel1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-tunnel"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:tunnel1-ip"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    },
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-connection"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-f7866d9e"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:routes"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:customer-gateway+cgw-b0e803d9"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:tunnel2"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:tunnel1"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable+rtb-1642d46e:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable+rtb-1642d46e:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providedByService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:ec2service+us-east-1"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Node"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "instance+m4.large"
                    },
                    {
                        "type": "literal",
                        "value": "image+ami-0d1bf860"
                    },
                    {
                        "type": "literal",
                        "value": "keypair+driver_key"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasVolume": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:volume+root"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:public-ip+52.86.31.49"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:fqdn"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:eth0"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:customer-gateway+cgw-8b9378e2": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:customer"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "206.196.179.151"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpcservice+us-east-1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#VirtualCloudService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesVPC": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:labelgroup+1764": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "1764"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.10.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.10.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-f7866d9e"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:tunnel2": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-tunnel"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:tunnel2-ip"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-vpngw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-gateway"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:switchingservice": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-f7866d9e": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-gateway"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:fqdn": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "fqdn"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "ec2-54-162-49-201.compute-1.amazonaws.com"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-igw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "internet-gateway"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:s3service+us-east-1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#ObjectStorageService"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:tunnel2": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-tunnel"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:tunnel2-ip"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Topology"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasNode": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:switchingservice"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingservice"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-f7866d9e"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Topology"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasNode": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:switchingservice"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingservice"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingservice": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoutingTable": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.10.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-192.168.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:public-ip+54.173.86.34": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:public"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "54.173.86.34"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:eth0:ip+10.0.0.49": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:private"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.49"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable+rtb-1642d46e:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:public-ip+54.236.44.91": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:public"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "54.236.44.91"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765:asn": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "bgp-asn"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "65000"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:switchingservice": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providesSubnet": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Topology"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasNode": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingservice"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:switchingservice"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:volume+root": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Volume"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "gp2"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#target_device": [
                    {
                        "type": "literal",
                        "value": "/dev/xvda"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#disk_gb": [
                    {
                        "type": "literal",
                        "value": "8"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providedByService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:ec2service+us-east-1"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Node"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "instance+m4.large"
                    },
                    {
                        "type": "literal",
                        "value": "image+ami-146e2a7c"
                    },
                    {
                        "type": "literal",
                        "value": "keypair+driver_key"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasVolume": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:volume+root"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:fqdn"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:public-ip+54.173.86.34"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:eth0"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-192.168.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "192.168.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:routes": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list:customer"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "192.168.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-vpngw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-gateway"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingservice": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoutingTable": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:volume+root": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Volume"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "standard"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#target_device": [
                    {
                        "type": "literal",
                        "value": "/dev/xvda"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#disk_gb": [
                    {
                        "type": "literal",
                        "value": "8"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "206.196.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:public-ip+54.162.49.201": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:public"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "54.162.49.201"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:switchingservice": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providesSubnet": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:fqdn": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "fqdn"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "ec2-34-207-155-11.compute-1.amazonaws.com"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:eth0:ip+10.0.0.148": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:private"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.148"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "dxvif-fgkdmi06"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "direct-connect-vif"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765:labelgroup+1765"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765:asn"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765:customer_ip"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765:amazon_ip"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "direct-connect-vif+private"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingSubnet"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:switchingservice": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providesSubnet": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:tunnel1-ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:amazon"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "34.230.229.145"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:eth0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:eth0:ip+10.0.0.172"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingservice": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoutingTable": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable+rtb-1642d46e"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable+rtb-1642d46e:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.10.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-192.168.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:public-ip+52.86.31.49": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:public"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "52.86.31.49"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-vpngw"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-vpngw"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providedByService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:ec2service+us-east-1"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Node"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "instance+m4.large"
                    },
                    {
                        "type": "literal",
                        "value": "image+ami-146e2a7c"
                    },
                    {
                        "type": "literal",
                        "value": "keypair+driver_key"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasVolume": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:volume+root"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:fqdn"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:public-ip+54.162.49.201"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:eth0"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:routes": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list:customer"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.10.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-igw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "internet-gateway"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Topology"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasTopology": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-b08a61d9"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-f7866d9e"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-6b5ab202"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:ebsservice+us-east-1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:ec2service+us-east-1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:s3service+us-east-1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpcservice+us-east-1"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765:labelgroup+1765": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#values": [
                    {
                        "type": "literal",
                        "value": "1765"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#LabelGroup"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:fqdn": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "fqdn"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "ec2-54-236-44-91.compute-1.amazonaws.com"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providedByService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:ec2service+us-east-1"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Node"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "instance+m4.large"
                    },
                    {
                        "type": "literal",
                        "value": "image+ami-146e2a7c"
                    },
                    {
                        "type": "literal",
                        "value": "keypair+driver_key"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasVolume": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:volume+root"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:public-ip+54.236.44.91"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:fqdn"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:eth0"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:amazon_ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:amazon"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.10.0.2/24"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:customer_ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:customer"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.10.0.1/24"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:asn": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "bgp-asn"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "65000"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:tunnel2-ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:amazon"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "34.226.37.152"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-connection"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-b08a61d9"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:routes"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:customer-gateway+cgw-8b9378e2"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:tunnel2"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:tunnel1"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-192.168.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-192.168.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-vpngw"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:public-ip+34.207.155.11": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:public"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "34.207.155.11"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765:amazon_ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:amazon"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.10.0.2/24"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:cidr": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "206.196.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingservice": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoutingTable": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingtable+rtb-576c582f"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingtable+rtb-576c582f:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:label+1764": {
                "http://schemas.ogf.org/nml/2013/03/base#labeltype": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2012/10/ethernet#vlan"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Label"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#value": [
                    {
                        "type": "literal",
                        "value": "1764"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:customer-gateway+cgw-b0e803d9": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:customer"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "206.196.179.155"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:fqdn": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "fqdn"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "ec2-54-173-86-34.compute-1.amazonaws.com"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/24"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    },
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingtable+rtb-576c582f": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingtable+rtb-576c582f:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:eth0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:eth0:ip+10.0.0.222"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:tunnel1-ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:amazon"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "34.225.65.57"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-vpngw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-gateway"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-vpngw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-gateway"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:tunnel2": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-tunnel"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:tunnel2-ip"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:tunnel1-ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:amazon"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "34.197.109.192"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:switchingservice": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providesSubnet": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:ec2service+us-east-1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#HypervisorService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesVM": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:volume+root": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Volume"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "gp2"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#target_device": [
                    {
                        "type": "literal",
                        "value": "/dev/xvda"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#disk_gb": [
                    {
                        "type": "literal",
                        "value": "8"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-igw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "internet-gateway"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:eth0:ip+10.0.0.172": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:private"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.172"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:switchingservice": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providesSubnet": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingService"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:eth0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:eth0:ip+10.0.0.196"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-vpngw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-gateway"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-igw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "internet-gateway"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Topology"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasNode": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:switchingservice"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingservice"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:fqdn": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "fqdn"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "ec2-52-86-31-49.compute-1.amazonaws.com"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764": {
                "http://schemas.ogf.org/nml/2013/03/base#name": [
                    {
                        "type": "literal",
                        "value": "dxvif-ffv1ibzc"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "direct-connect-vif"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabelGroup": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:labelgroup+1764"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasLabel": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:label+1764"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-vpngw"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:amazon_ip"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:customer_ip"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1764:asn"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "direct-connect-vif+private"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:tunnel1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-tunnel"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:tunnel1-ip"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable+rtb-1642d46e": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable+rtb-1642d46e:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:eth0:ip+10.0.0.222": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:private"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.222"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:customer-gateway+cgw-b4648cdd": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:customer"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "206.196.179.134"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    },
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/24"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:routes": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list:customer"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "192.168.0.0/24"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-connection"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-vpngw"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:routes"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:customer-gateway+cgw-b4648cdd"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:tunnel1"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-4ffaea2e:tunnel2"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Topology"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:cidr"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingservice"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:switchingservice"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingSubnet"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:eth0"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "0.0.0.0/0"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingTable"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/24"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-vpngw"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-vpngw"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingSubnet"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:eth0"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/24"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:ebsservice+us-east-1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#BlockStorageService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesVolume": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:volume+root"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:volume+root"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:volume+root"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:volume+root"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:volume+root"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.10.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix-list"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.10.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingSubnet"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_machines:tag+ec2-vpc1-vm1:eth0"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingservice": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoutingTable": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00"
                    }
                ]
            },
            "urn:ogf:network:service+2df5b940-7a55-4994-8cfb-c31a6cbd7b8b:resource+virtual_clouds:tag+vpc1:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae:tunnel2-ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:amazon"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "34.231.13.111"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:directconnect+us-east-1:vlanport+1765:customer_ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:customer"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.10.0.1/24"
                    }
                ]
            },
            "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1": {
                "http://schemas.ogf.org/mrs/2013/12/topology#providedByService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:ec2service+us-east-1"
                    }
                ],
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Node"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "instance+m4.large"
                    },
                    {
                        "type": "literal",
                        "value": "image+ami-0d1bf860"
                    },
                    {
                        "type": "literal",
                        "value": "keypair+driver_key"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasVolume": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:volume+root"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:fqdn"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:public-ip+34.207.155.11"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+c5c3c3fa-ca0e-4d2c-a879-12afef7d3e43:resource+virtual_machines:tag+vm1:eth0"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-igw": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "internet-gateway"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-206.196.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016:routeto": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/16"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-3b77645a:tunnel2-ip": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-address:amazon"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "52.4.34.229"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4-prefix"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.0/24"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:gateway+vgw-b08a61d9": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#BidirectionalPort"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "vpn-gateway"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#isAlias": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpn+vpn-cf4b58ae"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:eth0:ip+10.0.0.196": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:private"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.196"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingservice": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#RoutingService"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoutingTable": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#providesRoute": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-10.0.0.016"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-main:route-0.0.0.00"
                    }
                ]
            },
            "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/nml/2013/03/base#Topology"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasService": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:switchingservice"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1:routingservice"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-igw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+0f9107d7-5619-410c-99e2-1aa208031c55:resource+virtual_clouds:tag+vpc1-vpngw"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1:routingtable-subnet0:route-0.0.0.00:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeFrom": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-subnet0"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-vpngw"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-igw"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:eth0:ip+10.0.0.91": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#NetworkAddress"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "ipv4:private"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#value": [
                    {
                        "type": "literal",
                        "value": "10.0.0.91"
                    }
                ]
            },
            "urn:ogf:network:service+7a70139d-c24d-41b0-87f8-30c277dea3eb:resource+virtual_machines:tag+VM_1:volume+root": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Volume"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#type": [
                    {
                        "type": "literal",
                        "value": "standard"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#target_device": [
                    {
                        "type": "literal",
                        "value": "/dev/xvda"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#disk_gb": [
                    {
                        "type": "literal",
                        "value": "8"
                    }
                ]
            },
            "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-subnet0": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#SwitchingSubnet"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#hasNetworkAddress": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_clouds:tag+vpc1-subnet0:networkaddress"
                    }
                ],
                "http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_1:eth0"
                    },
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:service+56eed732-a3cf-497c-a94e-9218fa1894e3:resource+virtual_machines:tag+VM_2:eth0"
                    }
                ]
            },
            "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingtable+rtb-576c582f:route-10.0.0.016": {
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": [
                    {
                        "type": "uri",
                        "value": "http://schemas.ogf.org/mrs/2013/12/topology#Route"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#routeTo": [
                    {
                        "type": "uri",
                        "value": "urn:ogf:network:aws.amazon.com:aws-cloud:vpc+vpc-1bc9ad62:routingtable+rtb-576c582f:route-10.0.0.016:routeto"
                    }
                ],
                "http://schemas.ogf.org/mrs/2013/12/topology#nextHop": [
                    {
                        "type": "literal",
                        "value": "local"
                    }
                ]
            }
        }
    }
];