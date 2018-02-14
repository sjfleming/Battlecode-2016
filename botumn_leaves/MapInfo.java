package botumn_leaves;

import battlecode.common.*;

import java.util.*;

public class MapInfo extends RobotPlayer
{
	public static FastLocSet zombieDenLocations = new FastLocSet();
	public static FastLocSet goodPartsLocations = new FastLocSet();
	
	public static MapLocation mapMin = new MapLocation(-18000,-18000);
	public static MapLocation mapMax = new MapLocation(18001,18001);
	private static MapLocation nullLocation = null;
	
	// update the boundary on one of the map min/maxes
	public static boolean newMapEdge = false;
	
	// other stuff
	private static final int DEN_ADD = 1;
	private static final int DEN_SENT = 2;
	private static final int DEN_DEL = 3;
	
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
	
	public static MapLocation ourArchonCenter = null;
	public static MapLocation farthestArchonLoc = null;

	
	
	// the following functions are to be called mainly by Archons to set destinations
	
	// just do something random for now
	public static MapLocation getExplorationWaypoint()
	{
		// this one should get transmitted to a scout
		return new MapLocation(rand.nextInt(mapMax.x - mapMin.x) + mapMin.x,
							   rand.nextInt(mapMax.y - mapMin.y) + mapMin.y);
	}
	
	public static MapLocation getClosestPart()
	{
		return Micro.getClosestLocationTo(goodPartsLocations.elements(), farthestArchonLoc);
	}
	
