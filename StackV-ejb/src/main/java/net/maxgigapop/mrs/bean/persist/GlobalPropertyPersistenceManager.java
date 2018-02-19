/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2013

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

package net.maxgigapop.mrs.bean.persist;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import net.maxgigapop.mrs.bean.GlobalProperty;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;
import net.maxgigapop.mrs.common.StackLogger;

/*
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class GlobalPropertyPersistenceManager extends PersistenceManager {
    
    private static final StackLogger logger = new StackLogger(GlobalPropertyPersistenceManager.class.getName(), "GlobalPropertyPersistenceManager");

    public static GlobalProperty findById(Long id) {
        return PersistenceManager.find(GlobalProperty.class, id);
    }

    public static GlobalProperty findByProperty(String property) {
        try {
            Query q = createQuery(String.format("FROM %s WHERE property='%s'", GlobalProperty.class.getSimpleName(), property));
            List<GlobalProperty> list = (List<GlobalProperty>) q.getResultList();
            if (list == null || list.isEmpty()) {
                return null;
            }
            return list.get(0);
        } catch (Exception e) {
            if (e.getMessage().contains("No entity found")) {
                logger.warning("findByProperty", "no property:"+property);
                return null;
            }
            throw logger.error_throwing("findByProperty", e.getMessage());
        }
    }

    public static String getProperty(String property) {
        GlobalProperty gp = findByProperty(property);
        if (gp == null) {
            return null;
        }
        return gp.getValue();
    }

    public static void setProperty(String property, String value) {
        GlobalProperty gp = findByProperty(property);
        if (gp != null) {
            if (!value.equals(gp.getValue())) {
                gp.setValue(value);
                save(gp);
            }
        } else {
            gp = new GlobalProperty();
            gp.setProperty(property);
            gp.setValue(value);
            save(gp);
        }
    }

    public static void deleteProperty(String property) {
        GlobalProperty gp = findByProperty(property);
        if (gp != null) {
            delete(gp);
        }
    }
}
