package com.claystone.server.timeout;


import org.apache.log4j.Logger;

public class Timeout {

	TimerThread upDB ;
	Thread t;
	TimerThreadCompany upDB1 ;
	Thread th;
	TimerThreadEFCON upDBEF ;
	Thread EF;
	TimerThreadTrimple upDBTM ;
	Thread TM;
	TimerThreadENMOVIL upDBENMO ;
	Thread ENMO;
	TimerThreadATIC upDBATIC ;
	Thread ATIC;
	
	TimerThreadFleetRadar upDBFleetRadar ;
	Thread FleetRadar;
	
	TimerThreadFastTag upDBFastTag ;
	Thread FastTag;
	
	

	private Logger log = null; 

	public static void main(String args[])
	{

		
		Timeout timeout = new Timeout();
		 timeout.StartTimeoutProcess();
		 timeout.StartTimeoutProcessCompany();
		 timeout.StartTimeoutProcessEFCON();
		 timeout.StartTimeoutProcessTrimple();
		 timeout.StartTimeoutProcessENMOVIL();
		 timeout.StartTimeoutProcessATIC();//This is for ATIC's new api but existing one is in StartTimeoutProcess
		 timeout.StartTimeoutProcessFLEETRADAR();
		 timeout.StartTimeoutProcessFastTag();//FastTag(TMS company vehicles...ARV,PPL)
		
		
	}

	
	public void StartTimeoutProcess()
	{
		log = Logger.getLogger(Timeout.class);
		log.info("Timer server started");

		upDB = new TimerThread(null);
		t = new Thread(upDB);
		t.start();
		try
		{
			t.join();
		}
		catch(InterruptedException e)
		{log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void StartTimeoutProcessCompany()
	{
		log = Logger.getLogger(Timeout.class);
		log.info("Timer server started");

		upDB1 = new TimerThreadCompany(null);
		th = new Thread(upDB1);
		th.start();
		try
		{
			th.join();
		}
		catch(InterruptedException e)
		{log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void StartTimeoutProcessEFCON()
	{
		log = Logger.getLogger(Timeout.class);
		log.info("Timer server started");

		upDBEF = new TimerThreadEFCON(null);
		EF = new Thread(upDBEF);
		EF.start();
		try
		{
			EF.join();
		}
		catch(InterruptedException e)
		{log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	public void StartTimeoutProcessTrimple()
	{
		log = Logger.getLogger(Timeout.class);
		log.info("Trimple Timer server started");

		upDBTM = new TimerThreadTrimple(null);
		TM = new Thread(upDBTM);
		TM.start();
		try
		{
			TM.join();
		}
		catch(InterruptedException e)
		{log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void StartTimeoutProcessENMOVIL() {
		log = Logger.getLogger(Timeout.class);
		log.info("Timer server ENMOVIL started");

		upDBENMO = new TimerThreadENMOVIL(null);
		ENMO = new Thread(upDBENMO);
		ENMO.start();
		try
		{
			ENMO.join();
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
			log.error(e.getMessage());
		}
		
	}
	public void StartTimeoutProcessATIC()
	{
		log = Logger.getLogger(Timeout.class);
		log.info("Timer server ATIC started");

		upDBATIC = new TimerThreadATIC(null);
		ATIC = new Thread(upDBATIC);
		ATIC.start();
		try
		{
			ATIC.join();
		}
		catch(InterruptedException e)
		{
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	public void StartTimeoutProcessFLEETRADAR()
	{
		log = Logger.getLogger(Timeout.class);
		log.info("Timer server Fleet Radar started");

		upDBFleetRadar = new TimerThreadFleetRadar(null);
		FleetRadar = new Thread(upDBFleetRadar);
		FleetRadar.start();
		try
		{
			FleetRadar.join();
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}
	public void StartTimeoutProcessFastTag()
	{
		log = Logger.getLogger(Timeout.class);
		log.info("Timer server FastTag started");

		upDBFastTag = new TimerThreadFastTag(null);
		FastTag = new Thread(upDBFastTag);
		FastTag.start();
		try
		{
			FastTag.join();
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}
}
