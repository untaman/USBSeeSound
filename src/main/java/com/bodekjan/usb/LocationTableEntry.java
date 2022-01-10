package com.bodekjan.usb;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;

public class LocationTableEntry {
	//THE POINT IN SPACE
	Point p;

	//DISTANCE TO EACH MIC
	public ArrayList<Double> dMics;
	//TOA (Time of Arrival) TO EACH MIC
	public ArrayList<Double> tMics;
	//Delta TOA (Time difference between Time of Arrival) TO EACH MIC
	public ArrayList<Double> tdMics;


	public LocationTableEntry() {

	}

	public LocationTableEntry(int x, int y) {
		p = new Point(x,y);
	}


	public String toString() {
		return "["+p.x+","+p.y+"]";
	}


	//GIVEN A LIST OF MIC LOCATIONS, CALCULATE THE DISTANCES, TOA AND DTOA TO ALL MICS FROM THIS LOCATION

	//GIVEN A LIST OF MIC LOCATIONS, CALCULATE THE DISTANCES, TOA AND DTOA TO ALL MICS FROM THIS LOCATION
//	public void calc(ArrayList<MicInfo> mics, double soundSpeed) {
//		dMics = new ArrayList<Double>();
//		tMics = new ArrayList<Double>();
//		tdMics = new ArrayList<Double>();
//
//		//CALCULATE DISTANCES AND TOA
//		for (int k = 0; k < mics.size();k++) {
//			double dmic = (p.distance(mics.get(k).location));
//			double tmic = dmic/soundSpeed;
//			dMics.add(dmic);
//			tMics.add(tmic);
//		}
//
//		//CALCULATE ALL DTOA BETWEEN ALL MICS FOR THIS LOCATION, FOR ALL MIC COMBINATIONS
//		//IE: MIC2TOA-MIC1TOA, MIC3TOA-MIC1TOA, MIC3TOA-MIC2TOA,....
//		for (int k = 0; k < tMics.size()-1;k++) {
//			for (int kk = k+1; kk < tMics.size();kk++) {
//				tdMics.add(tMics.get(kk)-tMics.get(k));
//			}
//		}
//	}


	public void calc(ArrayList<MicInfo> mics, double soundSpeed) {
		dMics = new ArrayList<Double>();
		tMics = new ArrayList<Double>();
		tdMics = new ArrayList<Double>();

		//Log.i("Here", "base x:" +   mics.get(0).location.x +"y"+ mics.get(0).location.y);



		//CALCULATE DISTANCES AND TOA
		for (int k = 0; k < mics.size();k++) {
			float x = mics.get(k).location.x;
			float y = mics.get(k).location.y;
			//Log.i("Here", "k x:" +   mics.get(k).location.x +"y"+ mics.get(k).location.y);
			//int z = 0;
			//double dmic = (p.distance(mics.get(k).location));
			//Going to base all measurements of distance relative to origin (0,0,0) base mic
			double dmic = distance3D(x,y,mics,p/**,z*/);
			//Log.i("Here", "dmic:" +  dmic);
			double tmic = dmic/soundSpeed;
			dMics.add(dmic);
			tMics.add(tmic);
		}

		//CALCULATE ALL DTOA BETWEEN ALL MICS FOR THIS LOCATION, FOR ALL MIC COMBINATIONS
		//IE: MIC2TOA-MIC1TOA, MIC3TOA-MIC1TOA, MIC3TOA-MIC2TOA,....
		for (int k = 0; k < tMics.size()-1;k++) {
			for (int kk = k+1; kk < tMics.size();kk++) {
				tdMics.add(tMics.get(kk)-tMics.get(k));
			}
		}
	}

	public void calcTds(ArrayList<MicInfo> mics, double samplesPerUSec) {
		dMics = new ArrayList<Double>();
		tMics = new ArrayList<Double>();
		tdMics = new ArrayList<Double>();

		Log.i("Here", "mics.size():" + mics.size());
		Log.i("Here", "samplesPerUSec" + samplesPerUSec);

		for (int k = 0; k < mics.size();k++) {
			Log.i("Here", "mics.get(k).sample:" + mics.get(k).sample);
		}

		for (int k = 0; k < mics.size();k++) {
			double tmic = mics.get(k).sample/samplesPerUSec;
			tMics.add(tmic);
			Log.i("Here", "|tMics|:" + tMics.get(k));
		}

		for (int k = 0; k < tMics.size()-1;k++) {
			for (int kk = k+1; kk < tMics.size();kk++) {
				tdMics.add(tMics.get(kk)-tMics.get(k));
			}
		}
		splitTdMics(tdMics);

	}

	public void splitTdMics(ArrayList<Double> tdMics) {
		for (int k = 0; k < tdMics.size();k++) {
			Log.i("Here", "tdMics: "+k+" " + tdMics.get(k));
		}
	}



//////////new

	public Double distance3D(float xk, float yk,/**, double zk*/ArrayList<MicInfo> mics, Point p){
		double distance = 0;


		//setting x0,y0,z0 to point p.x p.y so that all point are subtract from all points
		double x0 = p.x;
		double y0 = p.y;
		double z0 = 0;

		 distance = Math.sqrt(Math.pow(xk-x0, 2) + Math.pow(yk-y0, 2));
		//	distance = Math.hypot(xk - x0,yk - y0);/**-Math.pow(zk - z0, 2));*/


		return distance;
	}


}