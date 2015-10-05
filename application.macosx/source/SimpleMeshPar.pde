class SimpleMeshPar {
  PVector location;
  float uu = random(0, 40);
  float vv = random(0, 40);
  float movingspeed = random(0.002, 0.0001);
  color c;
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

