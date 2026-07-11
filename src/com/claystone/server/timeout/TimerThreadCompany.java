package com.claystone.server.timeout;

import java.util.Timer;

public class TimerThreadCompany implements Runnable{

	private final Timer timer = new Timer();
	private int PollingInterval;
	private CheckDBCompany mCheckDBCompany;

	public TimerThreadCompany(String startDate) {
		PollingInterval = 10 * 60; //to be read from config file
		mCheckDBCompany = new CheckDBCompany(startDate);
	}
	public void cancelTimer(){
		timer.cancel();
		mCheckDBCompany.StopPool(); //04-Aug-2009
	}
	@Override
	public void run()
	{
		timer.schedule(mCheckDBCompany,0, PollingInterval * 1000);
	}
}
