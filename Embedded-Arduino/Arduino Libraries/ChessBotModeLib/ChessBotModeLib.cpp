/*
 ChessBotModeLib.cpp
 
 This file contains definitions of methods within ChessBotModeLib.h. Ensure that this and the .h file are both
 within the same directory under the '~/Arduino/libraries' directory.
*/

#include <ChessBotModeLib.h>

ChessBot::ChessBot(){
        
    errorFlag = 0; 

    needToCenter = false; 

    _LeftEncoderTicks = 0;    
    _RightEncoderTicks = 0; 

    velocityState=0;                            
    integralOffsetR=0, integralOffsetL=0;     
    derivativeOffsetR=0, derivativeOffsetL=0; 

    prevAngle = 0;              
    angleState=0;                
    my3IMU = FreeSixIMU(); 
}

/*
 void Center()
 
 Description:           Checks if ChessBot is in black or white square and does the corresponding centering routine. Flags an 
                        error if asked to center while not in either a black or white square. 
 
 Methods Called by:     None
 
 Methods Called:        RotateBaseTo
                        alignWithEdgeBlack
                        alignWithEdgeWhite
                        CheckSquareState
 
 Global Vars effected:  errorFlag
 */
void ChessBot::Center() {
    
    CheckSquareState();
    if(squareState == "f")
    {
        RotateBaseTo(90); 
        alignWithEdgeBlack();
        RotateBaseTo(-90);
        alignWithEdgeBlack();
    }
    else if(squareState == "0")
    {
        RotateBaseTo(90);  
        alignWithEdgeWhite();
        RotateBaseTo(-90);
        alignWithEdgeWhite(); 
    }
    else
        errorFlag = 1;
    
    
}

/*
 void CrossSquares()
 
 Description:           This method is used for crossing a certain number of squares in either a diagonal or straight fashion. It first checks
                        to see whether the ChessBot is completely within a black or white square and flags an error if it is not. If there is no
                        error, the method saves the color of the starting square and calls AccelTo. Once AccelTo is finished, the method looks for
                        crossings until the numOfCrossings reaches the desired numOfSquares to be crossed. Within this loop, the squareState is continually
                        checked and a few things can happen based on the squareState of the ChessBot as it is moving. The method checks to see if at least 
                        one of the photodiodes crosses into the next square first. It then adjusts the heading of the ChessBot to try to make both of the front 
                        photodiodes crossover at the same time. Once both front photodiodes are crossed over, this procedure it repeated for the back photodiodes.
                        Once they have crossed over, that means a square has been completely crossed and numOfCrossings can be incremented. Notice that 
                        case 1 is for the ChessBot moving straight across and case 2 is for the ChessBot moving diagonally across. 
 
 Methods Called by:     None
 
 Methods Called:        CheckSquareState()
                        AccelTo()
                        SetWheelVelocitiesStraight()
                        
 Global Vars effected:  None
 */
