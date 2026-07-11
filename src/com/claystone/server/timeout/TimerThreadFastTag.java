package com.claystone.server.timeout;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;

import org.hibernate.Session;
import org.jfree.util.Log;

import com.claystone.common.utils.CommonConstants;
import com.claystone.server.util.HibernateUtil;

public class TimerThreadFastTag implements Runnable {

	private final Timer timer = new Timer();
	private int PollingInterval;
	private CheckDBFastTag mCheckDBFastTag;

	public TimerThreadFastTag(String startDate) {	 
		PollingInterval = 10 * 60; //to be read from config file
		mCheckDBFastTag = new CheckDBFastTag(startDate);
	}
	public void cancelTimer(){
		timer.cancel();
		mCheckDBFastTag.StopPool(); //04-Aug-2009
	}
	@Override
	public void run()
	{
//		System.out.println("Timer triggered at:"+new Date());
		timer.schedule(mCheckDBFastTag,0, PollingInterval * 1000);
	}
}
