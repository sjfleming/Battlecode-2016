package blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

class ArchonLocation extends Message
{
	public MapLocation loc;
	public int round;
	public int id;
	private static final int ARCHON_TIMEOUT = 200;
	
	public ArchonLocation (int newid, int newround, MapLocation newloc)
	{
		loc = newloc;
		round = newround;
		id = newid;
	}
	
	public boolean update(int newid, int newround, MapLocation newloc)
	{
		// does this correspond to the same archon id? 
		// if it's newer, update the archon's info
		if (id == newid && newround > round)
		{
			loc = newloc;
			round = newround;
			id = newid;
			return true;
		}
		
		return false;
	}
	
	public boolean isRecent()
	{
		return roundsSince(round) < ARCHON_TIMEOUT;
	}
}

class SignalRound extends RobotPlayer
{
	public Signal sig;
	public int round;
	private final int timeout;
	
	public SignalRound(int timeout)
	{
		sig = null;
		round = -1000000;
		this.timeout = timeout;
	}
	
	public boolean update(Signal newsig)
	{
		if (roundsSince(round) > timeout || 
				here.distanceSquaredTo(newsig.getLocation()) < 
				here.distanceSquaredTo(sig.getLocation()))
		{
			sig = newsig;
			round = rc.getRoundNum();
			return true;
		}
		return false;
	}
	
	public boolean isRecent()
	{
		return roundsSince(round) < timeout;
	}
}

class SignalDelay extends RobotPlayer
{
	private final int timeout;
	private int round;
	
	public SignalDelay(int timeout)
	{
		this.timeout = timeout;
		this.round = -100000;
	}
	
	public boolean canSend()
	{
		if (roundsSince(round) > timeout)
		{
			this.round = rc.getRoundNum();
			return true;
		}
		return false;
	}
	
	public void reset()
	{
		this.round = rc.getRoundNum();
	}
}

public class Message extends RobotPlayer
{
	// enum for encoding the type of a message in the contents
	enum Type
	{
		UNDER_ATTACK, // int free signals always mean under attack
		ZOMBIE_DEN,
		SIGHT_TARGET,
		SPAM,
		FREE_BEER,
		MAP_EDGE,
		NEW_STRATEGY,
		LOTSA_FRIENDLIES,
		NEUTRAL_ARCHON,
		ARCHON_LOCATION
	}
	
	// bookkeeping stuff
	public static final int FULL_MAP_DIST_SQ = GameConstants.MAP_MAX_HEIGHT*GameConstants.MAP_MAX_HEIGHT +
											   GameConstants.MAP_MAX_WIDTH*GameConstants.MAP_MAX_WIDTH;
	
	// get set by MapInfo... awkward
	public static int MAP_OFF_X = 0;
	public static int MAP_OFF_Y = 0;
	
	private static SignalRound	recentAllyAttacked = new SignalRound(15);
	private static SignalRound	recentArchonAttacked = new SignalRound(50);
	
	private static ArrayList<ArchonLocation> recentArchonLocations = new ArrayList<ArchonLocation>();
	private static SignalDelay archonLocationTimer = new SignalDelay(20);
	
	private static SignalDelay	recentSignalTimer = new SignalDelay(5);
	
	private static final int LOCATION_NO_SYM = 1;
	
	// and for any transmitted enemy messages, only keep the latest received
	public static MapLocation recentEnemySignal = null;
	
	// and other things
	public static Strategy.Type recentStrategySignal = null;
	public static int recentStrategySender = -1;
	
