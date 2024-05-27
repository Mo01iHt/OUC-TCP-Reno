/***************************Selective-Response: 选择响应协议
**************************** 郝文轩; 2023-12-21*/

package com.ouc.tcp.test;

import com.ouc.tcp.test.CheckSum;

import java.util.Arrays;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class ReceiverWindow {
	private int WinSize = 16;	//窗口大小
	private TCP_PACKET[] tcpPacks;	//接收TCP数据报窗口（循环队列）
	private int queue_base = 0;	//窗口始端指针
	private int queue_rear = WinSize-1;	//队末指针
	private int[] flag; //0:expected not yet received; 1: already ack'ed
	Client client;
	
	public ReceiverWindow(Client client, int WinSize) {
		this.client = client;
		this.WinSize = WinSize;
		tcpPacks = new TCP_PACKET[WinSize];
		flag = new int[WinSize];
		Arrays.fill(flag, 0);
	}
	
	public boolean isBase(int seq) {
		return queue_base == seq;
	}
	
	public int pullPack(TCP_PACKET packet) {
		int[] data = packet.getTcpS().getData();
		int datalen = data.length;
		int sequence = (packet.getTcpH().getTh_seq() - 1) / datalen;
		
		//求各项相对索引
		int seq_index = sequence % WinSize;
		int rear_index = queue_rear % WinSize;
		int base_index = queue_base % WinSize;
		if (sequence >= queue_base && sequence <= queue_rear) {
			if (flag[seq_index] == 0) {	//若未收到packet
				tcpPacks[seq_index] = packet;
				flag[seq_index] = 1;	//标记为已收到
			}
		}
		else if (sequence > queue_rear) {
			return -1;
		}
		return sequence;
	}
	
	public int seq_deliver() {
		int rear_index = queue_rear % WinSize;
		int base_index = queue_base % WinSize;
		if(flag[base_index] == 1) {
			queue_base++;
			queue_rear++;
			flag[base_index] = 0;
			return base_index;
		}
		else {
			return -1;
		}
	}
	
	public TCP_PACKET getPack(int index) {
		TCP_PACKET pack = tcpPacks[index];
		tcpPacks[index] = null;
		return pack;
	}
	
	
}
