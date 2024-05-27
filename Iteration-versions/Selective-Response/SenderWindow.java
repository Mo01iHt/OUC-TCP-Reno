/***************************Selective-Response: 选择响应协议
**************************** 郝文轩; 2023-12-21*/

package com.ouc.tcp.test;


import com.ouc.tcp.test.CheckSum;
import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;

import java.util.Arrays;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class SenderWindow {
	private int WinSize = 16;	//窗口大小
	private TCP_PACKET[] tcpPacks = new TCP_PACKET[WinSize];	//待发送的TCP数据报窗口（循环队列）
	private UDT_Timer[] timers = new UDT_Timer[WinSize];	//为窗口内每个包分配一个计时器
	private int queue_base = 0;	//窗口始端指针
	private int next_to_send = 0;	//待发送包指针
	private int queue_rear = 0;	//队末指针
	private int[] flag; //0:have no pkt; 1:usable not yet send; 2:sent not yet ack'ed; 3: already ack'ed
	Client client;
	
	public SenderWindow(Client client, int WinSize) {
		this.client = client;
		this.WinSize = WinSize;
		flag = new int[WinSize];
		Arrays.fill(flag, 0);
	}
	
	//窗口满判断
	public boolean isFull() {
		return (queue_rear - queue_base) == WinSize;
	}
	
	//在队末处缓冲包
	public void pushPack(TCP_PACKET packet) {
		int rear_index = queue_rear % WinSize;	//求队末相对索引
		tcpPacks[rear_index] = packet;
		flag[rear_index] = 1;	//标记为可发的包
		queue_rear++;
		int[] data = packet.getTcpS().getData();
		int datalen = data.length;
		int sequence = (packet.getTcpH().getTh_seq() - 1) / datalen;
		//System.out.println(sequence);
	}
	
	//对ACK处理
	public void rcvAck(int sequence) {
		//求各项相对索引
		int seq_index = sequence % WinSize;
		int rear_index = queue_rear % WinSize;
		int base_index = queue_base % WinSize;
		if (sequence >= queue_base && sequence <= queue_rear) {
			if (flag[seq_index] == 2) {	//若未收到ACK
				timers[seq_index].cancel();	//计时器暂停
				timers[seq_index] = null;
				flag[seq_index] = 3;	//标记为已收到ACK
			}
			else {	//收到重复/错误ACK
				return;
			}
			if(sequence == queue_base) {	//若窗口左端的包收到ACK
				while(flag[base_index] == 3) {	//检索最大已ACK位置索引
					tcpPacks[base_index] = null;
					flag[base_index] = 0;	//标记为空（可放入包）
					queue_base++;
					base_index = queue_base % WinSize;
				}
			}
		}
		else {
			return;
		}
	}
	
	//获取可发送包
	public int getSendIndex() {
		int send_index = next_to_send % WinSize;
		if(flag[send_index] == 1 ) {
			flag[send_index] = 2;	//标识为已发送但未ACK
			next_to_send++;
			//System.out.println(send_index);
			return send_index;
		}
		else {
			return -1;
		}
	}
	
	//可发包预备工作
	public TCP_PACKET getPack(int index) {
		UDT_RetransTask retrans = new UDT_RetransTask(client, tcpPacks[index]);
		timers[index] = new UDT_Timer();
		timers[index].schedule(retrans, 1000, 1000); //计时开始，超时重发
		return tcpPacks[index];
	}
}
