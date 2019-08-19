package irt.flash.data.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.flash.data.MyThreadFactory;
import irt.flash.data.ToHex;
import purejavacomm.SerialPort;

public class MicrocontrollerSTM32 extends Observable implements Runnable {

	public static final int KB = 1024;

	private static final Logger logger = LogManager.getLogger();

	public static final String BIAS_BOARD = "Bias Board";
	public static final String HP_BIAS_BOARD = "HP Bias Board";

	private static Answer commandAnswer;

	public enum ProfileProperties {
		/**
		 * Defines a type of the device (range 0 to 2^^32-1).<br>
		 *  Right to now, the following types are implemented:<br>
		 *  1 – IFC main controller<br>
		 *  2 – Generic PicoBUC Module<br>
		 *  100 – Ku-band PicoBUC Module<br>
		 *  101 – C-band PicoBUC Module<br>
		 *  1001 – 70MHz to L-band Up-Converter Module<br>
		 *  1002 – L-band to 70MHz Down-Converter Module<br>
		 *  1003 – 140MHz to L-band Up-Converter Module<br>
		 *  1004 – L-band to 140MHz Down-Converter Module<br>
		 *  1005 – L-band to Ku-band Up-Converter Module<br>
		 *  1006 – L-band to C-band Up-Converter Module<br>
		 *  1007 – 70MHz to Ku-band Up-Converter Module<br>
		 *  1008 – Ku-band to 70MHz Down-Converter Module<br>
		 *  1009 – 140MHz to Ku-band Up-Converter  Module<br>
		 *  1010 – Ku-band to 140MHz Down-Converter Module
		 */
		DEVICE_TYPE			("device-type", 			Arrays.asList(new String[] { "Common" })),
		DEVICE_REVISION		("device-revision", 		Arrays.asList(new String[] { "Common" })),
		DEVICE_SUBTYPE		("device-subtype",			Arrays.asList(new String[] { "Common", "Optional"})),
		SERIAL_NUMBER("device-serial-number", 			Arrays.asList(new String[] { "Common" })),
		DEVICE_PART_NUMBER	("device-part-number", 		Arrays.asList(new String[] { "Common" })),
		PRODUCT_DESCRIPTION	("product-description", 	Arrays.asList(new String[] { "Common" })),
		CONTACT_INFORMATION	("contact-information ",	Arrays.asList(new String[] { "Common", "Optional" })),
		SYSTEM_NAME			("system-name", 			Arrays.asList(new String[] { "Common" })),
		MAC_ADDRESS			("mac-address", 			Arrays.asList(new String[] { "Common", "Network"})),
		ZERO_ATTENUATION_GAIN("zero-attenuation-gain",	Arrays.asList(new String[] { "Common", "User interface"})),

		INPUT_FREQUENCY		("input-frequency ",		Arrays.asList(new String[] { "FCM", "PLLs"})),
		REF_CLOCK			("ref-clock",				Arrays.asList(new String[] { "FCM", "PLLs"})),
		/**Defines the PLL1 output frequency (range 0 to 2^^64-1).<br>Value is 4120 MHz for 70 MHz converters and<br>4190 MHz – for 140 MHz converters*/
		PLL1_FREQUENCY		("pll1-frequency",			Arrays.asList(new String[] { "FCM", "PLLs"})),
		/**Third DAC configuration value (range 0 to 2^^16-1).<br>It’s relevant just to 70/140 MHz Up/Down converters*/
		DAC_I_VALUE			("dac-I-value",				Arrays.asList(new String[] { "FCM", "DACs"})),
		/**Third DAC configuration value (range 0 to 2^^16-1).<br>It’s relevant just to 70/140 MHz Up/Down converters*/
		DAC_Q_VALUE			("dac-I-value",				Arrays.asList(new String[] { "FCM", "DACs"})),
		/**Defines a RF gain offset parameter (range 0 to 2^^16-1). It’s relevant just for 70/140 MHz Up converters*/
		RF_GAIN_OFFSET		( "rf-gain-offset",			Arrays.asList(new String[] { "FCM", "DACs"})),
		/**Defines an attenuation offset (range 0 to 2^^32-1). This parameter is relevant just for 70/140 MHz Up/Down converters*/
		ATTENUATION_OFFSET	("attenuation-offset",		Arrays.asList(new String[] { "FCM", "DACs"})),
		ATTENUATION_COEFFICIENT("attenuation-coefficient",Arrays.asList(new String[]{"FCM", "DACs"})),

