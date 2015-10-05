class Cube {
  float xpos;
  float ypos;  
  float zpos;
  float size = 60;
  float yspeed=0;
  int target = -200;
//  boolean isMoving = false; 

  color fill = color(0);
  color strokeColor = color(255);


  Cube(float tempXpos, float tempYpos, float tempZpos, float tempSize) {
    xpos = tempXpos;
    ypos = tempYpos;
    zpos = tempZpos;
    size = tempSize;
  }

  void display() {
    pushMatrix();
    translate(xpos, ypos, zpos);
    fill(fill);
    stroke(strokeColor, 100);
    strokeWeight(1);
    box(size, size*10, size);
    popMatrix();
  }

  void move() {
    ypos = ypos + yspeed;
    if (ypos < target || ypos > 0) {
      yspeed=-yspeed;
    }
  }

  void assignSpeed(){
    yspeed = -2;
  }

  void reset() {
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

