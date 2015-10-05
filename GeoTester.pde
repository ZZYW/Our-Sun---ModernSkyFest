class GeoTester {

  float size = 100;
  float scaler = 1;
  int details = 5;

  void display() {
    pushMatrix();
    pushStyle();
    noFill();
    stroke(255, 100);
    strokeWeight(0.5);
    sphereDetail(details);
    sphere(size);
    popStyle();
    popMatrix();
    scaler = 1;
  }

  void changeScale(float _mul){
    scaler += _mul;
  }

  void sizeIncre(ArrayList<PVector> points) {
    size = 100;
    if (points!=null) {
      size+=PVector.dist(points.get(0), points.get(points.size()-1));
    }
    
  }
}