		FREQUENCY_RANGE			("frequency-range",			Arrays.asList(new String[] { "FCM", "User interface"})),
		FREQUENCY_SET			("frequency-set",			Arrays.asList(new String[] { "FCM", "User interface"})),
		ATTENUATION_RANGE		("attenuation-range",		Arrays.asList(new String[] { "FCM", "User interface"})),
		INPUT_POWER_LUT_SIZE	("in-power-lut-size",		Arrays.asList(new String[] { "FCM", "User interface"})),
		INPUT_POWER_LUT_ENTRY	("in-power-lut-entry",		Arrays.asList(new String[] { "FCM", "User interface"})),
		OUTPUT_POWER_LUT_SIZE	("out-power-lut-size",		Arrays.asList(new String[] { "FCM", "User interface"})),
		OUTPUT_POWER_LUT_ENTRY	("out-power-lut-entry",		Arrays.asList(new String[] { "FCM", "User interface"})),
		TEMPERATURE_LUT_SIZE	( "temperature-lut-size",	Arrays.asList(new String[] { "FCM", "User interface"})),
		TEMPERATURE_LUT_ENTRY	("temperature-lut-entry",	Arrays.asList(new String[] { "FCM", "User interface"})),
		GAIN_LUT_SIZE			( "gain-lut-size",			Arrays.asList(new String[] { "FCM", "User interface"})),
		GAIN_LUT_ENTRY			("gain-lut-entry",			Arrays.asList(new String[] { "FCM", "User interface"})),
		/**Defines the relation between gain and DAC value (range ±3.40282347e38).<br>This parameter is relevant just for L-band to C-/Ku- Band Up converters*/
		RF_GAIN_LUT_SIZE		("rf-gain-lut-size",		Arrays.asList(new String[] { "FCM", "User interface"})),
		/**Defines the size of rf-gain table (range 0 to 2^^32-1).<br>This parameter is relevant just for 70/140 MHz Up converters*/
		RF_GAIN_LUT_ENTRY		("rf-gain-lut-entry",		Arrays.asList(new String[] { "FCM", "User interface"})),
		
		POWER_LUT_SIZE			("power-lut-size",			Arrays.asList(new String[] { "BUC", "User interface"})),
		POWER_LUT_ENTRY			("power-lut-entry",			Arrays.asList(new String[] { "BUC", "User interface"})),
		/**Defines thresholds of the current (range 0 to 2^^32-1), where the first argument is index and selects which value to set:<br>
		* 0 – for ZERO current (value under it means the current is zero);<br>
		* 1 – for SW over current of the output device (HS1);<br>
		* 2 – for SW over current of the others (HS2);<br>
		* 3 – for HW over current of the output device (HS1);<br>
		* 4 – for HW over current of the others (HS2).*/
		DEVICE_THRESHOLD_CURRENT("device-threshold-current",		Arrays.asList(new String[] { "BUC", "User interface"})),
		/**Defines three thresholds of the temperature (range 0 to 2^^32-1), where the first argument is index and select which value is set:<br>
		* 1 – mute threshold on over temperature;<br>
		* 2 – unmute threshold on over temperature*/
		DEVICE_THRESHOLD_TEMPERATURE("device-threshold-temperature",Arrays.asList(new String[] { "BUC", "User interface"})),
		/**Defines the source detector of the output power is current of output device (mode 1),<br>
		 * on-board detector (mode 0) or<br>
		 * the input power plus gain (mode 2).<br>
		 * Default is mode 1*/
		POWER_DETECTOR_SOURCE	("power-detector-source",	Arrays.asList(new String[] { "BUC", "User interface"})),
		/**Defines a gain for zero attenuation that will be used for Output Power calculation (range ±2^^15). It’s valid just in case the “power-detector-source” is mode 2*/
		OUTPUT_POWER_ZERO_ATTENUATION_GAIN("out-power-zero-attenuation-gain",	Arrays.asList(new String[] { "BUC", "User interface"})),
		/**Defines the range for output frequency to 5.85-6.7 GHz instead of default 5.85-6.4 GHz for C-band type device*/
		C_BAND_FREQUENCY_RANGE_EXTENDED	("cband-frequency-range-extended",		Arrays.asList(new String[] { "BUC", "User interface"}));

