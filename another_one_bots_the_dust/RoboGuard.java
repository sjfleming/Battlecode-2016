package another_one_bots_the_dust;

import battlecode.common.*;

public class RoboGuard extends RobotPlayer
{
	public static void init() throws GameActionException
	{
		myStrategy = new StratUnitRush();
	}
	
	public static void turn() throws GameActionException
	{
		if (roundsSince(lastDamageRound) == 0)
			Message.sendSignal(rc.getType().sensorRadiusSquared*2);
		
		myStrategy.tryTurn();
	}
}
