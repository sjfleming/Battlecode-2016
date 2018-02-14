package jene;

import battlecode.common.*;

import java.util.*;

// enum for encoding the type of a message in the contents
enum MessageType
{
	UNDER_ATTACK, // int free signals always mean under attack
	ZOMBIE_DEN,
	SIGHT_TARGET,
	SPAM,
	FREE_BEER,
	GOOD_PARTS,
	MAP_EDGE,
	REMOVE_WAYPOINT,
	NEW_STRATEGY,
	NEUTRAL_ARCHON
}

class SignalLocation extends RobotPlayer
{
	public MapLocation loc;
	public Signal sig;
	public int round;
	
	public SignalLocation (Signal s, MapLocation ml)
	{
		sig = s;
		loc = ml;
		round = rc.getRoundNum();
	}
}

public class Message extends RobotPlayer
{	
	// bookkeeping stuff
	public static final int FULL_MAP_DIST_SQ = GameConstants.MAP_MAX_HEIGHT*GameConstants.MAP_MAX_HEIGHT +
											   GameConstants.MAP_MAX_WIDTH*GameConstants.MAP_MAX_WIDTH;
	
	// get set by MapInfo... awkward
	public static int MAP_OFF_X = 0;
	public static int MAP_OFF_Y = 0;
	
	public static ArrayList<SignalLocation> underAttackLocs = new ArrayList<SignalLocation>();
	public static ArrayList<SignalLocation> neutralArchonLocs = new ArrayList<SignalLocation>();
	
	// and for any transmitted enemy messages, only keep the latest received
	public static MapLocation recentEnemySignal = null;
	
	// and other things
	public static MapLocation closestAllyUnderAttackLocation = null;
	public static MapLocation closestNeutralArchonLocation = null;
	public static Strategy.Type recentStrategySignal = null;
	
	public static void readSignalQueue()
	{
		Signal[] sigs = rc.emptySignalQueue();
		
		for (Signal sig : sigs)
		{
			// skip enemy signals for now
			if (sig.getTeam() != ourTeam)
			{
				recentEnemySignal = sig.getLocation();
				continue;
			}
			
			int[] vals = sig.getMessage();
			MessageType type;
			if (vals == null)
				type = MessageType.UNDER_ATTACK;
			else
				type = readType(vals[0]);
			
			switch (type)
			{
			case UNDER_ATTACK:
				underAttackLocs.add(new SignalLocation(sig,sig.getLocation()));
				break;
			case FREE_BEER:
				break;
			case SPAM:
				break;
			case SIGHT_TARGET:
				Sighting.addSightedTarget(readLocation(vals[0]),
						RobotType.values()[readByte(vals[1],0)],
						rc.getRoundNum()-1);
				break;
			case ZOMBIE_DEN:
				MapInfo.updateZombieDens(readLocation(vals[0]),false);
				break;
			case NEUTRAL_ARCHON:
				MapInfo.updateNeutralArchon(readLocation(vals[0]),false);
				break;
			case MAP_EDGE:
				MapInfo.updateMapEdges(readLocation(vals[0]), readLocation(vals[1]));
				break;
			case GOOD_PARTS:
				MapInfo.updateParts(readLocation(vals[0]),false);				
				break;
			case NEW_STRATEGY:
				recentStrategySignal = Strategy.Type.values()[vals[1]];
				break;
			case REMOVE_WAYPOINT:
				MapInfo.removeWaypoint(readLocation(vals[0]));
				break;
			}
		}
	}
	
	// to be called by Archon
	public static void sendBuiltMessage(Strategy.Type strat) throws GameActionException
	{
		// send all four map edges in one go
		Message.sendMessageSignal(9, MessageType.MAP_EDGE, MapInfo.mapMin, MapInfo.mapMax);
		// and a strategy to use
		Message.sendMessageSignal(9, MessageType.NEW_STRATEGY, strat.ordinal());
	}
	
