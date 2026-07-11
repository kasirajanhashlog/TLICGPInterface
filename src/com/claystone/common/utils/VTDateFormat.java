package com.claystone.common.utils;

import java.util.Date;

public class VTDateFormat {

	// returns date string in yyyy-mm-dd:hh:mm:ss format
	public static String formatDateYYYYMMDDhhmmss(Date pDate) {

		if (pDate == null)
			return "";

		StringBuffer dateString = new StringBuffer();
		dateString.append((pDate.getYear() + 1900));
		dateString.append("-");

		if (pDate.getMonth() < 9) {
			dateString.append("0");
		}
		dateString.append((pDate.getMonth() + 1));
		dateString.append("-");

		if (pDate.getDate() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getDate());
		dateString.append(":");

		if (pDate.getHours() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getHours());
		dateString.append(":");

		if (pDate.getMinutes() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getMinutes());
		dateString.append(":");

		if (pDate.getSeconds() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getSeconds());

		return dateString.toString();
	}

	// returns date string in yyyy-mm-dd hh:mm:ss format
	public static String formatDateYYYYMMDDhhmmss_Display(Date pDate) {

		if (pDate == null)
			return "";

		StringBuffer dateString = new StringBuffer();
		dateString.append((pDate.getYear() + 1900));
		dateString.append("-");

		if (pDate.getMonth() < 9) {
			dateString.append("0");
		}
		dateString.append((pDate.getMonth() + 1));
		dateString.append("-");

		if (pDate.getDate() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getDate());
		dateString.append(" ");

		if (pDate.getHours() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getHours());
		dateString.append(":");

		if (pDate.getMinutes() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getMinutes());
		dateString.append(":");

		if (pDate.getSeconds() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getSeconds());

		return dateString.toString();
	}

	// Returns only date part in yyyy-MM-dd format
	public static String formatDateYYYYMMDD(Date pDate) {

		if (pDate == null)
			return "";

		StringBuffer dateString = new StringBuffer();
		dateString.append((pDate.getYear() + 1900));
		dateString.append("-");

		if (pDate.getMonth() < 9) {
			dateString.append("0");
		}
		dateString.append((pDate.getMonth() + 1));
		dateString.append("-");

		if (pDate.getDate() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getDate());
		return dateString.toString();
	}

	// returns date string in hh:mm:ss format
	public static String formatDateHHMMSS(Date pDate) {

		if (pDate == null)
			return "";

		StringBuffer dateString = new StringBuffer();

		if (pDate.getHours() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getHours());
		dateString.append(":");

		if (pDate.getMinutes() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getMinutes());
		dateString.append(":");

		if (pDate.getSeconds() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getSeconds());

		return dateString.toString();
	}

	// returns date string in yyyy-mm-ddThh:mm:ssZ format
	public static String formatDateToXMLDate(Date pDate) {

		if (pDate == null)
			return "";

		StringBuffer dateString = new StringBuffer();
		dateString.append((pDate.getYear() + 1900));
		dateString.append("-");

		if (pDate.getMonth() < 9) {
			dateString.append("0");
		}
		dateString.append((pDate.getMonth() + 1));
		dateString.append("-");

		if (pDate.getDate() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getDate());
		dateString.append("T");

		if (pDate.getHours() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getHours());
		dateString.append(":");

		if (pDate.getMinutes() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getMinutes());
		dateString.append(":");

		if (pDate.getSeconds() < 10) {
			dateString.append("0");
		}
		dateString.append(pDate.getSeconds());
		dateString.append("Z");
		return dateString.toString();
	}

}