void ChessBot::CrossSquares(int numOfSquares){
    byte lookForCrossingSwitch = 0;
    String startingSquare;
    int numOfCrossings = 0;
    int crossingSpeed = 150;
    float adjustAngle = 0;
    
    CheckSquareState();
    if((squareState == "f") || (squareState == "0"))
    {
        startingSquare = squareState;
        AccelTo(crossingSpeed);
        
        if(abs(angleState) == 45 || abs(angleState) == 135 || abs(angleState) == 225 || abs(angleState) == 315)
        {
            while(numOfCrossings < numOfSquares)
            {
                
                CheckSquareState();
                switch (lookForCrossingSwitch)
                {
                    case 0:
                        if((squareState == "e") || (squareState == "1"))
                        {
                            adjustAngle += 1.5f;
                            break;
                        }
                        if((squareState == "d") || (squareState == "2"))
                        {
                            adjustAngle += -1.5f;
                            break;
                        }
                        
                        if((squareState == "c") || (squareState == "3"))
                            lookForCrossingSwitch = 1;
                        break;
                    case 1:
                        if((squareState == "b") || (squareState == "4"))
                        {
                            adjustAngle += -1.5f;
                            break;
                        }
                        if((squareState == "7") || (squareState == "8"))
                        {
                            adjustAngle += 1.5f;
                            break;
                        }
                        if(startingSquare == squareState)
                        {
                            numOfCrossings++;
                            lookForCrossingSwitch = 0;
                        }
                        break; 
                }
                
                SetWheelVelocitiesStraight(adjustAngle);
                delay(10);
                
            }
        }
        else
        {
            while(numOfCrossings < numOfSquares)
            {
                
                CheckSquareState();
                switch (lookForCrossingSwitch)
                {
                    case 0:
                        if((squareState == "e") || (squareState == "1"))
                        {
                            adjustAngle += -1.5f;
                            break;
                        }
                        if((squareState == "d") || (squareState == "2"))
                        {
                            adjustAngle += 1.5f;
                            break;
                        }
                        
                        if((squareState == "c") || (squareState == "3"))
                            lookForCrossingSwitch = 1;
                        break;
                    case 1:
                        if((squareState == "b") || (squareState == "4"))
                        {
                            adjustAngle += -1.5f;
                            break;
                        }
                        if((squareState == "7") || (squareState == "8"))
                        {
                            adjustAngle += 1.5f;
                            break;
                        }
                        if((startingSquare == "f" && squareState == "0") || (startingSquare == "0" && squareState == "f"))
                        {
                            numOfCrossings++;
                            lookForCrossingSwitch = 0;
                            startingSquare = squareState;
                        }
                        break;
                }
                
                SetWheelVelocitiesStraight(adjustAngle);
                delay(10);
                
            }
        }
    }
    else
        errorFlag = 1;
    
    delay(100);
    HardStop();
    
    if(abs(adjustAngle > 10))
        needToCenter = true;
}

/*
 void RotateBaseTo()
 
 Description:           This method is used to rotate the base of the ChessBot about its center. To do this, it first reinitializes the gyro until 
                        it initializes around 0 degrees. Then, it continues to rotate the ChessBot using SetWheelVelocitiesStraight() until the heading of 
                        the ChessBot is close enough to endAngle. At this point, it counts the number of milliseconds until fineTuneEndTime 
                        and then stops the motors. Finally, it updates the angleState by the amount of the endAngle.
 
 Methods Called by:     Center()
 
 Methods Called:        SetWheekVelocities()
                        HardStop()
 
 Global Vars effected:  velocityState
                        integralOffsetR
                        integralOffsetL
                        prevAngle
                        angleState 
 */
void ChessBot::RotateBaseTo(float endAngle){
    float fineTuneBeginTime, fineTuneElapsedTime = 0, fineTuneEndTime = 500;
    byte fineTuneTimingSwitch = 0; 
    
    integralOffsetR = 0;
    integralOffsetL = 0;
    velocityState = 0;
    prevAngle = 0;
    
    do
    {
        my3IMU.reinit();
        my3IMU.getEuler(currentAngles);
    } while(abs(currentAngles[0]) > 1);
    
    
    while(fineTuneElapsedTime<fineTuneEndTime)
    {
        
        SetWheelVelocitiesTurn(endAngle);
        delay(10);
        
        if(abs(currentAngles[0]-endAngle)<1)
        {
            integralOffsetR = 0;
            integralOffsetL = 0;
            switch(fineTuneTimingSwitch)
            {
                case 0:
                    fineTuneBeginTime = millis();
                    fineTuneTimingSwitch = 1;
                    break;
                case 1:
                    fineTuneElapsedTime = millis() - fineTuneBeginTime;
                    break;
            }
        }

    }
    
    HardStop();
    angleState += endAngle;
    if(abs(angleState) == 360)
        angleState = 0;
}

