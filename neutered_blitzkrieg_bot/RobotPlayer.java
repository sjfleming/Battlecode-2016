package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer
{
	public static RobotController rc;
	public static Random rand;
	public static Team ourTeam;
	public static Team theirTeam;
	public static MapLocation here;
	public static Direction lastMovedDirection = Direction.NORTH;
	
	public static MapLocation 	myBuilderLocation = null;
	public static int			myBuilderID = -1; // dad?
	public static int 			myBuiltRound;
	
	public static MicroBase Micro = null;
	public static Strategy myStrategy = null;
	
	public static double 	myHealth;
	public static boolean	tookHeavyDamageLastRound = false;
	public static int		lastDamageRound = -100;
	public static int		lastMovedRound = -100;
	public static int 		lastFiredRound = -100;
	public static int		lastDangerRound = -100;
	public static int		lastSafeRound = -100;
	
	public static int		lastMicroTime = 0;
	public static int		lastSurroundingsTime = 0;
	public static int		lastSignalsTime = 0;
	public static int		lastTurnTime = 0;
	
    public static void run(RobotController robotc)
	{
		// globals in our class
		RobotPlayer.rc = robotc;
		RobotPlayer.rand = new Random(rc.getID()+rc.getRoundNum());
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
				
		try
		{
			// also initialize Micro
			Micro = new MicroBase();
			// and try go get the map symmetry. everyone can do this
			MapInfo.calculateSymmetry();
			// and do this once, so init can see recent messages
			Message.readSignalQueue();

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
		while (true)
		{
			try
			{
				RobotPlayer.here = rc.getLocation();
				
				// clear all outstanding micro stuff
				Debug.startTiming();
				Micro = new MicroBase();
				//Micro.getRoundsUntilDanger();
				Micro.getSafeMoveDirs();
				lastMicroTime = Debug.stopTiming();
				
				// update health
				tookHeavyDamageLastRound = false;
				double health = rc.getHealth();
				if (health < myHealth)
				{
					if (myHealth-health > 10)
						tookHeavyDamageLastRound = true;
					lastDamageRound = rc.getRoundNum();
					myHealth = health;
				}
				else
				{
					// (in case of healing)
					myHealth = rc.getHealth();
				}
				
				// process incoming messages
				Debug.startTiming();
				Message.readSignalQueue();
				lastSignalsTime = Debug.stopTiming();
				
				// update stuff with sensing
				Debug.startTiming();
				MapInfo.doAnalyzeSurroundings();
				lastSurroundingsTime = Debug.stopTiming();
				
				if (StratZDay.shouldActivate())
					myStrategy = new StratZDay();
				
				Debug.startTiming();
				
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
				
				lastTurnTime = Debug.stopTiming();
				
				// let's see what we're doing
				if (myStrategy != null)
					Debug.setStringAK(myStrategy.getName());
				else
					Debug.setStringAK("null strategy???");
				
				Debug.setStringTS("D:" + MapInfo.zombieDenLocations.elements().size()
						+ " A:" + MapInfo.neutralArchonLocations.elements().size()
						+ " M:" + MapInfo.mapSymmetry
						+ " T:" + Sighting.enemySightedTurrets.elements().size()
						+ " E:" + Waypoint.enemyTargetStore.size());
				
				Debug.setStringSJF(lastMicroTime + "/" + lastSignalsTime + "/" +
									lastSurroundingsTime + "/" + lastTurnTime);

				Clock.yield();
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
	            e.printStackTrace();
			}
		}
    }
    
    public static int roundsSince(int start)
    {
    	return rc.getRoundNum() - start;
    }
}
