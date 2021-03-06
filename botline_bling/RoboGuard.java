package botline_bling;

import battlecode.common.*;

public class RoboGuard extends RobotPlayer
{
	public static void init() throws GameActionException
	{
	}
	
	public static void turn() throws GameActionException
	{
        int fate = rand.nextInt(1000);

        if (fate % 5 == 3) {
            // Send a normal signal
            rc.broadcastSignal(80);
        }

        boolean shouldAttack = false;

        // If this robot type can attack, check for enemies within range and attack one
        if (rc.getType().attackRadiusSquared > 0) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, theirTeam);
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, Team.ZOMBIE);
            if (enemiesWithinRange.length > 0) {
                shouldAttack = true;
                // Check if weapon is ready
                if (rc.isWeaponReady()) {
                    rc.attackLocation(enemiesWithinRange[rand.nextInt(enemiesWithinRange.length)].location);
                }
            } else if (zombiesWithinRange.length > 0) {
                shouldAttack = true;
                // Check if weapon is ready
                if (rc.isWeaponReady()) {
                    rc.attackLocation(zombiesWithinRange[rand.nextInt(zombiesWithinRange.length)].location);
                }
            }
        }

        if (!shouldAttack) {
            if (rc.isCoreReady()) {
            	NavSafetyPolicy safety = new SafetyPolicyAvoidAllUnits();
            	MapLocation target = new MapLocation(436,159);
                Nav.goTo(target, safety);
            }
        }
	}
}
