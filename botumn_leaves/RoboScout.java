package botumn_leaves;

import battlecode.common.*;

import java.util.*;

public class RoboScout extends RobotPlayer
{
	public static void init() throws GameActionException
	{
		if (Message.recentStrategySignal == null)
		{
			myStrategy = new StratExplore();
			return;
		}
		// start off balling around closest archon
		switch (Message.recentStrategySignal)
		{
		case SHADOW_ARCHON:
			myStrategy = new StratScoutShadow(myBuilderID);
			break;
		case SHADOW_SOLDIER:
			myStrategy = new StratScoutSighting();
			break;
		case EXPLORE:
		default:
			myStrategy = new StratExplore();
			break;
		}
	}

	
	public static void turn() throws GameActionException
	{
		// Free unit if everything else breaks
		if (!myStrategy.tryTurn())
			myStrategy = new StratFreeUnit();
		//myStrategy.tryTurn();
		
		Sighting.doSendSightingMessage();
		MapInfo.doScoutSendUpdates();
	}
}
