// ImageEnhancerWithUndoAndRedo.java
// by Tony Le
// This Image Enhancer program allows users to apply filters to images with the ability to undo and redo them.
// The user can also choose to save the filtered image.

// This is based on the "SaveImage.java" tutorial demo from Oracle.com.


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

public class ImageEnhancerWithUndoAndRedoV2 extends Component implements ActionListener {

    String startingImage = "obama.png";
    int opIndex;
    BufferedImage biTemp, biOriginal;
    BufferedImage biWorking;
    BufferedImage biFiltered;
    Graphics gOrig;
    Graphics gWorking;
    Graphics gFiltered;
    int w;
    int h;
    byte[] lut0, lut3, lut4, lut5, lut6, lut7;
    LookupOp op0, op3, op4, op5, op6, op7;

    static JPopupMenu popup;

    ImageStack undoStack; // instance of undo stack
    ImageStack redoStack; // instance of redo stack
    // Here, you should declare two variables to hold instances of your stack class, with one for Undo and one for Redo.

    /**
     * ==================================================================> NEW FEATURES FOR UI TEST
     */
    private ImageEnhancerWithUndoAndRedoV2 si;
	private JMenuItem undoItem;	 // Here's a new field to provide access to the Undo menu item.
    private JMenuItem redoItem;  // and one for the Redo menu item.

    private JComboBox formats;

    public static final float[] SHARPEN3x3 = { // sharpening filter kernel
        0.f, -1.f,  0.f,
       -1.f,  5.f, -1.f,
        0.f, -1.f,  0.f
    };

    public static final float[] BLUR3x3 = {
        0.1f, 0.1f, 0.1f,    // low-pass filter kernel
        0.1f, 0.2f, 0.1f,
        0.1f, 0.1f, 0.1f
    };

    /**
     * ==================================================================> NEW FEATURES FOR UI TEST
     */

    public ImageEnhancerWithUndoAndRedoV2(JMenuItem undoItem, JMenuItem redoItem) { // Version of the constructor taking 2 arguments.
   	 	this();
   	 	this.undoItem = undoItem;
   	 	this.redoItem = redoItem;
   	  this.undoItem.setEnabled(false); // undo menu item initially grayed out
   	  this.redoItem.setEnabled(false); // redo menu item initially grayed out
   	 	// end of code for initializing menu items' state.
    }

    public ImageEnhancerWithUndoAndRedoV2() { // Version of the constructor taking 0 arguments.
        try {
            biTemp = ImageIO.read(new File(startingImage));
            w = biTemp.getWidth(null);
            h = biTemp.getHeight(null);
            biOriginal = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gOrig = biOriginal.getGraphics();
            gOrig.drawImage(biTemp, 0, 0, null);
            biWorking = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gWorking = biWorking.getGraphics();
            gWorking.drawImage(biOriginal, 0, 0, null);
            biFiltered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gFiltered = biFiltered.getGraphics();

        } catch (IOException e) {
            System.out.println("Image could not be read: "+startingImage);
            System.exit(1);
        }

        // Add code to create empty stack instances for the Undo stack and the Redo stack.
        // Put your code for this here:
        undoStack = new ImageStack();
        undoStack.push(biOriginal); // "original"
        redoStack = new ImageStack();

        // We add a listener to this component so that it can bring up popup menus.
        MouseListener popupListener = new PopupListener();
        addMouseListener(popupListener);
    }

    /**
     * ==================================================================> NEW FEATURES FOR UI TEST
     */
    public ImageEnhancerWithUndoAndRedoV2 getImageEnhancer() {
    		if(si == null) {
    			si = new ImageEnhancerWithUndoAndRedoV2();
    		}
    		return si;
    }

    /**
     * ==================================================================> NEW FEATURES FOR UI TEST
     */
	public BufferedImage getBufferedImage() {
		return biWorking;
	}

	public JPopupMenu getPopupMenu() {
		return popup;
	}

    public Dimension getPreferredSize() {
        return new Dimension(w, h);
    }

    public void setOpIndex(int i) {
        opIndex = i;
    }

    public void paint(Graphics g) {
        g.drawImage(biWorking, 0, 0, null);
    }

