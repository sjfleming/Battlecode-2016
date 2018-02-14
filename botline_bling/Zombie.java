package botline_bling;

import battlecode.common.*;
import java.util.*;

public class Zombie extends RobotPlayer
{
	// to be used by turrets only
	public static MapLocation getSightedTarget()
	{
		MapLocation targetLoc = null;
		// if we're doing this, we can clear the list once we're done
		// because zombies will have moved by then
		for (SignalLocation sl : Message.sightLocs)
		{
			// zombie still there...
			if (rc.getRoundNum() - sl.round < 3 && sl.loc.distanceSquaredTo(here) <= rc.getType().attackRadiusSquared)
			{
				if (targetLoc == null || targetLoc.distanceSquaredTo(here) < sl.loc.distanceSquaredTo(here))
					targetLoc = sl.loc;
			}
		}
		
		Message.sightLocs.clear();
		
		return targetLoc;
	}
	
	// to be used by scouts only
	// (others can't send message signals dur)
	public static boolean sendSightedTarget() throws GameActionException
	{
		return Zombie.sendSightedTarget(rc.senseHostileRobots(here, rc.getType().sensorRadiusSquared));
	}

	public static boolean sendSightedTarget(RobotInfo[] nearbyTargets) throws GameActionException
	{
		RobotInfo closestTarget = null;
		
		// maybe prioritize ranged zombies/enemies?
		for (RobotInfo ri : nearbyTargets)
		{
			// prioritize enemy turrets
			if (ri.type == RobotType.TURRET)
			{
				closestTarget = ri;
				continue;
			}
			
			if (closestTarget == null || ri.location.distanceSquaredTo(here) < closestTarget.location.distanceSquaredTo(here))
				closestTarget = ri;
		}
		
		if (closestTarget != null)
		{
			if (closestTarget.type == RobotType.TURRET)
				Message.sendMessageSignal(7,MessageType.ENEMY_TURRET,closestTarget.location);
			else
				Message.sendMessageSignal(7,MessageType.SIGHT_TARGET,closestTarget.location);
			return true;
		}
		
		return false;
	}
}