		private String name;
		private List<String> properties;

		private ProfileProperties(String name, List<String> properties) {
			this.name = name;
			this.properties = properties;
		}

		@Override
		public String toString() {
			return name;
		}

		public List<String> getProperties() {
			return properties;
		}
	}

	public enum Status {
		ERROR, CONNECTING, READING, WRITING, BUTTON, ERASE;

		private String message;

		public String getMessage() {
			return message;
		}

		public Status setMessage(String message) {
			this.message = message;
			return this;
		}

		public String toString(){
			return name() + " : " + message;
		}
	}

	public enum Address {
		PROGRAM("PROGRAM", 0x08000000),
		CONVERTER("CONVERTER", 0x080C0000),
		BIAS(BIAS_BOARD, 0x080E0000),
		HP_BIAS(HP_BIAS_BOARD, 0x081E0000);

		private String name;
		private int addr;

		private Address(String name, int addr) {
			this.name = name;
			this.addr = addr;
		}

		public String getName() {
			return name;
		}

		public int getAddr() {
			return addr;
		}

		public static Optional<Address> parse(String name) {
			return Arrays.stream(Address.values()).filter(a->a.name.equals(name)).findAny();
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public enum Answer {
		UNKNOWN("UNKNOWN", (byte) -1),
		NULL("NULL", (byte) 0),
		ACK("ACK", (byte) 0x79),
		NACK("NACK", (byte) 0x1F);

		private byte answer;
		private String name;

		private Answer(String name, byte answer) {
			this.answer = answer;
			this.name = name;
		}

		public byte getAnswer() {
			return answer;
		}

		public Answer setAnswer(byte answer){
			if(this==UNKNOWN)
				this.answer = answer;
			return this;
		}

		@Override
		public String toString() {
			return name+"(0x"+ToHex.bytesToHex(answer)+ ")";
		}

		public static Answer parse(byte b){
			Answer.UNKNOWN.setAnswer((byte) -1);
			return Arrays.stream(values()).filter(v->v.getAnswer()==b).findAny().orElse(Answer.UNKNOWN.setAnswer(b));
		}
	}

	public enum Command {
		/** 0x7F */
		CONNECT("CONNECT", new byte[] { 0x7F }),
		/** byte[] { 0x00, 0xFF} */
		GET("GET", new byte[] { 0x00, (byte) 0xFF }),
		/** byte[] { 0x11, 0xEE} */
		READ_MEMORY("READ_MEMORY", new byte[] { 0x11, (byte) 0xEE }),
		/** byte[] { 0x31, 0xCE} */
		WRITE_MEMORY("WRITE_MEMORY", new byte[] { 0x31, (byte) 0xCE }),
		/** byte[] { 0x43, 0xBC} */
		ERASE("ERASE", new byte[] { 0x43, (byte) 0xBC }),
		/** byte[] { 0x44, 0xBB} */
		EXTENDED_ERASE("EXTENDED_ERASE", new byte[] { 0x44, (byte) 0xBB }),
		/**
		 * Defined By User <br>
		 * Use 'Command setUserCommand(String name, byte[] command)' to define
		 */
		USER_COMMAND("USER_COMMAND", null);

		private String name;
		private byte[] command;

		private Command(String name, byte[] command) {
			this.command = command;
			this.name = name;
		}

		public byte[] toBytes() {
			return command;
		}

		@Override
		public String toString() {
			return name;
		}

		public Command setUserCommand(String name, byte[] command) {
			if (this == USER_COMMAND) {
				this.name = name;
				this.command = command;
			} else
				USER_COMMAND.setUserCommand(name, command);
			return USER_COMMAND;
		}
	}

	protected final static ExecutorService service = Executors.newSingleThreadScheduledExecutor(new MyThreadFactory());

	public static final int COMMAND_NON = 0;
	public static final byte GET_VERSION_AND_READ_PROTECTION_STATUS = 0x01;
	public static final byte COMMAND_GET_ID = 0x02;
	public static final byte COMMAND_GO = 0x21;
	public static final byte[] COMMAND_WRITE_PROTECT = new byte[] { 0x63, (byte) 0x9C };
	public static final byte[] COMMAND_WRITE_UNPROTECT = new byte[] { 0x73, (byte) 0x8C };
	public static final byte COMMAND_READOUT_PROTECT = (byte) 0x82;
	public static final byte COMMAND_READOUT_UNPROTECT = (byte) 0x92;

	public static final int MAX_VAR_RAM_SIZE = 256;// K Bytes

	private static Thread thread;
	private static MicrocontrollerSTM32 microcontrollerSTM32 = new MicrocontrollerSTM32();

	private static FlashSerialPort serialPort;

	private volatile static byte[] buffer;
	private volatile static Address address;

	private static FutureTask<byte[]> futureTask;

	private static FutureTask<Answer> eraseTask;

	private volatile Command command;
	private int waitingByteCount;
	private int waitTime = 1000;

	private MicrocontrollerSTM32() {
		logger.info("* Start *");
	}

	public static MicrocontrollerSTM32 getInstance(FlashSerialPort serialPort) {
		MicrocontrollerSTM32.serialPort = serialPort;
		return getInstance();
	}

	public static MicrocontrollerSTM32 getInstance() {
		return microcontrollerSTM32;
	}

	@Override
	public void notifyObservers(Object obj) {
		logger.trace("{}", obj);
		setChanged();
		super.notifyObservers(obj);
	}

	private boolean writeToFlashMemory() throws InterruptedException, IOException {
		logger.traceEntry();

		int length = 256;
		int readFrom = 0;
		BigDecimal onePercent = new BigDecimal(buffer.length).divide(new BigDecimal(100), 3, RoundingMode.HALF_EVEN);
		int addr = address.getAddr();
		addEnd();

		notifyObservers(new Status[] { Status.WRITING.setMessage("Erasing The Flash Memory."), Status.BUTTON.setMessage("Stop") });
		if (eraseFlash(buffer.length)) {
			notifyObservers(Status.WRITING.setMessage("Writing to The Flash Memory."));

			boolean error = false;
			while (true) {

				Answer answer;
				if ((answer=sendCommand(Command.WRITE_MEMORY))==Answer.ACK) {
					if ((answer=sendCommand(Command.USER_COMMAND.setUserCommand("Send Address", addCheckSum(getBytes(addr)))))==Answer.ACK) {
						int readTo = readFrom + length;
						readTo = readTo <= buffer.length ? readTo : buffer.length;
						if ((answer=sendCommand(Command.USER_COMMAND.setUserCommand("Send Data length", addCheckSum(addDataLength(Arrays.copyOfRange(buffer, readFrom, readTo))))))==Answer.ACK) {
							addr += length;
							readFrom = readTo;
							BigDecimal percent = new BigDecimal(readFrom).divide(onePercent, 2, RoundingMode.HALF_EVEN);

							if (readFrom < buffer.length)
								notifyObservers(percent);
							else {
								notifyObservers(new BigDecimal(100));
								Thread.sleep(200);
								notifyObservers(new Status[] { Status.WRITING.setMessage("Flash Memory Write is Completed"), Status.BUTTON.setMessage("Ok") });
								break;
							}
						} else {
							logger.warn("Send Data length ERROR. answer: {}", answer);
							error = true;
							notifyObservers(new Status[] { Status.ERROR.setMessage("ERROR.(Command." + Command.USER_COMMAND + ")"), Status.BUTTON.setMessage("Ok") });
							break;
						}
					}else{
						logger.warn("Send Address ERROR. answer: {}", answer);
						error = true;
						notifyObservers(new Status[] { Status.ERROR.setMessage("ERROR.(Command." + Command.USER_COMMAND + ")"), Status.BUTTON.setMessage("Ok") });
						break;
					}
				} else {
					logger.warn("WRITE MEMORY ERROR. answer: {}", answer);
					error = true;
					notifyObservers(new Status[] { Status.ERROR.setMessage("ERROR.(Command." + Command.USER_COMMAND + ")"), Status.BUTTON.setMessage("Ok") });
					break;
				}
			}
			if (!error)
				notifyObservers((Object) null);
		} else {
			logger.warn("ERASE MEMORY ERROR.");
			notifyObservers(new Status[] { Status.ERROR.setMessage("ERROR.(Command.Erasing The Flash Memory.)"), Status.BUTTON.setMessage("Ok") });
		}

		return readFrom >= buffer.length;
	}

	private boolean eraseFlash(int length) throws IOException{
		logger.traceEntry();
		int tmp = waitTime;
		waitTime = 15000;

		byte[] pagesToErase = getPagesToExtendedErase(address.getAddr(), length);

		logger.debug("pagesToErase: {}", pagesToErase);

		Answer sendCommand = sendCommand(Command.EXTENDED_ERASE);

		if(sendCommand==Answer.ACK )
			sendCommand = sendCommand(Command.USER_COMMAND.setUserCommand("Pages To Erase", addCheckSum(pagesToErase)));

		waitTime = tmp;

		MyThreadFactory.startThread(eraseTask);

		return sendCommand==Answer.ACK ;
	}

	private byte[] addDataLength(byte[] data) {
		byte[] toSend = new byte[data.length + 1];
		System.arraycopy(data, 0, toSend, 1, data.length);
		toSend[0] = (byte) (data.length - 1);
		return toSend;
	}

	private byte[] getPagesToExtendedErase(int startAddress, int length) {
		int[] allPages = new int[] { 	16 * KB, 16 * KB, 16 * KB, 16 * KB, 64 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB,
										16 * KB, 16 * KB, 16 * KB, 16 * KB, 64 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB, 128 * KB};


		logger.debug("address: {}; length: {}", ()->Integer.toHexString(startAddress), ()->length);

		int stopAddress = startAddress + length;
		byte[] result = null;

		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
			outputStream.write(0);	// The bootloader receives one half-word (two bytes) that contain N, the number of pages to be erased
			outputStream.write(0);

			int sum = Address.PROGRAM.getAddr();	//Start address

			for(int page = 0; page<allPages.length && sum<stopAddress; page++) {

				if(sum>=startAddress){
					final byte[] bytes = toBytes((short) page);
					outputStream.write(bytes); // The bootloader receives one half-word (two bytes) that contain N, the number of pages to be erased
												//  each half-word containing a page number (coded on two bytes, MSB first).
				}

				sum += allPages[page];
			}

			result = outputStream.toByteArray();
			final int pages = result.length/2 - 2;
			final byte[] arrayPages = toBytes((short) pages);
			result[0] = arrayPages[0]; // the number of pages to be erased –1.
			result[1] = arrayPages[1]; 
		} catch (IOException e) {
			logger.catching(e);
		}

		return result;
	}

