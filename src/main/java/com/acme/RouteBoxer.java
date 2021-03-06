package com.acme;

// credit : https://raw.githubusercontent.com/PsyGik/routeboxer-java/master/RouteBoxer.java

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteBoxer {
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final int EarthRadiusKm = 6371;

	/**
	 * @author Cedric NICOLAS
	 * see wedrive.mobi for the service using this lib.
	 * Utility minimal LatLng class for the need of route boxer 
	 *
	 */
	public class LatLng implements Cloneable {
		public double lat;
		public double lng;
		public LatLng() {
			lat=0;
			lng=0;
		}
		public LatLng(double lat2, double lng2) {
			lat=lat2;
			lng=lng2;
		}

		public double lat() {
			return lat;
		}

		public double lng() {
			return lng;
		}

		public double latRad() {
			return toRad(lat);
		}

		public double lngRad() {
			return toRad(lng);
		}


		/**
		 * A ‘rhumb line’ (or loxodrome) is a path of constant bearing, which crosses all meridians at the same angle.
		 * see http://www.movable-type.co.uk/scripts/latlong.html
		 * @param brng
		 * @param dist
		 * @return
		 */
		public LatLng rhumbDestinationPoint(double brng, double dist) {
			double R = 6378137;
			double d = dist/R;  // d = angular distance covered on earth’s surface
			double lat1 = latRad(), lon1 = lngRad();
			brng = toRad(brng);

			double dLat = d*Math.cos(brng);
			// nasty kludge to overcome ill-conditioned results around parallels of latitude:
			if (Math.abs(dLat) < 1e-10) dLat = 0; // dLat < 1 mm

			double lat2 = lat1 + dLat;
			double dPhi = Math.log(Math.tan(lat2/2+Math.PI/4)/Math.tan(lat1/2+Math.PI/4));
			double q = (dPhi!=0) ? dLat/dPhi : Math.cos(lat1);  // E-W line gives dPhi=0
			double dLon = d*Math.sin(brng)/q;

			// check for some daft bugger going past the pole, normalise latitude if so
			if (Math.abs(lat2) > Math.PI/2) lat2 = lat2>0 ? Math.PI-lat2 : -Math.PI-lat2;

			double lon2 = (lon1+dLon+3*Math.PI)%(2*Math.PI) - Math.PI;

			return new LatLng(toDeg(lat2), toDeg(lon2));
		};

		/**
		 * Given a start point and a distance d along constant bearing θ, this will calculate the destination point. 
		 * If you maintain a constant bearing along a rhumb line, you will gradually spiral in towards one of the poles.
		 * see http://www.movable-type.co.uk/scripts/latlong.html
		 * @param dest
		 * @return
		 */
		public double rhumbBearingTo(LatLng dest) {
			double dLon = toRad(dest.lng() - this.lng());
			double dPhi = Math.log(Math.tan(dest.latRad() / 2 + Math.PI / 4) / Math.tan(this.latRad() / 2 + Math.PI / 4));
			if (Math.abs(dLon) > Math.PI) {
				dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
			}
			return toBrng(Math.atan2(dLon, dPhi));
		};
		
		public String toString() {
			return formatLatOrLong(lat)+","+formatLatOrLong(lng);
		}
		
		private String formatLatOrLong(double latOrLng) {
			NumberFormat nf;
			DecimalFormatSymbols fts = new DecimalFormatSymbols(Locale.US);
			nf = new DecimalFormat("##0.000000",fts);
			return nf.format(latOrLng);
		}
		
		public JSONArray toJSONArray() {
			JSONArray latLngJSON = new JSONArray();
			try {
				latLngJSON.put(0, Double.toString(lat));
				latLngJSON.put(1, Double.toString(lng));
			} catch (JSONException e) {
				
			}
			return latLngJSON;
		}

		public LatLng clone() {
			return new LatLng(lat,lng);
		}
		
		public double distanceFrom(LatLng otherLatLng) {
				double b = lat() * Math.PI / 180.;
				double c = otherLatLng.lat() * Math.PI / 180.;
				double d = b - c;
				double e = lng() * Math.PI / 180. - otherLatLng.lng() * Math.PI / 180.;

				double f = 2. * Math.asin(Math.sqrt(Math.pow(Math.sin(d / 2.), 2.) + Math.cos(b) * Math.cos(c)
						* Math.pow(Math.sin(e / 2.), 2.)));
				return f * 6378137.;
		}
	}

	/**
	 * minimal LatLngBounds utility class for RouteBoxer needs
	 * i do not guarantee the extend method in southern hemisphere
	 *
	 */
	public class LatLngBounds {
		private LatLng southwest, northeast;

		public LatLngBounds() {
		}

		public LatLngBounds(final LatLng southwest, final LatLng northeast) {
			this.southwest = southwest;
			this.northeast = northeast;
		}

		public LatLng getSouthWest() {
			return southwest;
		}

		public void setSouthWest(LatLng southwest) {
			this.southwest = southwest;
		}

		public LatLng getNorthEast() {
			return northeast;
		}

		public void setNorthEast(LatLng northeast) {
			this.northeast = northeast;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			LatLngBounds that = (LatLngBounds) o;

			if (northeast != null ? !northeast.equals(that.northeast) : that.northeast != null) return false;
			if (southwest != null ? !southwest.equals(that.southwest) : that.southwest != null) return false;

			return true;
		}

		public void extend(LatLng latLng) {
			if (southwest==null) {
				southwest=latLng.clone();
				if (northeast==null) northeast=latLng.clone();
				return;
			}
			if (northeast==null) {
				northeast=latLng.clone();
				return;
			}

			if (latLng.lat<southwest.lat) 
				southwest.lat=latLng.lat;
			else if (latLng.lat>northeast.lat) 
				northeast.lat=latLng.lat;
			if (latLng.lng<southwest.lng) 
				southwest.lng=latLng.lng;
			else if (latLng.lng>northeast.lng) 
				northeast.lng=latLng.lng;
		}

		public boolean contains(LatLng latLng) {
			if (southwest==null || northeast==null) return false;
			if (latLng.lat<southwest.lat) return false;
			else if (latLng.lat>northeast.lat) return false;
			else if (latLng.lng<southwest.lng) return false;
			else if (latLng.lng>northeast.lng) return false;
			return true;
		}

		public LatLng getCenter() {
			return new LatLng(southwest.lat+(northeast.lat-southwest.lat)/2,southwest.lng+(northeast.lng-southwest.lng)/2);
		}

		@Override
		public int hashCode() {
			int result = southwest != null ? southwest.hashCode() : 0;
			result = 31 * result + (northeast != null ? northeast.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return northeast.toString() + '|' + southwest.toString();
		}
	}


	private int[][] grid_;
	private List<Double> latGrid_;
	private List<Double> lngGrid_;
	private List<LatLngBounds> boxesX_;
	private List<LatLngBounds> boxesY_;
	/**
	 * Creates a new RouteBoxer
	 *
	 * @constructor
	 */
	public RouteBoxer() {

	}

	/**
	 * utility method to get a LatLng list from an encoded google polyline 
	 * You need to pass to box() method below such a long list of LatLngs in order to have it works well
	 * @param encodedPoints
	 * @return
	 */
	public List<LatLng> decodePath(String encodedPoints){
		ArrayList<LatLng> poly = new ArrayList<LatLng>();
		int index = 0, len = encodedPoints.length();
		int lat = 0, lng = 0;
		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encodedPoints.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;
			shift = 0;
			result = 0;
			do {
				b = encodedPoints.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;
			LatLng p = new LatLng((((double) lat / 1E5)),
					(((double) lng / 1E5)));
			poly.add(p);
		}
		return poly;

	}

	/**
	 * Generates boxes for a given route and distance
	 *
	 * @param {LatLng[] } path The path along
	 *           which to create boxes. The path object must be either an Array of
	 *           LatLng objects
	 * @param {Number} range The distance in kms around the route that the generated
	 *           boxes must cover.
	 * @return {LatLngBounds[]} An arrayList of boxes that covers the whole
	 *           path.
	 */
	public List<LatLngBounds> box(List<LatLng> path, double range) {
		// Two dimensional array representing the cells in the grid overlaid on the path
		this.grid_ = null;

		// Array that holds the latitude coordinate of each vertical grid line
		this.latGrid_ =  new ArrayList<Double>();

		// Array that holds the longitude coordinate of each horizontal grid line  
		this.lngGrid_ =  new ArrayList<Double>();

		// Array of bounds that cover the whole route formed by merging cells that
		//  the route intersects first horizontally, and then vertically
		this.boxesX_ =  new ArrayList<LatLngBounds>();

		// Array of bounds that cover the whole route formed by merging cells that
		//  the route intersects first vertically, and then horizontally
		this.boxesY_ =  new ArrayList<LatLngBounds>();

		// The array of LatLngs representing the vertices of the path
		List<LatLng> vertices = null;

		vertices = path;


		// Build the grid that is overlaid on the route
		this.buildGrid_(vertices, range);

		// Identify the grid cells that the route intersects
		this.findIntersectingCells_(vertices);

		// Merge adjacent intersected grid cells (and their neighbours) into two sets
		//  of bounds, both of which cover them completely
		this.mergeIntersectingCells_();

		// Return the set of merged bounds that has the fewest elements
		return (this.boxesX_.size() <= this.boxesY_.size() ?
				this.boxesX_ :
					this.boxesY_);
	};

	/**
	 * Generates boxes for a given route and distance
	 *
	 * @param {LatLng[]} vertices The vertices of the path over which to lay the grid
	 * @param {Number} range The spacing of the grid cells.
	 */
	private void buildGrid_(List<LatLng> vertices, double range) {

		// Create a LatLngBounds object that contains the whole path
		LatLngBounds routeBounds = new LatLngBounds();
		//logger.trace("vertices[0]"+vertices.get(0).toString()+" vertices["+(vertices.size()-1)+"] "+vertices.get(vertices.size()-1).toString());
		for (int i = 0; i < vertices.size(); i++) {
			routeBounds.extend(vertices.get(i));
		}
		//logger.trace("routeBounds "+routeBounds.toString());
		// Find the center of the bounding box of the path
		LatLng routeBoundsCenter = routeBounds.getCenter();
		//logger.trace("routeBoundsCenter "+routeBoundsCenter.toString());
		// Starting from the center define grid lines outwards vertically until they
		//  extend beyond the edge of the bounding box by more than one cell
		this.latGrid_.add(routeBoundsCenter.lat());
		LatLng rhumb = routeBoundsCenter.rhumbDestinationPoint(0, range);
		//logger.trace("rhumb 1 "+rhumb.toString());
		// Add lines from the center out to the north
		this.latGrid_.add(rhumb.lat());
		for (int i = 2; this.latGrid_.get(i - 2) < routeBounds.getNorthEast().lat(); i++) {
			this.latGrid_.add(routeBoundsCenter.rhumbDestinationPoint(0, range * i).lat());
			
		}
		//logger.trace("pass1 latGrid size"+latGrid_.size());
		// Add lines from the center out to the south  
		for (int i1 = 1; this.latGrid_.get(1) > routeBounds.getSouthWest().lat(); i1++) {
			this.latGrid_.add(0,routeBoundsCenter.rhumbDestinationPoint(180, range * i1).lat());
		}
		//logger.trace("pass2 latGrid size"+latGrid_.size());
		// Starting from the center define grid lines outwards horizontally until they
		//  extend beyond the edge of the bounding box by more than one cell  
		this.lngGrid_.add(routeBoundsCenter.lng());

		// Add lines from the center out to the east
		this.lngGrid_.add(routeBoundsCenter.rhumbDestinationPoint(90, range).lng());
		for (int i2 = 2; this.lngGrid_.get(i2 - 2) < routeBounds.getNorthEast().lng(); i2++) {
			this.lngGrid_.add(routeBoundsCenter.rhumbDestinationPoint(90, range * i2).lng());
		}
		//logger.trace("pass1 lngGrid_ size"+lngGrid_.size());
		// Add lines from the center out to the west
		for (int i3 = 1; this.lngGrid_.get(1) > routeBounds.getSouthWest().lng(); i3++) {
			this.lngGrid_.add(0,routeBoundsCenter.rhumbDestinationPoint(270, range * i3).lng());
		}
		//logger.trace("pass2 lngGrid_ size"+lngGrid_.size());

		// Create a two dimensional array representing this grid
		this.grid_ = new int[this.lngGrid_.size()][this.latGrid_.size()];
	};

	/**
	 * Find all of the cells in the overlaid grid that the path intersects
	 *
	 * @param {LatLng[]} vertices The vertices of the path
	 */
	private void findIntersectingCells_(List<LatLng> vertices) {
		// Find the cell where the path begins
		int[] hintXY = this.getCellCoords_(vertices.get(0));

		// Mark that cell and it's neighbours for inclusion in the boxes
		this.markCell_(hintXY);

		// Work through each vertex on the path identifying which grid cell it is in
		for (int i = 1; i < vertices.size(); i++) {
			//logger.trace("findIntersectingCells_ i "+i);
			// Use the known cell of the previous vertex to help find the cell of this vertex
			int[] gridXY = this.getGridCoordsFromHint_(vertices.get(i), vertices.get(i - 1), hintXY);
			//logger.trace("findIntersectingCells_ gridXY "+gridXY[0]+" "+gridXY[1]);
			if (gridXY[0] == hintXY[0] && gridXY[1] == hintXY[1]) {
				// This vertex is in the same cell as the previous vertex
				// The cell will already have been marked for inclusion in the boxes
				continue;

			} else if ((Math.abs(hintXY[0] - gridXY[0]) == 1 && hintXY[1] == gridXY[1]) ||
					(hintXY[0] == gridXY[0] && Math.abs(hintXY[1] - gridXY[1]) == 1)) {
				// This vertex is in a cell that shares an edge with the previous cell
				// Mark this cell and it's neighbours for inclusion in the boxes
				this.markCell_(gridXY);

			} else {
				// This vertex is in a cell that does not share an edge with the previous
				//  cell. This means that the path passes through other cells between
				//  this vertex and the previous vertex, and we must determine which cells
				//  it passes through
				this.getGridIntersects_(vertices.get(i - 1), vertices.get(i), hintXY, gridXY);
			}

			// Use this cell to find and compare with the next one
			hintXY = gridXY;
		}
	};

	/**
	 * Find the cell a path vertex is in by brute force iteration over the grid
	 *
	 * @param {LatLng[]} latlng The latlng of the vertex
	 * @return {Number[][]} The cell coordinates of this vertex in the grid
	 */ 
	private int[] getCellCoords_(LatLng latlng) {
		int x,y;
		for (x = 0; this.lngGrid_.get(x) < latlng.lng(); x++) {}
		for (y = 0; this.latGrid_.get(y) < latlng.lat(); y++) {}
		int[] result={x - 1, y - 1};
		return result;
	};

	/**
	 * Find the cell a path vertex is in based on the known location of a nearby
	 *  vertex. This saves searching the whole grid when working through vertices
	 *  on the polyline that are likely to be in close proximity to each other.
	 *
	 * @param {LatLng[]} latlng The latlng of the vertex to locate in the grid
	 * @param {LatLng[]} hintlatlng The latlng of the vertex with a known location
	 * @param {Number[]} hint The cell containing the vertex with a known location
	 * @return {Number[]} The cell coordinates of the vertex to locate in the grid
	 */ 
	private int[] getGridCoordsFromHint_(LatLng latlng, LatLng  hintlatlng,int[] hint) {
		int x=0, y=0;
		try {
			if (latlng.lng() > hintlatlng.lng()) {
				for (x = hint[0]; this.lngGrid_.get(x + 1) < latlng.lng(); x++) {}
			} else {
				for (x = hint[0]; this.lngGrid_.get(x) > latlng.lng(); x--) {}
			}

			if (latlng.lat() > hintlatlng.lat()) {
				for (y = hint[1]; this.latGrid_.get(y + 1) < latlng.lat(); y++) {}
			} else {        
				for (y = hint[1]; this.latGrid_.get(y) > latlng.lat(); y--) {}
			}
		} catch (IndexOutOfBoundsException e) {
			logger.warn("getGridCoordsFromHint_ IndexOutOfBoundsException x"+x+" y "+y);
		}
		int[] result = {x, y};
		return result;
	};


	/**
	 * Identify the grid squares that a path segment between two vertices
	 * intersects with by:
	 * 1. Finding the bearing between the start and end of the segment
	 * 2. Using the delta between the lat of the start and the lat of each
	 *    latGrid boundary to find the distance to each latGrid boundary
	 * 3. Finding the lng of the intersection of the line with each latGrid
	 *     boundary using the distance to the intersection and bearing of the line
	 * 4. Determining the x-coord on the grid of the point of intersection
	 * 5. Filling in all squares between the x-coord of the previous intersection
	 *     (or start) and the current one (or end) at the current y coordinate,
	 *     which is known for the grid line being intersected
	 *     
	 * @param {LatLng} start The latlng of the vertex at the start of the segment
	 * @param {LatLng} end The latlng of the vertex at the end of the segment
	 * @param {Number[]} startXY The cell containing the start vertex
	 * @param {Number[]} endXY The cell containing the vend vertex
	 */ 
	private void getGridIntersects_(LatLng start, LatLng end, int[] startXY, int [] endXY) {
		LatLng edgePoint;
		int[] edgeXY;
		int i;
		double brng = start.rhumbBearingTo(end);         // Step 1.

		LatLng hint = start;
		int[] hintXY = startXY;

		// Handle a line segment that travels south first
		if (end.lat() > start.lat()) {
			// Iterate over the east to west grid lines between the start and end cells
			for (i = startXY[1] + 1; i <= endXY[1]; i++) {
				// Find the latlng of the point where the path segment intersects with
				//  this grid line (Step 2 & 3)
				edgePoint = this.getGridIntersect_(start, brng, this.latGrid_.get(i));

				// Find the cell containing this intersect point (Step 4)
				edgeXY = this.getGridCoordsFromHint_(edgePoint, hint, hintXY);

				// Mark every cell the path has crossed between this grid and the start,
				//   or the previous east to west grid line it crossed (Step 5)
				this.fillInGridSquares_(hintXY[0], edgeXY[0], i - 1);

				// Use the point where it crossed this grid line as the reference for the
				//  next iteration
				hint = edgePoint;
				hintXY = edgeXY;
			}

			// Mark every cell the path has crossed between the last east to west grid
			//  line it crossed and the end (Step 5)
			this.fillInGridSquares_(hintXY[0], endXY[0], i - 1);

		} else {
			// Iterate over the east to west grid lines between the start and end cells
			for (i = startXY[1]; i > endXY[1]; i--) {
				// Find the latlng of the point where the path segment intersects with
				//  this grid line (Step 2 & 3)
				edgePoint = this.getGridIntersect_(start, brng, this.latGrid_.get(i));

				// Find the cell containing this intersect point (Step 4)
				edgeXY = this.getGridCoordsFromHint_(edgePoint, hint, hintXY);

				// Mark every cell the path has crossed between this grid and the start,
				//   or the previous east to west grid line it crossed (Step 5)
				this.fillInGridSquares_(hintXY[0], edgeXY[0], i);

				// Use the point where it crossed this grid line as the reference for the
				//  next iteration
				hint = edgePoint;
				hintXY = edgeXY;
			}

			// Mark every cell the path has crossed between the last east to west grid
			//  line it crossed and the end (Step 5)
			this.fillInGridSquares_(hintXY[0], endXY[0], i);

		}
	};

	/**
	 * Find the latlng at which a path segment intersects with a given
	 *   line of latitude
	 *     
	 * @param {LatLng} start The vertex at the start of the path segment
	 * @param {Number} brng The bearing of the line from start to end
	 * @param {Number} gridLineLat The latitude of the grid line being intersected
	 * @return {LatLng} The latlng of the point where the path segment intersects
	 *                    the grid line
	 */ 
	private LatLng getGridIntersect_(LatLng start, double brng, double gridLineLat) {
		double d = EarthRadiusKm * ((toRad(gridLineLat) - start.latRad()) / Math.cos(toRad(brng)));
		return start.rhumbDestinationPoint(brng, d);
	};

	/**
	 * Mark all cells in a given row of the grid that lie between two columns
	 *   for inclusion in the boxes
	 *     
	 * @param {Number} startx The first column to include
	 * @param {Number} endx The last column to include
	 * @param {Number} y The row of the cells to include
	 */ 
	private void fillInGridSquares_(int startx, int endx, int y) {
		//logger.trace("fillInGridSquares_ startx"+startx+" endx "+endx+" y "+y);
		int x;
		if (startx < endx) {
			for (x = startx; x <= endx; x++) {
				int [] cell = {x,y};
				this.markCell_(cell);
			}
		} else {
			for (x = startx; x >= endx; x--) {
				int [] cell = {x,y};
				this.markCell_(cell);
			}            
		}      
	};

	/**
	 * Mark a cell and the 8 immediate neighbours for inclusion in the boxes
	 *     
	 * @param {Number[]} square The cell to mark
	 */ 
	private void markCell_(int[] cell) {
		int x = cell[0];
		int y = cell[1];
		try {
		//logger.trace("markCell x"+x+" y "+y);
		this.grid_[x - 1][y - 1] = 1;
		this.grid_[x][y - 1] = 1;
		this.grid_[x + 1][y - 1] = 1;
		this.grid_[x - 1][y] = 1;
		this.grid_[x][y] = 1;
		this.grid_[x + 1][y] = 1;
		this.grid_[x - 1][y + 1] = 1;
		this.grid_[x][y + 1] = 1;
		this.grid_[x + 1][y + 1] = 1;
		} catch (IndexOutOfBoundsException e) {
			logger.warn("markCell_ IndexOutOfBoundsException x"+x+" y "+y);
		}
	};

	/**
	 * Create two sets of bounding boxes, both of which cover all of the cells that
	 *   have been marked for inclusion.
	 *
	 * The first set is created by combining adjacent cells in the same column into
	 *   a set of vertical rectangular boxes, and then combining boxes of the same
	 *   height that are adjacent horizontally.
	 *
	 * The second set is created by combining adjacent cells in the same row into
	 *   a set of horizontal rectangular boxes, and then combining boxes of the same
	 *   width that are adjacent vertically.
	 *     
	 */ 
	public void mergeIntersectingCells_() {
		int x, y;
		LatLngBounds box;

		// The box we are currently expanding with new cells
		LatLngBounds currentBox = null;

		// Traverse the grid a row at a time
		for (y = 0; y < this.grid_[0].length; y++) {
			for (x = 0; x < this.grid_.length; x++) {

				if (this.grid_[x][y]==1) {
					// This cell is marked for inclusion. If the previous cell in this
					//   row was also marked for inclusion, merge this cell into it's box.
					// Otherwise start a new box.
					int[] cell = {x, y};
					box = this.getCellBounds_(cell);
					if (currentBox!=null) {
						currentBox.extend(box.getNorthEast());
					} else {
						currentBox = box;
					}

				} else {
					// This cell is not marked for inclusion. If the previous cell was
					//  marked for inclusion, merge it's box with a box that spans the same
					//  columns from the row below if possible.
					this.mergeBoxesY_(currentBox);
					currentBox = null;
				}
			}
			// If the last cell was marked for inclusion, merge it's box with a matching
			//  box from the row below if possible.
			this.mergeBoxesY_(currentBox);
			currentBox = null;
		}

		// Traverse the grid a column at a time
		for (x = 0; x < this.grid_.length; x++) {
			for (y = 0; y < this.grid_[0].length; y++) {
				if (this.grid_[x][y]==1) {

					// This cell is marked for inclusion. If the previous cell in this
					//   column was also marked for inclusion, merge this cell into it's box.
					// Otherwise start a new box.
					int[] cell = {x, y};
					if (currentBox!=null) {

						box = this.getCellBounds_(cell);
						currentBox.extend(box.getNorthEast());
					} else {
						currentBox = this.getCellBounds_(cell);
					}

				} else {
					// This cell is not marked for inclusion. If the previous cell was
					//  marked for inclusion, merge it's box with a box that spans the same
					//  rows from the column to the left if possible.
					this.mergeBoxesX_(currentBox);
					currentBox = null;

				}
			}
			// If the last cell was marked for inclusion, merge it's box with a matching
			//  box from the column to the left if possible.
			this.mergeBoxesX_(currentBox);
			currentBox = null;
		}
	};

	/**
	 * Search for an existing box in an adjacent row to the given box that spans the
	 * same set of columns and if one is found merge the given box into it. If one
	 * is not found, append this box to the list of existing boxes.
	 *
	 * @param {LatLngBounds}  The box to merge
	 */ 
	public void mergeBoxesX_ (LatLngBounds box) {
		if (box != null) {
			for (int i = 0; i < this.boxesX_.size(); i++) {
				if (Math.abs(this.boxesX_.get(i).getNorthEast().lng()- box.getSouthWest().lng())<0.001 &&
						Math.abs(this.boxesX_.get(i).getSouthWest().lat() -box.getSouthWest().lat())<0.001 &&
						Math.abs(this.boxesX_.get(i).getNorthEast().lat() -box.getNorthEast().lat())<0.001) {
					this.boxesX_.get(i).extend(box.getNorthEast());
					return;
				}
			}
			this.boxesX_.add(box);
		}
	};

	/**
	 * Search for an existing box in an adjacent column to the given box that spans
	 * the same set of rows and if one is found merge the given box into it. If one
	 * is not found, append this box to the list of existing boxes.
	 *
	 * @param {LatLngBounds}  The box to merge
	 */ 
	public void mergeBoxesY_(LatLngBounds box) {
		if (box != null) {
			for (int i = 0; i < this.boxesY_.size(); i++) {
				if (Math.abs(this.boxesY_.get(i).getNorthEast().lat() - box.getSouthWest().lat())<0.001 &&
						Math.abs(this.boxesY_.get(i).getSouthWest().lng() - box.getSouthWest().lng())<0.001 &&
						Math.abs(this.boxesY_.get(i).getNorthEast().lng() - box.getNorthEast().lng())<0.001) {
					this.boxesY_.get(i).extend(box.getNorthEast());
					return;
				}
			}
			this.boxesY_.add(box);
		}
	};

	/**
	 * Obtain the LatLng of the origin of a cell on the grid
	 *
	 * @param {Number[]} cell The cell to lookup.
	 * @return {LatLng} The latlng of the origin of the cell.
	 */ 
	LatLngBounds getCellBounds_(int[] cell) {
		return new LatLngBounds(
				new LatLng(this.latGrid_.get(cell[1]), this.lngGrid_.get(cell[0])),
				new LatLng(this.latGrid_.get(cell[1]+1), this.lngGrid_.get(cell[0]+1)));
	};



	/**
	 * Extend the Number object to convert degrees to radians
	 *
	 * @return {Number} Bearing in radians
	 * @ignore
	 */ 
	public double toRad(double value) {
		return value * Math.PI / 180;
	};

	/**
	 * Extend the Number object to convert radians to degrees
	 *
	 * @return {Number} Bearing in degrees
	 * @ignore
	 */ 
	public double toDeg(double value) {
		return value * 180 / Math.PI;
	};

	/**
	 * Normalize a heading in degrees to between 0 and +360
	 *
	 * @return {Number} Return 
	 * @ignore
	 */ 
	public double toBrng(double value) {
		return (toDeg(value) + 360) % 360;
	};
}