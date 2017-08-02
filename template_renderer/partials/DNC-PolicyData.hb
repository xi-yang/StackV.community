
{{#connections}}
        "urn:ogf:network:vo1_maxgigapop_net:link={{name}}": {
{{#terminals}}
        "{{{uri}}}":{"vlan_tag":"{{vlan_tag}}"
{{~#if mac_address_list}}
,"mac_list":"{{mac_address_list}}"
{{~/if}}
}
{{~#unless @last}},{{/unless}}
{{/terminals}}
      }
{{~#unless @last}},{{/unless}}
{{/connections}}