	private static byte[] toBytes(final short pages) {
		return ByteBuffer.allocate(2).putShort(pages).array();
	}

	public void addEnd() {
		int length = buffer.length;
		logger.entry(length);
		int end = length % 4;
		if (end > 0) {
			buffer = Arrays.copyOf(buffer, length + 4 - end);
			Arrays.fill(buffer, length, buffer.length, (byte) 0xff);
		}
		logger.traceExit(buffer.length);
	}
//
//	private byte[] addLength(byte[] pages) {
//		byte[] toSend = new byte[pages.length + 2];
//		int pageCount = pages.length / 2 - 1;
//		toSend[0] = (byte) (pageCount >> 8);
//		toSend[1] = (byte) pageCount;
//		System.arraycopy(pages, 0, toSend, 2, pages.length);
//		return toSend;
//	}

	private boolean readFlashMemory() throws IOException{
		logger.traceEntry();

		boolean isRead = false;
		byte[] endBytes = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

		synchronized (this) {

			int length = MAX_VAR_RAM_SIZE - 1;
			int addr = address.getAddr();
			buffer = null;

			notifyObservers(new Status[] { Status.READING.setMessage("Reading"), Status.BUTTON.setMessage("Stop") });
			// 4 loops equals 1K Bytes
			for (int i = 0; i < ((MicrocontrollerSTM32.KB * 128) / MAX_VAR_RAM_SIZE); i++) {// max
																			// 128K
																			// bite
				Answer answer;
			// "read Command");
				if ((answer=sendCommand(Command.READ_MEMORY))==Answer.ACK) {
					// "Send Address");
					if ((answer=sendCommand(Command.USER_COMMAND.setUserCommand("Send Address", addCheckSum(getBytes(addr)))))==Answer.ACK) {
						// "Send Length");
						if ((answer=sendCommand(Command.USER_COMMAND.setUserCommand("Send Length", new byte[] { (byte) length, (byte) (length ^ 0xFF) })))==Answer.ACK) {
							byte[] read = serialPort.readBytes(MAX_VAR_RAM_SIZE);
							if (read != null) {
								buffer = addArray(buffer, read);
								if (Arrays.equals(Arrays.copyOfRange(read, read.length - 3, read.length), endBytes)) {
									isRead = true;
									break;
								}
							} else {
								logger.warn("ERROR. read: {}", read);
								notifyObservers(Status.ERROR.setMessage(Command.USER_COMMAND + " Error.(" + answer + ")"));
								break;
							}
						} else {
							logger.warn("Send Length ERROR. answer: {}", answer);
							notifyObservers(Status.ERROR.setMessage(Command.USER_COMMAND + " Error.(" + answer + ")"));
							break;
						}
					} else {
						logger.warn("Send Address ERROR. answer: {}", answer);
						notifyObservers(Status.ERROR.setMessage(Command.USER_COMMAND + " Error.(" + answer + ")"));
						break;
					}
				} else {
					logger.warn("READ_MEMORY ERROR. answer: {}", answer);
					notifyObservers(Status.ERROR.setMessage(Command.READ_MEMORY + " Error.(" + answer + ")"));
					break;
				}
				addr += MAX_VAR_RAM_SIZE;
			}
		}

		MyThreadFactory.startThread(futureTask);
		return logger.traceExit(isRead);
	}

