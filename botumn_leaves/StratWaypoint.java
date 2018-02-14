package botumn_leaves;

import battlecode.common.*;

import java.util.*;

public class StratWaypoint extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Mob movin'";
	}
	
	public boolean tryTurn() throws GameActionException
	{
		// do we have a strategy that takes precedence over this one?
		if (overrideStrategy != null)
		{
			if (overrideStrategy.tryTurn())
				return true;
			else
				overrideStrategy = null;
		}
		
		// ok let's see what the plan is
		// if we received a recent ping _or_ we are under attack,
		// we switch to mob fighing
		if (Micro.getRoundsUntilDanger() < 20)
		{
			overrideStrategy = new StratMobFight();
			overrideStrategy.tryTurn();
			return true;
		}
		MapLocation allyAttacked = Message.getClosestAllyUnderAttack();
		if (allyAttacked != null)
		{
			if (here.distanceSquaredTo(allyAttacked) > 200)
			{
				Nav.tryGoTo(allyAttacked, Micro.getSafeMoveDirs());
				return true;
			}
			overrideStrategy = new StratMobFight(Message.getClosestAllyUnderAttack());
			overrideStrategy.tryTurn();
			return true;
		}
		
		// or if we're close to a zombie den we also do
		MapLocation closestDen = MapInfo.getClosestDen();
		
		if (closestDen == null)
		{
			Nav.tryGoTo(MapInfo.ourArchonCenter, Micro.getSafeMoveDirs());
			return true;
		}
		
		/*if (closestDen != null && here.distanceSquaredTo(closestDen) < 400)
		{
			overrideStrategy = new MobFightStrat(closestDen);
			overrideStrategy.tryTurn();
			return true;
		}*/
		
		// otherwise, we mosey on to the closest zombie den if we have many units around us
		//int numfriendlies = Micro.getNearbyAllies().length;//rc.senseNearbyRobots(15, ourTeam).length;
		
		//if (numfriendlies > 3)
		Action.tryGoToWithoutBeingShot(closestDen, Micro.getSafeMoveDirs());
		
		return true;
	}
}
