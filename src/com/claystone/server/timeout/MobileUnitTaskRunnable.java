package com.claystone.server.timeout;

import java.util.Date;
import java.util.Hashtable;
import java.util.Timer;

import org.apache.log4j.Logger;

import com.claystone.server.util.HibernateUtil;

public class MobileUnitTaskRunnable implements Runnable {
	private final Timer timer = new Timer();
	private MobileUnitTask mobileUnitTask;
	private String mobileunitId;
	private Hashtable<String, MobileUnitTimestamp> mUnitTimestampHash;
	private Logger log = Logger.getLogger(MobileUnitTaskRunnable.class);

	public MobileUnitTaskRunnable() {
		mobileUnitTask = new MobileUnitTask();
	}

	public void cancelTimer() {
		log.info("Timer Thread Cancelled : " + Thread.currentThread().getId() +"  unit " + mobileunitId + " date: " + new Date());

		timer.cancel();
	}

	@Override
	public void run() {
		try {
			//log.info("MobileUnitTaskRunnable start: " + Thread.currentThread().getId() +" unit " + mobileunitId  + " date: " + new Date());
			if(mUnitTimestampHash.containsKey(mobileunitId) == true){
				MobileUnitTimestamp mobileUnitTimestamp = mUnitTimestampHash.get(mobileunitId); 
				if(mobileUnitTimestamp.isProcessedFlag()) {
					mobileUnitTask.setMobileunitId(mobileunitId);
					mobileUnitTask.setmUnitTimestampHash(mUnitTimestampHash);
					log.info("Thread schedule : " + Thread.currentThread().getId() +" / " + mobileunitId + "; Intervel : "+  mobileUnitTimestamp.getDataInterval() + " date : " + new Date());
					timer.schedule(mobileUnitTask, 0, mobileUnitTimestamp.getDataInterval() * 1000);
					
				}				
			}else{
				cancelTimer();
			}
			//log.info("MobileUnitTaskRunnable End: " + Thread.currentThread().getId() +" unit " + mobileunitId  + " date: " + new Date());

		} catch (Exception e) {
			log.error("Timeout Exception", e);
			e.printStackTrace();
			HibernateUtil.rollback();
		}

	}

	public Hashtable<String, MobileUnitTimestamp> getmUnitTimestampHash() {
		return mUnitTimestampHash;
	}

	public void setmUnitTimestampHash(Hashtable<String, MobileUnitTimestamp> mUnitTimestampHash) {
		this.mUnitTimestampHash = mUnitTimestampHash;
	}

	public String getMobileunitId() {
		return mobileunitId;
	}

	public void setMobileunitId(String mobileunitId) {
		this.mobileunitId = mobileunitId;
	}

}