    private LookupOp getOriginalOp() {
        byte[] lut = new byte[256];
        for (int j=0; j<256; j++) {
            lut[j] = (byte)j;
        }
        ByteLookupTable blut = new ByteLookupTable(0, lut);
        return new LookupOp(blut, null);
    }

    int lastOp;
    public void filterImage() {
    	if (undoStack.isEmpty()) {
    		undoStack.push(biOriginal);
    	}
        BufferedImage filtered = null;
        BufferedImageOp op = null;
        lastOp = opIndex;
        switch (opIndex) {
        case 0 : /* darken. */
           if (lut0==null) {
                lut0 = new byte[256];
                for (int j=0; j<256; j++) {
                    lut0[j] = (byte)(j*9.0 / 10.0);
                }
                ByteLookupTable blut0 = new ByteLookupTable(0, lut0);
                op0 = new LookupOp(blut0, null);
            }
            op = op0;
            break;
        case 1:  /* low pass filter */
        case 2:  /* sharpen */
            float[] data = (opIndex == 1) ? BLUR3x3 : SHARPEN3x3;
            op = new ConvolveOp(new Kernel(3, 3, data),
                                ConvolveOp.EDGE_NO_OP,
                                null);
            break;

        case 3 : /* photonegative */
            if (lut3==null) {
                lut3 = new byte[256];
                for (int j=0; j<256; j++) {
                    lut3[j] = (byte)(256-j);
                }
                ByteLookupTable blut3 = new ByteLookupTable(0, lut3);
                op3 = new LookupOp(blut3, null);
            }
            op = op3;
            break;

        case 4 : /* threshold RGB values. */
           if (lut4==null) {
                lut4 = new byte[256];
                for (int j=0; j<256; j++) {
                    lut4[j] = (byte)(j < 128 ? 0: 200);
                }
                ByteLookupTable blut4 = new ByteLookupTable(0, lut4);
                op4 = new LookupOp(blut4, null);

            }
            op = op4;
            break;

        case 5 : /* undo */
            if (lut5==null) {
                 lut5 = new byte[256];
                 for (int j=0; j<256; j++) {
                     lut5[j] = (byte)(j < 128 ? 0: 200);
                 }
                 ByteLookupTable blut5 = new ByteLookupTable(0, lut5);
                 op5 = new LookupOp(blut5, null);
             }
             op = op5;
             break;

        case 6 : /* redo*/
            if (lut6==null) {
                 lut6 = new byte[256];
                 for (int j=0; j<256; j++) {
                     lut6[j] = (byte)(j < 128 ? 0: 200);
                 }
                 ByteLookupTable blut6 = new ByteLookupTable(0, lut6);
                 op6 = new LookupOp(blut6, null);
             }
             op = op6;
             break;

        case 7 : /* drop redo item*/
            if (lut7==null) {
                 lut7 = new byte[256];
                 for (int j=0; j<256; j++) {
                     lut7[j] = (byte)(j < 128 ? 0: 200);
                 }
                 ByteLookupTable blut7 = new ByteLookupTable(0, lut7);
                 op7 = new LookupOp(blut7, null);
             }
             op = op7;
             break;

        default:return;
        }

        /* Rather than directly drawing the filtered image to the
         * destination, we filter it into a new image first, then that
         * filtered image is ready for writing out or painting.
         */

        // user chose (0-4) / a filter
        if(opIndex >= 0 && opIndex <= 4) {
          redoStack.clear();
      		biFiltered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      		undoStack.push(biFiltered); // adding the filtered image to the stack
      		op.filter(biWorking, biFiltered);
        } else if (opIndex == 5) { // user clicked undo
        	redoStack.push(undoStack.pop());
        	biFiltered = undoStack.peek();
        	// dropping original to get undoStack size to 0
        	if (undoStack.getSize() == 1) {
        		undoStack.pop();
        	}
        } else if (opIndex == 6) { // user clicked redo
        	undoStack.push(redoStack.pop());
        	biFiltered = undoStack.peek();
        } else if (opIndex == 7) {  // drop Redo item
        	redoStack.pop();
        	biFiltered = undoStack.peek();
        }

        // greys/ungreys redo menu option
        if (!redoStack.isEmpty()) {
        	redoItem.setEnabled(true);
        } else {
        	redoItem.setEnabled(false);
        }

        // greys/ungreys undo menu option
        if (undoStack.isEmpty()) {
        	undoItem.setEnabled(false);
        } else {
        	undoItem.setEnabled(true);
        }

        gWorking.drawImage(biFiltered, 0, 0, null); // this draws the image
        printNumberOfElementsInBothStack();
    }

