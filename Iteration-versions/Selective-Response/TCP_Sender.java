/***************************2.1: ACK/NACK
**************************** Feng Hong; 2015-12-09*/

/***************************Selective-Response: 选择响应协议
**************************** 郝文轩; 2023-12-21*/

package com.ouc.tcp.test;

import com.ouc.tcp.test.CheckSum;
import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Sender extends TCP_Sender_ADT {
	
	private TCP_PACKET tcpPack;	//待发送的TCP数据报
	private volatile int flag = 1;
	//private UDT_Timer timer;	//计时器
	//private UDT_RetransTask reTrans;	//重传任务
	private SenderWindow window = new SenderWindow(client, 16);
	
	/*构造函数*/
	public TCP_Sender() {
		super();	//调用超类构造函数
		super.initTCP_Sender(this);		//初始化TCP发送端
	}
	
	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
	public void rdt_send(int dataIndex, int[] appData) {
		
		//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		tcpH.setTh_seq(dataIndex * appData.length + 1);//包序号设置为字节流号：
		tcpS.setData(appData);
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);		
		//更新带有checksum的TCP 报文头		
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		//System.out.println(dataIndex);
		//若未装满则载入包
		if(window.isFull()) {
			flag = 0;
		}
			
		//装满则等待
		while(flag == 0);
		
		try {
			window.pushPack(tcpPack.clone());
		} catch (CloneNotSupportedException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		
		//发送TCP数据报
		int index = window.getSendIndex();
		if(index != -1) {
			udt_send(window.getPack(index));
			
		}
	}
	
	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)7);		
		//System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());				
		//发送数据报
		client.send(stcpPack);
	}
	
	@Override
	//需要修改
	public void waitACK() {
		//循环检查ackQueue
		//循环检查确认号对列中是否有新收到的ACK		
		if(!ackQueue.isEmpty()){
			int currentAck=(ackQueue.poll() - 1) / 100;	//?此处已知字节流长度100
			window.rcvAck(currentAck);	//交于窗口处理
			if(!window.isFull()) {	//释放锁
				flag = 1;
			}
		}
	}

	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为-1；不需要修改
	public void recv(TCP_PACKET recvPack) {
		System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
		ackQueue.add(recvPack.getTcpH().getTh_ack());
	    System.out.println();	
	   
	    //处理ACK报文
	    waitACK();
	   
	}
	
}
