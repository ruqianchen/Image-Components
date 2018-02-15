/**
 * Created by chenruqian on 11/28/2016, adding codes to the starter java code for CSE373 optional union-find assignment
 *
 * Name: CHEN, RUQIAN
 * Email: ruqian@uw.edu
 */

/*
 *
 * ImageComponents.java
 * Starter code for optional assignment UF // Change this line to "UF Solution by " + YOUR_NAME and UWNetID.
 *
 *
 * CSE 373, University of Washington, Autumn 2016.
 *
 * Starter Code for CSE 373 Optional Assignment UF, Part II.    Starter Code Version 1.
 * S. Tanimoto
 *
 */

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImageComponents extends JFrame implements ActionListener {
    public static ImageComponents appInstance; // Used in main().

    String startingImage = "gettysburg2.png";
    BufferedImage biTemp, biWorking, biFiltered; // These hold arrays of pixels.
    Graphics gOrig, gWorking; // Used to access the drawImage method.
    int w;// width of the current image.
    int h; // height of the current image.
    int[][] parentID; // For the forest of up-trees.

    JPanel viewPanel; // Where the image will be painted.
    JPopupMenu popup;
    JMenuBar menuBar;
    JMenu fileMenu, imageOpMenu, ccMenu, helpMenu;
    JMenuItem loadImageItem, saveAsItem, exitItem;
    JMenuItem lowPassItem, highPassItem, photoNegItem, RGBThreshItem;

    JMenuItem CCItem1;
    JMenuItem aboutItem, helpItem;

    JFileChooser fileChooser; // For loading and saving images.

    public class Color {
        int r, g, b;

        Color(int r, int g, int b) {
            this.r = r; this.g = g; this.b = b;
        }

        double euclideanDistance(Color c2) {
            // CRQ implemented
            // Return the distance between this color and c2.
            return Math.abs(c2.r-this.r) + Math.abs(c2.g-this.g) + Math.abs(c2.b-this.b);
        }

    }


    // Some image manipulation data definitions that won't change...
    static LookupOp PHOTONEG_OP, RGBTHRESH_OP;
    static ConvolveOp LOWPASS_OP, HIGHPASS_OP;

    public static final float[] SHARPENING_KERNEL = { // sharpening filter kernel
            0.f, -1.f,  0.f,
            -1.f,  5.f, -1.f,
            0.f, -1.f,  0.f
    };

    public static final float[] BLURRING_KERNEL = {
            0.1f, 0.1f, 0.1f,    // low-pass filter kernel
            0.1f, 0.2f, 0.1f,
            0.1f, 0.1f, 0.1f
    };

    public ImageComponents() { // Constructor for the application.
        setTitle("Image Analyzer");
        addWindowListener(new WindowAdapter() { // Handle any window close-box clicks.
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });

        // Create the panel for showing the current image, and override its
        // default paint method to call our paintPanel method to draw the image.
        viewPanel = new JPanel(){public void paint(Graphics g) { paintPanel(g);}};
        add("Center", viewPanel); // Put it smack dab in the middle of the JFrame.

        // Create standard menu bar
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        fileMenu = new JMenu("File");
        imageOpMenu = new JMenu("Image Operations");
        ccMenu = new JMenu("Connected Components");
        helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        menuBar.add(imageOpMenu);
        menuBar.add(ccMenu);
        menuBar.add(helpMenu);

        // Create the File menu's menu items.
        loadImageItem = new JMenuItem("Load image...");
        loadImageItem.addActionListener(this);
        fileMenu.add(loadImageItem);
        saveAsItem = new JMenuItem("Save as full-color PNG");
        saveAsItem.addActionListener(this);
        fileMenu.add(saveAsItem);
        exitItem = new JMenuItem("Quit");
        exitItem.addActionListener(this);
        fileMenu.add(exitItem);

        // Create the Image Operation menu items.
        lowPassItem = new JMenuItem("Convolve with blurring kernel");
        lowPassItem.addActionListener(this);
        imageOpMenu.add(lowPassItem);
        highPassItem = new JMenuItem("Convolve with sharpening kernel");
        highPassItem.addActionListener(this);
        imageOpMenu.add(highPassItem);
        photoNegItem = new JMenuItem("Photonegative");
        photoNegItem.addActionListener(this);
        imageOpMenu.add(photoNegItem);
        RGBThreshItem = new JMenuItem("RGB Thresholds at 128");
        RGBThreshItem.addActionListener(this);
        imageOpMenu.add(RGBThreshItem);


        // Create CC menu stuff.
        CCItem1 = new JMenuItem("Compute Connected Components and Recolor");
        CCItem1.addActionListener(this);
        ccMenu.add(CCItem1);

        // Create the Help menu's item.
        aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(this);
        helpMenu.add(aboutItem);
        helpItem = new JMenuItem("Help");
        helpItem.addActionListener(this);
        helpMenu.add(helpItem);

        // Initialize the image operators, if this is the first call to the constructor:
        if (PHOTONEG_OP==null) {
            byte[] lut = new byte[256];
            for (int j=0; j<256; j++) {
                lut[j] = (byte)(256-j);
            }
            ByteLookupTable blut = new ByteLookupTable(0, lut);
            PHOTONEG_OP = new LookupOp(blut, null);
        }
        if (RGBTHRESH_OP==null) {
            byte[] lut = new byte[256];
            for (int j=0; j<256; j++) {
                lut[j] = (byte)(j < 128 ? 0: 200);
            }
            ByteLookupTable blut = new ByteLookupTable(0, lut);
            RGBTHRESH_OP = new LookupOp(blut, null);
        }
        if (LOWPASS_OP==null) {
            float[] data = BLURRING_KERNEL;
            LOWPASS_OP = new ConvolveOp(new Kernel(3, 3, data),
                    ConvolveOp.EDGE_NO_OP,
                    null);
        }
        if (HIGHPASS_OP==null) {
            float[] data = SHARPENING_KERNEL;
            HIGHPASS_OP = new ConvolveOp(new Kernel(3, 3, data),
                    ConvolveOp.EDGE_NO_OP,
                    null);
        }
        loadImage(startingImage); // Read in the pre-selected starting image.
        setVisible(true); // Display it.
    }

    /*
     * Given a path to a file on the file system, try to load in the file
     * as an image.  If that works, replace any current image by the new one.
     * Re-make the biFiltered buffered image, too, because its size probably
     * needs to be different to match that of the new image.
     */
    public void loadImage(String filename) {
        try {
            biTemp = ImageIO.read(new File(filename));
            w = biTemp.getWidth();
            h = biTemp.getHeight();
            viewPanel.setSize(w,h);
            biWorking = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gWorking = biWorking.getGraphics();
            gWorking.drawImage(biTemp, 0, 0, null);
            biFiltered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            pack(); // Lay out the JFrame and set its size.
            repaint();
        } catch (IOException e) {
            System.out.println("Image could not be read: "+filename);
            System.exit(1);
        }
    }

    /* Menu handlers
     */
    void handleFileMenu(JMenuItem mi){
        System.out.println("A file menu item was selected.");
        if (mi==loadImageItem) {
            File loadFile = new File("image-to-load.png");
            if (fileChooser==null) {
                fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(loadFile);
                fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", new String[] { "JPG", "JPEG", "GIF", "PNG" }));
            }
            int rval = fileChooser.showOpenDialog(this);
            if (rval == JFileChooser.APPROVE_OPTION) {
                loadFile = fileChooser.getSelectedFile();
                loadImage(loadFile.getPath());
            }
        }
        if (mi==saveAsItem) {
            File saveFile = new File("savedimage.png");
            fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(saveFile);
            int rval = fileChooser.showSaveDialog(this);
            if (rval == JFileChooser.APPROVE_OPTION) {
                saveFile = fileChooser.getSelectedFile();
                // Save the current image in PNG format, to a file.
                try {
                    ImageIO.write(biWorking, "png", saveFile);
                } catch (IOException ex) {
                    System.out.println("There was some problem saving the image.");
                }
            }
        }
        if (mi==exitItem) { this.setVisible(false); System.exit(0); }
    }

    void handleEditMenu(JMenuItem mi){
        System.out.println("An edit menu item was selected.");
    }

    void handleImageOpMenu(JMenuItem mi){
        System.out.println("An imageOp menu item was selected.");
        if (mi==lowPassItem) { applyOp(LOWPASS_OP); }
        else if (mi==highPassItem) { applyOp(HIGHPASS_OP); }
        else if (mi==photoNegItem) { applyOp(PHOTONEG_OP); }
        else if (mi==RGBThreshItem) { applyOp(RGBTHRESH_OP); }
        repaint();
    }

    void handleCCMenu(JMenuItem mi) {
        System.out.println("A connected components menu item was selected.");
        if (mi==CCItem1) { computeConnectedComponents(); }
    }
    void handleHelpMenu(JMenuItem mi){
        System.out.println("A help menu item was selected.");
        if (mi==aboutItem) {
            System.out.println("About: Well this is my program.");
            JOptionPane.showMessageDialog(this,
                    "Image Components, Starter-Code Version.",
                    "About",
                    JOptionPane.PLAIN_MESSAGE);
        }
        else if (mi==helpItem) {
            System.out.println("In case of panic attack, select File: Quit.");
            JOptionPane.showMessageDialog(this,
                    "To load a new image, choose File: Load image...\nFor anything else, just try different things.",
                    "Help",
                    JOptionPane.PLAIN_MESSAGE);
        }
    }

    /*
     * Used by Swing to set the size of the JFrame when pack() is called.
     */
    public Dimension getPreferredSize() {
        return new Dimension(w, h+50); // Leave some extra height for the menu bar.
    }

    public void paintPanel(Graphics g) {
        g.drawImage(biWorking, 0, 0, null);
    }

    public void applyOp(BufferedImageOp operation) {
        operation.filter(biWorking, biFiltered);
        gWorking.drawImage(biFiltered, 0, 0, null);
    }

    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource(); // What Swing object issued the event?
        if (obj instanceof JMenuItem) { // Was it a menu item?
            JMenuItem mi = (JMenuItem)obj; // Yes, cast it.
            JPopupMenu pum = (JPopupMenu)mi.getParent(); // Get the object it's a child of.
            JMenu m = (JMenu) pum.getInvoker(); // Get the menu from that (popup menu) object.
            //System.out.println("Selected from the menu: "+m.getText()); // Printing this is a debugging aid.

            if (m==fileMenu)    { handleFileMenu(mi);    return; }  // Handle the item depending on what menu it's from.
            if (m==imageOpMenu) { handleImageOpMenu(mi); return; }
            if (m==ccMenu)      { handleCCMenu(mi);      return; }
            if (m==helpMenu)    { handleHelpMenu(mi);    return; }
        } else {
            System.out.println("Unhandled ActionEvent: "+e.getActionCommand());
        }
    }


    // Use this to put color information into a pixel of a BufferedImage object.
    void putPixel(BufferedImage bi, int x, int y, int r, int g, int b) {
        int rgb = (r << 16) | (g << 8) | b; // pack 3 bytes into a word.
        bi.setRGB(x,  y, rgb);
    }



    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////
    //
    //
    //               My added code - CHEN, RUQIAN
    //
    //
    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////

    /**
     *
     * @return a 2-dim array of the same size as the input image, each element
     * uniquely encodes the location of the pixel within the image.
     *
     * Example: an image of width 2 and height 3 has the following pixelID array:
     * 4, 5
     * 2, 3
     * 0, 1
     *
     */
    public int[][] pixelID(){
        // height is y, width is x
        int height = biTemp.getHeight(); // or just use w, h defined earlier
        int width = biTemp.getWidth();
        int[][] ans = new int[width][height];
        for (int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                ans[i][j] = width * j + i;
            }
        }
        return ans;
    }

    /**
     * A pair of methods getYcoord and getXcoord, recovering the x- and the y- coordinates of a pixelID
     */
    public int getYcoord(int inputPixelID){
        return (inputPixelID - getXcoord(inputPixelID)) / biTemp.getWidth();
    }

    public int getXcoord(int inputPixelID){
        return inputPixelID % biTemp.getWidth();
    }

    /**
     *
     * @param pixelID of a given pixel
     * @return the pixelID of the root of the up-tree that this input pixel belongs to, ...
     * by tracing up the up-tree all the way to the root.
     *
     * Prompt: We follow the path from any pixel to the root of its up-tree...
     * by repeatedly getting the parent node's pixelID from the parentID array,...
     * and then from the pixelID getting the x and y coordinates of the parent,...
     * and then getting the its parent's pixelID, etc.,...
     * until the root of the up-tree is reached
     */
    int find(int pixelID){
        int tmpX = getXcoord(pixelID);
        int tmpY = getYcoord(pixelID);
        int parent = parentID[tmpX][tmpY];
        if (parent != -1){
            while(parent != -1){
                tmpX = getXcoord(parent);
                tmpY = getYcoord(parent);
                parent = parentID[tmpX][tmpY];
                //System.out.println("The current tmpX is "+tmpX+" and the tmpY is " +tmpY + " and the parent is "+parentID);
            }
            //System.out.println("The end tmpX is "+tmpX+" and the tmpY is " +tmpY + " and the parent is "+parentID);
        }
        return w * tmpY + tmpX;
    }

    /**
     *
     * @param pixelID1
     * @param pixelID2
     *
     * This takes two pixelIDs, that are both parents. It makes the smaller of them the parent of the other.
     * One. Wrong ID input: input two of the same IDs
     * Two. Wrong ID input: not both IDs are roots
     * Three. Correct ID input, ID1 is strictly smaller, make it the root
     * Four. Correct ID input, ID2 is (not strictly) smaller, make it the root
     */
    void union(int pixelID1, int pixelID2) { // has to input two root pixel IDs.
        //System.out.println(pixelID1);
        //System.out.println(pixelID2);
        //System.out.println("The locations are, at [" + getXcoord(pixelID1)+ ", "+getYcoord(pixelID1)+"]" + "and [" + getXcoord(pixelID2)+ ", "+getYcoord(pixelID2)+"]");

        // one
        if (pixelID1 == pixelID2){
            System.out.println("Same input pixel IDs.");
        }
        // two
        else if (parentID[getXcoord(pixelID2)][getYcoord(pixelID2)] != -1 || parentID[getXcoord(pixelID1)][getYcoord(pixelID1)] != -1){
            System.out.println("Not all inputs are roots, at [" + getXcoord(pixelID1)+ ", "+getYcoord(pixelID1)+"]" + "and [" + getXcoord(pixelID2)+ ", "+getYcoord(pixelID2)+"]");
        }
        // three
        else if (pixelID1 < pixelID2){ // make ID1 root
            int tmpX = getXcoord(pixelID2);
            int tmpY = getYcoord(pixelID2);
            parentID[tmpX][tmpY] = pixelID1;
        }
        // four
        else {
            int tmpX = getXcoord(pixelID1);
            int tmpY = getYcoord(pixelID1);
            parentID[tmpX][tmpY] = pixelID2;
        }
    }


    /**
     * A pair of methods getColorFromBI and isEdge, constructing the "strict pixel graph" of an image,...
     * that is,...
     * the vertices of the graph are the pixels, and ...
     * there is an edge between two vertices/pixels if and only if,...
     * 1) the two pixels are adjacent in location;
     * 2) the two pixels represent the same color,...
     * i.e. the euclidean distance between the two colors is exactly zero.
     * The euclidean distance computation is a method implemented in the Color class, in this java file.
     */

    /**
     * @param inputImage
     * @param x
     * @param y
     * @return the "Color" at position [x,y] of the input image
     *
     * This method is then used in construction of the "strict pixel graph".
     */
    public Color getColorFromBI(BufferedImage inputImage, int x, int y){
        int clr = inputImage.getRGB(x,y);
        int red   = (clr & 0x00ff0000) >> 16;
        int green = (clr & 0x0000ff00) >> 8;
        int blue  =  clr & 0x000000ff;
        return new Color(red, green, blue);
        // Reference http://stackoverflow.com/questions/22391353/get-color-of-each-pixel-of-an-image-using-bufferedimages
    }

    /**
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return if there is an edge between vertex [x1, y1] and vertex [x2, y2]
     */
    public boolean isEdge(int x1, int y1, int x2, int y2){
        boolean ans;
        //System.out.println("biTemp width "+ w  + " height "+ h+" IsEdge x1 "+ x1 + " y1 " +y1 + " x2 "+x2 + " y2 "+y2 );

        Color color1 = getColorFromBI(biTemp, x1, y1);
        Color color2 = getColorFromBI(biTemp, x2, y2);
        if (color1.euclideanDistance(color2) == 0){
            ans = true;
        }
        else { ans = false; }
        return ans;
    }


    /**
     * The meat of the program. We do union find.
     *
     * Prompt:
     * Merge groups of pixels so that, at the end,...
     * each connected component of the strict pixel graph will be represented by one up-tree.
     *
     * We do this by scanning the image considering all the pixel pairs for which edges exist
     * (according to the definition of Gs, in Part I). We then perform FIND_UNION on all such pairs,...
     * starting at (x, y) = (0, 0), check to see if Gs contains an edge to (x+1, y) = (1, 0)...
     * and/or to (x, y+1) = (0, 1). If it does, perform the FIND operations on the endpoints of this edge,...
     * and if the results are different, then UNION the two subsets.
     * After processing this pixel, go on to the next (incrementing x).
     * When the first row of pixels is complete, do the same for the second row, etc.,...
     * until all rows of pixels have been processed.
     *
     * Here, a count is maintained of the number of times UNION is called.
     * That count should be set to zero before the scan begins.
     * At the end, print out the count in the following style.
     */
    void unionFind(){
        parentID = new int[w][h];
        for (int i = 0; i < w; i++){
            for (int j = 0; j < h; j++){
                parentID[i][j] = -1;
            }
        }
        int countUnion = 0;
        for (int i = 0; i < w-1; i ++){ //[width][height], i is x = width, h is y = height
            for (int j = 0; j < h-1; j++){
                if (isEdge(i, j, i+1, j)){
                    int pid1 = pixelID()[i][j];
                    int pid2 = pixelID()[i+1][j];
                    int find1 = find(pid1);
                    int find2 = find(pid2);
                    if (find1 != find2){
                        union(find1, find2);
                        countUnion ++;
                    }
                }
                if (isEdge(i, j, i, j+1)){
                    int pid1 = pixelID()[i][j];
                    int pid2 = pixelID()[i][j+1];
                    int find1 = find(pid1);
                    int find2 = find(pid2);
                    if (find1 != find2){
                        union(find1, find2);
                        countUnion ++;
                    }
                }
            }
        }
        // at y coord = height - 1, i.e. top row horizontal edges
        for (int i = 0; i < w-1; i++){
            if (isEdge(i, h-1, i+1, h-1)){
                int pid1 = pixelID()[i][h-1];
                int pid2 = pixelID()[i+1][h-1];
                int find1 = find(pid1);
                int find2 = find(pid2);
                if (find1 != find2){
                    union(find1, find2);
                    countUnion ++;
                }
            }
        }
        // at x coord = width - 1, i.e. rightmost column vertical edges
        for (int j = 0; j < h-1; j++){
            if (isEdge(w-1, j, w-1, j+1)){
                int pid1 = pixelID()[w-1][j];
                int pid2 = pixelID()[w-1][j+1];
                int find1 = find(pid1);
                int find2 = find(pid2);
                if (find1 != find2){
                    union(find1, find2);
                    countUnion ++;
                }
            }
        }
        System.out.println("The number of times that the method UNION was called for this image is: " + countUnion);
    }

    /**
     *
     * Simply finds roots of connected components.
     *
     * Here we locate the roots of each connected compoenents and count the total number of components.
     * We set an integer variable, numComponent, to zero, and do another scan of the parentID array.
     * Each time a root of an uptree is encountered, increment count.
     * At the end of the scan print out the value of numComponent with explanatory text.
     *
     * For debugging:
     * https://catalyst.uw.edu/gopost/conversation/tanimoto/979628
     * The number of connected components PLUS the number of union operations performed...
     * EQUAL the total number of pixels.
     */
    int numComponent;
    void computeConnectedComponentsNoHash() {
        numComponent = 0;
        for (int i = 0; i < parentID.length; i++){
            for (int j = 0; j < parentID[0].length; j++){
                if ( parentID[i][j] == -1 ){
                    numComponent++;
                    // System.out.println("A root at position ["+i+", "+j+"].");
                }
            }
        }
        System.out.println("The number of connected components in this image is: "+ numComponent+".");
    }

    /**
     * A pair of methods computeConnectedComponent and colorPaint, illustrating the connected components of input image.
     *
     * Here we label the connected components and store the components info in a hash map,...
     * where the key is the pixelID of the root of a connected component,...
     * and the value is the numbering of the connected component this root belongs to.
     *
     * We then recolor the pixels of the image such that...
     * all pixels in the same connected component are assigned the same color,...
     * and different connected components are colored differently.
     *
     * Steps:
     * (a) FINDing the root of the pixel's up-tree;
     * (b) Looking up the count value for that root in the hashtable.
     *     Then convert the Integer to an int. Let's call the resulting int k.
     * (c) Determine the kth progressive color by calling the provided method getProgressiveColor(k).
     * (d) Replace the rgb information of biWorking with the new color.
     * (e) When the scan is complete, the method repaint() should be called to show the newly colored image.
     */
    HashMap<Integer, Integer> connectedComponentHashMap = new HashMap<>();
    void computeConnectedComponents() {
        numComponent = 0;
        for (int i = 0; i < parentID.length; i++){
            for (int j = 0; j < parentID[0].length; j++){
                if ( parentID[i][j] == -1 ){
                    Integer key = pixelID()[i][j];
                    Integer value = new Integer(numComponent); //TODO CRQ: unnnecessary boxing?
                    connectedComponentHashMap.put(key, value);
                    numComponent++;
                    // System.out.println("A root at position ["+i+", "+j+"].");
                }
            }
        }
        System.out.println("The number of connected components in this image is: "+ numComponent+".");
    }

    void colorPaint(){
        ProgressiveColors pc = new ProgressiveColors();
        computeConnectedComponents();
        int[][] allRoots = new int[w][h];
        for (int i = 0; i < w; i++){
            for (int j = 0; j < h; j++){
                int currRoot = find(i + j * w);
                int currConnComp = connectedComponentHashMap.get(currRoot);
                int[] currProgressiveColor = pc.progressiveColor(currConnComp);
                putPixel(biWorking, i, j, currProgressiveColor[0], currProgressiveColor[1], currProgressiveColor[2]);
            }
        }
        //gWorking.drawImage(biWorking, 0, 0, null); // as an alternative to repaint()
        repaint();
    }

    /* This main method can be used to run the application. */
    public static void main(String s[]) {
        appInstance = new ImageComponents();
        appInstance.unionFind();
        appInstance.colorPaint();
    }
}


/**
 * Reference
 * https://catalyst.uw.edu/gopost/conversation/tanimoto/977858
 * Q: I was wondering why can't we just use map, considering the fact that it also reflects the one to one relationship perfectly
 * A: Note that a Java Map is an interface, rather than an implementation of an interface.  The approach used in assignment UF is actually a very efficient implementation of a sort of map -- one which is much more efficient here than a hash table would be.
 */
