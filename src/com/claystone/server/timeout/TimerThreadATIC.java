package com.claystone.server.timeout;

import java.util.Timer;

public class TimerThreadATIC implements Runnable {
	private final Timer timer = new Timer();
	private int PollingInterval;
	private CheckDBATIC mCheckDBATIC;

	public TimerThreadATIC(String startDate) {	 
		PollingInterval = 10 * 60; //to be read from config file
		mCheckDBATIC = new CheckDBATIC(startDate);
		
	}
	public void cancelTimer(){
		timer.cancel();
		//mCheckDBEFCON.StopPool(); //04-Aug-2009
	}
	@Override
	public void run()
	{
		timer.schedule(mCheckDBATIC,0, PollingInterval * 1000);
	}
}
