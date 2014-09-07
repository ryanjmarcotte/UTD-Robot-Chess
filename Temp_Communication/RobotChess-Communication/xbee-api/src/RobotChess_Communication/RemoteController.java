package RobotChess_Communication;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

	public class RemoteController 
	{
	//----------------------------------------------------------------------------------------------------------------------------------------------------
		/*	Test Program: Write any test programs for this class in the main method.	*/
	//----------------------------------------------------------------------------------------------------------------------------------------------------	
		public static void main(String[] arguments)
		{
			RemoteController.InitializeControllers();
			PrintAvailableControllers();
			//SetControllerToSystem(0);
			PrintControllerMap();
			//DisplayXYValues();
			SendAPIMoveCommands(0x01);
			
		}

	//----------------------------------------------------------------------------------------------------------------------------------------------------	
		/*	Variables	*/
	//----------------------------------------------------------------------------------------------------------------------------------------------------
		public static Controller controller;
		
		private static int direction=0x00, rotation=0x00, directionMagnitude=0x00, rotationMagnitude=0x00;
		
		private static int[][]destinationLow = new int[32][4];
		
	//----------------------------------------------------------------------------------------------------------------------------------------------------	
		/*	Methods		*/
	//----------------------------------------------------------------------------------------------------------------------------------------------------
		public static void InitializeControllers()
		{
			try 
			{
				Controllers.create();
			} 
			catch (LWJGLException e) 
			{
				System.out.println("\n [RemoteController] Problem initializing controllers.\n");
				e.printStackTrace();
				System.exit(0);
			}
			Controllers.poll();
			if(Controllers.getControllerCount() != 0)
				controller = Controllers.getController(0);
			else
			{
				System.out.println("\n Could not Find any controllers.");
				System.exit(0);
			}
				
		}
	
		public static void SendAPIMoveCommands(int botId)
		{
			boolean run = false, A_ButtonIsPressed = false;
			int botAddressIndex = 0;
			CommunicatorAPI xbee = new CommunicatorAPI();
			xbee.InitializeCommunication();
				long startTime = System.currentTimeMillis();
				while(!controller.isButtonPressed(6))
				{
					controller.poll();
					ZeroJoyStick();
					if(controller.isButtonPressed(7))
						run = true;

					while(System.currentTimeMillis()-startTime > 100 && run == true)
					{
						controller.poll();
						if(controller.isButtonPressed(3))
							run = false;
						
						ComputeWheelVelocities();
						
						if(controller.isButtonPressed(0))
							A_ButtonIsPressed = true;
						else if(A_ButtonIsPressed == true && !controller.isButtonPressed(0))
						{
								int[]temp = {0x00, 0x00, 0x00, 0x00};
								xbee.SendMessage(temp,destinationLow[botAddressIndex],0);
								botAddressIndex++;
								botAddressIndex %= 32;
								A_ButtonIsPressed = false;
						}
						
						int[]temp = {direction, directionMagnitude, rotation, rotationMagnitude};
						/*if (botId < 16)
							xbee.SendMessage(temp,destinationLow[botAddressIndex],0);
						else
							xbee.SendMessage(temp, destinationLow[botAddressIndex+16],0);*/
					//the following line is temporary. It will be removed in the future.
						xbee.SendMessage(temp,destinationLow[botAddressIndex],0);
						System.out.println((float)directionMagnitude+"	"+(float)rotationMagnitude);
						startTime = System.currentTimeMillis();
						ZeroJoyStick();
					}
				}
			xbee.EndCommunication();
		}
		
		@Deprecated
		public static void SendATMoveCommands(int botId)
		{
			boolean run = false;
			CommunicatorAT xbee = new CommunicatorAT();
			xbee.InitializeCommunication();		
				long startTime = System.currentTimeMillis();
				while(!controller.isButtonPressed(6))
				{
					controller.poll();
					ZeroJoyStick();
					if(controller.isButtonPressed(7))
					{
						run = true;
					}
					if(System.currentTimeMillis()-startTime > 100 && run == true)
					{
						controller.poll();
						if(controller.isButtonPressed(3))
						{
							run = false;
						}
						
						ComputeWheelVelocities();
						
						System.out.println((float)directionMagnitude+"	"+(float)rotationMagnitude);
						char[]temp = {(char)botId, (char)direction, (char)directionMagnitude, (char)rotation, (char)rotationMagnitude, 0x00, 0x00, 0x75};
						xbee.SendMessage(temp);	
						startTime = System.currentTimeMillis();
						ZeroJoyStick();
					}	
				}
				char[]temp = {(char)botId, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0x75};
				xbee.SendMessage(temp);
			xbee.EndCommunication();
		}
		
		private static void ComputeWheelVelocities()
		{
			if(controller.getAxisValue(0) < 0) //forward
				direction = 0x01; 
			else							   //backward
				direction = 0x00;
			if(controller.getAxisValue(3) < 0) //left
				rotation = 0x01; 
			else 							   //right
				rotation = 0x00;
			
			directionMagnitude = (char)Math.abs(controller.getAxisValue(0)*255);
			rotationMagnitude = (char)Math.abs(controller.getAxisValue(3)*255);
			
			if(controller.isButtonPressed(4))
				directionMagnitude *= .5;
			if(controller.isButtonPressed(5))
				rotationMagnitude *= .5;
		}
		
		public static void PrintAvailableControllers()
		{	
			System.out.println("\n Note: controller is automatically set 0 index, unless a new one is chosen.");
			for(int index = 0;index < Controllers.getControllerCount();index++)
			{
				controller = Controllers.getController(index);
				System.out.println("\n Controller found at index_"+index+": "+ controller.getName()+"\n");
			}
		}
		
		public static void SetControllerToSystem(int index)
		{
			controller = Controllers.getController(index);
			ZeroJoyStick();
		}
		
		public static void SetDestinationLowAddresses(int[][] addressLowList)
		{
			destinationLow = addressLowList;
		}
		
		public static void PrintControllerMap()
		{
			String[] controllerButtonNames = {"A_Button","B_Button","X_Button","Y_Button","Left_Bumper","Right_Bumper","Back_Button",
											  "Start_Button","Lower_Joystick(Push Down)","Upper_Joystick(Push Down)"};
			
			for(int index = 0;index < controller.getAxisCount();index++)
			{
				if(index==2 || index ==3)
					System.out.printf("%10s %14s","	"+controller.getAxisName(index),"at index_"+index+"\n");	
				else
					System.out.printf("%10s %18s","	"+controller.getAxisName(index),"at index_"+index+"\n");
			}
			System.out.println();
			for(int index = 0;index < 9;index++)
				System.out.printf("%10s %15s %s","	"+controller.getButtonName(index),"at index_"+index,"  "+controllerButtonNames[index]+"\n");
		}
		
		private static void ZeroJoyStick()
		{
			while(controller.getAxisValue(0) != 0.0 && controller.getAxisValue(1) != 0.0 && controller.getAxisValue(2) != 0.0 && controller.getAxisValue(3) != 0.0)
			{
				controller.poll();
				for(int index = 0;index < controller.getAxisCount();index++)
					controller.setDeadZone(index,(float)0.3);
			}
		}
	}