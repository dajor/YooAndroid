package com.fellow.yoo.xmpp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.packet.PacketExtension;

import android.util.Log;

public class VCardTempXUpdateExtension implements PacketExtension {


	private byte [] data;

	public String getElementName() {
		return "x";
	}


	/**
	 * Returns the XML namespace of the extension sub-packet root element.
	 * According the specification the namespace is always "http://jabber.org/protocol/xhtml-im"
	 *
	 * @return the XML namespace of the packet extension.
	 */

	public String getNamespace() {
		return "vcard-temp:x:update";
	}



	public String toXML() {
		
		final StringBuilder sha1 = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte [] digest = md.digest(data);
		    
		    for (byte b: digest) {
		    	sha1.append(String.format("%02x", b & 0xff));
		    }
		} catch (NoSuchAlgorithmException e) {
			Log.e("ChatTools", "SHA-1 algorithm missing", e);
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\">");

		// Loop through all the bodies and append them to the string buffer
		buf.append("<photo>").append(sha1).append("</photo>");
		buf.append("</").append(getElementName()).append(">");

		return buf.toString();

	}


	public void setPhotoData(byte [] pData) {
		data = pData;
	}
}
