package templateengine.helpers;

import java.util.ArrayList;

public class add implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        Object obj1 = obj.get(0);
        Object obj2 = obj.get(1);
        if (obj1 instanceof String) {
            return Integer.toString(Integer.parseInt((String) obj1) + Integer.parseInt((String) obj2));
        } else {
            return "-1";
        }
    }
}
