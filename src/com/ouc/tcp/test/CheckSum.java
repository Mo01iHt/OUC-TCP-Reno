package com.ouc.tcp.test;

import java.util.Arrays;
import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_SEGMENT;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		long checkSum = 0;
		
		CRC32 crc = new CRC32();
		
		/*从首部获取seq、ack和sum*/
		TCP_HEADER header = tcpPack.getTcpH();
		crc.update(header.getTh_seq());
		crc.update(header.getTh_ack());
//		crc.update(header.getTh_sum());
		
		/*从数据部获取数据*/
		TCP_SEGMENT segment = tcpPack.getTcpS();
		int[] data = segment.getData();
		for(int i = 0;i < data.length; i++) {
			crc.update(data[i]);
		}

		/*计算校验和*/
		checkSum = crc.getValue();
		return (short) checkSum;
	}
	
}