	private Answer sendCommand(Command command) throws IOException {
		logger.debug("{} : {}", ()->command, ()->ToHex.bytesToHex(command.toBytes()));

		serialPort.writeBytes(command);

		byte[] readBytes;
		long start = System.currentTimeMillis();
		do{
			readBytes= serialPort.readBytes(1);
		}while(readBytes==null && System.currentTimeMillis()-start<=waitTime);

		logger.trace("EXIT Answer: {}", readBytes);

		return commandAnswer = Optional.ofNullable(readBytes).map(bs->bs[0]).map(Answer::parse).orElse(Answer.NULL);
	}

	private byte[] addCheckSum(byte[] original) {
		byte[] result = Arrays.copyOf(original, original.length + 1);
		result[original.length] = getCheckSum(original);
		return result;
	}

	private byte getCheckSum(byte[] original) {
		byte xor = 0;

		for (byte b : original)
			xor ^= b;

		return xor;
	}

	private byte[] getBytes(int flashAddr) {
		return new byte[] { (byte) (flashAddr >> 24), (byte) (flashAddr >> 16), (byte) (flashAddr >> 8), (byte) flashAddr };
	}

	private byte[] addArray(byte[] flash, byte[] read) {

		if (flash == null)
			flash = read;
		else {
			int oldSize = flash.length;
			flash = Arrays.copyOf(flash, oldSize + read.length);
			System.arraycopy(read, 0, flash, oldSize, read.length);
		}

		return flash;
	}

