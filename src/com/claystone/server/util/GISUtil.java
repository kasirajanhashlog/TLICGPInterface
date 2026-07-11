package com.claystone.server.util;

import java.awt.geom.Point2D;

import org.apache.log4j.Logger;

import com.claystone.common.utils.CommonMethods;


public class GISUtil {
	static double pi = 22.0/7.0;
	private static Logger log = Logger.getLogger(GISUtil.class);
    public static double getDecDegree(double hhmmssss) {
        double deg, mm;
        
        deg = Math.floor(hhmmssss);
        mm = (hhmmssss*1000000 - deg*1000000)/10000;
        deg = deg + (mm*100/60)/100;
        
        return deg;
    }
    public static String getDecDegree(String hhmmssssString) {
        
        try {
            
            int i = hhmmssssString.indexOf(".");
            
            if ( i < 0 ) {
                
                int l = hhmmssssString.length();
                
                hhmmssssString = hhmmssssString.substring(0, l-5) + "." + hhmmssssString.substring(l-5, l);
                
            }
            
        } catch ( Exception ex ){
        	log.error(ex.getMessage());
        }
        
        return Double.toString(getDecDegree(Double.parseDouble(hhmmssssString)));
        
    }
    
    public static String getGPSDegreeString(double degIn) {
        
        StringBuffer sb = new StringBuffer(Double.toString(getGPSDegree(degIn)));
        
        int i = 6 - (sb.length() - sb.indexOf(".") - 1);
                
        for ( int j = 0; j < i; j++ ) {
            
            sb.append('0');
            
        }
        
        return sb.toString();
                
    }
    public static double getGPSDegree(double degIn) {
        double deg, mm;
        deg = Math.floor(degIn);
        
        mm = (degIn*1000000 - deg*1000000)/10000;
        deg = deg + (mm*60/100)/100;
        deg*=1000000;
        deg = Math.round(deg);
        deg/=1000000;
        return deg;
    }

    public static String getGPSDegree(String degString) {
        return Double.toString(getGPSDegree(Double.parseDouble(degString)));
    }

    //For GPS latitude, longitude
    public static double getDistanceGPS(double lat1, double lon1, double lat2, double lon2) {
        
        return getDistanceDeg(getDecDegree(lat1), getDecDegree(lon1), getDecDegree(lat2), getDecDegree(lon2));
        
    }

    public static double getDistanceDeg(int lat1, int lon1, int lat2, int lon2) {
        
        return getDistanceDeg(((double)lat1)/1000000, ((double)lon1)/1000000, ((double)lat2)/1000000, ((double)lon2)/1000000);
        
    }
    public static double getDistanceDeg(double lat1, double lon1, double lat2, double lon2) {
      double d =0;
      
      lat1=(lat1)*pi/180;
      lon1=(lon1)*pi/180;
      lat2=(lat2)*pi/180;
      lon2=(lon2)*pi/180;
      
      d = Math.acos(Math.sin(lat1)*Math.sin(lat2)+Math.cos(lat1)*Math.cos(lat2)*Math.cos(lon1-lon2));
      
      d = d*180*60*1.852*1000/pi;
      Double tempD = d;
      if(tempD.isNaN()){
    	  d = 0; 
      }
      return d;
    }

    //For GPS latitude, longitude
    public static double getBearingGPS(double lat1, double lon1, double lat2, double lon2) {
        
        return getBearingDeg(getDecDegree(lat1), getDecDegree(lon1), getDecDegree(lat2), getDecDegree(lon2));
        
    }
    //For HH:MM.SSSS latitude, longitude
    public static double getBearingDeg(double lat1, double lon1, double lat2, double lon2){
        
        double tc1 = 0;
        lat1=(lat1)*pi/180.0;
        lon1=(lon1)*pi/180.0;
        lat2=(lat2)*pi/180.0;
        lon2=(lon2)*pi/180.0;
        
        double y = Math.sin(lon2-lon1)*Math.cos(lat2);
        
        y = (Math.round(y * 1000000.0))/1000000.0;
        
        double x = Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon2-lon1);
        
        x = (Math.round(x * 1000000.0))/1000000.0;
        
        if ( y > 0 ) {
            if ( x > 0 ) tc1 = Math.atan(y/x);
            if ( x < 0 ) tc1 = 180*(pi/180.0) - Math.atan(-y/x);
            if ( x == 0 ) tc1 = 90*(pi/180.0);
        } else if ( y < 0 ){
            //if y < 0 then
            if ( x > 0 ) tc1 = -Math.atan(-y/x)+(360.0*(pi/180.0));
            if ( x < 0 ) tc1 = Math.atan(y/x)+(180.0*(pi/180.0));
            if ( x == 0 ) tc1 = 270.0*(pi/180.0);
        } else if ( y == 0 ) {
            //if y = 0 then
            if ( x > 0 ) tc1 = 0;
            if ( x < 0 ) tc1 = 180.0*(pi/180.0);
            if ( x == 0 ) tc1 = 0;
        }
        
        //Convert to degree
        tc1 = tc1*180.0/pi;
        
        return tc1;
    }
    
    public static String getDirectionThai(int course){
    	if(course < 23) return "" ;
    	if(course < 68) return "" ;
    	if(course < 113) return "" ;
    	if(course < 148) return "" ;
    	if(course < 203) return "" ;
    	if(course < 248) return "" ;
    	if(course < 298) return "" ;
    	if(course < 333) return "" ;

    	
    	return "";
    }
    public static final Point2D getPointOnRadialKm(double lon, double lat,double km, double radial) {
    	/* Convert km to nautical miles to arc radians; 1 nm = 1.852km */
		double dRadArc = ((km/1.852)*Math.PI)/10800; 
		double tc = Math.toRadians(radial);
		double lat1 = Math.toRadians(lat);
		double lon1 = Math.toRadians(lon);
		double pLat = Math.asin((Math.sin(lat1)*Math.cos(dRadArc))+
		(Math.cos(lat1)*Math.sin(dRadArc)*Math.cos(tc)));
		double dLon = Math.atan2(Math.sin(tc)*Math.sin(dRadArc)*Math.cos(lat1),
		Math.cos(dRadArc)-Math.sin(lat1)*Math.sin(lat1));
		double pLon = (lon1+dLon+Math.PI % (2*Math.PI))-Math.PI;
		/* Convert radians back into decimal degrees */
		pLat = (pLat*180)/Math.PI;
		pLon = (pLon*180)/Math.PI;
		return new Point2D.Double(pLon,pLat);
    }
    
}
