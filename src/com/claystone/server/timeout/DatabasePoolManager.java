package com.claystone.server.timeout;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.postgresql.ds.PGPoolingDataSource;

public class DatabasePoolManager {

	private ThreadPoolExecutor threadPool;
	
	private int poolSize;
	 
	private int maxPoolSize;
 
	private long keepAliveTime;
    
    private static PGPoolingDataSource  source;    
    
    private static DataSource  dataSource;

	private LinkedBlockingQueue<Runnable> queue;
	
	private static DatabasePoolManager databasePoolManager;
	private Properties properties = new Properties();
    private Logger log = Logger.getLogger(DatabasePoolManager.class);

	
	public static DatabasePoolManager getDatabasePoolManager()
	{
		
		
		if(databasePoolManager == null)
		{
			databasePoolManager = new DatabasePoolManager(); 
		}
		return databasePoolManager;
	}
	
	public int GetQueueLength()
	{
		return databasePoolManager.queue.size();
	}
	
	private DatabasePoolManager()
	
	{
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
		} catch (IOException e1) {
			log.error(e1.getMessage());
		}
		poolSize = Integer.parseInt(properties.getProperty("PoolSizeDB"));
		 
	    maxPoolSize = Integer.parseInt(properties.getProperty("MaxPoolSizeDB"));
	 
	   keepAliveTime = Integer.parseInt(properties.getProperty("KeepAliveTimeDB"));
		queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
                keepAliveTime, TimeUnit.SECONDS, queue);
		
        source = new PGPoolingDataSource();
        try {
			source.setDatabaseName(properties.getProperty("DatabaseName"));
			source.setServerName(properties.getProperty("ServerName"));
	        source.setUser(properties.getProperty("User"));
	        source.setPassword(properties.getProperty("Password"));
	        source.setDatabaseName("vTrack");
//	        source.setDatabaseName("vTrack-restore1");
	        source.setPortNumber(Integer.parseInt(properties.getProperty("PortNumber")));
	        int IntitialConnection = Integer.parseInt(properties.getProperty("InitialConnections"));
	        source.setInitialConnections(IntitialConnection);
	        int MaximumConnections = Integer.parseInt(properties.getProperty("MaxConnections"));
	        source.setMaxConnections(MaximumConnections);
	        source.initialize();
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
	}
	
	private DatabasePoolManager(short test)
	{
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
		} catch (IOException e1) {
			log.error(e1.getMessage());
		}
		poolSize = Integer.parseInt(properties.getProperty("PoolSize"));
		 
	    maxPoolSize = Integer.parseInt(properties.getProperty("MaxPoolSize"));
	 
	   keepAliveTime = Integer.parseInt(properties.getProperty("KeepAliveTime"));
		queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
                keepAliveTime, TimeUnit.SECONDS, queue);
		
        try {
        	java.lang.Class.forName("org.postgresql.Driver");
        	dataSource = setupDataSource(properties.getProperty("bambooConnectionURL"),poolSize,maxPoolSize,
        							properties.getProperty("User"),properties.getProperty("Password"));
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
	}

	public void StopPool()
	{
		threadPool.shutdown();
	}
	
	public void ProcessUnitData(Runnable task)
	{
		threadPool.execute(task);
	}
	
	public static Connection getConnection() throws SQLException
	{
		return source.getConnection();
	}

	public static Connection getConnection(short test) throws SQLException
	{
		return dataSource.getConnection();
	}
	
	 public static DataSource setupDataSource(String connectURI,int minPool, int maxPool,
			 								  String username, String password) throws Exception {
			        //
			        // First, we'll need a ObjectPool that serves as the
			        // actual pool of connections.
			        //
			        // We'll use a GenericObjectPool instance, although
			        // any ObjectPool implementation will suffice.
			        //
			        GenericObjectPool connectionPool = new GenericObjectPool(null);

			        connectionPool.setMinIdle(minPool);
			        connectionPool.setMaxActive( maxPool );
			        

			        //
			        // Next, we'll create a ConnectionFactory that the
			        // pool will use to create Connections.
			        // We'll use the DriverManagerConnectionFactory,
			        // using the connect string from configuration
			        //
			        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI,username, password);

			        //
			        // Now we'll create the PoolableConnectionFactory, which wraps
			        // the "real" Connections created by the ConnectionFactory with
			        // the classes that implement the pooling functionality.
			        //
			        PoolableConnectionFactory poolableConnectionFactory =
			                new PoolableConnectionFactory(
			        	connectionFactory,connectionPool,null,null,false,false);

			        PoolingDataSource dataSource = 
			        	new PoolingDataSource(connectionPool);

			        return dataSource;
			    }

}


