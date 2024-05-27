/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/

/***************************TCP-Tahoe
**************************** 郝文轩; 2024-01-02*/

package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

import com.ouc.tcp.test.CheckSum;
import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;	//回复的ACK报文段
	int sequence=1;//用于记录当前待接收的包序号，注意包序号不完全是
//	int pre_seq=-1;	//用于记录上一个接收成功的包序号
	int excSeq = 0;	//期望数据包seq
	private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<Integer, TCP_PACKET>();
//	private ReceiverWindow window = new ReceiverWindow(client, 16);
		
	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码，生成ACK
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			int[] data = recvPack.getTcpS().getData();
			int datalen = data.length;
			int sequence = (recvPack.getTcpH().getTh_seq() - 1) / datalen;
			if(sequence == excSeq) {	//若收到期望包
				dataQueue.add(data);
				excSeq ++;
				while(packets.containsKey(excSeq)) {	//将有序包逐个上传，并从窗口中剔除
	            	dataQueue.add(packets.get(excSeq).getTcpS().getData());
	            	packets.remove(excSeq);
	    	        excSeq ++;
	            }
				if(dataQueue.size() >= 20 || excSeq >= 999)
	            	deliver_data();
			}        
            else {	//若收到失序包
    			if(!packets.contains(sequence) && sequence > excSeq) {
    				packets.put(sequence, recvPack);
    			}
            }
			tcpH.setTh_ack((excSeq - 1) * datalen + 1);//生成ACK报文段
	        ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
	        tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
	        reply(ackPack);
				
		}
		
		System.out.println();
		
		
		//交付数据（每20组数据交付一次）
		deliver_data();	
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			//循环检查data队列中是否有新交付数据
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();
				
				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				
				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)7);	//
				
		//发送数据报
		client.send(replyPack);
	}
	
}
