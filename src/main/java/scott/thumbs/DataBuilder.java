package scott.thumbs;

import java.io.*;
import java.util.*;

/** This class is responsible for building a data structure consiting of
 *  Links and Directories. A Link contains one image file and one thumbnail
 *  that image.
 */
public class DataBuilder implements ThumbMaker.ThumbEvents {

  private Map rootDirs;
  private Map dirs;
  private java.util.List links;

  public DataBuilder() {
    links = new ArrayList();
    rootDirs = new HashMap();
    dirs = new HashMap();
  }

  public java.util.List getLinks() {
    return links;
  }

  public void organizeLinks() throws IOException {
    Collections.sort( links );
    Iterator i = links.iterator();
    while(i.hasNext()) {
      setupDirs((Link)i.next());
    }
  }

  public java.util.List getRootDirs() {
    return new ArrayList(rootDirs.values());
  }

  public void notifyThumbMatched(File sourceFile, File thumbFile) {
    addLink(sourceFile, thumbFile);
    System.out.println("match " + sourceFile);
  }

  public void notifyThumbCreated(File sourceFile, File thumbFile)  {
    addLink(sourceFile, thumbFile);
  }

  private Directory getDirectory(File file) throws IOException {
    File c = file.getCanonicalFile();
    Directory d = (Directory)dirs.get( c );
    if (d == null) {
      d = new Directory( file );
      dirs.put(c, d);
    }
    return d;
  }

  private void setupDirs(Link link) throws IOException  {
    File parentDir = link.getSourceFile().getParentFile();
    Directory d = getDirectory( parentDir );
    d.addLink( link );

    Directory childDir = d;
    parentDir = parentDir.getParentFile();
    while(parentDir != null) {
      d = getDirectory( parentDir );
      d.addChildDir( childDir );
      childDir.setParent( d );
      childDir = d;
      parentDir = parentDir.getParentFile();
    }

    //childDir is now the root
    addRoot( childDir );
  }

  private void addRoot(Directory rootDir) {
    rootDirs.put(rootDir.getFile(), rootDir);
  }

  private void addLink(File sourceFile, File thumbFile) {
    Link link = new Link(sourceFile, thumbFile);
    links.add( link );
  }
}


class Directory {
  private File dir;
  private Directory parent;
  private java.util.List childList;
  private java.util.List linkList;


  public Directory(File dir) {
    this.dir = dir;
    childList = new ArrayList();
    linkList = new ArrayList(0);
  }

  public void setParent(Directory parent) {
    this.parent = parent;
  }

  public File getFile() {
    return dir;
  }

  public void addChildDir(Directory dir) {
    childList.remove( dir );
    childList.add( dir );
  }

  public Iterator childDirs() {
    return childList.iterator();
  }

  public void addLink(Link link) {
    linkList.add( link );
  }

  public java.util.List getLinks() {
    return linkList;
  }

  public java.util.List getAllLinks() {
    ArrayList l = new ArrayList();
    l.addAll( linkList );

    Iterator i = childList.iterator();
    while(i.hasNext()) {
      Directory child = (Directory)i.next();
      l.addAll(  child.getAllLinks() );
    }
    return l;
  }

  public int getLocalCount() {
    return linkList.size();
  }

  public int getCount() {
    int total = 0;
    Iterator i = childList.iterator();
    while(i.hasNext()) {
      total += ((Directory)i.next()).getCount();
    }
    total += linkList.size();
    return total;
  }

}


/** a link between an image file and a thumbnail version */
class Link implements Comparable {
  private File sourceFile;
  private File thumbFile;

  public Link(File sourceFile, File thumbFile) {
    this.sourceFile = sourceFile;
    this.thumbFile = thumbFile;
  }

  public File getSourceFile() {
    return sourceFile;
  }

  public File getThumbFile() {
    return thumbFile;
  }

  public int compareTo(Object object) {
    Link link = null;
    if (object instanceof Link == false) {
      throw new ClassCastException(object.getClass().getName());
    }

    link = (Link)object;
    return sourceFile.compareTo( link.getSourceFile() );
  }
}