    /* Returns the formats sorted alphabetically and in lower case */
    public String[] getFormats() {
        String[] formats = ImageIO.getWriterFormatNames();
        TreeSet<String> formatSet = new TreeSet<String>();
        for (String s : formats) {
            formatSet.add(s.toLowerCase());
        }
        return formatSet.toArray(new String[0]);
    }

    class PopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(),
                           e.getX(), e.getY());
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        try {
            JComboBox cb = (JComboBox)e.getSource();
            if (cb.getActionCommand().equals("SetFilter")) {
                    setOpIndex(cb.getSelectedIndex());
                    filterImage();
                    repaint();
            } else if (cb.getActionCommand().equals("Formats")) {
                /* Saves the filtered image in the selected format.
                 * The selected item will be the name of the format to use
                 */
                String format = (String)cb.getSelectedItem();
                /* Uses the format name to initialise the file suffix.
                 * Format names typically correspond to suffixes.
                 */
                File saveFile = new File("savedimage."+format);
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(saveFile);
                int rval = chooser.showSaveDialog(cb);
                if (rval == JFileChooser.APPROVE_OPTION) {
                    saveFile = chooser.getSelectedFile();
                    /* Writes the filtered image in the selected format,
                     * to the file chosen by the user.
                     */
                    try {
                         ImageIO.write(biFiltered, format, saveFile);
                    } catch (IOException ex) {

                    }

                }
            }
        }
        catch (Exception ee) {
            JMenuItem mi = (JMenuItem)e.getSource();
            String filterCommand = mi.getText();
            Integer i = new Integer(filterCommand.substring(0,1));
            int index = i.intValue();
            System.out.println(filterCommand);
            setOpIndex(index);
            filterImage();
            repaint();
        }
    }

    public static void main(String s[]) {
    		new ImageEnhancerWithUndoAndRedoV2().run();
    }

    public void run() {
        JFrame f = new JFrame("ImageEnhancer WITH Undo or Redo by Tony Le");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        si = new ImageEnhancerWithUndoAndRedoV2();

        // menu items for undo / redo / drop redo
        JMenuItem undoIt = new JMenuItem("5: Undo");
        JMenuItem redoIt = new JMenuItem("6: Redo");
        JMenuItem dropRedo = new JMenuItem("7: Drop this Redo item");
        si = new ImageEnhancerWithUndoAndRedoV2(undoIt, redoIt);
        undoIt.addActionListener(si);
        redoIt.addActionListener(si);
        dropRedo.addActionListener(si);

        f.add("Center", si);
        formats = new JComboBox(si.getFormats());
        formats.setActionCommand("Formats");
        formats.addActionListener(si);
        JPanel panel = new JPanel();
        panel.add(new JLabel("Save As"));
        panel.add(formats);
        f.add("South", panel);
        f.pack();
        f.setVisible(true);

        // We create the popup menu in the following.
        popup = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("0: Darken by 10%");
        menuItem.addActionListener(si);
        popup.add(menuItem);
        menuItem = new JMenuItem("1: Convolve: Low-Pass");
        menuItem.addActionListener(si);
        popup.add(menuItem);
        menuItem = new JMenuItem("2: Convolve: High-Pass");
        menuItem.addActionListener(si);
        popup.add(menuItem);
        menuItem = new JMenuItem("3: Photonegative");
        menuItem.addActionListener(si);
        popup.add(menuItem);
        menuItem = new JMenuItem("4: RGB Thresholds at 128");
        menuItem.addActionListener(si);
        popup.add(menuItem);

        // pop up for undo and redo menu items
        popup.add(undoIt);
        popup.add(redoIt);
        // pop up for drop redo menu item
        popup.add(dropRedo);
    }

    private void printNumberOfElementsInBothStack() {
    	// Uncomment this code that prints out the numbers of elements in each of the two stacks (Undo and Redo):
        System.out.println("Undo stack has " + undoStack.getSize() + " elements.");
        System.out.println("Redo stack has " + redoStack.getSize() + " elements.");
    }
}
