package com.fellow.yoo.service;

import java.util.NoSuchElementException;
import java.util.Stack;

import org.jivesoftware.smack.packet.Packet;

public class PacketQueue {

	private static Stack<Packet> pending = new Stack<Packet>();
	
	public static void addPacket(Packet packet) {
		pending.push(packet);
	}
	
	public static Packet getPacket() {
		try {
			return pending.firstElement();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	
	public static void removePacket(Packet packet) {
		pending.remove(packet);
	}
	
	
	
	
}
