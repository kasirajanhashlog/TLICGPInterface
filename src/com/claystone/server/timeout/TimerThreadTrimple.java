package com.claystone.server.timeout;

import java.util.Timer;

public class TimerThreadTrimple implements Runnable {

	private final Timer timer = new Timer();
	private int PollingInterval;
	private CheckDBTrimble mCheckDBTrimble;

	public TimerThreadTrimple(String startDate) {	 
		PollingInterval = 10 * 60; //to be read from config file
		mCheckDBTrimble = new CheckDBTrimble(startDate);
	}
	public void cancelTimer(){
		timer.cancel();
		//mCheckDBTrimple.StopPool(); 
	}
	@Override
	public void run()
	{
		timer.schedule(mCheckDBTrimble,0, PollingInterval * 1000);
	}
}
