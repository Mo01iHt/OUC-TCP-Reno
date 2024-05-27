/***************************TCP-Tahoe
**************************** 郝文轩; 2024-01-02*/

package com.ouc.tcp.test;


import com.ouc.tcp.test.CheckSum;
import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class SenderWindow {
	private int cwnd = 1;	//拥塞窗口
	private volatile int ssthresh = 16;	//慢开始门限
	private Hashtable<Integer, TCP_PACKET> tcpPacks = new Hashtable<Integer, TCP_PACKET>();	//待发送的TCP数据报窗口（循环队列）
	private Hashtable<Integer, UDT_Timer> timers = new Hashtable<Integer, UDT_Timer>();	//为窗口内每个包分配一个计时器
//	private int queue_base = 0;	//窗口始端指针
//	private int next_to_send = 0;	//待发送包指针
//	private int queue_rear = 0;	//队末指针
//	private int[] flag; //0:have no pkt; 1:usable not yet send; 2:sent not yet ack'ed; 3: already ack'ed
	private int reAck = 0;	//重复包个数
	private int lastAck = -1;	//上一ACK序号
	private int curAcked = 0;	//窗口中已收到Ack的数量（等于cwnd时表示一个RTT结束）
	private int RTT = 1;
	
	Logger logger;
	Client client;
	
    private void resetCwndAndRtt() {	//慢开始初始化
        cwnd = 1;
        curAcked = 0;
        RTT = 1;
    }
	
    class Tahoe_Timer extends UDT_RetransTask {
        int seq;
        private TCP_PACKET packet;

        public Tahoe_Timer(Client client, TCP_PACKET packet) {
            super(client, packet);
            int[] data = packet.getTcpS().getData();
    		int datalen = data.length;
    		seq = (packet.getTcpH().getTh_seq() - 1) / datalen;
            this.packet = packet;
        }

        @Override
        public void run() {
        	logger.info("############### 拥塞慢开始 ###############");
            ssthresh = Math.max(cwnd / 2, 2); 
            resetCwndAndRtt();
            super.run();
            
            logger.info("[慢开始阶段]拥塞窗口信息：cwnd = " + cwnd + " ssthresh = " + ssthresh);
        }
    }
    
	private void initLogger(){	//设置log文件
		logger= Logger.getLogger(SenderWindow.class.getName());

		logger.setUseParentHandlers(false);
		FileHandler fh = null;
		try {
			fh = new FileHandler("RDTSender.log",false);
			fh.setFormatter(new SimpleFormatter());//输出格式

		} catch (IOException e) {
			e.printStackTrace();
		}
//		fh.setFormatter();
		logger.addHandler(fh);
	}
    
    public SenderWindow(Client client) {
    	initLogger();
        this.client = client;
    }

    public boolean isFull() {
        return cwnd <= tcpPacks.size();
    }
    
    public void pushPack(TCP_PACKET packet) {	//传包
    	int[] data = packet.getTcpS().getData();
		int datalen = data.length;
		int seq = (packet.getTcpH().getTh_seq() - 1) / datalen;
        timers.put(seq, new UDT_Timer());
        timers.get(seq).schedule(new Tahoe_Timer(client, packet), 1000, 1000);
        tcpPacks.put(seq, packet);
    }
    
	//对ACK处理
	public void rcvAck(int sequence) {
		logger.info("接收到ack: "+ sequence);
//		logger.info("cwnd = " + cwnd + " curAcked = " + curAcked);
		if(sequence == lastAck) {	//收到重复包
			reAck++;
			logger.info("############### 收到重复包：sequence = " 
						+ sequence + " 重复计数: " + reAck + " ###############");
			if(reAck >= 3) {	//收到3个重复包,快重传
				int resendSeq = sequence + 1;
				TCP_PACKET pack = tcpPacks.get(resendSeq);
				UDT_Timer timer = timers.get(resendSeq);
				if(pack != null && timer != null) {	
//					System.out.println("############### 快重传");
					client.send(pack);
					timer.cancel();
					timers.put(resendSeq, new UDT_Timer());
					timers.get(resendSeq).schedule(new Tahoe_Timer(client, pack), 1000, 1000);
					logger.info("############### 快重传：seq = " + resendSeq + " ###############");
				}
				System.out.println("############### Tahoe重传慢开始 ###############");
	            ssthresh = Math.max(cwnd / 2, 2); 	//乘法减小（但不能小于2）
	            resetCwndAndRtt();
	            logger.info("[慢开始阶段]拥塞窗口信息：cwnd = " + cwnd + " ssthresh = " + ssthresh);
			}
		}
		else {
			curAcked++;
			for(int i = lastAck +1; i <= sequence; i++) {	//累积确认
				tcpPacks.remove(i);
				if (timers.containsKey(i)) {
	                timers.get(i).cancel();
	                timers.remove(i);
	            }
			}
			lastAck = sequence;	//记录
			reAck = 0;
			if(cwnd < ssthresh) {	//慢开始阶段
				cwnd++;
				if(curAcked >= RTT) {
					logger.info("[一个传输轮次]拥塞窗口信息：cwnd = " + cwnd + " ssthresh = " + ssthresh);
					curAcked = 0;
					RTT *= 2;
				}
			}
			else {	//拥塞控制阶段
//				logger.info("############### cwnd = " + cwnd + "curAcked = " + curAcked + " ###############");
				if(curAcked >= RTT) {
					logger.info("############### 拥塞避免 ###############");
					curAcked = 0;
					cwnd ++;
					RTT ++;
					logger.info("[拥塞控制阶段]拥塞窗口信息：cwnd = " + cwnd);
				}
			}
		}
	}
}
