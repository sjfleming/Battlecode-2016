package neutered_blitzkrieg_bot;

import battlecode.common.*;

public class RoboViper extends RobotPlayer
{
	public static void init() throws GameActionException
	{
		if (rc.getRoundNum() < 200 || rand.nextInt(6) == 0)
			myStrategy = new StratViperRush();
		else
			myStrategy = new StratUnitCombat();
	}
	
	public static void turn() throws GameActionException
	{
		if (roundsSince(lastDamageRound) == 0)
			Message.sendSignal(rc.getType().sensorRadiusSquared*2);
		
		myStrategy.tryTurn();
		
		// did we fire? try to send a short ping
		if (roundsSince(lastFiredRound) == 0)
			Message.trySendSignal(rc.getType().sensorRadiusSquared*2);
	}
}
