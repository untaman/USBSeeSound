package com.bodekjan.usb;//import java.awt.Color;
//import java.awt.Font;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
import android.util.Log;

import java.util.ArrayList;

public class LocationTable {

	public static int						LOCMETHOD_AVGALLERRORS	= 1;
	public static int						LOCMETHOD_2MICCROSSOVER	= 2;
	public static int						LOCMETHOD_TRIANGLECROSSOVER	= 3;

	public ArrayList<LocationTableEntry>	table;
	public ArrayList<MicInfo>				mics;
	public int								scale;
	public int								x, y, sizex, sizey;
	public int possibleLocs = 0;
	public LocationTableEntry bestLocation = null;

	public LocationTable(int scale) {
		table = new ArrayList<LocationTableEntry>();
		this.scale = scale;
	}

	//CALCULATE A GRID OF SIZEX*SIZEY SIZE WITH LOCATIONS AND TIMINGS FOR EACH POINT IN RELATIONSHIP TO MICS
	//SEE LocationTableEntry.calc()

	public void generateTable(ArrayList<MicInfo> mics, double soundSpeed, int x, int y, int sizex, int sizey) {
		this.mics = mics;
		this.x = x;
		this.y = y;
		this.sizex = sizex;
		this.sizey = sizey;

		//GENERATES A GRID OF POINTS AND CALCULATES THE SOUND DIFFERENCES FROM POINT TO ALL MICS
		for (int k = x; k < x + sizex; k++) {
			for (int j = y; j < y + sizey; j++) {
				LocationTableEntry lte = new LocationTableEntry(k * scale, j * scale);
				lte.calc(mics, soundSpeed);
				table.add(lte);
			}
		}

//		for (int k = 0; k < table.size();k++) {
//			Log.i("Here", "table contents: "+table.get(k));
//		}
	}





	public void getBestLocation (LocationTableEntry origin, int locMethod, double sampleFrequency) {
//	public BufferedImage heatMap(LocationTableEntry origin, int locMethod, double sampleFrequency) {
		double minDiff = (1.0 / sampleFrequency);

		possibleLocs = 0;
//		BufferedImage image = new BufferedImage(sizex + 1, sizey + 1, BufferedImage.TYPE_4BYTE_ABGR);
//		image.createGraphics();
//		Graphics2D g2d = (Graphics2D) image.getGraphics();

		double r, g, b, a;

		double bestAlpha = 99999999;


		if (locMethod == LOCMETHOD_AVGALLERRORS) {
			for (LocationTableEntry lte : table) {
				// DO HEAT MAP
				g = r = b = a = 0;
				a = avgAllErrors(lte.tdMics, origin);
				//Log.i("Here", "a: "+a);
				//Log.i("Here", "lte.tdMics: "+lte.tdMics);
				//Log.i("Here", "origin: "+origin);
				if (a < bestAlpha) {
					bestAlpha = a;
					bestLocation = lte;
				}

				if (a < minDiff) {
					g = 255;
					r = 255;
					b = 255;
					possibleLocs++;
				}

				b = (minDiff / a * 256) * 3;
				a *= 100;
				//setRGB(image, lte.p.x / scale + sizex / 2, -lte.p.y / scale + sizey / 2 - 1, (int) Math.round(r), (int) Math.round(g), (int) Math.round(b), (int) Math.round(a));
			}
//			Log.i("Here", "bestLocationx: "+bestLocation.p.x);
//			Log.i("Here", "bestLocationy: "+bestLocation.p.y);
//			Log.i("Here", "origin: "+origin);
		}

		if (locMethod == LOCMETHOD_2MICCROSSOVER || locMethod == LOCMETHOD_TRIANGLECROSSOVER) {
			ArrayList<ArrayList> micPosCombinations = null;
			if (locMethod == LOCMETHOD_2MICCROSSOVER) micPosCombinations = getDualCombinations();
			if (locMethod == LOCMETHOD_TRIANGLECROSSOVER) micPosCombinations = getTriangleCombinations();

			for (LocationTableEntry lte : table) {
				int c = 0;
				double aa = 0;
				int allPossibleLocs = 0;
				g = r = b = a = 0;

				for (ArrayList micposi : micPosCombinations) {

					// DO HEAT MAP
					a = avgErrors(lte.tdMics, origin, micposi);

					if (a < minDiff) {
						allPossibleLocs++;
					}
					c++;
					aa += a;
				}

				if (aa < bestAlpha) {
					bestAlpha = aa;
					bestLocation = lte;
				}

				if (allPossibleLocs == micPosCombinations.size()) {
					r = 255;
					g = 255;
					b = 255;
					possibleLocs++;
				}

				a = aa * 1.0 / c;
				if (locMethod == LOCMETHOD_2MICCROSSOVER) g = (minDiff / a * 256) * 3;
				if (locMethod == LOCMETHOD_TRIANGLECROSSOVER) r = (minDiff / a * 256) * 3;
				a *= 100;
			//	setRGB(image, lte.p.x / scale + sizex / 2, -lte.p.y / scale + sizey / 2 - 1, (int) Math.round(r), (int) Math.round(g), (int) Math.round(b), (int) Math.round(a));
			}
		}


		double x = mics.get(0).location.x;
		double y = mics.get(0).location.y;
	}


