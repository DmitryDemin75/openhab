/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tcp.protocol.internal;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Dictionary;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.openhab.binding.tcp.AbstractDatagramChannelBinding;
import org.openhab.binding.tcp.Direction;
import org.openhab.binding.tcp.internal.TCPActivator;
import org.openhab.binding.tcp.protocol.ProtocolBindingProvider;
import org.openhab.binding.tcp.protocol.UDPBindingProvider;
//import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UDPBinding is most "simple" implementation of a UDP based ASCII protocol. It sends and received 
 * data as ASCII strings. Data sent out is padded with a CR/LF. This should be sufficient for a lot
 * of home automation devices that take simple ASCII based control commands, or that send back
 * text based status messages
 * 
 * 
 * @author Karel Goderis
 * @since 1.1.0
 *
 */
public class UDPBinding extends AbstractDatagramChannelBinding<UDPBindingProvider> implements ManagedService {

	static private final Logger logger = LoggerFactory.getLogger(UDPBinding.class);
	
	/** RegEx to extract a parse a function String <code>'(.*?)\((.*)\)'</code> */
	private static final Pattern EXTRACT_FUNCTION_PATTERN = Pattern.compile("(.*?)\\((.*)\\)");

	// time to wait for a reply, in milliseconds
	private static int timeOut = 3000;
	// flag to use only blocking write/read operations
	private static boolean blocking = false;
	// string to prepend to data being sent
	private static String preAmble = "";
	// string to append to data being sent
	private static String postAmble = "\r\n";
	// flag to use the reply of the remote end to update the status of the Item receiving the data
	private static boolean updateWithResponse = true;
	// used character set
	private static String charset = "ASCII";
	int lvl2;

