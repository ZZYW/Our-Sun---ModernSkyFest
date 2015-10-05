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

  void display() {
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

  void assignSpeed() {
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

  void move() {
    for (int j=0; j<n; j++) { 
      for (int i=0; i<n; i++) {
        myCubes[i][j].move();
      }
    }
  }

  void reset() {
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

