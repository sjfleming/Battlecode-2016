package botline_bling;

import battlecode.common.*;

public class RoboSoldier extends RobotPlayer
{
	static int fate;
	
	public static void init()
	{
		fate = rand.nextInt(1000);
	}
	
	public static void turn() throws GameActionException
	{
        if (rc.isCoreReady()) {
        	if (!Micro.tryRetreatIfOverpowered())
        	{
//        		NavSafetyPolicy safety = new SafetyPolicyAvoidZombies();
//        		// diffuse randomly
//        		int dx = rand.nextInt(3)-1;
//        		int dy = rand.nextInt(3)-1;
//        		Nav.goTo(here.add(dx,dy), safety);
        		
        		// do nothing
        	}
        }
	}
}
