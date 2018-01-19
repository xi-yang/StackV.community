package templateengine.helpers.dnc;

import java.util.ArrayList;
import templateengine.helpers.Helper;

public class DNCType implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        String type = (String) obj.get(0);
        switch (type) {
            case "Multi-Path_P2P_VLAN":
                return "MCE_MPVlanConnection";
            case "Multi-Point_VLAN_Bridge":
                return "MCE_MultiPointVlanBridge";
        }
        return "";
    }    
}
