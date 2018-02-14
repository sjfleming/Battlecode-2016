package team023;

import battlecode.common.*;

import java.util.*;

public class RoboScout extends RobotPlayer
{
	public static void init() throws GameActionException
	{
		if (Message.recentStrategySignal == null || rc.getRoundNum() < 200)
		{
			myStrategy = new StratScoutExplore();
			return;
		}
		// start off balling around closest archon
		switch (Message.recentStrategySignal)
		{
		case SHADOW_ARCHON:
			myStrategy = new StratScoutShadow(Message.recentStrategySender);
			break;
		case SHADOW_SOLDIER:
			myStrategy = new StratScoutSighting();
			break;
		case EXPLORE:
		default:
			myStrategy = new StratScoutExplore();
			break;
		}
	}

	
	public static void turn() throws GameActionException
	{
		// Free unit if everything else breaks
		if (!myStrategy.tryTurn())
			myStrategy = new StratScoutExplore();
		
		Sighting.doSendSightingMessage();
		// keep sending, only continues if scout is near death
		while (MapInfo.tryScoutSendUpdates())
			continue;
	}
}