	//Basic signals always only done when under attack
	public static MapLocation getClosestAllyUnderAttack() throws GameActionException
	{
		if (closestAllyUnderAttackLocation != null)
			return closestAllyUnderAttackLocation;
		
		int distToClosestAlly = 1000000;
		MapLocation bestLoc = null;
		for (SignalLocation sm : Message.underAttackLocs )
		{
			int distToThisSignalOrigin = here.distanceSquaredTo(sm.loc);
			if (distToThisSignalOrigin < distToClosestAlly)
			{
				distToClosestAlly = distToThisSignalOrigin;
				bestLoc = sm.loc;
			}
		}
		
		if (bestLoc == null) return null;
		
		closestAllyUnderAttackLocation = bestLoc;
		
		if (rc.canSenseLocation(closestAllyUnderAttackLocation) && (rc.senseRobotAtLocation(closestAllyUnderAttackLocation)==null || rc.senseRobotAtLocation(closestAllyUnderAttackLocation).team == ourTeam))
				closestAllyUnderAttackLocation = null;
		
		// clear the buffer
		underAttackLocs.clear();
		
		return closestAllyUnderAttackLocation;
	}
	
	public static MapLocation getClosestNeutralArchonLoc() throws GameActionException
	{
		if (closestNeutralArchonLocation != null)
			return closestNeutralArchonLocation;
		
		int distToClosest = 1000000;
		MapLocation bestLoc = null;
		for (SignalLocation sm : Message.neutralArchonLocs )
		{
			int distToThisSignalOrigin = here.distanceSquaredTo(sm.loc);
			if (distToThisSignalOrigin < distToClosest)
			{
				distToClosest = distToThisSignalOrigin;
				bestLoc = sm.loc;
			}
		}
		
		if (bestLoc == null) return null;
		
		closestNeutralArchonLocation = bestLoc;
		
		if (rc.canSenseLocation(closestNeutralArchonLocation) && (rc.senseRobotAtLocation(closestNeutralArchonLocation)==null || rc.senseRobotAtLocation(closestNeutralArchonLocation).team == ourTeam))
			closestNeutralArchonLocation = null;
		
		// clear the buffer
		//neutralArchonLocs.clear();
		
		return closestNeutralArchonLocation;
	}
	
	public static void sendSightingSignal(int sq_distance, MapLocation loc)
	{
		
	}
	
	
	
	// low-level functions past here
	
	
	private static int readByte(int val, int ind)
	{
		return (val>>>(ind*8))&255;
	}
	
	private static MapLocation readLocation(int val)
	{
		return new MapLocation(readByte(val,0)+MAP_OFF_X, readByte(val,1)+MAP_OFF_Y);
	}
	
	// writes a particular byte
	// (gotta | all the bytes together to make a message)
	public static int writeByte(int val, int ind)
	{
		return (val << (ind*8));
	}
	
	// writes a maplocation to the lowest two bytes of ints n stuff
	public static int writeLocation(MapLocation loc)
	{
		return writeByte(loc.x-MAP_OFF_X,0) | writeByte(loc.y-MAP_OFF_Y,1);
	}
	
	// and the type byte
	public static int writeType(MessageType type)
	{
		return writeByte(type.ordinal(), 3);
	}
	
	public static MessageType readType(int val)
	{
		return MessageType.values()[readByte(val, 3)];
	}
	
	public static void sendMessageSignal(int sq_distance, MessageType type, int val) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type), val);
	}
	
	public static void sendMessageSignal(int sq_distance, MessageType type, MapLocation loc1, MapLocation loc2) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type) | writeLocation(loc1), writeLocation(loc2));
	}
	
	public static void sendMessageSignal(int sq_distance, MessageType type, MapLocation loc) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type) | writeLocation(loc), 0);
	}

	private static void sendMessageSignal(int sq_distance, int v1, int v2) throws GameActionException
	{
		rc.broadcastMessageSignal(v1,v2,sq_distance);
	}
	
	// sends a message-free signal
	public static void sendSignal(int sq_distance) throws GameActionException
	{
		rc.broadcastSignal(sq_distance);
	}
}
