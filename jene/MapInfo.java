package jene;

import battlecode.common.*;

import java.util.*;

public class MapInfo extends RobotPlayer
{
	public static FastLocSet zombieDenLocations = new FastLocSet();
	public static FastLocSet goodPartsLocations = new FastLocSet();
	public static FastLocSet neutralArchonLocations = new FastLocSet();
	
	public static MapLocation mapMin = new MapLocation(-18000,-18000);
	public static MapLocation mapMax = new MapLocation(18001,18001);
	
	// update the boundary on one of the map min/maxes
	public static boolean newMapEdge = false;
	
	public static MapLocation newZombieDen = null;
	public static MapLocation newParts = null;
	public static MapLocation newNeutralArchon = null;
	
	public static int numInitialArchons = 0;

	// note - HFLIP means flip along horizontal axis (same x)
	// and VFLIP means flip along vertical axis (same y)
	// (DIAG is 
	private static final int SYM_ROT = 1;
	private static final int SYM_HFLIP = 2;
	private static final int SYM_VFLIP = 4;
	
	// if it is neither of these, it's some other bullshit
	private static int mapSymmetry = SYM_ROT|SYM_HFLIP|SYM_VFLIP;
	
	public static MapLocation mapCenter = null;
	private static MapLocation dblCenter = null;

	// the following functions are to be called mainly by Archons to set destinations
	
	// just do something random for now
	public static MapLocation getExplorationWaypoint()
	{
		// this one should get transmitted to a scout
//		return new MapLocation(rand.nextInt(mapMax.x - mapMin.x) + mapMin.x,
//							   rand.nextInt(mapMax.y - mapMin.y) + mapMin.y);
		return here.add((rand.nextInt(3)-1)*100, (rand.nextInt(3)-1)*100);
	}
	
	public static MapLocation getClosestPart()
	{
		return Micro.getClosestLocationTo(goodPartsLocations.elements(), here);
	}
	
	public static MapLocation getClosestDen()
	{
		return Micro.getClosestLocationTo(zombieDenLocations.elements(), here);
	}
	
	public static MapLocation getClosestNeutralArchon()
	{
		return Micro.getClosestLocationTo(neutralArchonLocations.elements(), here);
	}
	
	public static MapLocation getClosestDenThenPart()
	{
		MapLocation ml = Micro.getClosestLocationTo(zombieDenLocations.elements(), here);
		if (ml == null)
			ml = Micro.getClosestLocationTo(goodPartsLocations.elements(), here);
		return ml;
	}
	
	public static MapLocation getClosestPartOrDen()
	{
		MapLocation partloc = getClosestPart();
		MapLocation denloc = getClosestDen();
		
		if (partloc == null)
			return denloc;
		
		if (denloc == null || here.distanceSquaredTo(partloc) < here.distanceSquaredTo(denloc))
			return partloc;
		
		return denloc;
	}
	
	public static boolean isOnMap(MapLocation loc)
	{
		return (loc.x > mapMin.x) && (loc.y > mapMin.y) &&
				(loc.x < mapMax.x) && (loc.y < mapMax.y);
	}
	
	// distance required to cover the full map, for scout transmission
	public static int fullMapDistanceSq()
	{
		int maxDx = Math.abs(mapMin.x-here.x);
		int maxDy = Math.abs(mapMin.y-here.y);
		maxDx = Math.max(maxDx, Math.abs(mapMax.x-here.x));
		maxDy = Math.max(maxDy, Math.abs(mapMax.y-here.y));
		
		int maxDistSq = maxDx*maxDx + maxDy*maxDy;
		if (maxDistSq > Message.FULL_MAP_DIST_SQ)
			maxDistSq = Message.FULL_MAP_DIST_SQ;
		
		return maxDistSq;
	}
	
	// distance to closest corner
	public static int closestCornerDistanceSq()
	{
		int minDx = Math.abs(mapMin.x-here.x);
		int minDy = Math.abs(mapMin.y-here.y);
		minDx = Math.min(minDx, Math.abs(mapMax.x-here.x));
		minDy = Math.min(minDy, Math.abs(mapMax.y-here.y));
		
		return minDx*minDx + minDy*minDy;
	}
	
	// from receiving a message, don't need to bother signaling
	public static void updateMapEdges(MapLocation newMin, MapLocation newMax)
	{
		mapMin = new MapLocation(
					Math.max(mapMin.x, newMin.x),
					Math.max(mapMin.y, newMin.y)
				);
		mapMax = new MapLocation(
				Math.min(mapMax.x, newMax.x),
				Math.min(mapMax.y, newMax.y)
			);
	}
	
