import peasy.test.*;
import peasy.org.apache.commons.math.*;
import peasy.*;
import peasy.org.apache.commons.math.geometry.*;
import java.util.Map;
import java.util.Iterator;
import SimpleOpenNI.*;
import ddf.minim.analysis.*;
import ddf.minim.*;

PVector overallTranslation;
PShader blur;

SimpleOpenNI context;
float        zoomF =0.5f;
float        rotX = radians(180);  // by default rotate the hole scene 180deg around the x-axis, 
// the data from openni comes upside down
float        rotY = radians(0);
int          handVecListSize = 30;
Map<Integer, ArrayList<PVector>>  handPathList = new HashMap<Integer, ArrayList<PVector>>();
color[]       userClr = new color[] { 
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

color sunTargetRedness;
color sunRedness;

//cube matrix
CubeMatrix cubeMatrix;

//mesh
SimpleMesh mesh;
SimpleMesh scatterParticles;



void setup()
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

void draw()
{


  if (keyBandHeights[4] > 0.4 || handMovingDisp > 0.6) {
    background(255);
    sphereFillAlphaModifier = 255;
    for (int j=0; j<14; j++) { 
      for (int i=0; i<14; i++) {
        cubeMatrix.myCubes[i][j].fill = #FFCFCF;
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

  if ((keyBandHeights[4] > 0.5 || mousePressed) && mesh.particles.size() < mesh.maxLength) {
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


  rotateAngle += 0.003;
  radio = radioOri;
}

void mouseClicked() {
  //  magnitude = random(10, 100);
  cubeMatrix.isMoving = !cubeMatrix.isMoving;
}



// -----------------------------------------------------------------
// hand events


void onNewUser(SimpleOpenNI curContext, int userId)
{
  println("onNewUser - userId: " + userId);
  println("\tstart tracking skeleton");
  userCount++;
  println(userCount);
  context.startTrackingSkeleton(userId);
}

void onLostUser(SimpleOpenNI curContext, int userId)
{
  println("onLostUser - userId: " + userId);
  userCount--;
  println(userCount);
}


void onNewHand(SimpleOpenNI curContext, int handId, PVector pos)
{
  println("onNewHand - handId: " + handId + ", pos: " + pos); 

  ArrayList<PVector> vecList = new ArrayList<PVector>(); 
  vecList.add(pos); 

  handPathList.put(handId, vecList);
}

void onTrackedHand(SimpleOpenNI curContext, int handId, PVector pos)
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

void onLostHand(SimpleOpenNI curContext, int handId)
{
  println("onLostHand - handId: " + handId); 
  handMovingDisp = 0; 
  handMovingDir = new PVector(0, 0, 0); 
  handPathList.remove(handId);
}

// -----------------------------------------------------------------
// gesture events

void onCompletedGesture(SimpleOpenNI curContext, int gestureType, PVector pos)
{
  println("onCompletedGesture - gestureType: " + gestureType + ", pos: " + pos); 

  context.startTrackingHand(pos); 

  int handId = context.startTrackingHand(pos); 
  println("hand stracked: " + handId);
}

// -----------------------------------------------------------------