/*
 void CheckSquareState()
 
 Description:           Reads values of each of the photodiodes and stores in corresponding variables. Stores whether each of the 
                        photodiode values is larger than 800 (1 if true, 0 if false). Converts the squareStateArray, which
                        represents a 4 digit binary number, into a 1 digit hex number.
 
 Methods Called by:     Center()
                        CrossSquares()
                        alignWithEdgeBlack()
                        centerAgsinstEdgeWhite()
 
 Methods Called:        None
 
 Global Vars effected:  squareState
                        squareStateArray
                        backRightLight
                        backLeftLight
                        frontLeftLight
                        frontRightLight
 */
void ChessBot::CheckSquareState(){
    backRightLight = analogRead(backRightLightPin);
    backLeftLight = analogRead(backLeftLightPin);
    frontLeftLight = analogRead(frontLeftLightPin);
    frontRightLight = analogRead(frontRightLightPin);
    
    squareStateArray[0] = (backRightLight > 800);
    squareStateArray[1] = (backLeftLight > 800);
    squareStateArray[2] = (frontLeftLight > 800);
    squareStateArray[3] = (frontRightLight > 800);
    
    squareState = String(squareStateArray[0]*8 + squareStateArray[1]*4 + squareStateArray[2]*2 + squareStateArray[3],HEX);
}

/*
 void AccelTo()
 
 Description:           Determines whether to deccelerate or accelerate depending on calculation that determines accelBy, which 
                        can be +1 or -1. Sets integral offsets and prevAngle to 0 so that offsets from previous movements do not
                        affect this one. Checks if desired speed is greater than 70 and sets the velocityState to 70, or checks if
                        it is less than -70 and sets the velocityState to -70 (this is because the motors usually don't move until
                        around an input of 80 or -80). Resets the gyroscope until the initialized gyroscope angle is close to 0. 
                        Increments velocityState and calls SetWheelVelocitiesStraight until the velocityState reaches the desired endspeed. 
                        
 
 Methods Called by:     CrossSquares()
                        alignWithEdgeBlack()
                        centerAgsinstEdgeWhite()
 
 Methods Called:        None
 
 Global Vars effected:  velocityState
                        integralOffsetR
                        integralOffsetL
                        prevAngle
 */
void ChessBot::AccelTo(int endspeed){
    int accelBy = (endspeed - velocityState)/abs(endspeed - velocityState);
    integralOffsetR = 0;
    integralOffsetL = 0;
    prevAngle = 0;
    
    
    if(endspeed < -70 && velocityState > -70)
        velocityState = -70;
    
    if(endspeed > 70 && velocityState < 70)
        velocityState = 70;
    
    
    do
    {
        my3IMU.reinit();
        my3IMU.getEuler(currentAngles);
    } while(abs(currentAngles[0]) > 1);
    
    
    
    while(endspeed != velocityState)
    {
        velocityState += accelBy; 
        SetWheelVelocitiesStraight(0);
        delay(10);
    }
}

/*
 void HardStop()
 
 Description:           Checks the current velocity values for each of the wheels and writes to the motors the opposite values for 20
                        milliseconds followed by writing 0 to each of the motors.
 
 Methods Called by:     CrossSquares()
                        RotateBaseTo()
                        alignWithEdgeBlack()
                        alignWithEdgeWhite()
 
 Methods Called:        RotateWheels()
 
 Global Vars effected:  velocityState
 */
void ChessBot::HardStop(){
    
    RotateWheels(-1*currentVelocityL,-1*currentVelocityR);
    delay(20);
    RotateWheels(0, 0);
    velocityState = 0;
}

/*
 void SetWheelVelocitiesStraight()
 
 Description:           This method is used to calculate the velocity of each wheel so that a desired heading is achieved. It is basically
                        the implementation of a PID controller, with the motors being the actuators and the  heading angle being the variable
                        to be controlled. There is a special case when the desired heading is 180, in which case the control variable is disconinuous,
                        wrapping back around to -180. After calculating what the velocities should be, it calls the RotateWheels method. 
 
 Methods Called by:     CrossSquares()
                        AccelTo()
                        alignWithEdgeBlack()
                        alignWithEdgeWhite()
                        
 Methods Called:        RotateWheels()
 
 Global Vars effected:  currentVelocityL
                        currentVelocityR
 */
