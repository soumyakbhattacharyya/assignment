package com.acme.model;

import com.acme.RouteBoxer;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Reading {

	private String id;
	private RouteBoxer.LatLng latlng;
	private Long timestamp;
}