	public static void readSignalQueue() throws GameActionException
	{
		Signal[] sigs = rc.emptySignalQueue();
		
		boolean didEnemyMessage = false;
		
		for (Signal sig : sigs)
		{
			// skip enemy signals for now
			if (sig.getTeam() != ourTeam)
			{
				recentEnemySignal = sig.getLocation();
				// only do this once per round to prevent getting spammed
				// also ignore scout messages
				if (!didEnemyMessage)
				{
					Waypoint.enemyTargetStore.add(new Waypoint.TargetInfo(sig.getLocation(),1));
					didEnemyMessage = true;
				}
				continue;
			}
			
			int[] vals = sig.getMessage();
			Message.Type type;
			if (vals == null)
				type = Message.Type.UNDER_ATTACK;
			else
				type = readType(vals[0]);
			
			switch (type)
			{
			case UNDER_ATTACK:
				if (vals == null)
					recentAllyAttacked.update(sig);
				else
					recentArchonAttacked.update(sig);
				break;
			case FREE_BEER:
				StratZDay.receivedZDaySignal = true;
				StratZDay.archonLocations.add(sig.getLocation());
				break;
			case SPAM:
				break;
			case SIGHT_TARGET:
				MapLocation loc0 = readLocation(vals[0]);
				MapLocation loc1 = readLocation(vals[1]);
				int val = readShort(vals[1],1);
				Sighting.addSightedTarget(loc0,loc1);
				
				Waypoint.enemyTargetStore.add(new Waypoint.TargetInfo(loc0,val));
				Waypoint.enemyTargetStore.add(new Waypoint.TargetInfo(loc1,val));
				break;
			case ZOMBIE_DEN:
				MapInfo.updateZombieDens(readLocation(vals[0]), readLocation(vals[1]), readByte(vals[1],3)==0);
				break;
			case MAP_EDGE:
				MapInfo.updateMapEdges(readLocation(vals[0]), readLocation(vals[1]));
				break;
			case NEW_STRATEGY:
				// was this intended for me?
				if (vals[1] == rc.getID())
				{
					recentStrategySignal = Strategy.Type.values()[readByte(vals[0],0)];
					recentStrategySender = sig.getID();
				}
				break;
			case LOTSA_FRIENDLIES:
				Waypoint.friendlyTargetStore.add(new Waypoint.TargetInfo(sig.getLocation(),vals[1]));
				break;
			case NEUTRAL_ARCHON:
				MapInfo.updateNeutralArchons(readLocation(vals[0]), readLocation(vals[1]), readByte(vals[1],3)==0);
				break;
			case ARCHON_LOCATION:
				MapLocation loc = readLocation(vals[0]);
				updateArchonLocations(loc,readShort(vals[1],0),readShort(vals[1],1));
				Waypoint.friendlyTargetStore.add(new Waypoint.TargetInfo(loc,readByte(vals[0],2)));
				break;
			default:
				break;
			}
		}
	}
	
	// if we received a nearby archon message
	private static void updateArchonLocations(MapLocation loc, int round, int id)
	{
		// don't save if we _are_ an archon, so we know about all the others
		if (id == rc.getID())
			return;
		
		// look through the list of id/loc pairs and see if we already have this dude on file
		for (ArchonLocation aloc : recentArchonLocations)
		{
			if (aloc.id == id)
			{
				aloc.update(id, round, loc);
				return;
			}
		}
		recentArchonLocations.add(new ArchonLocation(id,round,loc));
	}
	
	// scouts send archon location messages
	public static void sendArchonLocation(RobotInfo ri) throws GameActionException
	{
		int messageDist = (rc.getType() == RobotType.SCOUT) ? MapInfo.fullMapDistanceSq() : 500;
		if (archonLocationTimer.canSend())
			Message.sendMessageSignal(messageDist, Type.ARCHON_LOCATION, ri.location, 
					Micro.getNearbyAllies().length, rc.getRoundNum(), ri.ID);
	}
	
	// who is the closest to meeeeeeee
	public static MapLocation getClosestArchon()
	{
		ArchonLocation closest = null;
		
		for (ArchonLocation al : recentArchonLocations)
		{
			if (closest == null || al.loc.distanceSquaredTo(here) < closest.loc.distanceSquaredTo(here))
				closest = al;
		}
		
		// if we're there, get rid of it
		if (closest != null && here.distanceSquaredTo(closest.loc) <= 24)
		{
			// and recurse to find another good one
			recentArchonLocations.remove(closest);
			return getClosestArchon();
		}
		
		if (closest == null)
			return null;
		
		return closest.loc;
	}
	