void ChessBot::SetWheelVelocitiesStraight(float endAngle){
    int setVelocityL, setVelocityR;
    int minOffsetL, minOffsetR;
    my3IMU.getEuler(currentAngles); 
    
    integralOffsetR += (endAngle - currentAngles[0])/25;
    integralOffsetL += (endAngle - currentAngles[0])/-25;
    
    proportionalOffSetR = 6*(endAngle - currentAngles[0]);
    proportionalOffSetL = -6*(endAngle - currentAngles[0]);
    
    derivativeOffsetR = -80*(currentAngles[0] - prevAngle);
    derivativeOffsetL = 80*(currentAngles[0] - prevAngle);
    
    prevAngle = currentAngles[0];
    
    setVelocityR = velocityState + integralOffsetR + proportionalOffSetR + derivativeOffsetR; 
    setVelocityL = velocityState + integralOffsetL + proportionalOffSetL + derivativeOffsetL;
    
    RotateWheels(setVelocityL, setVelocityR);
    
        
} 

/*
 void SetWheelVelocitiesTurn()
 
 Description:           This method is used to calculate the velocity of each wheel so that a desired heading is achieved. It is basically
                        the implementation of a PID controller, with the motors being the actuators and the  heading angle being the variable
                        to be controlled. There is a special case when the desired heading is 180, in which case the control variable is disconinuous,
                        wrapping back around to -180. After calculating what the velocities should be, it calls the RotateWheels method. 
 
 Methods Called by:     RotateBaseTo()
 
 Methods Called:        RotateWheels()
 
 Global Vars effected:  currentVelocityL
                        currentVelocityR
 */
void ChessBot::SetWheelVelocitiesTurn(float endAngle){
    int setVelocityL, setVelocityR;
    int minOffsetL, minOffsetR;
    my3IMU.getEuler(currentAngles); 
    
    integralOffsetR += (endAngle - currentAngles[0])/50;
    integralOffsetL += (endAngle - currentAngles[0])/-50;
    
    proportionalOffSetR = 2*(endAngle - currentAngles[0]);
    proportionalOffSetL = -2*(endAngle - currentAngles[0]);
    
    if(velocityState == 0)
    {
        minOffsetR = 60*(endAngle - currentAngles[0])/abs(endAngle - currentAngles[0]);
        minOffsetL = -60*(endAngle - currentAngles[0])/abs(endAngle - currentAngles[0]);
    }
    else
    {
        minOffsetR = 0;
        minOffsetL = 0;
    }
    
    derivativeOffsetR = -200*(currentAngles[0] - prevAngle);
    derivativeOffsetL = 200*(currentAngles[0] - prevAngle);
    
    prevAngle = currentAngles[0];
    
    setVelocityR = velocityState + minOffsetR  + integralOffsetR + proportionalOffSetR + derivativeOffsetR; 
    setVelocityL = velocityState + minOffsetL + integralOffsetL + proportionalOffSetL + derivativeOffsetL;
    
    RotateWheels(setVelocityL, setVelocityR);
    
    
} 

/*
 void RotateWheels()
 
 Description:           Takes input wheel speeds and ensures that they do not exceed abs(255). Then writes the corrresponding
                        values to the motor pins. Finally, sets the global variables for the wheel velocities to the input values. 
 
 Methods Called by:     RotateBaseTo()
                        CrossSquares()
                        AccelTo()
                        HardStop()
                        
 Methods Called:        None
 
 Global Vars effected:  currentVelocityL
                        currentVelocityR
 */