	@Override
	protected boolean internalReceiveChanneledCommand(String itemName,
			Command command, Channel sChannel, String commandAsString) {

		ProtocolBindingProvider provider = findFirstMatchingBindingProvider(itemName);

		if(command != null ){
			
			ByteBuffer outputBuffer = null;
			/*byte[] itemButtonSwitch = {(byte)0x42, (byte)0x75, (byte)0x74, (byte)0x74, (byte)0x6f, (byte)0x6e, (byte)0x53, (byte)0x77, (byte)0x69, (byte)0x74, (byte)0x63, (byte)0x68};
			byte[] itemB = null;
			try {
				itemB = "ButtonSwitch".getBytes(charset);
			} catch (UnsupportedEncodingException e){
				logger.warn("Exception while attempting an unsupported encoding scheme");
			}
			*/
			String lightItem = "UDPswitch";
			String audioItem = "AudioSwitch";
			String dimmerItem = "DimmerTest";
			int SID, DID, Command, ch, lvl;
			
			byte[] AddArray;
			if (itemName.matches(lightItem) && commandAsString == "ON"){
				//byte[] LightON = {(byte)0xc0, (byte)0xa8, (byte)0x0a, (byte)0x07, (byte)0x53, (byte)0x4d, (byte)0x41, (byte)0x52, (byte)0x54, (byte)0x43, (byte)0x4c, (byte)0x4f, (byte)0x55, (byte)0x44, (byte)0xaa, (byte)0xaa, (byte)0x0f, (byte)0x03, (byte)0xfe, (byte)0xff, (byte)0xfe, (byte)0x00, (byte)0x31, (byte)0x01, (byte)0x02, (byte)0x21, (byte)0x64, (byte)0x00, (byte)0x00, (byte)0xaf, (byte)0x13};
				SID = 1;
				DID = 2;
				Command = 0x0031;
				ch = 21;
				lvl = 100;
				AddArray = new byte[4];
				AddArray[0] = (byte)ch;
				AddArray[1] = (byte)lvl;
				AddArray[2] = (byte)0;
				AddArray[3] = (byte)0;
				byte[] SBUSCommand = PackCommand(SID, DID, Command, AddArray);
				outputBuffer = ByteBuffer.allocate(SBUSCommand.length).put(SBUSCommand);
			} 
			else if (itemName.matches(lightItem) && commandAsString == "OFF"){
				//byte[] LightOFF = {(byte)0xc0, (byte)0xa8, (byte)0x0a, (byte)0x07, (byte)0x53, (byte)0x4d, (byte)0x41, (byte)0x52, (byte)0x54, (byte)0x43, (byte)0x4c, (byte)0x4f, (byte)0x55, (byte)0x44, (byte)0xaa, (byte)0xaa, (byte)0x0f, (byte)0x03, (byte)0xfe, (byte)0xff, (byte)0xfe, (byte)0x00, (byte)0x31, (byte)0x01, (byte)0x16, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xb4, (byte)0xb3};
				SID = 1;
				DID = 2;
				Command = 0x0031;
				ch = 21;
				lvl = 0;
				AddArray = new byte[4];
				AddArray[0] = (byte)ch;
				AddArray[1] = (byte)lvl;
				AddArray[2] = (byte)0;
				AddArray[3] = (byte)0;
				byte[] SBUSCommand = PackCommand(SID, DID, Command, AddArray);
				outputBuffer = ByteBuffer.allocate(SBUSCommand.length).put(SBUSCommand);
			} 
			else if (itemName.matches(dimmerItem) && commandAsString == "DECREASE"){
				//byte[] LightOFF = {(byte)0xc0, (byte)0xa8, (byte)0x0a, (byte)0x07, (byte)0x53, (byte)0x4d, (byte)0x41, (byte)0x52, (byte)0x54, (byte)0x43, (byte)0x4c, (byte)0x4f, (byte)0x55, (byte)0x44, (byte)0xaa, (byte)0xaa, (byte)0x0f, (byte)0x03, (byte)0xfe, (byte)0xff, (byte)0xfe, (byte)0x00, (byte)0x31, (byte)0x01, (byte)0x16, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xb4, (byte)0xb3};
				SID = 1;
				DID = 2;
				Command = 0x0031;
				ch = 2;
				lvl2 = lvl2 - 25;
				if(lvl2<0) lvl2 = 0;
				AddArray = new byte[4];
				AddArray[0] = (byte)ch;
				AddArray[1] = (byte)lvl2;
				AddArray[2] = (byte)0;
				AddArray[3] = (byte)0;
				byte[] SBUSCommand = PackCommand(SID, DID, Command, AddArray);
				outputBuffer = ByteBuffer.allocate(SBUSCommand.length).put(SBUSCommand);
			} 
			else if (itemName.matches(dimmerItem) && commandAsString == "INCREASE"){
				//byte[] LightOFF = {(byte)0xc0, (byte)0xa8, (byte)0x0a, (byte)0x07, (byte)0x53, (byte)0x4d, (byte)0x41, (byte)0x52, (byte)0x54, (byte)0x43, (byte)0x4c, (byte)0x4f, (byte)0x55, (byte)0x44, (byte)0xaa, (byte)0xaa, (byte)0x0f, (byte)0x03, (byte)0xfe, (byte)0xff, (byte)0xfe, (byte)0x00, (byte)0x31, (byte)0x01, (byte)0x16, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xb4, (byte)0xb3};
				SID = 1;
				DID = 2;
				Command = 0x0031;
				ch = 2;
				lvl2 = lvl2 + 25;
				if(lvl2>100) lvl2 = 100;
				AddArray = new byte[4];
				AddArray[0] = (byte)ch;
				AddArray[1] = (byte)lvl2;
				AddArray[2] = (byte)0;
				AddArray[3] = (byte)0;
				byte[] SBUSCommand = PackCommand(SID, DID, Command, AddArray);
				outputBuffer = ByteBuffer.allocate(SBUSCommand.length).put(SBUSCommand);
			}
			else if (itemName.matches(audioItem) && commandAsString.matches("3")) {
				//byte[] Play = {(byte)0xC0, (byte)0xA8, (byte)0x0A, (byte)0x7, (byte)0x53, (byte)0x4D, (byte)0x41, (byte)0x52, (byte)0x54, (byte)0x43, (byte)0x4C, (byte)0x4F, (byte)0x55, (byte)0x44, (byte)0xAA, (byte)0xAA, (byte)0x0D, (byte)0x3, (byte)0xFE, (byte)0xFF, (byte)0xFE, (byte)0x2, (byte)0x18, (byte)0x1, (byte)0xC8, (byte)0x4, (byte)0x3, (byte)0xFC, (byte)0x7C};
				SID = 1;
				DID = 200;
				Command = 0x0218;
				AddArray = new byte[4];
				AddArray[0] = (byte)4;
				AddArray[1] = (byte)3;
				AddArray[2] = (byte)0;
				AddArray[3] = (byte)0;
				byte[] SBUSCommand = PackCommand(SID, DID, Command, AddArray);
				outputBuffer = ByteBuffer.allocate(SBUSCommand.length).put(SBUSCommand);
			} 
			else if (itemName.matches(audioItem) && commandAsString.matches("4")) {
				//byte[] Stop = {(byte)0xC0, (byte)0xA8, (byte)0x0A, (byte)0x7, (byte)0x53, (byte)0x4D, (byte)0x41, (byte)0x52, (byte)0x54, (byte)0x43, (byte)0x4C, (byte)0x4F, (byte)0x55, (byte)0x44, (byte)0xAA, (byte)0xAA, (byte)0x0D, (byte)0x3, (byte)0xFE, (byte)0xFF, (byte)0xFE, (byte)0x2, (byte)0x18, (byte)0x1, (byte)0xC8, (byte)0x4, (byte)0x3, (byte)0xFC, (byte)0x7C};
				SID = 1;
				DID = 200;
				Command = 0x0218;
				AddArray = new byte[4];
				AddArray[0] = (byte)4;
				AddArray[1] = (byte)4;
				AddArray[2] = (byte)0;
				AddArray[3] = (byte)0;
				byte[] SBUSCommand = PackCommand(SID, DID, Command, AddArray);
				outputBuffer = ByteBuffer.allocate(SBUSCommand.length).put(SBUSCommand);
			} 
			else if (itemName.matches(audioItem) && commandAsString.matches("1")) {
				//byte[] Prev = {(byte)0xC0, (byte)0xA8, (byte)0x0A, (byte)0x7, (byte)0x53, (byte)0x4D, (byte)0x41, (byte)0x52, (byte)0x54, (byte)0x43, (byte)0x4C, (byte)0x4F, (byte)0x55, (byte)0x44, (byte)0xAA, (byte)0xAA, (byte)0x0D, (byte)0x3, (byte)0xFE, (byte)0xFF, (byte)0xFE, (byte)0x2, (byte)0x18, (byte)0x1, (byte)0xC8, (byte)0x4, (byte)0x1, (byte)0xDC, (byte)0x3E};
				SID = 1;
				DID = 200;
				Command = 0x0218;
				AddArray = new byte[4];
				AddArray[0] = (byte)4;
				AddArray[1] = (byte)1;
				AddArray[2] = (byte)0;
				AddArray[3] = (byte)0;
				byte[] SBUSCommand = PackCommand(SID, DID, Command, AddArray);
				outputBuffer = ByteBuffer.allocate(SBUSCommand.length).put(SBUSCommand);
			} 
			else if (itemName.matches(audioItem) && commandAsString.matches("2")) {
				//byte[] Next = {(byte)0xC0, (byte)0xA8, (byte)0x0A, (byte)0x7, (byte)0x53, (byte)0x4D, (byte)0x41, (byte)0x52, (byte)0x54, (byte)0x43, (byte)0x4C, (byte)0x4F, (byte)0x55, (byte)0x44, (byte)0xAA, (byte)0xAA, (byte)0x0D, (byte)0x3, (byte)0xFE, (byte)0xFF, (byte)0xFE, (byte)0x2, (byte)0x18, (byte)0x1, (byte)0xC8, (byte)0x4, (byte)0x2, (byte)0xEC, (byte)0x5D};
				SID = 1;
				DID = 200;
				Command = 0x0218;
				AddArray = new byte[4];
				AddArray[0] = (byte)4;
				AddArray[1] = (byte)2;
				AddArray[2] = (byte)0;
				AddArray[3] = (byte)0;
				byte[] SBUSCommand = PackCommand(SID, DID, Command, AddArray);
				outputBuffer = ByteBuffer.allocate(SBUSCommand.length).put(SBUSCommand);
			}
			else {
				//String transformedMessage = transformResponse(provider.getProtocolCommand(itemName, command),commandAsString);
				String UDPCommandName = "123";//commandAsString;//preAmble + transformedMessage + postAmble ;
				try {
					outputBuffer = ByteBuffer.allocate(UDPCommandName.getBytes(charset).length);
					outputBuffer.put(UDPCommandName.getBytes(charset));
				} catch (UnsupportedEncodingException e) {
					logger.warn("Exception while attempting an unsupported encoding scheme");
				}
			}
						
			// send the buffer in an asynchronous way
			ByteBuffer result = null;
			try {
				result = writeBuffer(outputBuffer,sChannel,blocking,timeOut);
			} catch (Exception e) {
				logger.error("An exception occurred while writing a buffer to a channel: {}",e.getMessage());
			}

			if(result!=null && blocking) {
				String resultString = "";
				try {
					resultString = new String(result.array(), charset);
				} catch (UnsupportedEncodingException e) {
					logger.warn("Exception while attempting an unsupported encoding scheme");
				}
				
				logger.info("Received {} from the remote end {}", resultString, sChannel.toString());
				String transformedResponse = transformResponse(provider.getProtocolCommand(itemName, command), resultString);

				// if the remote-end does not send a reply in response to the string we just sent, then the abstract superclass will update
				// the openhab status of the item for us. If it does reply, then an additional update is done via parseBuffer.
				// since this TCP binding does not know about the specific protocol, there might be two state updates (the command, and if
				// the case, the reply from the remote-end)

				if(updateWithResponse) {

					List<Class<? extends State>> stateTypeList = provider.getAcceptedDataTypes(itemName,command);
					State newState = createStateFromString(stateTypeList,transformedResponse);

					if(newState != null) {
						eventPublisher.postUpdate(itemName, newState);						        						
					} else {
						logger.warn("Can not parse transformed input "+transformedResponse+" to match command {} on item {}  ",command,itemName);
					}

					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * Main function to parse ASCII string received 
	 * @return 
	 * 
	 */
	
	private static final int[] mbufintCRCTable ={
		0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7,
		0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
		0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
		0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
		0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485,
		0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
		0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4,
		0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
		0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
		0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
		0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
		0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
		0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
		0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
		0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
		0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
		0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f,
		0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
		0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e,
		0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
		0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
		0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
		0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c,
		0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
		0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab,
		0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
		0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
		0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
		0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9,
		0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
		0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
		0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0
	};
	
	private static final byte[] headBuf = {(byte)0xc0,	(byte)0xa8,	(byte)0x0a,	(byte)0x07,	(byte)0x53,	(byte)0x4d,	(byte)0x41,	(byte)0x52,	(byte)0x54,	(byte)0x43,	(byte)0x4c,	(byte)0x4f,	(byte)0x55,	(byte)0x44,	(byte)0xaa,	(byte)0xaa};
	
	protected void PackCRC(byte[] arrayBuf,short shortLenOfBuf)
	{
		try
		{
	   	    short shortCRC=0;
	   	    byte bytTMP=0;
	   	    short shortIndexOfBuf=0;
	   	    byte byteIndex_Of_CRCTable=0;
			while (shortLenOfBuf!=0) 
			{
				bytTMP= (byte) (shortCRC >> 8) ;    //>>: right move bit                              
				shortCRC=(short) (shortCRC << 8);   //<<: left  move bit   
				byteIndex_Of_CRCTable=(byte) (bytTMP ^ arrayBuf[shortIndexOfBuf]);
				shortCRC=(short) (shortCRC ^ mbufintCRCTable[(byteIndex_Of_CRCTable & 0xFF)]);   //^: xor
				shortIndexOfBuf=(short) (shortIndexOfBuf+1);
			    shortLenOfBuf=(short) (shortLenOfBuf-1);
			};
			
			arrayBuf[shortIndexOfBuf]=(byte) (shortCRC >> 8);
			shortIndexOfBuf=(short) (shortIndexOfBuf+1);
			arrayBuf[shortIndexOfBuf]=(byte) (shortCRC & 0x00FF);
			
				
		}catch(Exception e)
		{
			//Toast.makeText(getApplicationContext(), e.getMessage(),
	  		         // Toast.LENGTH_SHORT).show();	
		}
		
	}
	
	public byte[] PackCommand(int SID, int DID, int Command, byte[] AddArray)
	{
		int i;
		byte[] comBuf = new byte[15];
		comBuf[0] = (byte)0x0f;
		comBuf[1] = (byte)0x03;
		comBuf[2] = (byte)0xfe;
		comBuf[3] = (byte)0xff;
		comBuf[4] = (byte)0xfe;
		comBuf[5] = (byte)(Command/256);
		comBuf[6] = (byte)(Command%256);
		comBuf[7] = (byte)SID;
		comBuf[8] = (byte)DID;
		for(i = 0;i<AddArray.length;i++) 
			comBuf[i+9] = AddArray[i];
		
		PackCRC(comBuf,(short)(comBuf.length-2));
		
		byte[] finalComBuf = new byte[comBuf.length+16];
		for(i = 0; i<(int)finalComBuf.length;i++){
			if(i<16){
				finalComBuf[i] = headBuf[i];
			} else {
				finalComBuf[i] = comBuf[i-16];
			}
		}
		return finalComBuf;
	}
	
	@Override
	protected void parseBuffer(String itemName, Command aCommand, Direction theDirection,ByteBuffer byteBuffer){
		
		String theUpdate = "";
		try {
			theUpdate = new String(byteBuffer.array(), charset);
		} catch (UnsupportedEncodingException e) {
			logger.warn("Exception while attempting an unsupported encoding scheme");
		}
		
		ProtocolBindingProvider provider = findFirstMatchingBindingProvider(itemName);

		List<Class<? extends State>> stateTypeList = provider.getAcceptedDataTypes(itemName,aCommand);

		String transformedResponse = transformResponse(provider.getProtocolCommand(itemName, aCommand),theUpdate);
		State newState = createStateFromString(stateTypeList,transformedResponse);

		if(newState != null) {
			eventPublisher.postUpdate(itemName, newState);							        						
		} else {
			logger.warn("Can not parse input "+theUpdate+" to match command {} on item {}  ",aCommand,itemName);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void updated(Dictionary config) throws ConfigurationException {

		super.updated(config);

		if (config != null) {

			String timeOutString = (String) config.get("buffersize");
			if (StringUtils.isNotBlank(timeOutString)) {
				timeOut = Integer.parseInt((timeOutString));
			} else {
				logger.info("The maximum time out for blocking write operations will be set to the default vaulue of {}",timeOut);
			}

			String blockingString = (String) config.get("retryinterval");
			if (StringUtils.isNotBlank(blockingString)) {
				blocking = Boolean.parseBoolean((blockingString));
			} else {
				logger.info("The blocking nature of read/write operations will be set to the default vaulue of {}",blocking);
			}

			String preambleString = (String) config.get("preamble");
			if (StringUtils.isNotBlank(preambleString)) {
				try {
					preAmble = preambleString.replaceAll("\\\\", "\\");
				}
				catch(Exception e) {
					preAmble = preambleString;
				}
			} else {
				logger.info("The preamble for all write operations will be set to the default vaulue of {}",preAmble);
			}

			String postambleString = (String) config.get("postamble");
			if (StringUtils.isNotBlank(postambleString)) {
				try {
					postAmble = postambleString.replaceAll("\\\\", "\\");
				}
				catch(Exception e) {
					postAmble = postambleString;
				}
			} else {
				logger.info("The postamble for all write operations will be set to the default vaulue of {}",postAmble);
			}
			
			String updatewithresponseString = (String) config.get("updatewithresponse");
			if (StringUtils.isNotBlank(updatewithresponseString)) {
				updateWithResponse = Boolean.parseBoolean((updatewithresponseString));
			} else {
				logger.info("Updating states with returned values will be set to the default vaulue of {}",updateWithResponse);
			}

			String charsetString = (String) config.get("charset");
			if (StringUtils.isNotBlank(charsetString)) {
				charset = charsetString;
			} else {
				logger.info("The characterset will be set to the default vaulue of {}",charset);
			}

		}

	}

	@Override
	protected void configureChannel(DatagramChannel channel) {
	}
	
	/**
	 * Splits a transformation configuration string into its two parts - the
	 * transformation type and the function/pattern to apply.
	 * 
	 * @param transformation the string to split
	 * @return a string array with exactly two entries for the type and the function
	 */
	protected String[] splitTransformationConfig(String transformation) {
		Matcher matcher = EXTRACT_FUNCTION_PATTERN.matcher(transformation);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("given transformation function '" + transformation + "' does not follow the expected pattern '<function>(<pattern>)'");
		}
		matcher.reset();

		matcher.find();			
		String type = matcher.group(1);
		String pattern = matcher.group(2);

		return new String[] { type, pattern };
	}

	protected String transformResponse(String transformation, String response) {
		String transformedResponse;

		try {
			String[] parts = splitTransformationConfig(transformation);
			String transformationType = parts[0];
			String transformationFunction = parts[1];

			TransformationService transformationService = 
					TransformationHelper.getTransformationService(TCPActivator.getContext(), transformationType);
			if (transformationService != null) {
				transformedResponse = transformationService.transform(transformationFunction, response);
			} else {
				transformedResponse = response;
				logger.warn("couldn't transform response because transformationService of type '{}' is unavailable", transformationType);
			}
		}
		catch (Exception te) {
			logger.error("transformation throws exception [transformation="
					+ transformation + ", response=" + response + "]", te);

			// in case of an error we return the response without any
			// transformation
			transformedResponse = response;
		}

		logger.debug("transformed response is '{}'", transformedResponse);

		return transformedResponse;
	}
	


	/**
	 * @{inheritDoc}
	 */
	@Override
	protected String getName() {
		return "UDP Refresh Service";
	}


}	<style type="text/css">
		#mybb_error_content { border: 1px solid #B60101; background: #fff; }
		#mybb_error_content h2 { font-size: 12px; padding: 4px; background: #B60101; color: #fff; margin: 0; }
		#mybb_error_error { padding: 6px; }
		#mybb_error_footer { font-size: 11px; border-top: 1px solid #ccc; padding-top: 10px; }
		#mybb_error_content dt { font-weight: bold; }
	</style>
	<div id="mybb_error_content">
		<h2>MyBB SQL Error</h2>
		<div id="mybb_error_error">
		<p>MyBB has experienced an internal SQL error and cannot continue.</p><dl>
<dt>SQL Error:</dt>
<dd>1054 - Unknown column 'longlastip' in 'field list'</dd>
<dt>Query:</dt>
<dd>UPDATE mybb_users SET lastvisit='1452306804', lastactive='1452309408' , lastip='62.249.146.155', longlastip='1056543387' WHERE uid='1'</dd>
</dl>

			<p id="mybb_error_footer">Please contact the <a href="http://mybb.com">MyBB Group</a> for support.</p>
		</div>
	</div>
