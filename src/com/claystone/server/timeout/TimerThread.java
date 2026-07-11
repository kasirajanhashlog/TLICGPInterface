package com.claystone.server.timeout;

import java.util.Timer;


public class TimerThread implements Runnable {

	private final Timer timer = new Timer();
	private int PollingInterval;
	private CheckDB mCheckDB;

	public TimerThread(String startDate) {	 
		PollingInterval = 10 * 60; //to be read from config file
		mCheckDB = new CheckDB(startDate);
	}
	public void cancelTimer(){
		timer.cancel();
		mCheckDB.StopPool(); //04-Aug-2009
	}
	@Override
	public void run()
	{
		timer.schedule(mCheckDB,0, PollingInterval * 1000);
	}
}
