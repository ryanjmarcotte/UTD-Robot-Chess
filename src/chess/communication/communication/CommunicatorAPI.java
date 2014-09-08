package chess.communication;

import org.apache.log4j.PropertyConfigurator;

import chess.communication.XBeeAPI.ApiId;
import chess.communication.XBeeAPI.AtCommand;
import chess.communication.XBeeAPI.AtCommandResponse;
import chess.communication.XBeeAPI.PacketListener;
import chess.communication.XBeeAPI.XBee;
import chess.communication.XBeeAPI.XBeeAddress64;
import chess.communication.XBeeAPI.XBeeException;
import chess.communication.XBeeAPI.XBeeResponse;
import chess.communication.XBeeAPI.wpan.NodeDiscover;
import chess.communication.XBeeAPI.zigbee.ZNetRxResponse;
import chess.communication.XBeeAPI.zigbee.ZNetTxRequest;
import chess.communication.XBeeAPI.util.ByteUtils;

public class CommunicatorAPI 
{
	//----------------------------------------------------------------------------------------------------------------------------------------------------
		/*	Test Program: Write any test programs for this class in the main method.	*/
	//----------------------------------------------------------------------------------------------------------------------------------------------------	
		public static void main(String[] args) throws XBeeException, InterruptedException  
		{
			
			PropertyConfigurator.configure("log4j.properties");
			CommunicatorAPI communicator =  new CommunicatorAPI();
			communicator.InitializeCommunication();
			
			//Use the following method to test finding Nodes
				int[][] currentAddress = communicator.FindNodeAddresses(); 
				System.out.println();
				for(int index = 0; index < rowIndex; index++)
				{
					System.out.println("index"+index+":	"+ByteUtils.toBase16(currentAddress[index]));
				}
				System.out.println();
			
			//Use the following to test sending messages.
				//System.out.println(ByteUtils.toBase16(nodeAddresses[2]));
				int[] temp = new int[] {0x0A,0x00,0x00,0x00,0x00,0x00};
				communicator.SendMessage(temp,nodeAddresses[1],0);
				int[] execute = new int[] {0xff,0x00,0x00,0x00,0x00,0x00};
				communicator.SendMessage(execute,nodeAddresses[1],0);
				
			//Use the following method to test whether incoming messages are being read
				communicator.ReadMessage();
			
			communicator.EndCommunication();
		}

	//----------------------------------------------------------------------------------------------------------------------------------------------------	
		/*	Variables	*/
	//----------------------------------------------------------------------------------------------------------------------------------------------------
		static XBee xbee = new XBee();
		
		private static ZNetRxResponse rx;
		
		private static String COM = "COM17";
		
		private static int BAUD_RATE = 57600;
		
		private static int[][] nodeAddresses = new int[32][8];
		
		private static int rowIndex = 0;
		
		public int [] input;
		
	//----------------------------------------------------------------------------------------------------------------------------------------------------	
		/*	Methods		*/
	//----------------------------------------------------------------------------------------------------------------------------------------------------			
		public void InitializeCommunication() 
		{	
			try 
			{
				xbee.open(COM, BAUD_RATE);
			} 
			catch (XBeeException e) 
			{
				System.out.println("\n[CommunicatorAPI-InitializeCommunication] problem opening COM ports.");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		public void ReadMessage()
		{
			try 
			{
				XBeeResponse response = xbee.getResponse();
				
				if(response.getApiId() == ApiId.ZNET_RX_RESPONSE)
				{
					rx = (ZNetRxResponse) response;
					input = rx.getData();
					
					PrintResponseDetails();	//This is for testing purposes
				
					PrintResponse(input);
					
					/*[Optional] Prints the signal strength of the last hop.
								 If routers are in your network, this signal
								 will be of the last hop.
					*/	
						//GetRSSI();		
				}
				else
					System.out.println("\n received unexpected packet "+response.toString()+"\n");
			} 
			catch (XBeeException e) 
			{
				System.out.println("\n[CommunicatorAPI-ReadMessage] Problem accessing recieved data.\n");
				e.printStackTrace();
			}
		}
		
		public void SendMessage(int[] bytearr, int[] destinationLow,int frameId)
		{
			int[] information = bytearr;
			XBeeAddress64 destination = new XBeeAddress64(destinationLow);
			ZNetTxRequest request = new ZNetTxRequest(destination,information);
			request.setOption(ZNetTxRequest.Option.UNICAST);
			request.setFrameId(frameId);
			try 
			{
				xbee.sendAsynchronous(request);
			} 
			catch (XBeeException e) 
			{
				System.out.println("\n[CommunincatorAPI-SendMessage] problem sending message.\n");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		private static void PrintResponse(int[] message)
		{
			char[] output = new char[8];
			for(int index = 0; index < message.length; index++)
				output[index] = (char)message[index];
			System.out.println("\n RFData: "+ new String(output)+"\n");
		}
		
		private static void PrintResponseDetails()
		{
			System.out.println("\n Received RX packet, under option: " + rx.getOption());
			System.out.println(" Sender 64-bit address is: " + ByteUtils.toBase16(rx.getRemoteAddress64().getAddress()));
			System.out.println(" Remote 16-bit address is: " + ByteUtils.toBase16(rx.getRemoteAddress16().getAddress()));
			System.out.println(" RFData in hexadecimal is: " + ByteUtils.toBase16(rx.getData()));
		}
		
		@SuppressWarnings("unused")
		private static void GetRSSI()
		{
			
			try 
			{
				AtCommand at = new AtCommand("DB");
				xbee.sendAsynchronous(at);
			} 
			catch (XBeeException e) 
			{
				System.out.println("\n[CommunicatorAPI-GetRSSI] Problem sending message.\n");
				e.printStackTrace();
				System.exit(0);
			}
			try 
			{
				XBeeResponse atResponse = xbee.getResponse();
				if (atResponse.getApiId() == ApiId.AT_RESPONSE)
				{
					// Note: RSSI is a negative db value.
					System.out.println("\n RSSI of last response is " + -((AtCommandResponse)atResponse).getValue()[0]+"\n");
				}
				else
					System.out.println("expected RSSI, but received " + atResponse.toString()+"\n");
			} 
			catch (XBeeException e) 
			{
				System.out.println("\n[CommunicatorAPI-GetRSSI] Problem accessing recieved data.\n");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		public static void SetUpCommunication(String com, int baud_rate)
		{
			COM = com;
			BAUD_RATE = baud_rate;
		}
		
		public int[][] FindNodeAddresses() throws XBeeException,InterruptedException
		{
			xbee.sendAsynchronous(new AtCommand("NT"));
			AtCommandResponse nodeTimeOut =(AtCommandResponse)xbee.getResponse();
			long nodeDiscoveryTimeOut = ByteUtils.convertMultiByteToInt(nodeTimeOut.getValue())*100;
			xbee.addPacketListener(
									new PacketListener()
									{
										public void processResponse(XBeeResponse response)
										{
											if(response.getApiId() == ApiId.AT_RESPONSE)
											{
												NodeDiscover nd = NodeDiscover.parse((AtCommandResponse)response);
												nodeAddresses[rowIndex] = nd.getNodeAddress64().getAddress();
												rowIndex++;
											}
										}
									}
								  );
			xbee.sendAsynchronous(new AtCommand("ND"));
			Thread.sleep(nodeDiscoveryTimeOut);
			
			return nodeAddresses;
		}
		
		public void EndCommunication()
		{
			xbee.close();
		}
}
