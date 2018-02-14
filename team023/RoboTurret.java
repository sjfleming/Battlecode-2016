package team023;

import battlecode.common.*;

public class RoboTurret extends RobotPlayer
{
	public static void init() throws GameActionException
	{
		myStrategy = new StratTurretCombat();
	}
	
	public static void turn() throws GameActionException
	{
		myStrategy.tryTurn();
	}
}

