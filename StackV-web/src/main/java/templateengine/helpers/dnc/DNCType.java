package templateengine.helpers.dnc;

import java.util.ArrayList;
import templateengine.helpers.Helper;

public class DNCType implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        String type = (String) obj.get(0);
        switch (type) {
            case "Multi-Path P2P VLAN":
                return "MCE_MPVlanConnection";
            case "Multi-Point VLAN Bridge":
                return "MCE_MultiPointVlanBridge";
        }
        return "";
    }    
}
