package edu.utdallas.robotchess.pathplanning;

import edu.utdallas.robotchess.robotcommunication.commands.*;

import java.util.*;

public class Path
{
    private int robotID;
    private int origin;
    private ArrayList<Integer> path;

    public Path(int robotID, int origin)
    {
        this.robotID = robotID;
        this.origin = origin;
        path = new ArrayList<>();
    }

    protected void add(int location)
    {
        path.add(location);
    }

    public ArrayList<Integer> getPath()
    {
        return path;
    }

    public int getRobotID()
    {
        return robotID;
    }

    public Command generateCommand()
    {
        ArrayList<Integer> payload = new ArrayList<>();

        int previousDirection = path.get(0) - origin;

        for(int i = 1; i < path.size(); i++) {
            int newDirection = path.get(i) - path.get(i-1);
            if (newDirection != previousDirection) {
                payload.add(path.get(i - 1));
                previousDirection = newDirection;
            }
        }

        payload.add(path.get(path.size() - 1));

        int[] payloadArr = new int[payload.size()];
        for (int i = 0; i < payloadArr.length; i++)
            payloadArr[i] = payload.get(i);

        return new MoveToSquareCommand(robotID, payloadArr);
    }
 }