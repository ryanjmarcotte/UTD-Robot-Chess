package edu.utdallas.robotchess.robotcommunication.commands;

import com.rapplogic.xbee.api.XBeeAddress64;

public class ReadBotIDCommand extends Command
{

    public ReadBotIDCommand(int robotID)
    {
        commandID = 0x2;
        payloadLength = 0x1;
        this.robotID = robotID;
    }

    public ReadBotIDCommand(XBeeAddress64 xbeeAddress)
    {
        commandID = 0x2;
        payloadLength = 0x1;
        this.xbeeAddress = xbeeAddress;
    }

    @Override
    public int[] generatePayload()
    {
        int payload[] = new int[payloadLength];

        payload[0] =  commandID;

        return payload;
    }

    @Override
    public String toString()
    {
        return String.format("Read Bot ID Command: (Robot ID %d) (Command ID %d)",
                robotID, commandID);
    }
}
