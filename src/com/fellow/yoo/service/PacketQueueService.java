package com.fellow.yoo.service;

import org.jivesoftware.smack.packet.Packet;

import com.fellow.yoo.chat.ChatTools;

import android.app.IntentService;
import android.content.Intent;

public class PacketQueueService extends IntentService {

	public PacketQueueService() {
		super("ChatService");
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		// send all pending packets
		while (true) {
			Packet packet = PacketQueue.getPacket();
			if (packet == null) break;
			if (ChatTools.sharedInstance().canSend(packet)) {
				PacketQueue.removePacket(packet);
			}
		}
	}

}