void ChessBot::RotateWheels(int angularSpeedL, int angularSpeedR){
    
    if(angularSpeedL > 255)
        angularSpeedL = 255;
    else if(angularSpeedL < -255)
        angularSpeedL = -255;
    
    if(angularSpeedR > 255)
        angularSpeedR = 255;
    else if(angularSpeedR < -255)
        angularSpeedR = -255;
    
    if(angularSpeedR >= 0) //forward
    {
        analogWrite(motor2Pin, angularSpeedR); 
        digitalWrite(motor1Pin, LOW);
    }
    else                   //reverse
    {
        analogWrite(motor1Pin, abs(angularSpeedR));
        digitalWrite(motor2Pin, LOW);
    }
    
    
    if(angularSpeedL >= 0) //forward
    {
        analogWrite(motor4Pin, angularSpeedL);
        digitalWrite(motor3Pin, LOW);
    }
    else                   //reverse
    {
        analogWrite(motor3Pin, abs(angularSpeedL));
        digitalWrite(motor4Pin, LOW);
    }
    
    currentVelocityL = angularSpeedL;
    currentVelocityR = angularSpeedR;
    
}

/*
 void Setup()
 
 Description:           Sets encoder pins as inputs and turns on pullup resistors to allow for normal function of the encoder 
                        circuits. Begins Serial communication at 9600 BAUD. Begins I2C communication and initializes the gyroscope. 
                        Assigns motorpins as outputs. This method should only be called once in the setup() method of an arduino 
                        sketch and should not be used anywhere else. 
 
 Methods Called by:     None
 Methods Called:        HandleRightMotorInterruptA
                        HandleLeftMotorInterruptA
 
 Global Vars effected:  None
*/
void ChessBot::Setup(){
    
    pinMode(c_LeftEncoderPinA, INPUT);       
    digitalWrite(c_LeftEncoderPinA, LOW);  // turn on pullup resistors 
    pinMode(c_LeftEncoderPinB, INPUT);
    digitalWrite(c_LeftEncoderPinB, LOW);  // turn on pullup resistors
    
    pinMode(c_RightEncoderPinA, INPUT); 
    digitalWrite(c_RightEncoderPinA, LOW);  // turn on pullup resistors 
    pinMode(c_RightEncoderPinB, INPUT);
    digitalWrite(c_RightEncoderPinB, LOW);  // turn on pullup resistors 
    
    Serial.begin(9600); 
    Wire.begin();
    delay(5);
    my3IMU.init();
    delay(5);
    
    pinMode(motor1Pin, OUTPUT); 
    pinMode(motor2Pin, OUTPUT);
    pinMode(motor3Pin, OUTPUT); 
    pinMode(motor4Pin, OUTPUT);    
    
    pinMode(12, OUTPUT);
    digitalWrite(12, HIGH); 
}

/*
 void alignWithEdgeBlack()
 
 Description:           This method is used for aligning the front two photodiodes of the ChessBot with the edge of a black square. To do this,
                        it first accelerates to a low speed, and continues moving while completely within the black square. It calls HardStop()
                        as soon as one of the photodiodes is over a white square. It then continually moves either the right wheel or left wheel
                        (depending on whichever one needs to move to align the front photodiodes with the edge) until both front photodiodes read
                        white. It then stops, and slowly backs up until 1400 ticks are counted for both wheels, which should get the ChessBot to about
                        the center of the black square.
 
 Methods Called by:     Center()
 
 Methods Called:        CheckSquareState()
                        RotateWheels()
                        HardStop()
                        SetWheelVelocitiesStraight()
 
 Global Vars effected:  _LeftEncoderTicks
                        _RightEncoderTicks
 */
void ChessBot::alignWithEdgeBlack(){
    
    AccelTo(70);
    
    while(squareState == "f")
    {
        CheckSquareState();
        SetWheelVelocitiesStraight(0);
    }
    
    HardStop();
    
    while(squareState != "c")
    {
        CheckSquareState();
        if(squareState == "d")
            RotateWheels(0, 90);
        else if(squareState == "e")
            RotateWheels(90, 0);
    }
    
    HardStop();
    _LeftEncoderTicks = 0;
    _RightEncoderTicks = 0;
    AccelTo(-70);
    
    while((abs(_LeftEncoderTicks) < 1400) && (abs(_RightEncoderTicks) < 1400))
        SetWheelVelocitiesStraight(0);
    
    HardStop();
    CheckSquareState();
}

