package com.claystone.server.timeout;

import java.util.Timer;

public class TimerThreadENMOVIL implements Runnable {

	private final Timer timer = new Timer();
	private int PollingInterval;
	private CheckDBENMOVIL mCheckDBENMOVIL;

	public TimerThreadENMOVIL(String startDate) {	 
		PollingInterval = 10 * 60; //to be read from config file
		mCheckDBENMOVIL = new CheckDBENMOVIL(startDate);
	}
	public void cancelTimer(){
		timer.cancel();
	}
	@Override
	public void run()
	{
		timer.schedule(mCheckDBENMOVIL,0, PollingInterval * 1000);
	}
}
