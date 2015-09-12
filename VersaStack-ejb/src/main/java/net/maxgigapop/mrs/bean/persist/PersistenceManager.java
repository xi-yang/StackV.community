/*

 * Borrowed from "Overactive Source"
 * Authors: Ezequiel Cuellar <overactive.source@gmail.com>

 */

package net.maxgigapop.mrs.bean.persist;

/**
 *
 * @author xyang
 */
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

//@TODO: change into Singleton with READ and WRITE locks ? 
@SuppressWarnings("unchecked")
public class PersistenceManager {
    protected static EntityManager entityManager = null;
   
    public static EntityManager getEntityManager() {
        return entityManager;
    }
    
    public static void initialize(EntityManager anEntityManager) {
        entityManager = anEntityManager;
    }

    public static <T> T find(Class<T> anEntityClass, Object aPrimaryKey) {
        T theResult = null;
        if (entityManager != null) {
            theResult = entityManager.find(anEntityClass, aPrimaryKey);
        }
        return theResult;
    }

    public static Query createQuery(String aQuery) {
        Query theQuery = null;
        if (entityManager != null) {
            theQuery = entityManager.createQuery(aQuery);
        }
        return theQuery;
    }

    public static void delete(PersistentEntity aPersistentObject) {
        if (entityManager != null) {
            aPersistentObject = merge(aPersistentObject);
            entityManager.remove(aPersistentObject);
            entityManager.flush();
        }
    }

    public static void save(PersistentEntity aPersistentObject) {
        if (entityManager != null) {
            if (aPersistentObject.isPersistent()) {
                aPersistentObject = merge(aPersistentObject);
            }
            entityManager.persist(aPersistentObject);
            entityManager.flush();
        }
    }

    public static PersistentEntity merge(PersistentEntity anEntityClass) {
        PersistentEntity theResult = null;
        if (entityManager != null) {
            if (entityManager.contains(anEntityClass)) {
                theResult = anEntityClass;
            } else {
                theResult = entityManager.merge(anEntityClass);
            }
        }
        return theResult;
    }

    public static void refresh(PersistentEntity aPersistentObject) {
        if (entityManager != null) {
            entityManager.refresh(aPersistentObject);
        }
    }

}
