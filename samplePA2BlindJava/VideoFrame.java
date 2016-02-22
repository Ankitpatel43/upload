import java.awt.Frame;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.media.*;
import javax.swing.*;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.*;
import java.io.IOException;

public class VideoFrame extends Frame implements ControllerListener
{
  Processor processor;
  Object waitSync = new Object();
  boolean stateTransitionOK=true;
  File file;
  Component cComponent;
  Component vComponent;

  public VideoFrame()
  {
    setTitle("VideoFrame ");
    setBounds(200,200,300,300);
    	MenuAction me = new MenuAction(this);
        MenuBar mbar = new MenuBar();
        Menu fileMenu = new Menu("File");
        Menu manipulationMenu = new Menu("Function");
        MenuItem mitem;
        mitem = new MenuItem("Open");
        mitem.addActionListener(me);
        fileMenu.add(mitem);
        mitem = new MenuItem("Exit");
        mitem.addActionListener(me);
        fileMenu.add(mitem);
        mitem = new MenuItem("Frame");
        mitem.addActionListener(me);
        manipulationMenu.add(mitem);
        mbar.add(fileMenu);
        mbar.add(manipulationMenu);
        setMenuBar(mbar);
    setVisible(true);
  }

  public static void main(String[] args) throws IOException
  {
    VideoFrame app= new VideoFrame();	//creates the main program window
    app.addWindowListener
    ( new WindowAdapter()
     {
       public void windowClosing( WindowEvent e)
       {
         System.exit(0);
       }
     }
    );
  }

  public void controllerUpdate(ControllerEvent evt)
  {

    if (evt instanceof ConfigureCompleteEvent ||
        evt instanceof RealizeCompleteEvent ||
        evt instanceof PrefetchCompleteEvent)
    {
        synchronized (waitSync)
        {
            stateTransitionOK = true;
            waitSync.notifyAll();
        }
    }
    else if (evt instanceof ResourceUnavailableEvent)
    {
        synchronized (waitSync)
        {
            stateTransitionOK = false;
            waitSync.notifyAll();
        }
    }
    else if (evt instanceof EndOfMediaEvent)
    {
          processor.setMediaTime(new Time(0));
    }
  }

 boolean waitForState(int state)
 {
    synchronized (waitSync)
    {
        try
        {
            while (processor.getState() != state && stateTransitionOK)
                waitSync.wait();
        } catch (Exception e) {}
    }
    return stateTransitionOK;
 }

  public void openFile()
  {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.CANCEL_OPTION)
        {
            file = null;
            JOptionPane.showMessageDialog(null, "No file was selected. Can not play media the Media player without a file");
        }
        else
            file = fileChooser.getSelectedFile();
  }

  public void setFrame()
  {
    if (processor != null)
    {
    	processor.stop();
    	processor.deallocate();
    	processor.close();
    	
    	if (cComponent!=null)
    		remove(cComponent);
    	
    	if (vComponent!=null)
    		remove(vComponent);
    }
    try
    {
        processor = Manager.createProcessor(new MediaLocator(file.toURI().toURL()));
        processor.addControllerListener(this);
    }
    catch (Exception e)
    {
        System.err.println("Failed to create a processor from the given url: " + e);
    }

    // Put the Processor into configured state.
    processor.configure();
    
    if (!waitForState(processor.Configured)) 
    {
        System.err.println("Failed to configure the processor.");
        return;
    }

    // So I can use it as a player.
    processor.setContentDescriptor(null);

    // Obtain the track controls.
    TrackControl trackcontrols[] = processor.getTrackControls();

    if (trackcontrols == null) {
        System.err.println("Failed to obtain track controls from the processor.");
        return;
    }

    // Search for the track control for the video track.
    TrackControl videoTrack = null;

    for (int i = 0; i < trackcontrols.length; i++) {
        if (trackcontrols[i].getFormat() instanceof VideoFormat) {
            videoTrack = trackcontrols[i];
            break;
        }
    }

    if (videoTrack == null) {
        System.err.println("The input media does not contain a video track.");
        return;
    }

    System.err.println("Video format: " + videoTrack.getFormat());

    // Instantiate and set the frame access codec to the data flow path.
    try
    {
        Codec codec[] = { new ProcessFrame() };
        videoTrack.setCodecChain(codec);
    }
    catch (UnsupportedPlugInException e) {
        System.err.println("The processor does not support effects.");
    }

    // Realize the processor.
    postSet();
  }

  private boolean postSet()
 {
    processor.prefetch();
    
    if (!waitForState(processor.Prefetched))
    {
        System.err.println("Failed to realize the processor.");
        return false;
    }

    // Display the visual & control component if there's one.
    setLayout(new BorderLayout());

    if ((vComponent = processor.getVisualComponent()) != null)
    {
        add("Center", vComponent);
    }

    if ((cComponent = processor.getControlPanelComponent()) != null)
    {
        add("South", cComponent);
    }
/*
    // Start the processor.
    processor.start();
*/
    setVisible(true);

    return true;
  }

}