/*
 void alignWithEdgeWhite()
 
 Description:           This method is used for aligning the front two photodiodes of the ChessBot with the edge of a white square. To do this,
                        it first accelerates to a low speed, and continues moving while completely within the white square. It calls HardStop()
                        as soon as one of the photodiodes is over a black square. It then continually moves either the right wheel or left wheel
                        (depending on whichever one needs to move to align the front photodiodes with the edge) until both front photodiodes read
                        black. It then stops, and slowly backs up until 1400 ticks are counted for both wheels, which should get the ChessBot to about
                        the center of the white square.
 
 Methods Called by:     Center()
 
 Methods Called:        CheckSquareState()
                        RotateWheels()
                        HardStop()
                        SetWheelVelocitiesStraight()
 
 Global Vars effected:  _LeftEncoderTicks
                        _RightEncoderTicks
 */
void ChessBot::alignWithEdgeWhite(){
    
    AccelTo(70);
    
    while(squareState == "0")
    {
        CheckSquareState();
        SetWheelVelocitiesStraight(0);
    }
    
    HardStop();
    
    while(squareState != "3")
    {
        CheckSquareState();
        if(squareState == "2")
            RotateWheels(0, 90);
        else if(squareState == "1")
            RotateWheels(90, 0);
    }
    
    HardStop();
    _LeftEncoderTicks = 0;
    _RightEncoderTicks = 0;
    AccelTo(-70);
    
    while((abs(_LeftEncoderTicks) < 1400) && (abs(_RightEncoderTicks) < 1400))
        SetWheelVelocitiesStraight(0);
    
    HardStop();
    CheckSquareState();
}

/*
 void HandleLeftMotorInterruptA()
 
 Description:           Called when interrupt pin defined in Setup() is rising. Checks to see if wheel is going forwards or
                        backwards based on whether c_leftEncoderPinB is high or low when interrupt is called. Then increments
                        encoder ticks if there is a forward movement, or decrements if there is a backwards movement.  
 
 
 Methods Called by:     Setup()
 Methods Called:        None
 
 Global Vars effected:  _LeftEncoderTicks
                        _LeftEncoderBSet
 */
void ChessBot::HandleLeftMotorInterruptA(){
    // Test transition; since the interrupt will only fire on 'rising' we don't need to read pin A 
    _LeftEncoderBSet = digitalReadFast(c_LeftEncoderPinB);   // read the input pin 
    
    // and adjust counter + if A leads B 
#ifdef LeftEncoderIsReversed 
    _LeftEncoderTicks -= _LeftEncoderBSet ? -1 : +1; 
#else 
    _LeftEncoderTicks += _LeftEncoderBSet ? -1 : +1; 
#endif 
}

/*
 void HandleRightMotorInterruptA()
 
 Description:           Called when interrupt pin defined in Setup() is rising. Checks to see if wheel is going forwards or
                        backwards based on whether c_rightEncoderPinB is high or low when interrupt is called. Then increments
                        encoder ticks if there is a forward movement, or decrements if there is a backwards movement.  
 
 
 Methods Called by:     Setup()
 Methods Called:        None
 
 Global Vars effected:  _RightEncoderTicks
                        _RightEncoderBSet
 */
void ChessBot::HandleRightMotorInterruptA(){
    // Test transition; since the interrupt will only fire on 'rising' we don't need to read pin A 
    _RightEncoderBSet = digitalReadFast(c_RightEncoderPinB);   // read the input pin 
    
    // and adjust counter + if A leads B 
#ifdef RightEncoderIsReversed 
    _RightEncoderTicks -= _RightEncoderBSet ? -1 : +1; 
#else 
    _RightEncoderTicks += _RightEncoderBSet ? -1 : +1; 
#endif
}