	public static void connect() throws InterruptedException {
		microcontrollerSTM32.waitingByteCount = 1;
		runThread(Command.CONNECT);
	}

	public static FutureTask<byte[]> read(String unitType) throws InterruptedException {
		return read(getAddress(unitType));
	}

	public static FutureTask<byte[]> read(Address address) throws InterruptedException {
		MicrocontrollerSTM32.address = address;
		microcontrollerSTM32.waitingByteCount = MAX_VAR_RAM_SIZE;

		futureTask = new FutureTask<byte[]>(new Callable<byte[]>() {

			@Override
			public byte[] call() throws Exception {
				return buffer;
			}
		});

		runThread(Command.READ_MEMORY);

		return futureTask;
	}

	private static Thread runThread(Command command) throws InterruptedException {
		logger.entry(command);

		if(service.isTerminated()){
			logger.error("Test message. Have to add some code");
			return null;
		}

		microcontrollerSTM32.command = command;

		service.execute(microcontrollerSTM32);

		return thread;
	}

	public synchronized byte[] getReadBytes() {
		return buffer;
	}

	@Override
	public String toString() {
		return "MicrocontrollerSTM32 [command=" + command + ", readBytes=" + ToHex.bytesToHex(buffer) + ", waitingByteCount=" + waitingByteCount + "]";
	}

