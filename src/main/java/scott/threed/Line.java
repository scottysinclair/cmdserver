package scott.threed;

public class Line {
  private int start, end;

  public Line(int s, int e) {
		this.start = s;
		this.end = e;
	}

  public int start() {
		return start;
	}

  public int end() {
		return end;
	}
}