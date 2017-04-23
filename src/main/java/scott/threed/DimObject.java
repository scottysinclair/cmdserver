package scott.threed;

import java.awt.Graphics;

public class DimObject {
	private Point calculatedPoints[];
	private Point points[];
	private Line lines[];
	private Point angle;
	private Point position;
	private TestBoard board;

	public DimObject(TestBoard board, Point points[], Line lines[]) {
		this.board = board;
		this.points = points;
		this.lines = lines;
		this.angle = new Point(0, 0,0);
		this.position = new Point(0, 0, 0);
		this.calculatedPoints = new Point[ points.length ];
		calculate();
	}

	public void setPosition(Point position) {
		this.position = position;
	}

	public void incrementAngle(Point diff) {
		angle.setX( angle.getX() + diff.getX() );
		angle.setY( angle.getY() + diff.getY() );
		angle.setZ( angle.getZ() + diff.getZ() );
	}

	public void calculate() {
		double xang = Math.toRadians(angle.getX());
		double yang = Math.toRadians(angle.getY());
		double zang = Math.toRadians(angle.getZ());

		for (int i=0; i<points.length; i++) {
			int x1 = (int)(  (Math.cos(zang) * (double)points[i].getX()) +
											(Math.sin(zang) * (double)points[i].getY()) );

			int y1 = (int)(  (Math.cos(zang) * (double)points[i].getY()) -
											(Math.sin(zang) * (double)points[i].getX()) );

			int z1 = points[i].getZ();

			int x2 = (int)(  (Math.cos(yang) * (double)x1) +
											 (Math.sin(yang) * (double)z1) );

			int z2 = (int)(  (Math.cos(yang) * (double)z1) -
											 (Math.sin(yang) * (double)x1) );

			int y2 = (int)(  (Math.cos(xang) * (double)y1) -
								 			 (Math.sin(xang) * (double)z2) );

			int z3 = (int)(  (Math.cos(xang) * (double)z2) -
											 (Math.sin(xang) * (double)y1) );

			calculatedPoints[i] = new Point(x2, y2, z3);
	  }
	}


	public void draw(Graphics g) {
		calculate();
		double fov = Math.toRadians(120);
		double tanfov = Math.tan(fov);

		int px = position.getX();
		int py = position.getY();
		int pz = position.getZ();
		for (int i=0; i<lines.length; i++) {
			Point a = calculatedPoints[ lines[i].start() ];
			Point b = calculatedPoints[ lines[i].end() ];

			int z1 = a.getZ() + pz;
			int z2 = b.getZ() + pz;

		  //do perspective
 		  double factor1 = tanfov * z1;
 		  double factor2 = tanfov * z2;
 		  System.out.println(factor1);

			int x1 = (int)(a.getX() * factor1);
			int y1 = (int)(a.getY() * factor1);
			int x2 = (int)(b.getX() * factor2);
			int y2 = (int)(b.getY() * factor2);

			x1 += px;
			y1 += py;
			x2 += px;
			y2 += py;

			g.drawLine(x1, y1, x2, y2);
		}
	}

}