	public static MapLocation getClosestDen()
	{
		return Micro.getClosestLocationTo(zombieDenLocations.elements(), farthestArchonLoc);
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
	public static void updateMapEdge(Direction dir, int val)
	{
		
		// update the correct direction
		switch (dir)
		{
		case NORTH:
			if (val > mapMin.y)
			{
				mapMin = new MapLocation(mapMin.x,val);
				newMapEdge = true;
			}
			break;
		case EAST:
			if (val < mapMax.x)
			{
				mapMax = new MapLocation(val,mapMax.y);
				newMapEdge = true;
			}
			break;
		case WEST:
			if (val > mapMin.x)
			{
				mapMin = new MapLocation(val,mapMin.y);
				newMapEdge = true;
			}
			break;
		case SOUTH:
			if (val < mapMax.y)
			{
				mapMax = new MapLocation(mapMax.x,val);
				newMapEdge = true;
			}
			break;
		default:
			System.out.print("Invalid direction to updateMapEdge");
			break;
		}
	}

	
	// these are to be called by message to add a zombie den/part if it isn't in there already
	public static void updateZombieDens(MapLocation add_loc, MapLocation del_loc)
	{
		// if we already reported it, or we already have one queued to send,
		// don't do anything
		if (!add_loc.equals(nullLocation))
			zombieDenLocations.add(add_loc, DEN_SENT);
		if (!del_loc.equals(nullLocation))
			zombieDenLocations.remove(del_loc);
	}
	/*
	public static void updateParts(MapLocation loc, boolean sendUpdate)
	{
		if (goodPartsLocations.contains(loc) || newParts != null)
			return;
		
		goodPartsLocations.add(loc);
		
		if (sendUpdate)
			newParts = loc;
	}
	*/
	
	// function to send updated info as a scout
	public static boolean doScoutSendUpdates() throws GameActionException
	{
		if (Micro.isInDanger() || rc.getCoreDelay() > 5)
			return false;
		
		// only send one at a time
		if (newMapEdge)
		{
			Message.sendMessageSignal(fullMapDistanceSq(), MessageType.MAP_EDGE, mapMin, mapMax);
			newMapEdge = false;
			return true;
		}
		
		// quickly loop through zombie dens, see if any need sendin'
		for (MapLocation loc : zombieDenLocations.elements())
		{
			int val = zombieDenLocations.get(loc);
			if (val == DEN_ADD)
			{
				// send that we have seen a new zombie den
				Message.sendMessageSignal(fullMapDistanceSq(), MessageType.ZOMBIE_DEN, loc, nullLocation);
				// and flag it as sent in the loc set
				zombieDenLocations.set(loc, DEN_SENT);
				// and don't do any more this round
				return true;
			}
			else if (val == DEN_DEL)
			{
				// send that we have unseen a zombie den
				Message.sendMessageSignal(fullMapDistanceSq(), MessageType.ZOMBIE_DEN, nullLocation, loc);
				// and actually remove it from the array
				zombieDenLocations.remove(loc);
				// and don't do any more this round
				return true;
			}
		}
		
		return false;
	}
	
	// function to quickly look around us and see what's new or what's changed
	public static void doAnalyzeSurroundings() throws GameActionException
	{
		double visibleParts = 0;
		
		// neutral robot check
		/*RobotInfo[] neutralRobots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, Team.NEUTRAL);
		for (RobotInfo ri : neutralRobots)
		{
			updateParts(ri.location, isScout);
			visibleParts += ri.type.partCost;
		}*/
		
		// zombie den check
		for (RobotInfo ri : Micro.getNearbyHostiles())
		{
			if (ri.type == RobotType.ZOMBIEDEN)
			{
				// if it's already in there, screw it
				if (zombieDenLocations.contains(ri.location))
					continue;
				
				// otherwise, let's add stuff
				zombieDenLocations.add(ri.location, DEN_ADD);
			}
		}
		
		// now check if we need to remove any
		MapLocation closestDen = getClosestDen();
		if (closestDen != null && rc.canSense(closestDen))
		{
			RobotInfo ri = rc.senseRobotAtLocation(closestDen);
			if (ri == null || ri.type != RobotType.ZOMBIEDEN)
			{
				// if we're a scout, flag it so we send the removal message
				// otherwise just straight up remove it
				if (rc.getType() == RobotType.SCOUT)
					zombieDenLocations.set(closestDen, DEN_DEL);
				else
					zombieDenLocations.remove(closestDen);
			}
		}
		
		// loop through and see if we need to update anyone
		Direction d = Direction.NORTH;
		for (int j=0; j<4; j++)
		{
			int newval = 0;
			// loop until we hit a location on the map
			// sight range start point gets set by our class
			int i = 4;
			if (rc.getType() == RobotType.SCOUT)
				i = 7;
			else if (rc.getType() == RobotType.ARCHON)
				i = 5;
			
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
				updateMapEdge(d, newval);
				break;
			}
			
			d = d.rotateRight().rotateRight();
		}
		
		/*
		MapLocation[] partsLocs = rc.sensePartLocations(rc.getType().sensorRadiusSquared);
		
		if (partsLocs.length > 0)
		{
			for (MapLocation loc : partsLocs)
			{
				updateParts(loc, isScout);
				visibleParts += rc.senseParts(loc);
			}
		}*/
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
		
		int ourx = 0;
		int oury = 0;
		int theirx = 0;
		int theiry = 0;
		
		// first compute the center of the map
		// which is just the average of all of the archon locations
		for (int i=0; i<ourArchons.length; i++)
		{
			theirx += theirArchons[i].x;
			theiry += theirArchons[i].y;
			ourx += ourArchons[i].x;
			oury += ourArchons[i].y;
			archonLocs.add(ourArchons[i]);
		}
		
		int xtot = ourx+theirx;
		int ytot = oury+theiry;
		
		// this _might_ be like half a unit away from the actual center
		mapCenter = new MapLocation(xtot/(2*numInitialArchons),ytot/(2*numInitialArchons));
		ourArchonCenter = new MapLocation(ourx/numInitialArchons,oury/numInitialArchons);
		farthestArchonLoc = Micro.getFarthestLocationFrom(ourArchons, mapCenter);
		// and set Message's map offsets
		Message.MAP_OFF_X = mapCenter.x - 128;
		Message.MAP_OFF_Y = mapCenter.y - 128;
		// and the map edge min and max
		mapMin = mapCenter.add(-GameConstants.MAP_MAX_WIDTH/2-1,-GameConstants.MAP_MAX_HEIGHT/2-1);
		mapMax = mapCenter.add(GameConstants.MAP_MAX_WIDTH/2+1,GameConstants.MAP_MAX_HEIGHT/2+1);
		
		// add a few to the max location to signify "invalid location"
		nullLocation = mapMax.add(5,5);
		
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
