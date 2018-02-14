package botumn_leaves;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer
{
	public static RobotController rc;
	public static Random rand;
	public static Team ourTeam;
	public static Team theirTeam;
	public static MapLocation here;
	
	public static MapLocation 	myBuilderLocation = null;
	public static int			myBuilderID = -1; // dad?
	public static int 			myBuiltRound;
	
	public static MicroBase Micro = null;
	public static Strategy myStrategy = null;
	
	public static double 	myHealth;
	public static int		lastDamageRound = -100;
	public static int		lastMovedRound = 0;
	
    public static void run(RobotController robotc)
	{
		// globals in our class
		RobotPlayer.rc = robotc;
		RobotPlayer.rand = new Random(rc.getID());
		RobotPlayer.ourTeam = rc.getTeam();
		RobotPlayer.theirTeam = ourTeam.opponent();
		RobotPlayer.here = rc.getLocation();
		RobotPlayer.myBuiltRound = rc.getRoundNum();
		
		Debug.setStringTS("Tamas");
		Debug.setStringAK("A-aron");
		Debug.setStringSJF("Stephen J. Fry");
		Debug.setStringRR("Ryan");
				
		// look for an archon close by, if we aren't an Archon
		if (rc.getType() != RobotType.ARCHON)
		{
			RobotInfo[] nearbyFriends = rc.senseNearbyRobots(8, ourTeam);
			for (RobotInfo ri : nearbyFriends)
			{
				if (ri.type != RobotType.ARCHON)
					continue;
				
				if (myBuilderLocation == null || 
						here.distanceSquaredTo(ri.location) < here.distanceSquaredTo(myBuilderLocation))
				{
					myBuilderLocation = ri.location;
					myBuilderID = ri.ID;
				}
			}
		}
		
		myHealth = rc.getHealth();
		
		// also initialize Micro
		Micro = new MicroBase();
		// and try go get the map symmetry. everyone can do this
		MapInfo.calculateSymmetry();
		// and do this once, so init can see recent messages
		Message.readSignalQueue();
		
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
				// clear all outstanding micro stuff
				Micro = new MicroBase();
				// update health
				double health = rc.getHealth();
				if (health < myHealth)
				{
					lastDamageRound = rc.getRoundNum();
					myHealth = health;
				}
				else
				{
					// (in case of healing)
					myHealth = rc.getHealth();
				}
				// process incoming messages
				Message.readSignalQueue();
				// update stuff with sensing
				MapInfo.doAnalyzeSurroundings();
				
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
				
				// let's see what we're doing
				if (myStrategy != null)
					Debug.setStringAK(myStrategy.getName());
				Debug.setStringTS("Dens known: " + MapInfo.zombieDenLocations.elements().size());
				
				Clock.yield();
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
            e.printStackTrace();
		}

    }
    
    public static int roundsSince(int start)
    {
    	return rc.getRoundNum() - start;
    }
}
