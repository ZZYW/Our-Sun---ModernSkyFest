import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import peasy.test.*; 
import peasy.org.apache.commons.math.*; 
import peasy.*; 
import peasy.org.apache.commons.math.geometry.*; 
import java.util.Map; 
import java.util.Iterator; 
import SimpleOpenNI.*; 
import ddf.minim.analysis.*; 
import ddf.minim.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class handstracking extends PApplet {











PVector overallTranslation;
PShader blur;

SimpleOpenNI context;
float        zoomF =0.5f;
float        rotX = radians(180);  // by default rotate the hole scene 180deg around the x-axis, 
// the data from openni comes upside down
float        rotY = radians(0);
int          handVecListSize = 30;
Map<Integer, ArrayList<PVector>>  handPathList = new HashMap<Integer, ArrayList<PVector>>();
int[]       userClr = new int[] { 
  color(255, 0, 0), 
  color(0, 255, 0), 
  color(0, 0, 255), 
  color(255, 255, 0), 
  color(255, 0, 255), 
  color(0, 255, 255)
};

int userCount = 0;


float handMovingDisp = 0;
PVector handMovingDir = new PVector(0, 0, 0);

//geo1
GeoTester geotest = new GeoTester();

//music
Minim minim;  
AudioInput micIn;
AudioPlayer bgmusic;
FFT fftLin;
FFT fftLog;

float spectrumScale = 4;
float[] keyBandHeights = new float[5];

boolean toggleSketch = false;


//sphere
int cuantos = 3000;
Pelo[] lista ;
float[] z = new float[cuantos]; 
float[] phi = new float[cuantos]; 
float[] largos = new float[cuantos];

float radio, radioOri;
float rx = 0;
float ry =0;
float sphereFillAlphaModifier = 0;

float sunTargetHeight;
float sunYpos;

int sunTargetRedness;
int sunRedness;

//cube matrix
CubeMatrix cubeMatrix;

//mesh
SimpleMesh mesh;
SimpleMesh scatterParticles;



public void setup()
{
  size(846, 1504, P3D);
  overallTranslation = new PVector(width/2, height/2, -600);
  radioOri = 250;
  radio = radioOri;

  lista = new Pelo[cuantos];
  for (int i=0; i<cuantos; i++) {
    lista[i] = new Pelo();
  }
  mesh = new SimpleMesh();
  cubeMatrix = new CubeMatrix();
  cubeMatrix.isMoving = true;
  noiseDetail(3);
  blur = loadShader("blur.glsl"); 
  context = new SimpleOpenNI(this);
  if (context.isInit() == false)
  {
    println("Can't init SimpleOpenNI, maybe the camera is not connected!"); 
    exit();
    return;
  }
  background(255);

  // disable mirror
  context.setMirror(true);

  // enable depthMap generation 
  context.enableDepth();

  // enable hands + gesture generation
  context.enableHand();
  context.enableUser();
  context.startGesture(SimpleOpenNI.GESTURE_WAVE);

  // set how smooth the hand capturing should be
  //context.setSmoothingHands(.5);

  stroke(0);
  smooth();

  minim = new Minim(this);
  bgmusic = minim.loadFile("04a.mp3", 1024);

  micIn = minim.getLineIn();

  // loop the file
  bgmusic.loop();

  fftLin = new FFT( bgmusic.bufferSize(), bgmusic.sampleRate() );

  // calculate the averages by grouping frequency bands linearly. use 30 averages.
  fftLin.linAverages( 30 );

  // create an FFT object for calculating logarithmically spaced averages
  fftLog = new FFT( bgmusic.bufferSize(), bgmusic.sampleRate() );

  // calculate averages based on a miminum octave width of 22 Hz
  // split each octave into three bands
  // this should result in 30 averages
  fftLog.logAverages( 22, 3 );

  //flock 
  {
    //    flock = new Flock();
  }
}


float rotateNoiseIndex=0;
float rotateAngle=0;
float sphereFillAlpha = 255;
float sphereFadeOutAlpha=255;

public void draw()
{


  if (keyBandHeights[4] > 0.4f || handMovingDisp > 0.6f) {
    background(255);
    sphereFillAlphaModifier = 255;
    for (int j=0; j<14; j++) { 
      for (int i=0; i<14; i++) {
        cubeMatrix.myCubes[i][j].fill = 0xffFFCFCF;
        cubeMatrix.myCubes[i][j].strokeColor = color(0);
      }
    }
  } else {
    sphereFillAlphaModifier = 0; 
    beginShape(QUADS); 
    fill(0, 60); 
    vertex(-width, -height+700, -1200); 
    vertex(width*2, -height+700, -1200); 
    fill(0, 60); 
    vertex(width*2, height*2, -1200); 
    vertex(-width, height*2, -1200); 
    endShape();
    for (int j=0; j<14; j++) { 
      for (int i=0; i<14; i++) {
        cubeMatrix.myCubes[i][j].fill = color(0);
        cubeMatrix.myCubes[i][j].strokeColor = color(255);
      }
    }
  }


  context.update(); 

  float transitionSpeed = 100;
  if (toggleSketch) {
    if (overallTranslation.z > -2500) {
      overallTranslation.z -= transitionSpeed;
    }
    //    overallTranslation.z =  -3000;
  } else {
    if (overallTranslation.z < -600) {
      overallTranslation.z += transitionSpeed;
    }
    //    overallTranslation.z = -600;
  }

  translate(overallTranslation.x, overallTranslation.y, overallTranslation.z); 



  fftLog.forward( bgmusic.mix ); 
  // draw the logarithmic averages


  float[] keyBandHeightsOld = keyBandHeights;
  //  println("old: " + keyBandHeightsOld[0]);

  {
    // since logarithmically spaced averages are not equally spaced
    // we can't precompute the width for all averages
    for (int i = 0; i < fftLog.avgSize (); i++)
    {
      if (i==10)keyBandHeights[0]=fftLog.getAvg(i)*spectrumScale; 
      if (i==12)keyBandHeights[1]=fftLog.getAvg(i)*spectrumScale; 
      if (i==14)keyBandHeights[2]=fftLog.getAvg(i)*spectrumScale; 
      if (i==2)keyBandHeights[3]=fftLog.getAvg(i)*spectrumScale; 
      if (i==25)keyBandHeights[4]=fftLog.getAvg(i)*spectrumScale;
    }
  }

  //  float smoothRate = 0.1;
  //  for (int i=0; i<keyBandHeights.length-1; i++) {
  //    keyBandHeights[i] = (keyBandHeights[i] + keyBandHeightsOld[i]) / 2 ;
  //  }
  //  println(keyBandHeights[0]);

  if ((keyBandHeights[4] > 0.5f || mousePressed) && mesh.particles.size() < mesh.maxLength) {
    mesh.particles.add(new SimpleMeshPar());
    //    println(" particle size:::: "+mesh.particles.size());
  }

  pushMatrix(); 
  if (handPathList.size() > 0)  
  {    
    Iterator itr = handPathList.entrySet().iterator(); 
    while (itr.hasNext ())
    {
      Map.Entry mapEntry = (Map.Entry)itr.next(); 
      int handId =  (Integer)mapEntry.getKey(); 
      ArrayList<PVector> vecList = (ArrayList<PVector>)mapEntry.getValue(); 
      PVector p; 

      pushStyle(); 
      stroke(userClr[ (handId - 1) % userClr.length ]); 
      noFill(); 
      Iterator itrVec = vecList.iterator(); 
      beginShape(); 
      pushStyle(); 
      while ( itrVec.hasNext () ) 
      { 
        p = (PVector) itrVec.next();
      }
      endShape(); 
      popStyle(); 

      stroke(userClr[ (handId - 1) % userClr.length ]); 
      strokeWeight(4); 
      p = vecList.get(0); 
      point(p.x, p.y, p.z); 
      popStyle(); 

      handMovingDisp = PVector.dist(vecList.get(0), vecList.get(vecList.size()-1)); 
      handMovingDir = PVector.sub(vecList.get(0), vecList.get(vecList.size()-1)); 
      handMovingDisp = map(handMovingDisp, 0, 600, 0, 1); 
      geotest.sizeIncre(vecList);
    }
  }

  println("Hand Moving Displacement:  " + handMovingDisp);

  for (int i=0; i<lista.length-1; i++) {
    lista[i].extraLen = keyBandHeights[2]/200;
  }

  popMatrix(); 
  cubeMatrix.display();
  if (cubeMatrix.isMoving) {
    cubeMatrix.move();
    if (!cubeMatrix.speedAssigned) {
      cubeMatrix.assignSpeed();
    }
  } else {
    cubeMatrix.reset();
  }

  {
    pushMatrix();
    mesh.calculate();
    mesh.reactMus(keyBandHeights[1]);
    mesh.display();
    popMatrix();
  }

  //SUN
  {
    pushMatrix(); 
    rotateY(rotateAngle); 
    stroke(255, 198, 53, sphereFillAlphaModifier); 
    int maxuser = 3;
    sunTargetRedness = ceil(map(userCount, 0, maxuser, 0, 255));
    if (sunRedness > sunTargetRedness && sunRedness > 0)sunRedness-=10;
    if (sunRedness < sunTargetRedness && sunRedness < 255)sunRedness+=10;
    //    println("sun alpha::" + (sphereFillAlpha-sphereFillAlphaModifier));
    fill(sunRedness, 0, 0, sphereFillAlpha-sphereFillAlphaModifier);
    //    println("redness::::" + sunRedness);
    sunTargetHeight = map(userCount, 0, maxuser, 0, -500);

    //    println("targetHeight--->" + sunTargetHeight);
    //    println("currentHeight->" + sunYpos);
    if (sunYpos > sunTargetHeight)sunYpos-=5;
    if (sunYpos < sunTargetHeight)sunYpos+=5;
    translate(0, sunYpos, 0); 
    //    translate(0, -300, 0);
    sphereDetail(50); 
    radio += keyBandHeights[1] * 3;
    sphere(radio); 
    for (int i = 0; i < cuantos; i++) {
      lista[i].display();
    }
    popMatrix();
  }


  rotateAngle += 0.003f;
  radio = radioOri;
}

public void mouseClicked() {
  //  magnitude = random(10, 100);
  cubeMatrix.isMoving = !cubeMatrix.isMoving;
}



// -----------------------------------------------------------------
// hand events


public void onNewUser(SimpleOpenNI curContext, int userId)
{
  println("onNewUser - userId: " + userId);
  println("\tstart tracking skeleton");
  userCount++;
  println(userCount);
  context.startTrackingSkeleton(userId);
}

public void onLostUser(SimpleOpenNI curContext, int userId)
{
  println("onLostUser - userId: " + userId);
  userCount--;
  println(userCount);
}


public void onNewHand(SimpleOpenNI curContext, int handId, PVector pos)
{
  println("onNewHand - handId: " + handId + ", pos: " + pos); 

  ArrayList<PVector> vecList = new ArrayList<PVector>(); 
  vecList.add(pos); 

  handPathList.put(handId, vecList);
}

public void onTrackedHand(SimpleOpenNI curContext, int handId, PVector pos)
{
  //println("onTrackedHand - handId: " + handId + ", pos: " + pos );

  ArrayList<PVector> vecList = handPathList.get(handId); 
  if (vecList != null)
  {
    vecList.add(0, pos); 
    if (vecList.size() >= handVecListSize)
      // remove the last point 
      vecList.remove(vecList.size()-1);
  }
}

public void onLostHand(SimpleOpenNI curContext, int handId)
{
  println("onLostHand - handId: " + handId); 
  handMovingDisp = 0; 
  handMovingDir = new PVector(0, 0, 0); 
  handPathList.remove(handId);
}

// -----------------------------------------------------------------
// gesture events

public void onCompletedGesture(SimpleOpenNI curContext, int gestureType, PVector pos)
{
  println("onCompletedGesture - gestureType: " + gestureType + ", pos: " + pos); 

  context.startTrackingHand(pos); 

  int handId = context.startTrackingHand(pos); 
  println("hand stracked: " + handId);
}

// -----------------------------------------------------------------

class Cube {
  float xpos;
  float ypos;  
  float zpos;
  float size = 60;
  float yspeed=0;
  int target = -200;
//  boolean isMoving = false; 

  int fill = color(0);
  int strokeColor = color(255);


  Cube(float tempXpos, float tempYpos, float tempZpos, float tempSize) {
    xpos = tempXpos;
    ypos = tempYpos;
    zpos = tempZpos;
    size = tempSize;
  }

  public void display() {
    pushMatrix();
    translate(xpos, ypos, zpos);
    fill(fill);
    stroke(strokeColor, 100);
    strokeWeight(1);
    box(size, size*10, size);
    popMatrix();
  }

  public void move() {
    ypos = ypos + yspeed;
    if (ypos < target || ypos > 0) {
      yspeed=-yspeed;
    }
  }

  public void assignSpeed(){
    yspeed = -2;
  }

  public void reset() {
    if(yspeed<0){
      yspeed = yspeed*-1;
    }
    if (ypos < 0) {
      ypos = ypos + yspeed;
    }else if(ypos >= 0){
      yspeed=0;
    }
  }
  
}

class CubeMatrix {

  int n = 14;
  Cube[][] myCubes = new Cube[n][n];
  boolean isMoving = false;
  boolean speedAssigned = false;
  long lastTime = 0;
  int tempN = 13;

  CubeMatrix() {
    for (int j=0; j<n; j++) { 
      for (int i=0; i<n; i++) {
        myCubes[i][j] = new Cube(i*150, 0, j*150, 100);
      }
    }
  }

  public void display() {
    pushMatrix();
    translate(0,300, -1000);
    rotateY(-PI/4);
    rotateX(-PI/15);
    rotateZ(PI/15);
    for (int j=0; j<n; j++) { 
      for (int i=0; i<n; i++) {
        myCubes[i][j].display();
      }
    }
    popMatrix();
  }

  public void assignSpeed() {
    if (millis() - lastTime > 200) {
      for (int j=0; j<n; j++) {
        myCubes[tempN][j].assignSpeed();
      }
      lastTime = millis();
      if (tempN>0) {
        tempN--;
        //        println(tempN);
      } else if (tempN==0) {
        speedAssigned=true;
      }
    }
  }

  public void move() {
    for (int j=0; j<n; j++) { 
      for (int i=0; i<n; i++) {
        myCubes[i][j].move();
      }
    }
  }

  public void reset() {
    for (int j=0; j<n; j++) { 
      for (int i=0; i<n; i++) {
        myCubes[i][j].reset();
      }
    }
    speedAssigned = false;
    tempN=13;
    lastTime = 0;
  }
}

class GeoTester {

  float size = 100;
  float scaler = 1;
  int details = 5;

  public void display() {
    pushMatrix();
    pushStyle();
    noFill();
    stroke(255, 100);
    strokeWeight(0.5f);
    sphereDetail(details);
    sphere(size);
    popStyle();
    popMatrix();
    scaler = 1;
  }

  public void changeScale(float _mul){
    scaler += _mul;
  }

  public void sizeIncre(ArrayList<PVector> points) {
    size = 100;
    if (points!=null) {
      size+=PVector.dist(points.get(0), points.get(points.size()-1));
    }
    
  }
}

class NoiseWave {



  float yoff = 0.0f;        // 2nd dimension of perlin noise

  public void display() {
    background(51);

    fill(255);
    // We are going to draw a polygon out of the wave points
    beginShape(); 
    float xoff = 0;       // Option #1: 2D Noise

    for (float x = 0; x <= width; x += 10) {
      // Calculate a y value according to noise, map to 
      float y = map(noise(xoff, yoff), 0, 1, 200, 300); // Option #1: 2D Noise

      // Set the vertex
      vertex(x, y); 
      // Increment x dimension for noise
      xoff += 0.05f;
    }
    // increment y dimension for noise
    yoff += 0.01f;
    vertex(width, height);
    vertex(0, height);
    endShape(CLOSE);
  }
}


class Pelo {

  float z = random(-radio, radio);
  float phi = random(TWO_PI);
  float len = random(0.9f, 1.2f);
  float theta = asin(z/radio);
  float extraLen = 0;
  boolean different;
  

  Pelo() {
    if (random(0, 100)>90) {
      different = true;
    } else {
      different = false;
    }
  }
  public void display() {
    float off = (noise(millis() * 0.0005f, sin(phi))-0.5f) * 0.3f;
    float offb = (noise(millis() * 0.0007f, sin(z) * 0.01f)-0.5f) * 0.3f;

    float thetaff = theta+off;
    float phff = phi+offb;
    float x = radio * cos(theta) * cos(phi);
    float y = radio * cos(theta) * sin(phi);
    float z = radio * sin(theta);
    float msx= screenX(x, y, z);
    float msy= screenY(x, y, z);


    float xo = radio * cos(thetaff) * cos(phff);
    float yo = radio * cos(thetaff) * sin(phff);
    float zo = radio * sin(thetaff);


    float xb, yb, zb;

    if (different) {
      xb = xo * (len + extraLen);
      yb = yo * (len + extraLen);
      zb = zo * (len + extraLen);
    } else {
      xb = xo * len;
      yb = yo * len;
      zb = zo * len;
    }



    beginShape(LINES);
    pushStyle();
    strokeWeight(0.5f);
    stroke(0);
    vertex(x, y, z);
    stroke(200, 150);
    vertex(xb, yb, zb);
    endShape();
    extraLen = 0;
    popStyle();
  }
  
}

class SimpleMesh {
  ArrayList<SimpleMeshPar> particles;
  float range = 400;
  PVector center = new PVector(0, 0, 0);
  float radius = 600;
  int initCount = 1;
  float minConnectDist;
  int maxLength = 25;

  SimpleMesh() {
    particles = new ArrayList<SimpleMeshPar>();
    minConnectDist = radius * 3;
    for (int i=0; i<initCount; i++) {
      particles.add(new SimpleMeshPar());
    };
  }

  public void calculate() {
    for (int i=0; i<particles.size ()-1; i++) {
      particles.get(i).uu = particles.get(i).uu + (0.001f + particles.get(i).movingspeed);  
      particles.get(i).vv = particles.get(i).vv + (0.001f + particles.get(i).movingspeed); 
      particles.get(i).location.x = radius * sin(particles.get(i).uu) * cos( particles.get(i).vv)+center.x;
      particles.get(i).location.y = radius * cos(particles.get(i).uu) * cos( particles.get(i).vv)+center.y;
      particles.get(i).location.z = radius * sin( particles.get(i).vv) + center.z;
    }

    for (int i=0; i<particles.size ()-1; i++) {
      if (millis()-particles.get(i).timer > particles.get(i).life) {
        particles.remove(i);
      }
    }
  }

  public void reactMus(float mag) {
    for (SimpleMeshPar p : particles) {
      p.location = PVector.add(PVector.mult(p.noiseDir, mag), p.location);
    }
  }

  public void display() {
    fill(0);
    stroke(255);
    strokeWeight(8);

    beginShape(POINTS);
    for (SimpleMeshPar p : particles) {
      vertex(p.location.x, p.location.y, p.location.z);
    }
    endShape();

    for (int i=0; i<particles.size ()-1; i++) {
      for (int y=0; y<particles.size ()-1; y++) {
        if (PVector.dist(particles.get(i).location, particles.get(y).location) < minConnectDist) {
          stroke(0);
          strokeWeight(1);
          beginShape(LINES);
          stroke(particles.get(i).c);
          vertex(particles.get(i).location.x, particles.get(i).location.y, particles.get(i).location.z);
          stroke(particles.get(y).c); 
          vertex(particles.get(y).location.x, particles.get(y).location.y, particles.get(y).location.z);
          endShape();
        }
      }
    }
  }

}

class SimpleMeshPar {
  PVector location;
  float uu = random(0, 40);
  float vv = random(0, 40);
  float movingspeed = random(0.002f, 0.0001f);
  int c;
  PVector noiseDir;
  float noiseDisScaler;
  float timer;
  float life = 20000;

  SimpleMeshPar() {
    noiseDisScaler = random(1, 3);
    location = new PVector();
    float luck = random(0, 100);
    //    if (luck > 50) c = color(0, 255, 255, 200);
    //    if (luck <  50) c =color(255, 255, 0, 100);
    c = color(200, 40);
    noiseDir = PVector.mult(PVector.random3D(), noiseDisScaler);
    timer = millis();
  }
  
}

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--full-screen", "--bgcolor=#FFFFFF", "--stop-color=#cccccc", "handstracking" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
