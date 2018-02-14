package neutered_blitzkrieg_bot;

import battlecode.common.*;

public class RoboSoldier extends RobotPlayer
{
	private static Strategy overrideStrategy = null;
	
	public static void init() throws GameActionException
	{
		myStrategy = new StratUnitCombat();
	}
	
	public static void turn() throws GameActionException
	{
		// check if we should dig out of rubble (one blocked square ahead, but clear two ahead)
		if (overrideStrategy == null && StratDig.shouldDigThroughSingleBlockage())
			overrideStrategy = new StratDig(Nav.myDest);
		else if (overrideStrategy == null && StratDig.shouldDigBecauseStuck())
			overrideStrategy = new StratDig(Nav.myDest);
		
		// do we have a strategy that takes precedence over this one?
		if (overrideStrategy != null)
		{
			if (!overrideStrategy.tryTurn())
				overrideStrategy = null;
		}
		
		if (roundsSince(lastDamageRound) == 0)
			Message.sendSignal(rc.getType().sensorRadiusSquared*2);
		
		myStrategy.tryTurn();
		
		// did we fire? try to send a short ping
		if (roundsSince(lastFiredRound) == 0)
			Message.trySendSignal(rc.getType().sensorRadiusSquared*2);
	}
}
	