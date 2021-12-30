package com.acme.model;

import java.util.UUID;

import com.acme.RouteBoxer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Device {

	private String id;

	public Reading emitReading() {
		return new Reading(UUID.randomUUID().toString()
				         , new RouteBoxer().new LatLng(50.5, 30.55)
				         , System.currentTimeMillis());
	}
}