	public Command getCommand() {
		return command;
	}

	private static Address getAddress(String unitType) {

		Address address = null;
		for(Address a:Address.values())
			if(a.toString().equals(unitType)){
				address = a;
				break;
			}

		if(address==null)
			address = Address.CONVERTER;

		return address;
	}

	public static void writeProfile(String selectedItem, String fileContents) throws InterruptedException {
		address = getAddress(selectedItem);
		buffer = fileContents.getBytes();
		runThread(Command.WRITE_MEMORY);
	}

	public static void writeProgram(byte[] fileContents) throws InterruptedException {
		MicrocontrollerSTM32.address = Address.PROGRAM;
		buffer = fileContents;
		runThread(Command.WRITE_MEMORY);
	}

	public static FutureTask<Answer> erase(String unitType) throws InterruptedException {
		return erase(getAddress(unitType));
	}

	private static FutureTask<Answer> erase(Address address) throws InterruptedException {

		MicrocontrollerSTM32.address = address;
		runThread(Command.ERASE);

		return eraseTask = new FutureTask<>(()->commandAnswer);
	}

	@Override
	public void run() {
		logger.entry(command, serialPort);
		try {
			synchronized (SerialPort.class) {
				switch (command) {
				case CONNECT:
					Status[] statuses = new Status[] { Status.CONNECTING.setMessage("Connecting"), Status.BUTTON.setMessage("Stop") };
					logger.trace("CONNECT; notifyObservers:{}", (Object[])statuses);
					notifyObservers(statuses);
					serialPort.writeBytes(command);
					buffer = serialPort.readBytes(waitingByteCount);
					break;
				case ERASE:
					logger.trace("ERASE");
					eraseFlash(1); // one page erase
					break;
				case EXTENDED_ERASE:
					logger.trace("EXTENDED_ERASE");
					break;
				case GET:
					logger.trace("GET");
					break;
				case READ_MEMORY:
					logger.trace("READ_MEMORY");
					readFlashMemory();
					break;
				case WRITE_MEMORY:
					logger.trace("WRITE_MEMORY");
					writeToFlashMemory();
				case USER_COMMAND:
					logger.trace("USER_COMMAND");
				}

			}
		} catch (Exception e) {
			logger.catching(e);
			// TODO Add Error Message
		}
		logger.trace("notifyObservers(); Obsorvers Count = {}", microcontrollerSTM32.countObservers());
		notifyObservers();
		logger.traceExit();
	}
}
