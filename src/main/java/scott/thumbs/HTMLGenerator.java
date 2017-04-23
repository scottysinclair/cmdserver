package scott.thumbs;

import java.io.*;
import java.util.*;

/**
 * This class is responsible for generating the HTML Pages from the links/directories
 * data structure.  This is done in two stages.
 * 1) IndexPage and ThumbsPage objects are built with a certain distribution of links
 * on the thumbnail pages.
 * 2) The IndexPage and ThumbsPage data structure is processed and the HTML files are created 1:1
 */
public class HTMLGenerator {

  private java.util.List indexPages;

  public HTMLGenerator() {
    indexPages = new ArrayList();
  }

  public void buildHTML(DataBuilder dataBuilder, int maxImages) throws IOException {
    dataBuilder.organizeLinks();

    java.util.List rootDirs = dataBuilder.getRootDirs();

    Iterator i = rootDirs.iterator();
    while(i.hasNext()) {
      Directory dir = (Directory)i.next();
      IndexPage indexPage = new IndexPage( dir.getFile() );
      indexPages.add( indexPage );
      processDir(indexPage, dir, maxImages);
    }
  }

  /** writes out the whole pages data structure */
  public void writeOut() throws IOException {
    Iterator i = indexPages.iterator();
    while(i.hasNext()) {
      IndexPage indexPage = (IndexPage)i.next();
      write( indexPage );
    }
  }

  private void write(IndexPage indexPage) throws IOException  {
    //if the index page will only have one item then we don't write the index.
    if (indexPage.numPages() == 1) {
      ThumbsPage page = (ThumbsPage)indexPage.thumbPages().next();
      write(indexPage, page);
      return;
    }

    //open a stream to the index file
    String indexDir = getIndexDirectoryName( indexPage );
    PrintWriter indexWriter = new PrintWriter( new BufferedWriter( new FileWriter( indexPage.getFile() ) ) );
    indexWriter.println("<html>\n<head>\n<title>Index Of " + indexDir + "</title>\n</head>\n<body>\n<p>");
    indexWriter.println("<h1>Index Of " + indexDir + "</h1>");

    //write out each thumb page and write out the entry in the index.
    Iterator i = indexPage.thumbPages();
    while(i.hasNext()) {
      ThumbsPage thumbsPage = (ThumbsPage)i.next();
      write(indexPage, thumbsPage);

      String thumbsURL = getThumbsURL(indexPage, thumbsPage);
      String thumbsName = getThumbsName(indexPage, thumbsPage);
      indexWriter.println("<a href=\"" + thumbsURL + "\">" + thumbsName + "</a></br>");
    }
    indexWriter.println("</p>\n</body>\n</html>");
    indexWriter.flush();
    indexWriter.close();
  }

  /** writes a directory description in a thumbs page. */
  private void writeDescription(PrintWriter thumbWriter, ThumbsPage thumbsPage, File thumbsDir) throws IOException  {
    File descriptionFile = new File(thumbsDir, "description.txt");

    if (descriptionFile.exists()) {
      BufferedReader reader = new BufferedReader( new FileReader( descriptionFile ) );
      String description = reader.readLine();
      while(description != null) {
        thumbWriter.println(description);
        description = reader.readLine();
      }
      thumbWriter.println("<br/>");
      reader.close();
    }
  }

  /** writes out a thumbs page. */
  private void write(IndexPage indexPage, ThumbsPage thumbsPage) throws IOException {
    System.out.println("writing thumb page " + thumbsPage.getFile());
    String thumbPath = getThumbsURL( indexPage, thumbsPage );
    PrintWriter thumbWriter = new PrintWriter( new BufferedWriter( new FileWriter( thumbsPage.getFile() ) ) );
    thumbWriter.println("<html>\n<head>\n<title>Index Of " + thumbPath + "</title>\n</head>\n<body>\n<p>");

    File currentDir = null;
    Iterator i = thumbsPage.getLinks().iterator();
    while(i.hasNext()) {
      Link link = (Link)i.next();
      String sourceURL = getSourceURL(thumbsPage, link);
      String thumbURL = getThumbURL(thumbsPage, link);
      File linkDir = getLinkDirectory(link);

      if (!linkDir.equals(currentDir)) {
        String dirName = getLinkDirName(thumbsPage, link);
        thumbWriter.println("<h1>" + dirName + "</h1>");

        //write out a description if we have one
        writeDescription(thumbWriter, thumbsPage, new File(linkDir, "thumbs"));
        currentDir = linkDir;
      }
      thumbWriter.println("<a href=\"" + sourceURL + "\"><img src=\"" + thumbURL + "\"/></a>");
    }
    thumbWriter.println("</p>\n</body>\n</html>");
    thumbWriter.flush();
    thumbWriter.close();
  }

  private File getLinkDirectory(Link link) throws IOException {
    return link.getSourceFile().getCanonicalFile().getParentFile();
  }

  private String getLinkDirName(ThumbsPage thumbPage, Link link) throws IOException {
    File linkDir = getLinkDirectory( link );
    File root = thumbPage.getFile().getCanonicalFile().getParentFile();
    if (root.equals( linkDir )) {
      String dirName = linkDir.getName();
      if (thumbPage.getInfo().length() > 0) {
        dirName = dirName + " - " + thumbPage.getInfo();
      }
      return dirName;
    }
    else {
      return getRelativePath(root, linkDir, ", ");
    }
  }

