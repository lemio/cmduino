import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener; 

import java.util.Enumeration;


public class CMDuino implements SerialPortEventListener {
	SerialPort serialPort;
	

        /** The port we're normally going to use. */
	
	/**
	* A BufferedReader which will be fed by a InputStreamReader 
	* converting the bytes into characters 
	* making the displayed results codepage independent
	*/
	private BufferedReader input;
	/** The output stream to the port */
	private OutputStream output;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;

	public void initialize(String port) {
		final String PORT_NAMES[] = { 
			port // Windows
	};
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(),
					TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

			// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * This should be called when you stop using the port.
	 * This will prevent port locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				String inputLine=input.readLine();
				parseString(inputLine);
				
				
			} catch (Exception e) {
				//System.err.println(e.toString());
			}
		}
		// Ignore all the other eventTypes, but you should consider the other ones.
	}
	public void parseString(String inputLine) throws IOException, AWTException{
		byte[] byteString = inputLine.getBytes();
		Robot robot = new Robot();
		System.out.println("Recieved data: " + inputLine);
		switch(byteString[0]){
		case 'c':
			String commandLine = inputLine.substring(2);
			System.out.print("Executing command: ");
			System.out.println(commandLine);
			executeCommand(commandLine);			
		break;
		case 'm':
			String[] mousePosition = inputLine.split(" ");
			int x = Integer.parseInt(mousePosition[1]);
			int y = Integer.parseInt(mousePosition[2]);
			
			robot.mouseMove(x,y);
			System.out.print("Setting mouse to position: ");
			System.out.print(x);
			System.out.print(" ");
			System.out.println(y);
			System.out.println("Please remember that you can always pause and restart the Arduino program using the reset button.");
		break;
		case 'l':
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.delay(1);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			System.out.println("Left mouse button pressed");
		break;
		case 'r':
			robot.mousePress(InputEvent.BUTTON3_MASK);
			robot.delay(1);
			robot.mouseRelease(InputEvent.BUTTON3_MASK);
			System.out.println("Right mouse button pressed");
		break;
		case 'k':
			robot.keyPress(byteString[2]);
			robot.delay(1);
			robot.keyRelease(byteString[2]);
			System.out.print("Key \"");
			System.out.print(byteString[2]);
			System.out.println("\" pressed");
		break;
		case 'v':
			Audio.setMasterOutputVolume(0.5f);
			break;
		default:
			System.out.println("Command syntax is not reconized");
		break;
		}
		
			
	}
	public static Process executeCommand(String commandString) throws IOException{
		return Runtime.getRuntime().exec(commandString);
	}
	public static String readLine()
	{
		String s = "";
		try {
			InputStreamReader converter = new InputStreamReader(System.in);
			BufferedReader in = new BufferedReader(converter);
			s = in.readLine();
		} catch (Exception e) {
			System.out.println("Error! Exception: "+e); 
		}
		return s;
	}
	public static void main(String[] args) throws Exception {
		System.out.println("Welcome to CMDuino");
		System.out.println("CMDuino converts Arduino serial commands to windows commands.");
		System.out.println("__________________");
		try {
			String path = CMDuino.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			System.out.println("Loading library from "+path+"rxtxSerial.dll");
			System.loadLibrary("rxtxSerial");
		} catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.out.println("Could not find library.");
		}
		System.out.println("__________________");
		System.out.println("Please give your Serial port name (e.g. COM3):");
		String comPort = readLine();
		System.out.println("You selected " + comPort);
		System.out.println("__________________");
		CMDuino main = new CMDuino();
		main.initialize(comPort);
		Thread t=new Thread() {
			public void run() {
				//the following line will keep this app alive for 1000 seconds,
				//waiting for events to occur and responding to them (printing incoming messages to console).
				try {Thread.sleep(1000000);} catch (InterruptedException ie) {}
			}
		};
		t.start();
		System.out.println("__________________");
		System.out.println("Please make sure that you close this program when re-programming the Arduino.");
		System.out.println("The program is started.");

	

	
	}
}