	// called by local units when checking for map edge info
	public static void updateMapEdge(Direction dir, int val, boolean sendUpdate)
	{
		
		// update the correct direction
		switch (dir)
		{
		case NORTH:
			if (val > mapMin.y)
			{
				mapMin = new MapLocation(mapMin.x,val);
				if (sendUpdate)
					newMapEdge = true;
			}
			break;
		case EAST:
			if (val < mapMax.x)
			{
				mapMax = new MapLocation(val,mapMax.y);
				if (sendUpdate)
					newMapEdge = true;
			}
			break;
		case WEST:
			if (val > mapMin.x)
			{
				mapMin = new MapLocation(val,mapMin.y);
				if (sendUpdate)
					newMapEdge = true;
			}
			break;
		case SOUTH:
			if (val < mapMax.y)
			{
				mapMax = new MapLocation(mapMax.x,val);
				if (sendUpdate)
					newMapEdge = true;
			}
			break;
		default:
			System.out.print("Invalid direction to updateMapEdge");
			break;
		}
	}

	
	// these are to be called by scouts/message to add a zombie den/part if it isn't in there already
	public static void updateZombieDens(MapLocation loc, boolean sendUpdate)
	{
		// if we already reported it, or we already have one queued to send,
		// don't do anything
		if (zombieDenLocations.contains(loc) || newZombieDen != null)
			return;
		
		zombieDenLocations.add(loc);
		
		// flag that we want to send this location
		if (sendUpdate)
			newZombieDen = loc;
	}
	
	public static void updateParts(MapLocation loc, boolean sendUpdate)
	{
		if (goodPartsLocations.contains(loc) || newParts != null)
			return;
		
		goodPartsLocations.add(loc);
		
		if (sendUpdate)
			newParts = loc;
	}
	
	public static void updateNeutralArchon(MapLocation loc, boolean sendUpdate)
	{
		if (neutralArchonLocations.contains(loc) || newNeutralArchon != null)
			return;
		
		neutralArchonLocations.add(loc);
		
//		if (!neutralArchonLocations.contains(getSymmetricLocation(loc)))
//			neutralArchonLocations.add(getSymmetricLocation(loc));
		
		if (sendUpdate)
			newNeutralArchon = loc;
	}
	
	public static void removeWaypoint(MapLocation loc)
	{
		// remove the part, and send that it got removed
		goodPartsLocations.remove(loc);
		zombieDenLocations.remove(loc);
		neutralArchonLocations.remove(loc);
	}
	
	public static void updateLocalWaypoints() throws GameActionException
	{
		MapLocation loc = getClosestPart();
		if (loc != null && rc.senseParts(loc) <= 0)
			goodPartsLocations.remove(loc);
		
		loc = getClosestDen();
		if (loc != null && rc.canSense(loc))
		{
			RobotInfo ri = rc.senseRobotAtLocation(loc);
			if (ri == null || ri.type != RobotType.ZOMBIEDEN)
				zombieDenLocations.remove(loc);
		}
	}
	
	// function to send updated info as a scout
	public static boolean doScoutSendUpdates() throws GameActionException
	{
		if (Micro.isInDanger() || rc.getCoreDelay() > 5)
			return false;
		
		// AK this function is not working - they don't recognize parts that are already on the list
		// keep messaging forever. Hack for now limit the updates to every 10 rounds.
		if (rc.getRoundNum()%10 != 0)
			return false;
		
		// only send one at a time
		if (newNeutralArchon != null)
		{
			Debug.setStringAK("sending new neutral archon signal");
			Message.sendMessageSignal(fullMapDistanceSq(), MessageType.NEUTRAL_ARCHON, newNeutralArchon);
			newNeutralArchon = null;
			return true;
		}
		if (newMapEdge)
		{
			Debug.setStringAK("sending new Map Edge Signal");
			Message.sendMessageSignal(fullMapDistanceSq(), MessageType.MAP_EDGE, mapMin, mapMax);
			newMapEdge = false;
			return true;
		}
		if (newZombieDen != null)
		{
			Debug.setStringAK("sending new Zombie Den signal");
			Message.sendMessageSignal(fullMapDistanceSq(), MessageType.ZOMBIE_DEN, newZombieDen);
			newZombieDen = null;
			return true;
		}
		if (newParts != null)
		{
			Debug.setStringAK("sending new Parts signal");
			Message.sendMessageSignal(fullMapDistanceSq(), MessageType.GOOD_PARTS, newParts);
			newParts = null;
			return true;
		}
		
		return false;
	}
	
	// function to look for nearby parts, neutrals, etc, can be used by scouts or archons
	public static void analyzeSurroundings() throws GameActionException
	{
		double visibleParts = 0;
		
		boolean isScout = (rc.getType() == RobotType.SCOUT);
		
		// neutral robot check
		RobotInfo[] neutralRobots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, Team.NEUTRAL);
		for (RobotInfo ri : neutralRobots)
		{
			updateParts(ri.location, isScout);
			visibleParts += ri.type.partCost;
			if (ri.type == RobotType.ARCHON)
			{
				updateNeutralArchon(ri.location, isScout);
			}
		}
		
