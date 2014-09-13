package chess.communication;

import org.apache.log4j.PropertyConfigurator;

import chess.communication.XBeeAPI.ApiId;
import chess.communication.XBeeApI.AtCommand;
import chess.communication.XBeeApI.AtCommandResponse;
import chess.communication.XBeeApI.PacketListener;
import chess.communication.XBeeApI.XBee;
import chess.communication.XBeeApI.XBeeAddress64;
import chess.communication.XBeeApI.XBeeException;
import chess.communication.XBeeApI.XBeeResponse;
import chess.communication.XBeeApI.wpan.NodeDiscover;
import chess.communication.XBeeApI.zigbee.ZNetRxResponse;
import chess.communication.XBeeApI.zigbee.ZNetTxRequest;
import chess.communication.XBeeApI.util.ByteUtils;

public class CommunicatorAPI
{
    /*----------------------------------------------------------------------------------------------
    Test Program: Write any test programs for this class in the main method.    
    ----------------------------------------------------------------------------------------------*/
    public static void main(String[] args) throws XBeeException, InterruptedException  
    {
        PropertyConfigurator.configure("log4j.properties");
        CommunicatorAPI communicator =  new CommunicatorAPI();
        communicator.FindAllNodes();
        //communicator.GetBotAddresses(nodeAddresses);

        /* Test to print all nodes in network
        for(int i = 1; i < numOfConnectNodes; i++)
            System.out.println(ByteUtils.toBase16(communicator.nodeAddresses[i]));
        */
        
        /* Test sending the ID query message
        int[] temp = new int[] {0x0A,0x00,0x00,0x00,0x00,0x00};
        communicator.SendMessage(temp,nodeAddresses[1],0);
        int[] execute = new int[] {0xff,0x00,0x00,0x00,0x00,0x00};
        communicator.SendMessage(execute,nodeAddresses[1],0);           
        */
          
        communicator.EndCommunication();
    }

    /*----------------------------------------------------------------------------------------------
    Class Member Variables
    ----------------------------------------------------------------------------------------------*/
    static XBee xbee = new XBee();

    private static String comPort;
    private static ZNetRxResponse rxPacket;
    private static int numOfConnectNodes = 0;

    public static int[][] nodeAddresses = new int[33][8];
    public static int[][] BotIDOrderedNodeAddresses = int[32][8]
        
    /*----------------------------------------------------------------------------------------------
    Class Member Methods
    ----------------------------------------------------------------------------------------------*/
    public CommunicatorAPI(String _comport) 
    {
        try 
            xbee.open(_comport, 57600);

        catch (XBeeException e) 
        {
            System.out.println("\n[CommunicatorAPI-Constructor]: Cannot open comport" + _comport);
            e.printStackTrace();
            System.exit(0);
        }

        SetComPort(_comport);
    }

    public void FindAllNodes() throws XBeeException, InterruptedException
    {
        xbee.sendAsynchronous(new AtCommand("NT"));
        AtCommandResponse nodeTimeOut = (AtCommandResponse)xbee.getResponse();
        long nodeDiscoveryTimeOut = ByteUtils.convertMultiByteToInt(nodeTimeOut.getValue())*100;
        xbee.addPacketListener(
                                new PacketListener()
                                {
                                    public void processResponse(XBeeResponse response)
                                    {
                                        if(response.getApiId() == ApiId.AT_RESPONSE)
                                        {
                                            NodeDiscover nd = NodeDiscover.parse((AtCommandResponse)response);
                                            nodeAddresses[numOfConnectNodes] = nd.getNodeAddress64().getAddress();
                                            numOfConnectNodes++;
                                        }
                                    }
                                }
                              );
        xbee.sendAsynchronous(new AtCommand("ND"));
        Thread.sleep(nodeDiscoveryTimeOut);
    }

    public void GetBotAddresses(int[][] _nodeAddresses)
    {
        for(int i = 1; i < numOfConnectNodes; i++)
            BotIDOrderedNodeAddresses[GetBotAddress(nodeAddresses[i])] = nodeAddresses[i];    
    }

    //Depends on ReadMessage()
    /*
    public int GetBotAddress(int[] nodeAddress)
    {
        int[] botIDRequest = new int[] {0x0A};
        SendMessage(botIDRequest, nodeAddress, 0);

        int[] execute = new int[] {0xff};
        SendMessage(execute, nodeAddress, 0);

        boolean recievedMessage = false;
        while(!recievedMessage)
        {
           if(ReadMessage());

        }
        
        return nodeAddresses;
    }*/
    
    //Needs to be rewritten to implement SerialEventHandler
    /*public int[] ReadMessage()
    {
        XBeeResponse response = xbee.getResponse();
        
        if(response.getApiId() == ApiId.ZNET_RX_RESPONSE)
            rxPacket = (ZNetRxResponse) response;

        return rxPacket.getData();
    }*/
    
    public void SendMessage(int[] bytearr, int[] destinationLow, int frameId)
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
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void SetComPort(String _comport)
    {
        comPort = _comport;
    }
    
    public void EndCommunication()
    {
        xbee.close();
    }
}
