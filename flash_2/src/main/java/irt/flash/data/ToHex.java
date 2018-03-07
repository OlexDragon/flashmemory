package irt.flash.data;

public class ToHex {

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte... bytes) {

		String string = null;

		if (bytes != null) {
			char[] hexChars = new char[bytes.length * 3];
			int v;
			for (int j = 0; j < bytes.length; j++) {
				v = bytes[j] & 0xFF;
				hexChars[j * 3] = hexArray[v >>> 4];
				hexChars[j * 3 + 1] = hexArray[v & 0x0F];
				hexChars[j * 3 + 2] = ' ';
			}
			string = new String(hexChars).trim();
		}


		return string;
	}

	public static byte parseToByte(String hexStr) {
		byte result = 0;

		if(hexStr!=null){
			if(hexStr.length()>=2)
				result = (byte) ((Character.digit(hexStr.charAt(0), 16) << 4)+ Character.digit(hexStr.charAt(1), 16));
			else if(!hexStr.isEmpty())
				result = (byte) Character.digit(hexStr.charAt(0), 16);
		}

		return result;
	}
}
