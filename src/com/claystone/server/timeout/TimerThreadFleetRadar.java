package com.claystone.server.timeout;

import java.util.Date;
import java.util.Timer;

public class TimerThreadFleetRadar implements Runnable  {

	private final Timer timer = new Timer();
	private int PollingInterval;
	private CheckDBFleetRadar mCheckDBFleetRadar;

	public TimerThreadFleetRadar(String startDate) {	 
		//09SEP2024--The following 10 seconds polling is commented and 60 seconds is added.
//		PollingInterval = 10 * 60; //to be read from config file
		PollingInterval = 60 * 60; //to be read from config file
		mCheckDBFleetRadar = new CheckDBFleetRadar(startDate);
		
	}
	public void cancelTimer(){
		timer.cancel();
		//mCheckDBEFCON.StopPool(); //04-Aug-2009
	}
	@Override
	public void run()
	{
		timer.schedule(mCheckDBFleetRadar,0, PollingInterval * 1000);
	}
}
