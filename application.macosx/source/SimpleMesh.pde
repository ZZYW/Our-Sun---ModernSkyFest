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

  void calculate() {
    for (int i=0; i<particles.size ()-1; i++) {
      particles.get(i).uu = particles.get(i).uu + (0.001 + particles.get(i).movingspeed);  
      particles.get(i).vv = particles.get(i).vv + (0.001 + particles.get(i).movingspeed); 
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

  void reactMus(float mag) {
    for (SimpleMeshPar p : particles) {
      p.location = PVector.add(PVector.mult(p.noiseDir, mag), p.location);
    }
  }

  void display() {
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

