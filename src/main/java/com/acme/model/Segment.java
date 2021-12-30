package com.acme.model;

import java.util.ArrayList;
import java.util.List;

import com.acme.RouteBoxer;
import com.acme.RouteBoxer.LatLng;
import com.acme.RouteBoxer.LatLngBounds;

import lombok.Data;

@Data
public class Segment {

	private String id;
	private RouteBoxer.LatLng startLatlng;
	private RouteBoxer.LatLng endLatlng;
	
	// range is in kms
	private double range;

	private List<LatLngBounds> boundingBoxes;

	public Segment(String id, LatLng startLatlng, LatLng endLatlng, double range) {
		
		RouteBoxer routeBoxer = new RouteBoxer();
		
		this.id = id;
		this.startLatlng = startLatlng;
		this.endLatlng = endLatlng;
		this.range = range;
		
		RouteBoxer.LatLng l1 = startLatlng;
		RouteBoxer.LatLng l2 = endLatlng;
		
		List<RouteBoxer.LatLng> path = new ArrayList<>();
		path.add(l1);
		path.add(l2);
		
		this.boundingBoxes = routeBoxer.box(path, this.range);		
	}

}
