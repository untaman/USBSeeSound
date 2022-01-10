package com.bodekjan.usb;

import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


public class MicInfo {//extends AppCompatActivity {


	public PointF location;
	public int sample;
	public int value;

	public String toString() {
		return  "["+location.x+","+location.y+"] S: "+sample+ " V: "+value;
	}

}