	// to be called by Archon
	// this takes about 1.5 core delay for 8 dens
	public static void sendBuiltMessage(Strategy.Type strat, int builtID) throws GameActionException
	{
		// send all four map edges in one go
		Message.sendMessageSignal(9, Message.Type.MAP_EDGE, MapInfo.mapMin, MapInfo.mapMax);
		// and a strategy to use
		Message.sendMessageSignal(9, Message.Type.NEW_STRATEGY, strat.ordinal(), builtID);
		// and then all of the zombie dens and archons, but signal
		for (MapLocation loc : MapInfo.zombieDenLocations.elements())
			Message.sendMessageSignal(9, Message.Type.ZOMBIE_DEN, loc, MapInfo.nullLocation, 
					LOCATION_NO_SYM);
		for (MapLocation loc : MapInfo.neutralArchonLocations.elements())
			Message.sendMessageSignal(9, Message.Type.NEUTRAL_ARCHON, loc, MapInfo.nullLocation, 
					LOCATION_NO_SYM);
	}
	
	// Basic signals always only done when under attack
	public static MapLocation getClosestAllyUnderAttack() throws GameActionException
	{
		// timeout for old signals
		if (recentArchonAttacked.isRecent())
			return recentArchonAttacked.sig.getLocation();
		
		if (recentAllyAttacked.isRecent())
			return recentAllyAttacked.sig.getLocation();
		
		return null;
	}
	
	// low-level functions past here
	
	private static int readShort(int val, int ind)
	{
		return (val>>>(ind*16))&65535;
	}
	
	private static int readByte(int val, int ind)
	{
		return (val>>>(ind*8))&255;
	}
	
	private static MapLocation readLocation(int val)
	{
		MapLocation loc = new MapLocation(readByte(val,0)+MAP_OFF_X, readByte(val,1)+MAP_OFF_Y);
		if (loc.equals(MapInfo.nullLocation))
			return null;
		return loc;
	}
	
	public static int writeShort(int val, int ind)
	{
		return (val << (ind*16));
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
	public static int writeType(Message.Type type)
	{
		return writeByte(type.ordinal(), 3);
	}
	
	public static Message.Type readType(int val)
	{
		return Message.Type.values()[readByte(val, 3)];
	}
	
	public static void sendMessageSignal(int sq_distance, Message.Type type, int val) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type), val);
	}
	
	public static void sendMessageSignal(int sq_distance, Message.Type type, MapLocation loc1, MapLocation loc2) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type) | writeLocation(loc1), writeLocation(loc2));
	}

	public static void sendMessageSignal(int sq_distance, Message.Type type, int val1, int val2) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type) | writeByte(val1,0), val2);
	}
	
	public static void sendMessageSignal(int sq_distance, Message.Type type, MapLocation loc, int val1, int val2) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type) | writeLocation(loc), writeShort(val1,0) | writeShort(val2,1));
	}
	
	public static void sendMessageSignal(int sq_distance, Message.Type type, MapLocation loc, int val0, int val1, int val2) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type) | writeByte(val0,2) | writeLocation(loc), writeShort(val1,0) | writeShort(val2,1));
	}
	
	public static void sendMessageSignal(int sq_distance, Message.Type type, MapLocation loc1, MapLocation loc2, int val) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type) | writeLocation(loc1), writeByte(val, 3) | writeLocation(loc2));
	}
	
	public static void sendMessageSignal(int sq_distance, Message.Type type, MapLocation loc) throws GameActionException
	{
		sendMessageSignal(sq_distance, writeType(type) | writeLocation(loc), 0);
	}

	private static void sendMessageSignal(int sq_distance, int v1, int v2) throws GameActionException
	{
		rc.broadcastMessageSignal(v1,v2,sq_distance);
	}
	
	public static void trySendSignal(int sq_distance) throws GameActionException
	{
		if (recentSignalTimer.canSend())
			rc.broadcastSignal(sq_distance);
	}
	
	// sends a message-free signal
	public static void sendSignal(int sq_distance) throws GameActionException
	{
		recentSignalTimer.reset();
		rc.broadcastSignal(sq_distance);
	}
}