class MenuAction implements ActionListener{

    private VideoFrame pl;

    public MenuAction(VideoFrame pl)
    {
        this.pl = pl;
    }

    public void actionPerformed(ActionEvent e)
    {
        if( e.getActionCommand().equals("Exit"))
        {
            System.exit(0);
        }
        else if( e.getActionCommand().equals("Open"))
        	{
            	pl.openFile();
        	}
        	else if(e.getActionCommand().equals("Frame"))
        		{
        			pl.setFrame();
        		}
    }
}

/**********************************************************************
***********************************************************************/
class ProcessFrame implements Effect 
{
    Format inputFormat;
    Format outputFormat;
    Format[] inputFormats;
    Format[] outputFormats;

    public ProcessFrame() 
    {

        inputFormats = new Format[] {
            new RGBFormat(null,
                          Format.NOT_SPECIFIED,
                          Format.byteArray,
                          Format.NOT_SPECIFIED,
                          24,
                          3, 2, 1,
                          3, Format.NOT_SPECIFIED,
                          Format.TRUE,
                          Format.NOT_SPECIFIED)
        };

        outputFormats = new Format[] {
            new RGBFormat(null,
                          Format.NOT_SPECIFIED,
                          Format.byteArray,
                          Format.NOT_SPECIFIED,
                          24,
                          3, 2, 1,
                          3, Format.NOT_SPECIFIED,
                          Format.TRUE,
                          Format.NOT_SPECIFIED)
        };

    }

    // methods for interface Codec
    public Format[] getSupportedInputFormats() {
    	return inputFormats;
    }

    public Format [] getSupportedOutputFormats(Format input) {
        return outputFormats;
    }

    public Format setInputFormat(Format input) {
    	return input;
    }

    public Format setOutputFormat(Format output) {
        return output;
    }

    public int process(Buffer inBuffer, Buffer outBuffer) 
    {
        outBuffer.setData(inBuffer.getData());
        outBuffer.setLength(inBuffer.getLength());
        outBuffer.setFormat(inBuffer.getFormat());
        outBuffer.setFlags(inBuffer.getFlags());
        outBuffer.setOffset(inBuffer.getOffset());

        byte [] inData = (byte[]) inBuffer.getData();	
        byte [] outData = (byte[]) outBuffer.getData();
        RGBFormat vfIn = (RGBFormat) inBuffer.getFormat();
        Dimension sizeIn = vfIn.getSize();

        int iw = sizeIn.width;
        int ih = sizeIn.height;

        int ip = 0;
        int op = 0;

        if ( outData.length < iw*ih*3 ) {
            System.out.println("the buffer is not full");
            return 0;
        }

/*
  Note: this is a example how to read the pixel values from the current frame which is saved
  in buffer inData.

        for ( int j = 0; j < ih; j++ )
        for ( int i = 0; i < iw; i++ )
        {
          pixelR = inData[ip++];
          pixelG = inData[ip++];
          pixelG = inData[ip++];
        }

*/
        return 1;

    }

    // methods for interface PlugIn
    public String getName() {
        return "Rotation Effect";
    }

    public void open() {
    }

    public void close() {
    }

    public void reset() {
    }

    // methods for interface javax.media.Controls
    public Object getControl(String controlType) {
    	return null;
    }

    public Object[] getControls() {
    	return null;
    }

    // Utility methods.
    Format matches(Format in, Format outs[]) 
    {
		for (int i = 0; i < outs.length; i++) {
		    if (in.matches(outs[i]))
		    	return outs[i];
		}

		return null;
    }


    byte[] validateByteArraySize(Buffer buffer,int newSize) 
    {
        Object objectArray=buffer.getData();
        byte[] typedArray;

        if (objectArray instanceof byte[]) 
        {     // is correct type AND not null
            typedArray=(byte[])objectArray;
            
            if (typedArray.length >= newSize ) 
            { // is sufficient capacity
                return typedArray;
            }

            byte[] tempArray=new byte[newSize];  // re-alloc array
            System.arraycopy(typedArray,0,tempArray,0,typedArray.length);
            typedArray = tempArray;
        } else {
            typedArray = new byte[newSize];
        }

        buffer.setData(typedArray);
        
        return typedArray;
    }
}