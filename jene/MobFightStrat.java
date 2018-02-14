package jene;

import battlecode.common.*;

import java.util.*;

public class MobFightStrat extends RobotPlayer implements Strategy
{
	private static String stratName;
	private static MapLocation target;
	private static MapLocation startingLoc;
	
	public MobFightStrat()
	{
		this.stratName = "MobFightStrat";
		this.target = null;
		this.startingLoc = here;
	}
	
	public MobFightStrat(MapLocation target)
	{
		this.stratName = "MobFightStrat";
		this.target = target;
		this.startingLoc = here;
	}
	
	public boolean tryTurn() throws GameActionException
	{
		Debug.setStringAK("My Strategy: " + this.stratName);
		
		switch (rc.getType())
		{
		case TURRET:
			return (Action.tryAttackSomeone());
			
		case GUARD:
			// get in their face
			// don't retreat unless you are out-numbered
			// retreating is for the weak
			if (Action.tryAttackSomeone())
				return true;
			
			// we can't attack anyone.  listen for calls for reinforcements, and move to help
			updateTarget();
			if (target != null)
			{
				RobotInfo enemyAttackingAlly = Micro.getClosestUnitTo(Micro.getNearbyHostiles(), target);
				if (enemyAttackingAlly == null)
					return Action.tryMove(here.directionTo(target));
				
				if (!Action.tryAdjacentSafeMoveToward(enemyAttackingAlly.location))
				{
					Direction dir = Micro.getCanFastMoveDirs().getDirectionTowards(here.directionTo(enemyAttackingAlly.location));
					if (!Action.tryMove(dir))
					{
						dir = Micro.getCanMoveDirs().getDirectionTowards(here.directionTo(enemyAttackingAlly.location));
						if (!Action.tryMove(dir))
							return Action.tryGoToWithoutBeingShot(enemyAttackingAlly.location, Micro.getCanMoveDirs().and(Micro.getTurretSafeDirs())); // don't just sit there, FULL SURROUND
						else
							return true;
					}
					else
					{
						return true;
					}
				}
				else
				{
					return true;
				}
			}
			
			// if out of their range and alone, retreat
			if (rc.senseNearbyRobots(2, ourTeam).length < 3)
			{
				Action.tryRetreatOrShootIfStuck();
				return true;
			}

			return Action.tryGoToWithoutBeingShot(startingLoc, Micro.getSafeMoveDirs());
		
		default:
			// if i am overpowered, kite retreat taking pot-shots
			Debug.setStringSJF("allies = " + Micro.getNearbyAllies().length + ", hostiles = " + Micro.getNearbyHostiles().length + ", overpowered: " + Micro.amOverpowered());
			if (Action.tryAttackSomeone())
				return true;
			
			if (Micro.amOverpowered())
			{
				// pot-shot when you can get away without getting hit, retreat when unable to
				if (Micro.getRoundsUntilDanger() > Micro.getRoundsUntilShootAndMove() + 1)
				{
					if (!Action.tryAttackSomeone())
						Action.tryRetreatOrShootIfStuck();
				}
				else
				{
					Action.tryRetreatOrShootIfStuck();
				}
				return true;
			}
			
			// not overpowered.  we don't see anyone.  listen for calls for reinforcements, and move to help
			updateTarget();
			
			if (target != null)
			{
				Debug.setStringSJF("target = " + target.toString());
				RobotInfo enemyAttackingAlly = Micro.getClosestUnitTo(Micro.getNearbyHostiles(), target);
				if (enemyAttackingAlly == null)
				{
					Nav.tryGoTo(target, Micro.getCanMoveDirs());
					return true;
				}
				
				if (enemyAttackingAlly != null && !Action.tryAdjacentSafeMoveToward(enemyAttackingAlly.location))
				{
					Direction dir = Micro.getCanFastMoveDirs().getDirectionTowards(here.directionTo(enemyAttackingAlly.location));
					if (!Action.tryMove(dir))
					{
						dir = Micro.getCanMoveDirs().getDirectionTowards(here.directionTo(enemyAttackingAlly.location));
						if (!Action.tryMove(dir))
							Nav.tryGoTo(enemyAttackingAlly.location, Micro.getCanMoveDirs()); // don't just sit there, FULL SURROUND
					}
				}
				return true;
			}
		}

		// not doing anything else, so look for parts and DIG
		MapLocation closestPart = Rubble.senseClosestPart();
		if (closestPart == null)
			closestPart = MapInfo.getClosestPart();
		if (closestPart != null)
		{
			if (Action.tryMove(here.directionTo(closestPart)))
				return true;

			if (Rubble.tryClearRubble(closestPart))
				return true;
		}

		//Debug.setStringSJF("going back to = " + startingLoc.toString());
		if (here.distanceSquaredTo(startingLoc) > 10 && Action.tryGoToWithoutBeingShot(startingLoc, Micro.getSafeMoveDirs()))
			return true;
		
		// don't stand on parts, that's not cool
		if (rc.senseParts(here) > 0)
		{
			if (Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()).any())
				Action.tryMove(Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()).getRandomValid());
			else
			{
				Direction rubDir = Rubble.getRandomAdjacentRubble();
				if (rubDir != null)
					Rubble.tryClearRubble(here.add(rubDir));
			}
		}
		
		// if we're really still doing nothing, move away from friends
		RobotInfo closestAlly = Micro.getClosestUnitTo(Micro.getNearbyAllies(), here);
		if (closestAlly != null)
			Action.tryMove(here.directionTo(closestAlly.location).opposite());

		return false;
	}
	
	public void updateTarget() throws GameActionException
	{
		// if you have no target (from instantiation), get one
		if (target == null)
		{
			target = Message.getClosestAllyUnderAttack();
			if (target == null)
				target = MapInfo.getClosestDen();
			return;
		}
		
		// if you can't sense the target location, you're not close
		if (target == null)
			return;
			
		if (!rc.canSenseLocation(target))
			return;
		
		// if you're close enough to see it, check if it's still there, if not, delete it
		if (rc.senseRobotAtLocation(target) == null || rc.senseRobotAtLocation(target).team == ourTeam)
			target = null;
		
		return;
	}
}
