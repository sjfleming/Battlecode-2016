package jene;

import battlecode.common.*;

public class RoboTurret extends RobotPlayer
{
	static Strategy myStrategy = null;

	public static void init() throws GameActionException
	{
		myStrategy = new TurtleTurretStrategy();
	}
	
	public static void turn() throws GameActionException
	{
		myStrategy.tryTurn();
	}
}

