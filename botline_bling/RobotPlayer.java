package botline_bling;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer
{
	public static RobotController rc;
	public static Random rand;
	public static Team ourTeam;
	public static Team theirTeam;
	public static MapLocation here;
	
	public static RobotInfo myArchon = null;
	public static int myArchonSenseRound = 0; 
	
	@SuppressWarnings("unused")
	// BC Engine -> RobotPlayer.run -> RoboXXX.run
    public static void run(RobotController robotc)
	{
		// globals in our class
		RobotPlayer.rc = robotc;
		RobotPlayer.rand = new Random(rc.getID());
		RobotPlayer.ourTeam = rc.getTeam();
		RobotPlayer.theirTeam = ourTeam.opponent();
		RobotPlayer.here = rc.getLocation();

		Debug.setStringTS("Tamas");
		Debug.setStringAK("A-aron");
		Debug.setStringSJF("Stephen J. Fry");
		Debug.setStringRR("Ryan");
		
		// look for an archon close by, if we aren't an Archon
		if (rc.getType() != RobotType.ARCHON)
		{
			RobotInfo[] nearbyFriends;
			// look at a larger distance if we started early in the game
			if (rc.getRoundNum() < 2)
				nearbyFriends = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, ourTeam);
			else
				nearbyFriends = rc.senseNearbyRobots(2, ourTeam);
			
			// find closest Archon
			for (RobotInfo ri : nearbyFriends)
			{
				if (ri.type == RobotType.ARCHON)
				{
					if (myArchon == null || here.distanceSquaredTo(myArchon.location) > here.distanceSquaredTo(ri.location))
						myArchon = ri;
				}
			}
			if (myArchon != null) myArchonSenseRound = rc.getRoundNum();
		}
		
		try
		{
			switch (robotc.getType())
			{
			case ARCHON:
				RoboArchon.init();
				break;
			case GUARD:
				RoboGuard.init();
				break;
			case SCOUT:
				RoboScout.init();
				break;
			case SOLDIER:
				RoboSoldier.init();
				break;
			case TTM:
			case TURRET:
				RoboTurret.init();
				break;
			case VIPER:
				RoboViper.init();
				break;
				
			case ZOMBIEDEN:
			case BIGZOMBIE:
			case FASTZOMBIE:
			case RANGEDZOMBIE:
			case STANDARDZOMBIE:
			default:
				System.out.println("WTF IS GOING ON!!!111");
				rc.disintegrate();
				break;
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
            e.printStackTrace();
		}
		
		// doing it like this so we can add robot-wide behaviors easily
		try
		{
			while (true)
			{
				RobotPlayer.here = rc.getLocation();
				Message.readSignalQueue();
				
				// try to re-sense archon
				if (myArchon != null)
				{
					if (rc.canSenseRobot(myArchon.ID))
					{
						myArchon = rc.senseRobot(myArchon.ID);
						myArchonSenseRound = rc.getRoundNum();
					}
					else
					{
						// we've really done it now
						if (rc.getRoundNum() - myArchonSenseRound > 50)
							myArchon = null;
					}
				}
				
				switch (robotc.getType())
				{
				case ARCHON:
					RoboArchon.turn();
					break;
				case GUARD:
					RoboGuard.turn();
					break;
				case SCOUT:
					RoboScout.turn();
					break;
				case SOLDIER:
					RoboSoldier.turn();
					break;
				case TTM:
				case TURRET:
					RoboTurret.turn();
					break;
				case VIPER:
					RoboViper.turn();
					break;
					
				case ZOMBIEDEN:
				case BIGZOMBIE:
				case FASTZOMBIE:
				case RANGEDZOMBIE:
				case STANDARDZOMBIE:
				default:
					System.out.println("WTF IS GOING ON!!!111");
					rc.disintegrate();
					break;
				}
				
				Clock.yield();
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
            e.printStackTrace();
		}

    }
}
