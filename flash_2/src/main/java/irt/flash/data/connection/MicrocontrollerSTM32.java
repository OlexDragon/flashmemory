package irt.flash.data.connection;

import irt.flash.data.ToHex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;

import jssc.SerialPortException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class MicrocontrollerSTM32 extends Observable implements Runnable {

	private static final Logger logger = (Logger) LogManager.getLogger();

	public static final String BIAS_BOARD = "Bias Board";

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
		SERIAL_NUMBER("device-serial-number", 	Arrays.asList(new String[] { "Common" })),
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

	}

	public enum Address {
		PROGRAM("PROGRAM", 0x08000000), CONVERTER("CONVERTER", 0x080C0000), BIAS(BIAS_BOARD, 0x080E0000);

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

		@Override
		public String toString() {
			return name;
		}
	}

	public enum Answer {
		UNKNOWN("UNKNOWN", (byte) -1), NULL("NULL", (byte) 0), ACK("ACK", (byte) 0x79), NACK("NACK", (byte) 0x1F);

		private byte answer;
		private String name;

		private Answer(String name, byte answer) {
			this.answer = answer;
			this.name = name;
		}

		public byte getAnswer() {
			return answer;
		}

		@Override
		public String toString() {
			return name;
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

	private volatile Command command;
	private int waitingByteCount;
	private int waitTime = 500;
	private volatile Answer lastAnswer;

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
		logger.trace(obj);
		setChanged();
		super.notifyObservers(obj);
	}

	private boolean writeToFlashMemory() throws SerialPortException, InterruptedException {
		logger.entry();

		int length = 256;
		int readFrom = 0;
		BigDecimal onePercent = new BigDecimal(buffer.length).divide(new BigDecimal(100), 3, RoundingMode.HALF_EVEN);
		int addr = address.getAddr();
		addEnd();

		notifyObservers(new Status[] { Status.WRITING.setMessage("Erasing The Flash Memory."), Status.BUTTON.setMessage("Stop") });
		if (eraseFlash()) {
			notifyObservers(Status.WRITING.setMessage("Wtiting to The Flash Memory."));

			boolean error = false;
			while (true) {

				if (sendCommand(Command.WRITE_MEMORY)) {
					if (sendCommand(Command.USER_COMMAND.setUserCommand("Send Address", addCheckSum(getBytes(addr))))) {
						int readTo = readFrom + length;
						readTo = readTo <= buffer.length ? readTo : buffer.length;
						if (sendCommand(Command.USER_COMMAND.setUserCommand("Send Data", addCheckSum(addDataLength(Arrays.copyOfRange(buffer, readFrom, readTo)))))) {
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
							error = true;
							notifyObservers(new Status[] { Status.ERROR.setMessage("ERROR.(Command." + Command.USER_COMMAND + ")"), Status.BUTTON.setMessage("Ok") });
							break;
						}
					}
				} else {
					error = true;
					notifyObservers(new Status[] { Status.ERROR.setMessage("ERROR.(Command." + Command.USER_COMMAND + ")"), Status.BUTTON.setMessage("Ok") });
					break;
				}
			}
			if (!error)
				notifyObservers((Object) null);
		} else {
			notifyObservers(new Status[] { Status.ERROR.setMessage("ERROR.(Command.Esasing The Flash Memory.)"), Status.BUTTON.setMessage("Ok") });
		}

		return logger.exit(readFrom >= buffer.length);
	}

	private boolean eraseFlash() throws SerialPortException {
		logger.entry(waitTime);
		int tmp = waitTime;
		waitTime = 10000;

		byte[] pagesToErase;
		if (address == Address.PROGRAM)
			pagesToErase = getProgramPages();
		else if (address == Address.CONVERTER)
			pagesToErase = new byte[] { 0, 10 };
		else
			pagesToErase = new byte[] { 0, 11 };

		boolean isDon = sendCommand(Command.EXTENDED_ERASE) && sendCommand(Command.USER_COMMAND.setUserCommand("Pages To Erase", addCheckSum(addLength(pagesToErase))));

		waitTime = tmp;

		return isDon;
	}

	private byte[] addDataLength(byte[] data) {
		byte[] toSend = new byte[data.length + 1];
		System.arraycopy(data, 0, toSend, 1, data.length);
		toSend[0] = (byte) (data.length - 1);
		return toSend;
	}

	private byte[] getProgramPages() {
		int[] allPages = new int[] { 16 * 1024, 16 * 1024, 16 * 1024, 16 * 1024, 64 * 1024, 128 * 1024, 128 * 1024, 128 * 1024, 128 * 1024, 128 * 1024 };
		byte[] pagesToErase = null;
		if (buffer != null) {
			addEnd();
			int length = buffer.length;
			int pageCount = 0;

			for (int i : allPages)
				if (length > 0) {
					pageCount++;
					length = length - i;
				} else
					break;

			pagesToErase = new byte[pageCount * 2];

			for (int i = 0; i < pageCount; i++) {
				byte[] b = toBytes((short) i);
				int index = i * 2;
				pagesToErase[index] = b[0];
				pagesToErase[++index] = b[1];
			}

		}
		return pagesToErase;
	}

	private byte[] toBytes(short shortToBytes) {
		return new byte[] { (byte) (shortToBytes >> 8), (byte) shortToBytes };
	}

	public void addEnd() {
		int length = buffer.length;
		logger.entry(length);
		int end = length % 4;
		if (end > 0) {
			buffer = Arrays.copyOf(buffer, length + 4 - end);
			Arrays.fill(buffer, length, buffer.length, (byte) 0xff);
		}
		logger.exit(buffer.length);
	}

	private byte[] addLength(byte[] pages) {
		byte[] toSend = new byte[pages.length + 2];
		int pageCount = pages.length / 2 - 1;
		toSend[0] = (byte) (pageCount >> 8);
		toSend[1] = (byte) pageCount;
		System.arraycopy(pages, 0, toSend, 2, pages.length);
		return toSend;
	}

	private boolean readFlashMemory() throws SerialPortException {
		logger.entry();

		boolean isRead = false;
		byte[] endBytes = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

		synchronized (this) {

			int length = MAX_VAR_RAM_SIZE - 1;
			int addr = address.getAddr();
			buffer = null;

			notifyObservers(new Status[] { Status.READING.setMessage("Reading"), Status.BUTTON.setMessage("Stop") });
			// 4 loops equals 1K Bytes
			for (int i = 0; i < ((1024 * 128) / MAX_VAR_RAM_SIZE); i++) {// max
																			// 128K
																			// bite
			// "read Command");
				if (sendCommand(Command.READ_MEMORY)) {
					// "Send Address");
					if (sendCommand(Command.USER_COMMAND.setUserCommand("Send Address", addCheckSum(getBytes(addr))))) {
						// "Send Length");
						if (sendCommand(Command.USER_COMMAND.setUserCommand("Send Length", new byte[] { (byte) length, (byte) (length ^ 0xFF) }))) {
							byte[] read = serialPort.readBytes(MAX_VAR_RAM_SIZE);
							if (read != null) {
								buffer = addArray(buffer, read);
								if (Arrays.equals(Arrays.copyOfRange(read, read.length - 3, read.length), endBytes)) {
									isRead = true;
									break;
								}
							} else {
								notifyObservers(Status.ERROR.setMessage(Command.USER_COMMAND + " Error.(" + lastAnswer + ")"));
								break;
							}
						} else {
							notifyObservers(Status.ERROR.setMessage(Command.USER_COMMAND + " Error.(" + lastAnswer + ")"));
							break;
						}
					} else {
						notifyObservers(Status.ERROR.setMessage(Command.USER_COMMAND + " Error.(" + lastAnswer + ")"));
						break;
					}
				} else {
					notifyObservers(Status.ERROR.setMessage(Command.READ_MEMORY + " Error.(" + lastAnswer + ")"));
					break;
				}
				addr += MAX_VAR_RAM_SIZE;
			}
		}

		return logger.exit(isRead);
	}

	private boolean sendCommand(Command command) throws SerialPortException {
		logger.entry(command, waitTime);

		Level level = logger.getLevel();
		if(level==Level.ALL || level==Level.TRACE)
			logger.trace("Write ={}", ToHex.bytesToHex(command.toBytes()));
		serialPort.writeBytes(command);

		byte[] readBytes = serialPort.readBytes(1, waitTime);

		if (readBytes == null)
			lastAnswer = logger.exit(Answer.NULL);
		else if (readBytes[0] == Answer.ACK.getAnswer())
			lastAnswer = logger.exit(Answer.ACK);
		else if (readBytes[0] == Answer.NACK.getAnswer())
			lastAnswer = logger.exit(Answer.NACK);
		else
			lastAnswer = logger.exit(Answer.UNKNOWN);

		return lastAnswer == Answer.ACK;
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

	public static void read(String unitType) throws InterruptedException {
		read(getAddress(unitType));
	}

	public static void read(Address address) throws InterruptedException {
		MicrocontrollerSTM32.address = address;
		microcontrollerSTM32.waitingByteCount = MAX_VAR_RAM_SIZE;
		runThread(Command.READ_MEMORY);
	}

	private static Thread runThread(Command command) throws InterruptedException {
		logger.entry(command);
		if (microcontrollerSTM32 != null) {
			if (thread != null)
				thread.join();
			microcontrollerSTM32.command = command;
			thread = new Thread(microcontrollerSTM32);
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.setDaemon(true);
			thread.start();
		}
		return logger.exit(thread);
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
		logger.entry(unitType);
		Address address;
		switch (unitType) {
		case MicrocontrollerSTM32.BIAS_BOARD:
			address = Address.BIAS;
			break;
		default:
			address = Address.CONVERTER;
		}
		return logger.exit(address);
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

	public static void erase(String unitType) throws InterruptedException {
		erase(getAddress(unitType));
	}

	private static void erase(Address address) throws InterruptedException {
		MicrocontrollerSTM32.address = address;
		runThread(Command.ERASE);
	}

	@Override
	public void run() {
		logger.entry(command, serialPort);
		try {
			synchronized (serialPort) {
				switch (command) {
				case CONNECT:
					Status[] statuses = new Status[] { Status.CONNECTING.setMessage("Connecting"), Status.BUTTON.setMessage("Stop") };
					logger.trace("CONNECT; notifyObservers:{}", (Object)statuses);
					notifyObservers(statuses);
					serialPort.writeBytes(command);
					buffer = serialPort.readBytes(waitingByteCount);
					break;
				case ERASE:
					logger.trace("ERASE");
					eraseFlash();
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
		logger.exit();
	}
}