		// zombie den check
		for (RobotInfo ri : Micro.getNearbyHostiles())
			if (ri.type == RobotType.ZOMBIEDEN)
				updateZombieDens(ri.location, isScout);
		
		// monkeypatch for now
		here = rc.getLocation();
		
		// loop through and see if we need to update anyone
		Direction d = Direction.NORTH;
		for (int j=0; j<4; j++)
		{
			int newval = 0;
			// loop until we hit a location on the map
			int i = isScout ? 7 : 5;
			for (; i>=1; i--)
			{
				MapLocation loc = here.add(d,i);
				if (rc.onTheMap(loc))
					break;
				// even means y, odd means x
				newval = ((j&1)==0)	? loc.y : loc.x;
			}
			if (newval > 0)
			{
				updateMapEdge(d, newval, true);
				break;
			}
			
			d = d.rotateRight().rotateRight();
		}
		
		MapLocation[] partsLocs = rc.sensePartLocations(rc.getType().sensorRadiusSquared);
		
		if (partsLocs.length > 0)
		{
			for (MapLocation loc : partsLocs)
			{
				updateParts(loc, isScout);
				visibleParts += rc.senseParts(loc);
			}
		}
		//System.out.println("Scanned " + nchecked + "/" + MapUtil.allOffsX.length + " locations");
	}
	
	public static void calculateSymmetry()
	{
		// create fastlocset without internal list
		FastLocSet archonLocs = new FastLocSet(false);

		MapLocation[] ourArchons = rc.getInitialArchonLocations(ourTeam);
		MapLocation[] theirArchons = rc.getInitialArchonLocations(theirTeam);
		
		numInitialArchons = ourArchons.length;
		
		if (numInitialArchons == 0)
			return;
		
		int xtot = 0;
		int ytot = 0;
		
		// first compute the center of the map
		// which is just the average of all of the archon locations
		for (int i=0; i<ourArchons.length; i++)
		{
			xtot += ourArchons[i].x;
			ytot += ourArchons[i].y;
			xtot += theirArchons[i].x;
			ytot += theirArchons[i].y;
			archonLocs.add(ourArchons[i]);
		}
		
		// this _might_ be like half a unit away from the actual center
		mapCenter = new MapLocation(xtot/(2*numInitialArchons),ytot/(2*numInitialArchons));
		// and set Message's map offsets
		Message.MAP_OFF_X = mapCenter.x - 128;
		Message.MAP_OFF_Y = mapCenter.y - 128;
		// and the map edge min and max
		mapMin = mapCenter.add(-GameConstants.MAP_MAX_WIDTH/2-1,-GameConstants.MAP_MAX_HEIGHT/2-1);
		mapMax = mapCenter.add(GameConstants.MAP_MAX_WIDTH/2+1,GameConstants.MAP_MAX_HEIGHT/2+1);
		
		// this, on the other hand, is exactly twice the center
		dblCenter = new MapLocation(xtot/numInitialArchons,ytot/numInitialArchons);
		
		// now go through and whittle down possible symmetries to what we hope is the real one
		for (MapLocation loc : theirArchons)
		{
			// calculate horizontal, vertical, and rotational symmetric locations
			MapLocation loc_horiz = new MapLocation(loc.x, dblCenter.y-loc.y);
			MapLocation loc_vert = new MapLocation(dblCenter.x-loc.x, loc.y);
			MapLocation loc_rot = new MapLocation(loc_vert.x,loc_horiz.y);
			
			if (!archonLocs.contains(loc_horiz))
				mapSymmetry &= ~(SYM_HFLIP);
			if (!archonLocs.contains(loc_vert))
				mapSymmetry &= ~(SYM_VFLIP);
			if (!archonLocs.contains(loc_rot))
				mapSymmetry &= ~(SYM_ROT);
		}
		
		// has more than one symmetry, so it doesn't really matter
		// and it's probably rotation because of the map editor
		if (Integer.bitCount(mapSymmetry) > 1)
			mapSymmetry = SYM_ROT;
	}
	
    public static MapLocation getSymmetricLocation(MapLocation loc) throws GameActionException
    {
    	MapLocation loc_sym = null;
    	
    	switch (mapSymmetry)
    	{
    	case SYM_ROT:
    		loc_sym = new MapLocation(dblCenter.x-loc.x,dblCenter.y-loc.y);
    		break;
    	case SYM_HFLIP:
    		loc_sym = new MapLocation(loc.x, dblCenter.y-loc.y);
    		break;
    	case SYM_VFLIP:
    		loc_sym = new MapLocation(dblCenter.x-loc.x, loc.y);
    		break;
		default:
			break;
    	}
    	
    	return loc_sym;
    }
}