	//GET AN ARRAY OF ALL POSSIBLE TRIPLETS OF MICS FOR THIS MIC CONFIGURATION
	//FOR EXAMPLE: IF THERE IS 3 MICS IN THE SETUP, THERE WILL ONLY BE ONE COMBINATION
	//IF THERE IS 4 MICS IN THE SETUP, WE CAN HAVE 4 POSSIBLE 'TRIANGLES' FOR THE 4 VERTICES
	//(LAZY CODING FOLLOWS)

	private ArrayList<ArrayList> getTriangleCombinations() {
		ArrayList<ArrayList> micPosCombinations = new ArrayList<ArrayList>();
		ArrayList<Integer> micPos;

		if (mics.size() == 3) {
			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(2);
			micPosCombinations.add(micPos);
		}

		if (mics.size() == 4) {
			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(2);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(3);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(2);
			micPos.add(3);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(1);
			micPos.add(2);
			micPos.add(3);
			micPosCombinations.add(micPos);
		}

		if (mics.size() == 5) {
			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(2);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(3);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(4);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(1);
			micPos.add(2);
			micPos.add(3);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(1);
			micPos.add(2);
			micPos.add(4);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(2);
			micPos.add(3);
			micPos.add(4);
			micPosCombinations.add(micPos);
		}
		if (mics.size() == 6) {
			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(2);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(3);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(4);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(0);
			micPos.add(1);
			micPos.add(5);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(1);
			micPos.add(2);
			micPos.add(3);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(1);
			micPos.add(2);
			micPos.add(4);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(1);
			micPos.add(2);
			micPos.add(5);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(2);
			micPos.add(3);
			micPos.add(4);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(2);
			micPos.add(3);
			micPos.add(5);
			micPosCombinations.add(micPos);

			micPos = new ArrayList<Integer>();
			micPos.add(3);
			micPos.add(4);
			micPos.add(5);
			micPosCombinations.add(micPos);

		}

		return micPosCombinations;
	}

	//GET AN ARRAY OF ALL POSSIBLE PAIRS OF MICS FOR THIS MIC CONFIGURATION
	//FOR EXAMPLE: IF THERE IS 3 MICS IN THE SETUP, THERE WILL BE 3 POSSIBLE COMBINATIONS
	//IE, (MIC1,MIC2), (MIC1,MIC3), (MIC2,MIC3)

	private ArrayList<ArrayList> getDualCombinations() {
		ArrayList<ArrayList> micPosCombinations = new ArrayList<ArrayList>();
		ArrayList<Integer> micPos;

		for (int k = 0; k < mics.size(); k++) {
			for (int j = k+1; j < mics.size(); j++) {
				micPos = new ArrayList<Integer>();
				micPos.add(k);
				micPos.add(j);
				micPosCombinations.add(micPos);
			}
		}
		return micPosCombinations;
	}


	//SUM AND AVERAGE THE DIFERENCE OF ALL DTOA BETWEEN TWO POINTS
	private double avgAllErrors(ArrayList<Double> tdMics, LocationTableEntry origin) {
		double a = 0;
		for (int i = 0; i < tdMics.size(); i++) {
			a += Math.abs(tdMics.get(i) - origin.tdMics.get(i));
		}
		a /= tdMics.size();
		return a;
	}

	//SUM AND AVERAGE THE DIFERENCE OF DTOA BETWEEN TWO POINTS, GIVEN A LIST OF MIC POSITIONS TO AVERAGE
	private double avgErrors(ArrayList<Double> tdMics, LocationTableEntry origin, ArrayList<Integer> micPos) {
		double a = 0;
		for (int i = 0; i < micPos.size(); i++) {
			a += (Math.abs(tdMics.get(micPos.get(i)) - origin.tdMics.get(micPos.get(i))));
		}
		a /= micPos.size();
		return a;
	}
}