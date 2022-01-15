# USBSeeSound
USB MiclocV2 for Android Proof of Concept. What this does is to allow a individual to detect a sound that is above a cutoff threshold and get the sounds
location relative to small microphone array location. This, with the assistance of an Android phone, allows the individual to get a gps coordinates and use Google Maps
API to drop a pin at the calculated sounds location. This pin is visible on Google Maps' top down view and also Google Maps' AR viewer. Allowing the individual to see in Augmented Realty the approximate origin of the sound via an Android phone. 

Based on kripthor MiclocV2 project found at https://ruralhacker.blogspot.com/p/micloc.html for sound location;
repos https://github.com/kripthor/teensy31-micloc-java for Java and here https://github.com/kripthor/teensy31-micloc for Arduino Teensy;
https://www.allaboutcircuits.com/projects/communicate-with-your-arduino-through-android/ for Arduino to Android USB communication;
https://developers.google.com/maps/documentation/android-sdk/map-with-marker for Google Maps API use;



What you will need
1 Arduino Teensy 3.1/3.2;
1 DTH11/DTH22 ( DTH22 not tested) sensor;
4 Electret Microphone Amplifier Module MAX4466 Adjustable Gain Blue Breakout Board or similar;
1 Breadboard;

Also to get the Android part to run you will have to get a API key to access google maps API. Instructions are found here  
https://developers.google.com/maps/documentation/android-sdk/get-api-key

Wiring Diagram: taken from and all credit to kripthor MiclocV2 project found at https://ruralhacker.blogspot.com/p/micloc.html 
![wiring diagram](https://user-images.githubusercontent.com/97481499/149629940-39481cfd-9298-4d0b-94be-99061c1a6706.jpg)

The code is AS IS! and not optimized
