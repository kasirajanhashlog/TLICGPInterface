package com.claystone.server.util;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final SessionFactory sessionFactory;

    static {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            sessionFactory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
    	   try
           {
         	  if(!sessionFactory.getCurrentSession().isOpen())
         	  {
         		 sessionFactory.getCurrentSession().close();
         		 sessionFactory.openSession();
         	  }
           }
           catch(SessionException Se)
           {
       		 sessionFactory.openSession();
           }
    	return sessionFactory;
    }

    public static Session beginTransaction()
    {
          Session lSession = getSessionFactory().getCurrentSession();
          try
          {
        	  lSession.beginTransaction();
          }
          catch(SessionException Se)
          {
        	  lSession = getSessionFactory().openSession();
          }
          if(!lSession.isConnected())
          {
                lSession.close();
                lSession = getSessionFactory().openSession();
                lSession.beginTransaction();                
          }          
          return lSession;
    }

    
    public static void commit()
    {
		if(getSessionFactory().getCurrentSession().getTransaction().isActive())
			getSessionFactory().getCurrentSession().getTransaction().commit();			
    }    
    
    public static void rollback()
    {
		if(getSessionFactory().getCurrentSession().getTransaction().isActive())
			getSessionFactory().getCurrentSession().getTransaction().rollback();			
    }    
    public static void close(){
    	if(getSessionFactory().getCurrentSession().isOpen())
			getSessionFactory().getCurrentSession().close();
    }
    public static <T> ArrayList<T> castList(Class<? extends T> clazz,
			Collection<?> c) {
		ArrayList<T> r = new ArrayList<T>(c.size());
		for (Object o : c)
			r.add(clazz.cast(o));
		return r;
	}
}