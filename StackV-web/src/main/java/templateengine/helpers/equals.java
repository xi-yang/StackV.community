package templateengine.helpers;

import java.util.ArrayList;

public class equals implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        Object obj1 = obj.get(0);
        Object obj2 = obj.get(1);
        boolean eval;
        if (obj1 instanceof String) {
            eval = ((String) obj1).equals((String) obj2);
        } else {
            eval = obj1.equals(obj2);
        }

        return Boolean.toString(eval);
    }
}
