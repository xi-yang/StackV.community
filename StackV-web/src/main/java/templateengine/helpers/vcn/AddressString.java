package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class AddressString implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        String name = (String) obj.get(0);
        JSONArray interfaces = (JSONArray) obj.get(1);
        String uuid = (String) obj.get(2);

//        var addressMRL = '';
        String addressMRL = "";
//        interfaces.forEach(function(i) {
        for (int i = 0; i < interfaces.size(); i++) {        
            JSONObject inter = (JSONObject) interfaces.get(i);
            
//            var addressString = undefined;
            String addressString = null;
        
//            if (i.type.toUpperCase() === 'ETHERNET' && i.hasOwnProperty('address')) {
            if (((String) inter.get("type")).toUpperCase().equals("ETHERNET") && inter.containsKey("address")) {
//                addressString = i.address;
//                addressString = addressString.includes('ipv') ? addressString.substring(addressString.indexOf('ipv') + 5) : addresString;
//                addressString = addressString.includes('/') ? addressString.substring(0, addressString.indexOf('/')) : addressString;
                addressString = (String) inter.get("address");
                addressString = addressString.contains("ipv") ? addressString.substring(addressString.indexOf("ipv") + 5) : addressString;
                addressString = addressString.contains("/") ? addressString.substring(0, addressString.indexOf("/")) : addressString;                
//            }
            }
//            if (addressString) {
            if (addressString != null) {
//                addressMRL += '    mrs:hasNetworkAddress    ' + '&lt;urn:ogf:network:service+' + refUuid + ':resource+virtual_machines:tag+' + name + ':eth0:floating&gt;.\n\n' +
//                    '&lt;urn:ogf:network:service+' + refUuid + ':resource+virtual_machines:tag+' + name + ':eth0:floating&gt;\n' +
//                    '    a        mrs:NetworkAddress;\n    mrs:type   "floating-ip";\n' + '    mrs:value       "' + addressString + '" .\n\n';
                addressMRL += "    mrs:hasNetworkAddress    " + "&lt;urn:ogf:network:service+" + uuid + ":resource+virtual_machines:tag+" + name + ":eth0:floating&gt;.\n\n" +
                    "&lt;urn:ogf:network:service+" + uuid + ":resource+virtual_machines:tag+" + name + ":eth0:floating&gt;\n" +
                    "    a        mrs:NetworkAddress;\n    mrs:type   'floating-ip';\n" + "    mrs:value       '" + addressString + "' .\n\n";
//            } else {
            } else {
//                addressMRL = ' .\n\n';
                addressMRL = " .\n\n";
//            }
            }
//        });
        }
//        return new handlebars.SafeString(addressMRL);
        return addressMRL;
    }
}
