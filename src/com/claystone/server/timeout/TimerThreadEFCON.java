package com.claystone.server.timeout;

import java.util.Timer;

public class TimerThreadEFCON implements Runnable {

	private final Timer timer = new Timer();
	private int PollingInterval;
	private CheckDBEFCON mCheckDBEFCON;

	public TimerThreadEFCON(String startDate) {	 
		PollingInterval = 10 * 60; //to be read from config file
		mCheckDBEFCON = new CheckDBEFCON(startDate);
	}
	public void cancelTimer(){
		timer.cancel();
		//mCheckDBEFCON.StopPool(); //04-Aug-2009
	}
	@Override
	public void run()
	{
		timer.schedule(mCheckDBEFCON,0, PollingInterval * 1000);
	}
}
