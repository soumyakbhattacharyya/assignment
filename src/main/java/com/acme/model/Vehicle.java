package com.acme.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Vehicle {

	private String id;
	private String regisrtationNumber;
	private String driverId;

	private Device device;

	

}
