package another_one_bots_the_dust;

import battlecode.common.*;

public class RoboViper extends RobotPlayer
{
	public static void init() throws GameActionException
	{
		if (rc.getRoundNum() < 200)
			myStrategy = new StratViperRush();
		else
			myStrategy = new StratUnitCombat();
	}
	
	public static void turn() throws GameActionException
	{
		if (roundsSince(lastDamageRound) == 0)
			Message.sendSignal(rc.getType().sensorRadiusSquared*2);
		
		myStrategy.tryTurn();
	}
}
