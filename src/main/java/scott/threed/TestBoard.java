package scott.threed;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class TestBoard extends JFrame implements MouseMotionListener {

    private Canvas canvas;
    private List objects;
    private Point center;

    public TestBoard() {
        super("test board");
        getContentPane().add(canvas = new Canvas() {
            public void paint(Graphics g) {
                draw(g);
            }
        });
        objects = new ArrayList();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        center = new Point(100, 100, 0);
        canvas.addMouseMotionListener(this);
    }

    public void addObject(DimObject obj) {
        objects.add(obj);
    }

    private void draw(Graphics g) {
        for (Iterator i=objects.iterator(); i.hasNext();) {
            DimObject obj = (DimObject)i.next();
            obj.draw(g);
        }
    }

    public Point getCenter() {
        return center;
    }

  int lastX = 0;
  int lastY = 0;
    public void mouseMoved(MouseEvent e)  {
        //System.out.println("moved");
    }

    public void mouseDragged(MouseEvent e)  {
        //System.out.println("dragged");
        int diffX = 0, diffY = 0, diffZ = 0;

        if (e.isShiftDown() == false) {
            diffX = e.getX() - lastX;
        }
        else {
            diffZ = (e.getX() - lastX);
        }
        diffY = (e.getY() - lastY) * -1;

        lastX = e.getX();
      lastY = e.getY();

//		System.out.println("dragged " + diffX + ", " + diffY);


        Point diff = new Point(diffX, diffY, diffZ);
        for (Iterator i=objects.iterator(); i.hasNext();) {
            DimObject obj = (DimObject)i.next();
            obj.incrementAngle(diff);
        }
        canvas.repaint();
    }


    public static void main(String args[]) {
        TestBoard board = new TestBoard();
        board.setSize(200, 300);
        board.setLocation(300, 300);

        Point points[] = new Point[]{ new Point(-10, 10, 10), new Point(10, 10, 10),
                                                                    new Point(10, -10, 10), new Point(-10, -10, 10),
                                                                    new Point(-10, 10, -10), new Point(10, 10, -10),
                                                                    new Point(10, -10, -10), new Point(-10, -10, -10)
                                                                };
        Line lines[] = new Line[]{  new Line(0,1), new Line(1,2), new Line(2,3), new Line(3,0),
                                                                new Line(4,5), new Line(5,6), new Line(6,7), new Line(7,4),
                                                                new Line(0,4), new Line(1,5), new Line(2,6), new Line(3,7),
                                                         };

        DimObject obj = new DimObject(board, points, lines);
        obj.setPosition(new Point(100, 100, 1000));
        board.addObject(obj);
        board.setVisible(true);
    }
}