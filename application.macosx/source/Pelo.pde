
class Pelo {

  float z = random(-radio, radio);
  float phi = random(TWO_PI);
  float len = random(0.9, 1.2);
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
  void display() {
    float off = (noise(millis() * 0.0005, sin(phi))-0.5) * 0.3;
    float offb = (noise(millis() * 0.0007, sin(z) * 0.01)-0.5) * 0.3;

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
    strokeWeight(0.5);
    stroke(0);
    vertex(x, y, z);
    stroke(200, 150);
    vertex(xb, yb, zb);
    endShape();
    extraLen = 0;
    popStyle();
  }
  
}