  //gets the url of the source image of a link relative to the thumbs page
  private String getSourceURL(ThumbsPage thumbPage, Link link) throws IOException {
    File root = thumbPage.getFile().getCanonicalFile().getParentFile();
    File sourceFile = link.getSourceFile().getCanonicalFile();
    return getRelativePath(root, sourceFile, "/");
  }

  private String getIndexDirectoryName(IndexPage indexPage) throws IOException {
    return indexPage.getFile().getCanonicalFile().getParentFile().getName();
  }

  private String getThumbsName(IndexPage indexPage, ThumbsPage thumbPage) throws IOException {
    File root = indexPage.getFile().getCanonicalFile().getParentFile();
    File thumbFile = thumbPage.getFile().getCanonicalFile().getParentFile();
    String path = getRelativePath(root, thumbFile, ", ");
    if (thumbPage.getInfo().length() > 0) {
      path = path + " - " + thumbPage.getInfo();
    }
    return path;
  }

  //the path of the thumb page relative to the index page
  private String getThumbsURL(IndexPage indexPage, ThumbsPage thumbPage) throws IOException {
    File root = indexPage.getFile().getCanonicalFile().getParentFile();
    File thumbFile = thumbPage.getFile().getCanonicalFile();
    return getRelativePath(root, thumbFile, "/");
  }

  private String getThumbURL(ThumbsPage thumbPage, Link link) throws IOException {
    File root = thumbPage.getFile().getCanonicalFile().getParentFile();
    File thumbFile = link.getThumbFile().getCanonicalFile();
    return getRelativePath(root, thumbFile, "/");
  }

  private String getRelativePath(File root, File to, String pathDelim) {
    //go back from thumbFile until we reach root
    StringBuffer url = new StringBuffer();
    File part = to;
    while(part != null && !part.equals( root )) {
      url.insert(0, part.getName());
      url.insert(0, pathDelim);
      part = part.getParentFile();
    }
    url.delete(0, pathDelim.length());
    return url.toString();
  }

  /** processes a directory and builds ThumbsPages objects */
  private void processDir(IndexPage indexPage, Directory dir, int maxImages) {
    //check if all of the pictures will fit on one page
    if (dir.getCount() > maxImages) {
      //if this directory contains images then we have to create a/many thumbs page for them
      if (dir.getLocalCount() > 0) {
        if (dir.getLocalCount() > maxImages) {  //create many thumb pages
          createManyThumbPages(indexPage, dir, maxImages);
        }
        else {
          ThumbsPage thumbsPage = new ThumbsPage("index.html", dir); //create one thumbs page
          thumbsPage.setLinks( dir.getLinks() );
          indexPage.addThumbsPage( thumbsPage );
        }
      }

      Iterator i = dir.childDirs();  //process the child directories and their pictures.
      while(i.hasNext()) {
        Directory d = (Directory)i.next();
        processDir(indexPage, d, maxImages );
      }
    }
    else {
      //all pictures fit onto this page.
      ThumbsPage thumbsPage = new ThumbsPage("index.html", dir);
      thumbsPage.setLinks( dir.getAllLinks() );
      indexPage.addThumbsPage( thumbsPage );
    }
  }

  /** creates multiple ThumbsPages for images in a single directory. */
  private void createManyThumbPages(IndexPage indexPage, Directory dir, int maxImages) {
    java.util.List links = dir.getLinks();
    int numPages = links.size() / maxImages;
    if (links.size() % maxImages > 0) {
      numPages++;
    }
    int linksConsumed = 0;
    int linksLeft = links.size();

    for (int i=1; i<=numPages; i++) {
      int numLinks = linksLeft < maxImages ? linksLeft : maxImages;
      java.util.List linkSet = links.subList(linksConsumed, linksConsumed + numLinks);
      linksConsumed += numLinks;
      linksLeft = links.size() - linksConsumed;

      ThumbsPage thumbsPage = new ThumbsPage("index" + i + ".html", dir, "part " + i);
      thumbsPage.setLinks( linkSet );
      indexPage.addThumbsPage( thumbsPage );
    }
  }
}

/** represents the index page. */
class IndexPage {
  private File file;
  private java.util.List pages;

  public IndexPage(File dir) {
    file = new File(dir, "picIndex.html");
    pages = new ArrayList();
  }

  public File getFile() {
    return file;
  }

  public Iterator thumbPages() {
    return pages.iterator();
  }

  public void addThumbsPage(ThumbsPage page) {
    pages.add( page );
  }

  public int numPages() {
    return pages.size();
  }
}

/** represents a thumbnail index. */
class ThumbsPage {
  private File file;
  private Directory dir;
  private java.util.List links;
  private String info;

  public ThumbsPage(String fileName, Directory dir) {
    this(fileName, dir, "");
  }

  public ThumbsPage(String fileName, Directory dir, String info) {
    this.dir = dir;
    this.file = new File(dir.getFile(), fileName);
    this.info = info;
    links = new ArrayList(0);
  }

  public String getInfo() {
    return info;
  }

  public File getFile() {
    return file;
  }

  public void setLinks(java.util.List links) {
    this.links = links;
  }

  public java.util.List getLinks() {
    return links;
  }

}
