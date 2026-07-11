package com.claystone.server.timeout;

import java.util.Date;
import java.util.Hashtable;
import java.util.Timer;

import org.apache.log4j.Logger;

import com.claystone.server.util.HibernateUtil;

public class MobileCompanyTaskRunnable implements Runnable {
	private final Timer timer = new Timer();
	private MobileCompanyTask mobileCompanyTask;
	private String mobileCompanyId;
	private String dataProviderName;
	private Hashtable<String, MobileUnitTimestamp> mCompanyTimestampHash;
	private Logger log = Logger.getLogger(MobileCompanyTaskRunnable.class);

	public MobileCompanyTaskRunnable() {
		mobileCompanyTask = new MobileCompanyTask();
	}

	public void cancelTimer() {
		log.info("Timer Thread Cancelled : " + Thread.currentThread().getId() +"  Company " + mobileCompanyId + " date: " + new Date());

		timer.cancel();
	}

	@Override
	public void run() {
		try {
			//log.info("MobileUnitTaskRunnable start: " + Thread.currentThread().getId() +" unit " + mobileunitId  + " date: " + new Date());
			if(mCompanyTimestampHash.containsKey(mobileCompanyId + dataProviderName) == true){
				MobileUnitTimestamp mobileCompanyTimestamp = mCompanyTimestampHash.get(mobileCompanyId + dataProviderName); 				
				if(mobileCompanyTimestamp.isProcessedFlag()) {
					mobileCompanyTask.setMobileCompanyId(mobileCompanyId);
					mobileCompanyTask.setDataProviderName(dataProviderName);
					mobileCompanyTask.setmCompanyTimestampHash(mCompanyTimestampHash);
					log.info("Thread schedule : " + Thread.currentThread().getId() + " / " + mobileCompanyId + "; Intervel : "+  mobileCompanyTimestamp.getDataInterval() + " date : " + new Date());
					timer.schedule(mobileCompanyTask, 0, mobileCompanyTimestamp.getDataInterval() * 1000);
				}  				
			}else {
				cancelTimer();
			}

			//log.info("MobileUnitTaskRunnable End: " + Thread.currentThread().getId() +" unit " + mobileunitId  + " date: " + new Date());

		} catch (Exception e) {
			log.error("Timeout Exception", e);
			e.printStackTrace();
			HibernateUtil.rollback();
		}

	}

	public Hashtable<String, MobileUnitTimestamp> getmCompanyTimestampHash() {
		return mCompanyTimestampHash;
	}

	public void setmCompanyTimestampHash(Hashtable<String, MobileUnitTimestamp> mCompanyTimestampHash) {
		this.mCompanyTimestampHash = mCompanyTimestampHash;
	}
	
	public String getMobileCompanyId() {
		return mobileCompanyId;
	}

	public void setMobileCompanyId(String mobileCompanyId) {
		this.mobileCompanyId = mobileCompanyId;
	}
	public String getDataProviderName() {
		return dataProviderName;
	}

	public void setDataProviderName(String dataProviderName) {
		this.dataProviderName = dataProviderName;
	}
}
